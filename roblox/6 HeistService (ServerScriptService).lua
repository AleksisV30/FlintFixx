-- Runs one heist on the private server the lobby sent a crew to: loads the map, waits for
-- the crew, deals the secret roles, runs the job (BankJob) and the clock, then sends
-- everyone back to the lobby.
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ServerStorage = game:GetService("ServerStorage")
local TeleportService = game:GetService("TeleportService")
local RunService = game:GetService("RunService")
local Lighting = game:GetService("Lighting")

local GameMode = require(ReplicatedStorage:WaitForChild("GameMode"))
-- In a Studio lobby this waits for the host to start, then runs the heist right here
if not GameMode.waitForHeist() then
	return
end

local Config = require(ReplicatedStorage:WaitForChild("LobbyConfig"))
local BankJob = require(script:WaitForChild("BankJob"))
local Photos = require(script:WaitForChild("Photos"))
local Huddle = require(script:WaitForChild("Huddle"))
local Rewards = require(script:WaitForChild("Rewards"))
local MapData = require(script:WaitForChild("MapData"))
local ProximityPromptService = game:GetService("ProximityPromptService")
-- The same saved progress the lobby uses (LobbyService itself does nothing on heist servers)
local PlayerData = require(game:GetService("ServerScriptService"):WaitForChild("LobbyService"):WaitForChild("PlayerData"))

local CREW_WAIT_SECONDS = 25 -- players arrive from the lobby one by one
local STUDIO_WAIT_SECONDS = 3
local REVEAL_SECONDS = 13 -- role deal (~5s) + time to read the role card, before the clock starts
local END_SECONDS = 30 -- end screen (cop reveal, replay, rewards, play again?) before the lobby
local RESPAWN_SECONDS = 3

local remotes = Instance.new("Folder")
remotes.Name = "HeistRemotes"
local HeistUpdate = Instance.new("RemoteEvent")
HeistUpdate.Name = "HeistUpdate"
HeistUpdate.Parent = remotes
local HeistRequest = Instance.new("RemoteFunction")
HeistRequest.Name = "HeistRequest"
HeistRequest.Parent = remotes
remotes.Parent = ReplicatedStorage

-- Characters only spawn once the map is in place
Players.CharacterAutoLoads = false
for _, name in ipairs({ "Baseplate", "SpawnLocation" }) do
	local lobbyPart = workspace:FindFirstChild(name)
	if lobbyPart then
		lobbyPart:Destroy()
	end
end

local state = { phase = "Waiting", heat = 0, endsAt = nil, result = nil, objectives = {} }
local roleInfo = {} -- [Player] = what that player is allowed to know about the roles
local cops = {} -- [Player] = true
local settings
local mapName
local mapLoaded = false
local job = nil
local photos = nil
local huddle = nil
local mapData = nil -- floor plan + markers for the minimap
local heistStartedAt = nil
local pausedAt = nil -- set while a huddle has the clock stopped
local pausedTotal = 0

-- Seconds of heist so far, not counting huddles
local function elapsed()
	if not heistStartedAt then
		return 0
	end
	local now = os.clock()
	return now - heistStartedAt - pausedTotal - (pausedAt and now - pausedAt or 0)
end

local function formatClock(seconds)
	seconds = math.max(0, math.floor(seconds))
	return ("%02d:%02d"):format(seconds // 60, seconds % 60)
end

local function broadcastState()
	HeistUpdate:FireAllClients("State", state)
end

-- Lots of small changes (Heat ticking up, drill progress) get bundled into one update
local broadcastQueued = false
local function queueBroadcast()
	if broadcastQueued then
		return
	end
	broadcastQueued = true
	task.delay(0.2, function()
		broadcastQueued = false
		broadcastState()
	end)
end

local function notifyAll(message)
	HeistUpdate:FireAllClients("Notice", message)
end

---------------------------------------------------------------- setup

-- Teleport data travels through the client, so every value is checked before use
local function readSettings(data)
	local raw = type(data) == "table" and type(data.settings) == "table" and data.settings or {}
	local clean = {
		copCount = type(raw.copCount) == "number" and math.floor(raw.copCount) or Config.DEFAULT_COPS,
		difficulty = table.find(Config.DIFFICULTIES, raw.difficulty) and raw.difficulty or "Normal",
		length = table.find(Config.LENGTHS, raw.length) and raw.length or "Normal",
		copsKnowEachOther = raw.copsKnowEachOther ~= false,
	}
	local map = type(data) == "table" and Config.getMap(data.map)
	local chosen = map and map.available and map.name or Config.availableMaps()[1].name
	return clean, chosen
end

-- A cold blue night outside, so the bank's warm lamps stand out (and the blackout bites)
local function setNight()
	Lighting.ClockTime = 0.5
	Lighting.Brightness = 1.2
	Lighting.Ambient = Color3.fromRGB(40, 44, 60)
	Lighting.OutdoorAmbient = Color3.fromRGB(66, 74, 102)
	Lighting.EnvironmentDiffuseScale = 0.4
	Lighting.EnvironmentSpecularScale = 0.6
	Lighting.GlobalShadows = true
	Lighting.FogColor = Color3.fromRGB(18, 22, 34)
	Lighting.FogStart = 120
	Lighting.FogEnd = 520
	-- The lobby's daytime effects don't belong here
	for _, effect in ipairs(Lighting:GetChildren()) do
		if effect:IsA("SunRaysEffect") or effect:IsA("DepthOfFieldEffect") then
			effect.Enabled = false
		end
	end
	local atmosphere = Lighting:FindFirstChildOfClass("Atmosphere") or Instance.new("Atmosphere")
	atmosphere.Density = 0.34
	atmosphere.Offset = 0.1
	atmosphere.Color = Color3.fromRGB(96, 108, 140)
	atmosphere.Decay = Color3.fromRGB(30, 36, 60)
	atmosphere.Glare = 0
	atmosphere.Haze = 1.6
	atmosphere.Parent = Lighting
	local bloom = Lighting:FindFirstChildOfClass("BloomEffect") or Instance.new("BloomEffect")
	bloom.Enabled = true
	bloom.Intensity = 0.7
	bloom.Size = 26
	bloom.Threshold = 1.4
	bloom.Parent = Lighting
	local grade = Lighting:FindFirstChild("HeistGrade") or Instance.new("ColorCorrectionEffect")
	grade.Name = "HeistGrade"
	grade.TintColor = Color3.fromRGB(226, 234, 255)
	grade.Contrast = 0.1
	grade.Saturation = -0.04
	grade.Parent = Lighting
end

local function loadMap(name)
	local maps = ServerStorage:WaitForChild("Maps")
	local template = maps:FindFirstChild(name) or maps:WaitForChild("Bank")
	local map = template:Clone()
	map.Name = "Map"
	map.Parent = workspace
	setNight()
	mapLoaded = true
end

local function setFrozen(player, frozen)
	local root = player.Character and player.Character:FindFirstChild("HumanoidRootPart")
	if root then
		root.Anchored = frozen
	end
end

local function spawnPlayer(player)
	if not mapLoaded or not player.Parent then
		return
	end
	-- Everyone plays the heist in first person
	player.CameraMode = Enum.CameraMode.LockFirstPerson
	player:LoadCharacter()
	if state.phase == "Intro" or (huddle and huddle.inProgress()) then
		setFrozen(player, true)
	end
	-- A dim personal light, so the crew can still see once the power is cut
	local root = player.Character and player.Character:FindFirstChild("HumanoidRootPart")
	if root then
		local light = Instance.new("PointLight")
		light.Name = "Flashlight"
		light.Range = 16
		light.Brightness = 0.8
		light.Color = Color3.fromRGB(255, 236, 200)
		light.Shadows = true
		light.Parent = root
	end
end

-- Waits until everyone from the lobby has arrived, or until it's clear the rest aren't coming
local function waitForCrew()
	local first = Players:GetPlayers()[1] or Players.PlayerAdded:Wait()
	local data = GameMode.studioHeistData() or first:GetJoinData().TeleportData
	local expected = type(data) == "table" and type(data.memberIds) == "table" and #data.memberIds or nil
	local deadline = os.clock() + (RunService:IsStudio() and STUDIO_WAIT_SECONDS or CREW_WAIT_SECONDS)
	while os.clock() < deadline do
		if expected and #Players:GetPlayers() >= expected then
			break
		end
		task.wait(0.25)
	end
	return data
end

---------------------------------------------------------------- roles

local function shuffled(list)
	local copy = table.clone(list)
	for i = #copy, 2, -1 do
		local j = math.random(i)
		copy[i], copy[j] = copy[j], copy[i]
	end
	return copy
end

local function buildRoleInfo(player, crew)
	local info = {
		role = cops[player] and "Cop" or "Robber",
		map = mapName,
		difficulty = settings.difficulty,
		length = settings.length,
		copCount = settings.copCount,
		copsKnowEachOther = settings.copsKnowEachOther,
		crew = {},
		fellowCops = {},
	}
	for _, other in ipairs(crew) do
		if other ~= player then
			table.insert(info.crew, other.DisplayName)
			if cops[player] and cops[other] and settings.copsKnowEachOther then
				table.insert(info.fellowCops, other.DisplayName)
			end
		end
	end
	return info
end

local function dealRoles()
	local crew = Players:GetPlayers()
	local copCount = math.clamp(settings.copCount, 1, Config.maxCopsFor(#crew))
	-- Solo / duo Studio tests: everyone robs, unless StudioTestRole asks for a cop
	if #crew < Config.MIN_PLAYERS_TO_START then
		copCount = 0
	end
	local order = shuffled(crew)
	for i = 1, copCount do
		cops[order[i]] = true
	end
	if RunService:IsStudio() and workspace:GetAttribute("StudioTestRole") == "Cop" and copCount == 0 then
		cops[crew[1]] = true
		copCount = 1
	end
	settings.copCount = copCount

	for _, player in ipairs(crew) do
		roleInfo[player] = buildRoleInfo(player, crew)
		HeistUpdate:FireClient(player, "Role", roleInfo[player])
	end
end

local function copNames()
	local names = {}
	for player in pairs(cops) do
		table.insert(names, player.DisplayName)
	end
	return names
end

---------------------------------------------------------------- end of the round

-- Same order as copNames (both walk the same table), so the end screen can match avatars up
local function copIds()
	local ids = {}
	for player in pairs(cops) do
		table.insert(ids, player.UserId)
	end
	return ids
end

local lastRewards = {} -- [UserId as text] = { cash, xp, won }: shown in the lobby on arrival

-- Sends these players (everyone if nil) back to a public lobby server
local function returnToLobby(players)
	players = players or Players:GetPlayers()
	if #players == 0 then
		return
	end
	if RunService:IsStudio() then
		for _, player in ipairs(players) do
			HeistUpdate:FireClient(player, "Notice", "🏠 In the real game you'd go back to the lobby now. Studio can't teleport: stop the test to get back.")
		end
		return
	end
	-- No access code means a normal public server: the lobby
	local options = Instance.new("TeleportOptions")
	options:SetTeleportData({ lastHeist = lastRewards })
	local ok, err = pcall(TeleportService.TeleportAsync, TeleportService, game.PlaceId, players, options)
	if not ok then
		warn("Couldn't send players back to the lobby:", err)
		for _, player in ipairs(players) do
			player:Kick("The heist is over. Rejoin to get back to the lobby.")
		end
	end
end

-- Everything the cops did and every huddle verdict, in order, for the end screen
local function buildReplay()
	local replay = {}
	for _, entry in ipairs(job and job.sabotageLog or {}) do
		table.insert(replay, { clock = entry.clock, seconds = entry.seconds, icon = entry.icon, text = entry.text, kind = "sabotage" })
	end
	for _, entry in ipairs(huddle and huddle.history or {}) do
		table.insert(replay, { clock = entry.clock, seconds = entry.seconds, icon = entry.icon, text = entry.text, kind = "huddle" })
	end
	table.sort(replay, function(a, b)
		return a.seconds < b.seconds
	end)
	return replay
end

-- Did this player photograph a cop in the act?
local function caughtCopOnCamera(player)
	for _, photo in ipairs(photos and photos.all or {}) do
		if photo.by == player.UserId then
			for _, subject in ipairs(photo.subjects) do
				local other = Players:GetPlayerByUserId(subject.userId)
				if subject.tone == "odd" and other and cops[other] then
					return true
				end
			end
		end
	end
	return false
end

-- Pays everyone and saves it before they go back to the lobby. The data is loaded fresh here
-- (not on arrival), so it includes anything the lobby saved after they left.
local function giveRewards(winner)
	for _, player in ipairs(Players:GetPlayers()) do
		task.spawn(function()
			local reward = Rewards.compute({
				winner = winner,
				isCop = cops[player] == true,
				bags = job and job.bagsLoaded or 0,
				sabotages = job and job:sabotageCount(player) or 0,
				bonus = job and job.bonus or 0,
				votesAgainst = huddle and huddle.votesAgainst[player.UserId] or 0,
				caughtCop = huddle and huddle.catchers[player.UserId] == true,
				evidence = caughtCopOnCamera(player),
			})
			local data = PlayerData.load(player)
			local xpBefore = data.xp
			data.cash += reward.cash
			data.xp += reward.xp
			data.heists += 1
			if reward.won then
				if cops[player] then
					data.copWins += 1
				else
					data.robberWins += 1
				end
			end
			local saved = PlayerData.save(player)
			-- Adds up over several rounds on this server, for the lobby's welcome-back message
			local key = tostring(player.UserId)
			local earlier = lastRewards[key]
			lastRewards[key] = {
				cash = reward.cash + (earlier and earlier.cash or 0),
				xp = reward.xp + (earlier and earlier.xp or 0),
				won = reward.won,
			}
			if player.Parent then
				HeistUpdate:FireClient(player, "Rewards", {
					lines = reward.lines,
					cash = reward.cash,
					xp = reward.xp,
					won = reward.won,
					xpBefore = xpBefore,
					xpAfter = data.xp,
					cashAfter = data.cash,
					saved = saved,
				})
			end
		end)
	end
end

-- winner: "Robbers", "Cop", "Police" (Heat maxed out with no cop in the round)
-- or "None" (no cop in a test round and time ran out)
local function endRound(winner, reason)
	if state.phase == "End" then
		return
	end
	if job then
		job:stop()
	end
	state.phase = "End"
	state.paused = nil
	for _, player in ipairs(Players:GetPlayers()) do
		player.CameraMode = Enum.CameraMode.Classic
		if player.Character then
			player.Character:SetAttribute("Action", nil)
		end
	end
	state.result = {
		winner = winner,
		reason = reason,
		cops = copNames(),
		copIds = copIds(),
		returnIn = END_SECONDS,
		replay = buildReplay(),
		bags = job and job.bagsLoaded or 0,
		heat = math.floor(state.heat),
	}
	broadcastState()
	giveRewards(winner)
end

---------------------------------------------------------------- after the round: again or lobby?

local endChoice = {} -- [Player] = "again" / "lobby"

local function broadcastEndVotes()
	local again = {}
	for _, player in ipairs(Players:GetPlayers()) do
		if endChoice[player] == "again" then
			table.insert(again, player.DisplayName)
		end
	end
	HeistUpdate:FireAllClients("EndVotes", { again = again, total = #Players:GetPlayers() })
end

local function chooseAfterRound(player, choice)
	if state.phase ~= "End" or (choice ~= "again" and choice ~= "lobby") then
		return false
	end
	endChoice[player] = choice
	if choice == "lobby" then
		returnToLobby({ player })
	end
	broadcastEndVotes()
	return true
end

-- Waits on the end screen. Returns the players who want another round (empty: nobody)
local function waitForEndChoices()
	table.clear(endChoice)
	broadcastEndVotes()
	local deadline = os.clock() + END_SECONDS
	while os.clock() < deadline do
		local players = Players:GetPlayers()
		if #players == 0 then
			return {}
		end
		-- Everyone who's still here has picked: no need to wait for the timer
		local undecided = 0
		for _, player in ipairs(players) do
			if not endChoice[player] then
				undecided += 1
			end
		end
		if undecided == 0 then
			break
		end
		task.wait(0.25)
	end
	local stay, leave = {}, {}
	for _, player in ipairs(Players:GetPlayers()) do
		if endChoice[player] == "again" then
			table.insert(stay, player)
		elseif endChoice[player] ~= "lobby" then
			table.insert(leave, player)
		end
	end
	-- A live round needs a proper crew; Studio tests can replay alone
	if #stay > 0 and #stay < Config.MIN_PLAYERS_TO_START and not RunService:IsStudio() then
		for _, player in ipairs(stay) do
			HeistUpdate:FireClient(player, "Notice", ("Not enough players for another round (need %d). Back to the lobby!"):format(Config.MIN_PLAYERS_TO_START))
			table.insert(leave, player)
		end
		stay = {}
	end
	returnToLobby(leave)
	return stay
end

---------------------------------------------------------------- wiring

local lastRequest = {} -- [Player] = os.clock()

---------------------------------------------------------------- action poses

-- Every client animates every character from its "Action" attribute (ActionPoses on the
-- client). Server prompts set it here; the cop's own sabotage prompts live on their client,
-- so those ask for a pose with the "Pose" request.
local PROMPT_POSES = {
	["Pick the lock"] = "Lockpick",
	["Smash the glass"] = "Smash",
	["Smash the camera"] = "Smash",
	["Pull the breaker"] = "Pull",
	["Search"] = "Search",
	["Set up the drill"] = "Work",
	["Fix the drill"] = "Work",
	["Bag the loot"] = "Grab",
	["Check for a tracker"] = "Crouch",
	["Watch the tapes"] = "Type",
	["Check the call log"] = "Phone",
	["Read the breaker log"] = "Search",
	["Check the repair notes"] = "Search",
	["Look at the note"] = "Search",
	["Load the bag"] = "Throw",
}
local CLIENT_POSES = { Type = true, Search = true, Work = true, Phone = true, Pull = true, Grab = true, Crouch = true, Photo = true }
local lastPose = {} -- [Player] = os.clock()

local function setPose(player, pose)
	local character = player.Character
	if character then
		character:SetAttribute("Action", pose)
	end
end

ProximityPromptService.PromptButtonHoldBegan:Connect(function(prompt, player)
	if state.phase == "Heist" then
		setPose(player, prompt:GetAttribute("Pose") or PROMPT_POSES[prompt.ActionText] or "Grab")
	end
end)
ProximityPromptService.PromptButtonHoldEnded:Connect(function(_, player)
	setPose(player, nil)
end)

---------------------------------------------------------------- sprinting

-- Sprinting is fast, but it costs you: stamina runs out (and leaves you winded and slow for
-- a moment), you can't sprint with a loot bag or zip-tied, and running footsteps inside the
-- bank raise the Heat.
local SPRINT_SPEED = 25
local NORMAL_SPEED = 16
local CARRY_SPEED = 12
local TIED_SPEED = 8
local WINDED_SPEED = 9
local SPRINT_SECONDS = 5 -- a full bar lasts this long
local RECOVER_SECONDS = 7 -- ...and takes this long to refill
local WINDED_SECONDS = 2.5
local SPRINT_HEAT_PER_SECOND = 0.6
local SPRINT_MIN_STAMINA = 0.2 -- needed to start sprinting again

local sprint = {} -- [Player] = { on, stamina, windedUntil, warned }

local function sprintState(player)
	local s = sprint[player]
	if not s then
		s = { on = false, stamina = 1, windedUntil = 0 }
		sprint[player] = s
	end
	return s
end

local function restingSpeed(player)
	if huddle and not huddle.canAct(player) then
		return TIED_SPEED
	elseif job and job:isCarrying(player) then
		return CARRY_SPEED
	elseif os.clock() < sprintState(player).windedUntil then
		return WINDED_SPEED
	end
	return NORMAL_SPEED
end

local function setSprinting(player, on)
	local s = sprintState(player)
	local humanoid = player.Character and player.Character:FindFirstChildOfClass("Humanoid")
	s.on = on
	player:SetAttribute("Sprinting", on or nil)
	if humanoid then
		humanoid.WalkSpeed = on and SPRINT_SPEED or restingSpeed(player)
	end
end

local function requestSprint(player, on)
	local s = sprintState(player)
	if not on then
		if s.on then
			setSprinting(player, false)
		end
		return true
	end
	if state.phase ~= "Heist" or state.paused then
		return false
	end
	if huddle and not huddle.canAct(player) then
		return false, "You can't run with your hands zip-tied."
	end
	if job and job:isCarrying(player) then
		return false, "Too heavy to run with a loot bag."
	end
	if os.clock() < s.windedUntil or s.stamina < SPRINT_MIN_STAMINA then
		return false, "You're out of breath."
	end
	if not s.warned then
		s.warned = true
		HeistUpdate:FireClient(player, "Notice", "🏃 Running is loud: inside the bank it raises the Heat, and it wears you out.")
	end
	setSprinting(player, true)
	return true
end

-- Stamina, footstep Heat and the winded state, ten times a second
task.spawn(function()
	local STEP = 0.1
	while true do
		task.wait(STEP)
		for _, player in ipairs(Players:GetPlayers()) do
			local s = sprintState(player)
			local humanoid = player.Character and player.Character:FindFirstChildOfClass("Humanoid")
			local root = player.Character and player.Character:FindFirstChild("HumanoidRootPart")
			if s.on then
				local moving = humanoid and humanoid.MoveDirection.Magnitude > 0.1
				if state.phase ~= "Heist" or state.paused or (job and job:isCarrying(player)) or (huddle and not huddle.canAct(player)) then
					setSprinting(player, false)
				elseif moving then
					s.stamina = math.max(0, s.stamina - STEP / SPRINT_SECONDS)
					if root and BankJob.inside(root.Position) then
						local heat = SPRINT_HEAT_PER_SECOND * STEP
						if job then
							job:_heat(heat)
						end
					end
					if s.stamina <= 0 then
						s.windedUntil = os.clock() + WINDED_SECONDS
						setSprinting(player, false)
						HeistUpdate:FireClient(player, "Notice", "😮‍💨 Out of breath! You're slow for a moment.")
						local until_ = s.windedUntil
						task.delay(WINDED_SECONDS + 0.05, function()
							if s.windedUntil == until_ and not s.on and humanoid and humanoid.Parent then
								humanoid.WalkSpeed = restingSpeed(player)
							end
						end)
					end
				end
			elseif os.clock() >= s.windedUntil and s.stamina < 1 then
				s.stamina = math.min(1, s.stamina + STEP / RECOVER_SECONDS)
			end
			-- Only send it when it visibly changes
			local shown = math.floor(s.stamina * 50 + 0.5) / 50
			if player:GetAttribute("Stamina") ~= shown then
				player:SetAttribute("Stamina", shown)
			end
			player:SetAttribute("Winded", os.clock() < s.windedUntil or nil)
		end
	end
end)

HeistRequest.OnServerInvoke = function(player, action, ...)
	if action == "GetState" then
		return state, roleInfo[player], huddle and huddle.state()
	end
	if action == "Sprint" then
		local on = ...
		return requestSprint(player, on == true)
	end
	if action == "EndChoice" then
		return chooseAfterRound(player, ...)
	end
	if action == "Pose" then
		-- Its own light limit, so letting go of a key right after pressing it still clears the pose
		local pose = ...
		local now = os.clock()
		if lastPose[player] and now - lastPose[player] < 0.08 then
			return false
		end
		lastPose[player] = now
		if pose == nil or (CLIENT_POSES[pose] and state.phase == "Heist") then
			setPose(player, pose)
		end
		return true
	end
	local now = os.clock()
	if lastRequest[player] and now - lastRequest[player] < 0.3 then
		return false, "Slow down!"
	end
	lastRequest[player] = now
	if action == "TakePhoto" then
		if not photos or (huddle and huddle.inProgress()) then
			return false, "Not now."
		end
		if huddle and not huddle.canAct(player) then
			return false, "Your hands are zip-tied."
		end
		return photos.take(player, ...)
	end
	if action == "CallHuddle" or action == "PinPhoto" or action == "Vote" then
		if not huddle or state.phase ~= "Heist" then
			return false, "Not now."
		end
		if action == "CallHuddle" then
			return huddle.call(player)
		elseif action == "PinPhoto" then
			return huddle.pin(player, ...)
		end
		return huddle.vote(player, ...)
	end
	if action == "EnterCode" or action == "Sabotage" then
		if not job or state.phase ~= "Heist" then
			return false, "Not now."
		end
		if action == "EnterCode" then
			return job:enterCode(player, ...)
		end
		return job:sabotage(player, ...)
	end
	return nil
end

-- What the job module can do to the round
local function jobContext()
	return {
		difficulty = settings.difficulty,
		addHeat = function(amount)
			if state.phase ~= "Heist" then
				return
			end
			state.heat = math.min(100, state.heat + amount)
			queueBroadcast()
			if state.heat >= 100 then
				endRound(next(cops) and "Cop" or "Police", "Heat hit 100%. The police surrounded the bank.")
			end
		end,
		notify = function(player, message)
			if player then
				HeistUpdate:FireClient(player, "Notice", message)
			else
				notifyAll(message)
			end
		end,
		notifyExcept = function(except, message)
			for _, player in ipairs(Players:GetPlayers()) do
				if player ~= except then
					HeistUpdate:FireClient(player, "Notice", message)
				end
			end
		end,
		send = function(player, kind, data)
			HeistUpdate:FireClient(player, kind, data)
		end,
		changed = function()
			state.objectives = job and job.objectives or state.objectives
			queueBroadcast()
		end,
		finish = function(winner, reason)
			endRound(winner, reason)
		end,
		isCop = function(player)
			return cops[player] == true
		end,
		cops = function()
			local list = {}
			for cop in pairs(cops) do
				table.insert(list, cop)
			end
			return list
		end,
		clock = function()
			return formatClock(elapsed())
		end,
		elapsed = elapsed,
		formatClock = formatClock,
		canAct = function(player)
			return huddle == nil or huddle.canAct(player)
		end,
		markers = mapData,
	}
end

local function onPlayerAdded(player)
	player.CharacterAdded:Connect(function(character)
		local humanoid = character:WaitForChild("Humanoid")
		humanoid.Died:Connect(function()
			if job then
				job:onPlayerGone(player)
			end
			task.delay(RESPAWN_SECONDS, function()
				if state.phase ~= "End" then
					spawnPlayer(player)
				end
			end)
		end)
	end)
	-- Someone who arrives after the roles were dealt joins as a robber
	if mapLoaded then
		if state.phase ~= "Waiting" and not roleInfo[player] then
			roleInfo[player] = buildRoleInfo(player, Players:GetPlayers())
			HeistUpdate:FireClient(player, "Role", roleInfo[player])
		end
		spawnPlayer(player)
		if photos then
			photos.sendFilm(player)
		end
	end
end
Players.PlayerAdded:Connect(onPlayerAdded)
for _, player in ipairs(Players:GetPlayers()) do
	task.spawn(onPlayerAdded, player)
end

Players.PlayerRemoving:Connect(function(player)
	if job then
		job:onPlayerGone(player)
	end
	lastRequest[player] = nil
	lastPose[player] = nil
	sprint[player] = nil
	endChoice[player] = nil
	roleInfo[player] = nil
	local wasCop = cops[player]
	cops[player] = nil
	if state.phase ~= "Heist" then
		return
	end
	if wasCop and next(cops) == nil then
		endRound("Robbers", player.DisplayName .. " left the game. They were the undercover cop!")
	elseif #Players:GetPlayers() - 1 <= 0 then
		endRound("None", "Everyone left.")
	end
end)

---------------------------------------------------------------- the round

local requestedCopCount -- what the host asked for; dealRoles may lower it for a small crew

-- Clears everything from the last round so the same crew can go again on this server
local function resetForReplay()
	HeistUpdate:FireAllClients("Restart")
	if huddle then
		huddle.stop()
	end
	job, photos, huddle, mapData = nil, nil, nil, nil
	heistStartedAt, pausedAt, pausedTotal = nil, nil, 0
	table.clear(roleInfo)
	table.clear(cops)
	table.clear(endChoice)
	settings.copCount = requestedCopCount
	state.phase = "Waiting"
	state.heat = 0
	state.endsAt = nil
	state.result = nil
	state.objectives = {}
	state.paused = nil
	for _, player in ipairs(Players:GetPlayers()) do
		player:SetAttribute("ZipTied", nil)
		player:SetAttribute("Sprinting", nil)
		sprint[player] = nil
	end
	local old = workspace:FindFirstChild("Map")
	if old then
		old:Destroy()
	end
	mapLoaded = false
	-- Give every client a moment to rebuild its HUD before the new round starts
	task.wait(1.5)
	loadMap(mapName)
end

local function runRound()
for _, player in ipairs(Players:GetPlayers()) do
	task.spawn(spawnPlayer, player)
end

state.phase = "Intro"
state.map = mapName
broadcastState()
task.wait(1)
for _, player in ipairs(Players:GetPlayers()) do
	setFrozen(player, true)
end
dealRoles()
task.wait(REVEAL_SECONDS)

local duration = (Config.LENGTH_MINUTES[settings.length] or 10) * 60
state.phase = "Heist"
state.endsAt = workspace:GetServerTimeNow() + duration
heistStartedAt = os.clock()
mapData = MapData.build(workspace:WaitForChild("Map"))
job = BankJob.start(workspace:WaitForChild("Map"), jobContext())
state.objectives = job.objectives
local context = jobContext()
photos = Photos.start({
	active = function()
		return state.phase == "Heist"
	end,
	elapsed = elapsed,
	addHeat = context.addHeat,
	job = function()
		return job
	end,
	points = workspace.Map:WaitForChild("Points"),
	send = context.send,
	notify = context.notify,
})
for _, player in ipairs(Players:GetPlayers()) do
	photos.sendFilm(player)
end
huddle = Huddle.start({
	active = function()
		return state.phase == "Heist"
	end,
	clock = context.clock,
	elapsed = elapsed,
	isCop = function(player)
		return cops[player] == true
	end,
	freeze = function(frozen)
		for _, player in ipairs(Players:GetPlayers()) do
			setFrozen(player, frozen)
		end
	end,
	pauseClock = function(paused)
		if paused and not pausedAt then
			pausedAt = os.clock()
			state.paused = true
		elseif not paused and pausedAt then
			local stopped = os.clock() - pausedAt
			pausedAt = nil
			pausedTotal += stopped
			state.endsAt += stopped
			state.paused = nil
		end
		broadcastState()
	end,
	broadcast = function(kind, data)
		HeistUpdate:FireAllClients(kind, data)
	end,
	notify = context.notify,
	photos = function()
		return photos.all
	end,
	finish = function(winner, reason)
		endRound(winner, reason)
	end,
	addHeat = context.addHeat,
	job = function()
		return job
	end,
})
broadcastState()
for _, player in ipairs(Players:GetPlayers()) do
	setFrozen(player, false)
end

while state.phase == "Heist" do
	if not pausedAt and workspace:GetServerTimeNow() >= state.endsAt then
		if next(cops) then
			endRound("Cop", "Time ran out. The police arrived before the crew got away.")
		else
			endRound("None", "Time ran out.")
		end
	end
	task.wait(0.5)
end
end

local crewData = waitForCrew()
settings, mapName = readSettings(crewData)
requestedCopCount = settings.copCount
loadMap(mapName)

-- Play, then either go again with whoever wants to, or send everyone to the lobby
while true do
	runRound()
	local stay = waitForEndChoices()
	if #stay == 0 then
		break
	end
	resetForReplay()
end
