-- Party system for the hub: create, quick join, join by code or invite, ready checks,
-- host-only settings, kicking and host transfer, the start countdown, crew chat and the shop.
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local MarketplaceService = game:GetService("MarketplaceService")
local TeleportService = game:GetService("TeleportService")
local RunService = game:GetService("RunService")
local TextChatService = game:GetService("TextChatService")

-- Heist servers run HeistService instead
local GameMode = require(ReplicatedStorage:WaitForChild("GameMode"))
if GameMode.get() ~= "Lobby" then
	return
end

local Config = require(ReplicatedStorage:WaitForChild("LobbyConfig"))
local PlayerData = require(script:WaitForChild("PlayerData"))

local remotes = Instance.new("Folder")
remotes.Name = "LobbyRemotes"
local LobbyRequest = Instance.new("RemoteFunction")
LobbyRequest.Name = "LobbyRequest"
LobbyRequest.Parent = remotes
local LobbyUpdate = Instance.new("RemoteEvent")
LobbyUpdate.Name = "LobbyUpdate"
LobbyUpdate.Parent = remotes
remotes.Parent = ReplicatedStorage

-- One TextChannel per party, so crew chat goes through Roblox's text filtering
local channels = Instance.new("Folder")
channels.Name = "PartyChannels"
channels.Parent = TextChatService

local CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" -- no 0/O or 1/I mix-ups
local REQUEST_COOLDOWN = 0.12

local parties = {} -- [id] = party
local partyOf = {} -- [Player] = party
local byCode = {} -- [code] = party
local lastRequest = {} -- [Player] = os.clock()
local nextId = 0

local function newCode()
	local code
	repeat
		local chars = table.create(Config.CODE_LENGTH)
		for i = 1, Config.CODE_LENGTH do
			local n = math.random(1, #CODE_CHARS)
			chars[i] = string.sub(CODE_CHARS, n, n)
		end
		code = table.concat(chars)
	until not byCode[code]
	return code
end

local function notify(player, message)
	LobbyUpdate:FireClient(player, "Notice", message)
end

local function notifyParty(party, message, except)
	for _, member in ipairs(party.members) do
		if member ~= except then
			notify(member, message)
		end
	end
end

local function fireParty(party, kind, data)
	for _, member in ipairs(party.members) do
		LobbyUpdate:FireClient(member, kind, data)
	end
end

local function memberInfo(party, member)
	local data = PlayerData.get(member)
	local mask = data and Config.getItem(data.equipped.Mask)
	return {
		userId = member.UserId,
		name = member.Name,
		displayName = member.DisplayName,
		isHost = member == party.host,
		isReady = member == party.host or party.ready[member] == true,
		xp = data and data.xp or 0,
		heists = data and data.heists or 0,
		mask = mask and mask.icon or nil,
	}
end

local function serialize(party)
	local members = {}
	for _, member in ipairs(party.members) do
		table.insert(members, memberInfo(party, member))
	end
	return {
		id = party.id,
		code = party.code,
		hostUserId = party.host.UserId,
		channel = party.channel.Name,
		starting = party.starting,
		members = members,
		settings = table.clone(party.settings),
	}
end

local function publicList()
	local list = {}
	for _, party in pairs(parties) do
		if not party.settings.isPrivate and not party.starting then
			table.insert(list, {
				id = party.id,
				hostName = party.host.DisplayName,
				hostUserId = party.host.UserId,
				count = #party.members,
				partySize = party.settings.partySize,
				cops = party.settings.copCount,
				map = party.settings.map,
			})
		end
	end
	table.sort(list, function(a, b)
		return a.id < b.id
	end)
	return list
end

-- Sends the party's new state to its members, and the public list to everyone
local function broadcast(party)
	if party and parties[party.id] then
		fireParty(party, "Party", serialize(party))
	end
	LobbyUpdate:FireAllClients("PublicList", publicList())
end

local function pushProfile(player)
	LobbyUpdate:FireClient(player, "Profile", PlayerData.profile(player))
end

local function joinChannel(party, player)
	task.spawn(function()
		local ok, err = pcall(party.channel.AddUserAsync, party.channel, player.UserId)
		if not ok then
			warn("Couldn't add", player.Name, "to crew chat:", err)
		end
	end)
end

local function leaveChannel(party, player)
	for _, source in ipairs(party.channel:GetChildren()) do
		if source:IsA("TextSource") and source.UserId == player.UserId then
			source:Destroy()
		end
	end
end

-- Stops a running start countdown. Callers broadcast the new state afterwards.
local function cancelCountdown(party, reason)
	if not party.starting then
		return
	end
	party.starting = false
	party.countdownToken += 1
	fireParty(party, "Countdown", nil)
	if reason then
		notifyParty(party, reason)
	end
end

local function removeMember(player)
	local party = partyOf[player]
	if not party then
		return
	end
	partyOf[player] = nil
	party.ready[player] = nil
	local index = table.find(party.members, player)
	if index then
		table.remove(party.members, index)
	end
	leaveChannel(party, player)
	if player.Parent then
		LobbyUpdate:FireClient(player, "Party", nil)
		if party.starting then
			LobbyUpdate:FireClient(player, "Countdown", nil)
		end
	end

	if #party.members == 0 then
		parties[party.id] = nil
		byCode[party.code] = nil
		party.countdownToken += 1
		party.channel:Destroy()
	else
		if party.host == player then
			party.host = party.members[1]
			party.ready[party.host] = nil
			notifyParty(party, party.host.DisplayName .. " is now the host.")
		end
		cancelCountdown(party, player.DisplayName .. " left, so the start was cancelled.")
	end
	broadcast(party)
end

local function addMember(player, party)
	if partyOf[player] == party then
		return false, "You're already in this crew."
	end
	if party.starting then
		return false, "That crew is already starting."
	end
	if #party.members >= party.settings.partySize then
		return false, "That crew is full."
	end

	removeMember(player)
	table.insert(party.members, player)
	partyOf[player] = party
	joinChannel(party, player)
	notifyParty(party, player.DisplayName .. " joined the crew.", player)
	broadcast(party)
	return true
end

local function createParty(player)
	removeMember(player)
	nextId += 1
	local channel = Instance.new("TextChannel")
	channel.Name = "Party" .. nextId
	channel.Parent = channels

	local party = {
		id = nextId,
		code = newCode(),
		host = player,
		members = { player },
		ready = {},
		starting = false,
		countdownToken = 0,
		channel = channel,
		settings = {
			partySize = Config.DEFAULT_PARTY_SIZE,
			copCount = Config.DEFAULT_COPS,
			isPrivate = false,
			map = "Random",
			difficulty = "Normal",
			length = "Normal",
			copsKnowEachOther = true,
		},
	}
	parties[party.id] = party
	byCode[party.code] = party
	partyOf[player] = party
	joinChannel(party, player)
	broadcast(party)
	return party
end

local function ownsMapPicker(player)
	if Config.MAP_PICKER_GAMEPASS_ID == 0 then
		return false
	end
	local ok, owns = pcall(
		MarketplaceService.UserOwnsGamePassAsync,
		MarketplaceService,
		player.UserId,
		Config.MAP_PICKER_GAMEPASS_ID
	)
	return ok and owns
end

---------------------------------------------------------------- launching

local function shuffled(list)
	local copy = table.clone(list)
	for i = #copy, 2, -1 do
		local j = math.random(i)
		copy[i], copy[j] = copy[j], copy[i]
	end
	return copy
end

local function pickMap(settings)
	if settings.map ~= "Random" then
		return settings.map
	end
	local available = Config.availableMaps()
	return available[math.random(#available)].name
end

-- Roles are secret, so the heist server deals them out itself. Studio can't teleport, so
-- there they're dealt here instead, so the role reveal can be tried from the lobby.
local function previewRoles(party, mapName)
	local settings = party.settings
	local order = shuffled(party.members)
	local cops = {}
	for i = 1, settings.copCount do
		cops[order[i]] = true
	end

	for _, member in ipairs(party.members) do
		local info = {
			role = cops[member] and "Cop" or "Robber",
			map = mapName,
			difficulty = settings.difficulty,
			length = settings.length,
			copCount = settings.copCount,
			copsKnowEachOther = settings.copsKnowEachOther,
			crew = {},
			fellowCops = {},
			preview = true,
		}
		for _, other in ipairs(party.members) do
			if other ~= member then
				table.insert(info.crew, other.DisplayName)
				if cops[member] and cops[other] and settings.copsKnowEachOther then
					table.insert(info.fellowCops, other.DisplayName)
				end
			end
		end
		LobbyUpdate:FireClient(member, "Role", info)
	end
end

local function teleportParty(party, mapName)
	return pcall(function()
		-- The heist runs on a private server of this same place (see GameMode)
		local accessCode = TeleportService:ReserveServer(game.PlaceId)
		local memberIds = {}
		for _, member in ipairs(party.members) do
			table.insert(memberIds, member.UserId)
		end
		local options = Instance.new("TeleportOptions")
		options.ReservedServerAccessCode = accessCode
		options:SetTeleportData({
			hostUserId = party.host.UserId,
			map = mapName,
			settings = party.settings,
			memberIds = memberIds,
		})
		TeleportService:TeleportAsync(game.PlaceId, party.members, options)
	end)
end

local function launch(party)
	local mapName = pickMap(party.settings)
	fireParty(party, "Countdown", nil)
	fireParty(party, "MapPick", { map = mapName, random = party.settings.map == "Random" })
	if not RunService:IsStudio() then
		-- Let the map spin finish before anyone leaves the lobby
		task.wait(Config.MAP_PICK_SECONDS)
		if not parties[party.id] then
			return
		end
		fireParty(party, "Launching", mapName)
		local ok, err = teleportParty(party, mapName)
		if ok then
			return
		end
		warn("Teleport failed:", err)
		fireParty(party, "Countdown", nil)
		notifyParty(party, "Couldn't start the game. Try again.")
	else
		-- Studio can't teleport, so this same test server becomes the heist server
		task.wait(Config.MAP_PICK_SECONDS)
		if not parties[party.id] then
			return
		end
		fireParty(party, "Launching", mapName)
		task.wait(1)
		local memberIds = {}
		for _, member in ipairs(party.members) do
			table.insert(memberIds, member.UserId)
		end
		GameMode.startStudioHeist({
			hostUserId = party.host.UserId,
			map = mapName,
			settings = party.settings,
			memberIds = memberIds,
		})
		return
	end
	party.starting = false
	table.clear(party.ready)
	broadcast(party)
end

---------------------------------------------------------------- requests

local handlers = {}

function handlers.GetState(player)
	local party = partyOf[player]
	return true,
		{
			party = party and serialize(party) or nil,
			publicList = publicList(),
			profile = PlayerData.profile(player),
		}
end

function handlers.ListPublic()
	return true, publicList()
end

function handlers.Create(player)
	createParty(player)
	return true
end

-- Joins the fullest public crew with room, or starts a new one if there isn't any
function handlers.QuickJoin(player)
	local best
	for _, party in pairs(parties) do
		local open = not party.settings.isPrivate
			and not party.starting
			and #party.members < party.settings.partySize
			and partyOf[player] ~= party
		if open and (not best or #party.members > #best.members) then
			best = party
		end
	end
	if best then
		local ok, message = addMember(player, best)
		return ok, ok and "JOINED" or message
	end
	createParty(player)
	return true, "CREATED"
end

function handlers.JoinByCode(player, code)
	if type(code) ~= "string" then
		return false, "Enter a party code."
	end
	code = string.upper((string.gsub(code, "%s", "")))
	local party = byCode[code]
	if not party then
		return false, "No crew has that code."
	end
	return addMember(player, party)
end

function handlers.JoinPublic(player, id)
	local party = type(id) == "number" and parties[id]
	if not party then
		return false, "That crew doesn't exist anymore."
	end
	if party.settings.isPrivate then
		return false, "That crew is private. Ask the host for the code."
	end
	return addMember(player, party)
end

function handlers.Leave(player)
	removeMember(player)
	return true
end

function handlers.SetReady(player, isReady)
	local party = partyOf[player]
	if not party or type(isReady) ~= "boolean" then
		return false
	end
	if party.host == player then
		return false, "The host is always ready."
	end
	party.ready[player] = isReady or nil
	if not isReady then
		cancelCountdown(party, player.DisplayName .. " isn't ready, so the start was cancelled.")
	end
	broadcast(party)
	return true
end

-- Everything below is host-only
local function hostParty(player)
	local party = partyOf[player]
	if party and party.host == player then
		return party
	end
	return nil
end

local function findMember(party, userId)
	for _, member in ipairs(party.members) do
		if member.UserId == userId then
			return member
		end
	end
	return nil
end

function handlers.Kick(player, userId)
	local party = hostParty(player)
	if not party then
		return false, "Only the host can kick players."
	end
	local member = findMember(party, userId)
	if not member or member == player then
		return false, "That player isn't in your crew."
	end
	removeMember(member)
	notify(member, "You were kicked from the crew.")
	notifyParty(party, member.DisplayName .. " was kicked.")
	return true
end

function handlers.TransferHost(player, userId)
	local party = hostParty(player)
	if not party then
		return false, "Only the host can hand over host."
	end
	if party.starting then
		return false, "Can't change host during the countdown."
	end
	local member = findMember(party, userId)
	if not member or member == player then
		return false, "That player isn't in your crew."
	end
	party.host = member
	party.ready[member] = nil
	notifyParty(party, member.DisplayName .. " is now the host.")
	broadcast(party)
	return true
end

function handlers.NewCode(player)
	local party = hostParty(player)
	if not party then
		return false, "Only the host can change the code."
	end
	byCode[party.code] = nil
	party.code = newCode()
	byCode[party.code] = party
	broadcast(party)
	return true
end

function handlers.UpdateSetting(player, key, value)
	local party = hostParty(player)
	if not party then
		return false, "Only the host can change settings."
	end
	if party.starting then
		return false, "Can't change settings during the countdown."
	end
	local settings = party.settings

	if key == "partySize" then
		if type(value) ~= "number" then
			return false
		end
		value = math.floor(value)
		if value < Config.MIN_PARTY_SIZE or value > Config.MAX_PARTY_SIZE then
			return false, ("Crew size must be %d-%d."):format(Config.MIN_PARTY_SIZE, Config.MAX_PARTY_SIZE)
		end
		if value < #party.members then
			return false, "Kick someone first, the crew has more players than that."
		end
		settings.partySize = value
		settings.copCount = math.min(settings.copCount, Config.maxCopsFor(value))
	elseif key == "copCount" then
		if type(value) ~= "number" then
			return false
		end
		value = math.floor(value)
		local maxCops = Config.maxCopsFor(settings.partySize)
		if value < 1 or value > maxCops then
			return false, ("A crew of %d can have 1-%d cops."):format(settings.partySize, maxCops)
		end
		settings.copCount = value
	elseif key == "isPrivate" or key == "copsKnowEachOther" then
		if type(value) ~= "boolean" then
			return false
		end
		settings[key] = value
	elseif key == "difficulty" or key == "length" then
		local options = key == "difficulty" and Config.DIFFICULTIES or Config.LENGTHS
		if not table.find(options, value) then
			return false
		end
		settings[key] = value
	elseif key == "map" then
		if value ~= "Random" then
			local map = Config.getMap(value)
			if not map then
				return false, "Unknown place."
			end
			if not map.available then
				return false, "🚧 The " .. map.name .. " isn't open yet. Coming soon!"
			end
			if not ownsMapPicker(player) then
				return false, "NEEDS_MAP_PICKER"
			end
		end
		settings.map = value
	else
		return false, "Unknown setting."
	end

	broadcast(party)
	return true
end

function handlers.Start(player)
	local party = hostParty(player)
	if not party then
		return false, "Only the host can start the heist."
	end
	if party.starting then
		return false, "Already starting."
	end
	local count = #party.members
	-- Studio tests can start with any crew size (even alone); the heist then has no cop
	-- unless Workspace's StudioTestRole attribute is "Cop"
	local studio = RunService:IsStudio()
	if count < Config.MIN_PLAYERS_TO_START and not studio then
		return false, ("You need at least %d players to start."):format(Config.MIN_PLAYERS_TO_START)
	end
	if party.settings.copCount >= count - party.settings.copCount and not studio then
		return false, "There must be more robbers than cops. Lower the cop count or invite more players."
	end
	local notReady = {}
	for _, member in ipairs(party.members) do
		if member ~= party.host and not party.ready[member] then
			table.insert(notReady, member.DisplayName)
		end
	end
	if #notReady > 0 then
		return false, "Still waiting for: " .. table.concat(notReady, ", ")
	end

	party.starting = true
	party.countdownToken += 1
	local token = party.countdownToken
	broadcast(party)
	task.spawn(function()
		for seconds = Config.COUNTDOWN_SECONDS, 1, -1 do
			if party.countdownToken ~= token then
				return
			end
			fireParty(party, "Countdown", seconds)
			task.wait(1)
		end
		if party.countdownToken == token then
			launch(party)
		end
	end)
	return true
end

function handlers.CancelStart(player)
	local party = hostParty(player)
	if not party or not party.starting then
		return false
	end
	cancelCountdown(party, "The host cancelled the start.")
	broadcast(party)
	return true
end

---------------------------------------------------------------- shop

local function onLoadoutChanged(player)
	pushProfile(player)
	local party = partyOf[player]
	if party then
		broadcast(party)
	end
end

-- Pays out the next tier of a quest once its goal is reached
function handlers.ClaimQuest(player, questId)
	local quest = type(questId) == "string" and Config.getQuest(questId)
	if not quest then
		return false, "Unknown quest."
	end
	local data = PlayerData.get(player)
	if not data then
		return false, "Your profile is still loading."
	end
	local claimed = data.quests[quest.id] or 0
	local tier = quest.tiers[claimed + 1]
	if not tier then
		return false, "You've finished every tier of this quest."
	end
	if (data[quest.stat] or 0) < tier.goal then
		return false, "Not finished yet. Keep going!"
	end
	data.quests[quest.id] = claimed + 1
	data.cash += tier.cash
	data.xp += tier.xp
	pushProfile(player)
	-- Rank shows on the party's name tags, so refresh the party too
	local party = partyOf[player]
	if party then
		broadcast(party)
	end
	task.spawn(PlayerData.save, player)
	return true, { cash = tier.cash, xp = tier.xp }
end

function handlers.BuyItem(player, itemId)
	local item = type(itemId) == "string" and Config.getItem(itemId)
	if not item then
		return false, "Unknown item."
	end
	local data = PlayerData.get(player)
	if not data then
		return false, "Your profile is still loading."
	end
	if data.owned[item.id] then
		return false, "You already own that."
	end
	if not item.cash then
		return false, "That item is bought with Robux."
	end
	if data.cash < item.cash then
		return false, ("You need $%d more."):format(item.cash - data.cash)
	end
	data.cash -= item.cash
	data.owned[item.id] = true
	data.equipped[item.category] = item.id
	onLoadoutChanged(player)
	return true
end

function handlers.EquipItem(player, itemId)
	local item = type(itemId) == "string" and Config.getItem(itemId)
	local data = PlayerData.get(player)
	if not item or not data or not data.owned[item.id] then
		return false, "You don't own that yet."
	end
	data.equipped[item.category] = item.id
	onLoadoutChanged(player)
	return true
end

MarketplaceService.ProcessReceipt = function(receipt)
	local player = Players:GetPlayerByUserId(receipt.PlayerId)
	local item = Config.getItemByProduct(receipt.ProductId)
	local data = player and PlayerData.get(player)
	if not item or not data then
		return Enum.ProductPurchaseDecision.NotProcessedYet
	end
	data.owned[item.id] = true
	data.equipped[item.category] = item.id
	if not PlayerData.save(player) then
		return Enum.ProductPurchaseDecision.NotProcessedYet
	end
	onLoadoutChanged(player)
	notify(player, "Thanks! " .. item.name .. " is yours.")
	return Enum.ProductPurchaseDecision.PurchaseGranted
end

---------------------------------------------------------------- wiring

LobbyRequest.OnServerInvoke = function(player, action, ...)
	local now = os.clock()
	if lastRequest[player] and now - lastRequest[player] < REQUEST_COOLDOWN then
		return false
	end
	lastRequest[player] = now

	local handler = type(action) == "string" and handlers[action]
	if not handler then
		return false, "Unknown request."
	end
	local ok, result, extra = pcall(handler, player, ...)
	if not ok then
		warn("Lobby request", action, "failed:", result)
		return false, "Something went wrong."
	end
	return result, extra
end

-- Safety net: now and then Roblox doesn't spawn a joining player's character, which would
-- leave them without an avatar in the hideout. If that happens, spawn it ourselves.
local SPAWN_CHECK_SECONDS = 6
local SPAWN_RETRIES = 5

local function ensureCharacter(player)
	task.spawn(function()
		task.wait(SPAWN_CHECK_SECONDS)
		-- A player who is still loading can't be spawned yet, so keep trying for a while
		for attempt = 1, SPAWN_RETRIES do
			if not player.Parent or player.Character then
				return
			end
			warn(("No character for %s, spawning one (try %d)"):format(player.Name, attempt))
			local ok, err = pcall(player.LoadCharacter, player)
			if not ok then
				warn("Couldn't spawn " .. player.Name .. ":", err)
			end
			task.wait(3)
		end
	end)
end

local function withCommas(n)
	local text = tostring(n)
	while true do
		local replaced, count = string.gsub(text, "^(-?%d+)(%d%d%d)", "%1,%2")
		text = replaced
		if count == 0 then
			break
		end
	end
	return text
end

local function onPlayerAdded(player)
	ensureCharacter(player)
	PlayerData.load(player)
	if not player.Parent then
		return
	end
	pushProfile(player)

	-- Back from a heist: say what they earned. The numbers come from the client's teleport
	-- data, so they're only shown, never added (the heist server already saved them).
	local teleportData = player:GetJoinData().TeleportData
	local lastHeist = type(teleportData) == "table" and type(teleportData.lastHeist) == "table"
		and teleportData.lastHeist[tostring(player.UserId)]
	if type(lastHeist) == "table" and type(lastHeist.cash) == "number" and type(lastHeist.xp) == "number" then
		task.delay(4, function()
			if player.Parent then
				notify(player, ("%s Welcome back! The heist paid you +%s cash and +%d XP."):format(
					lastHeist.won and "🏆" or "🏁",
					withCommas(math.floor(lastHeist.cash)),
					math.floor(lastHeist.xp)
				))
			end
		end)
	end

	-- Friends invited from a party arrive with its code as launch data
	local launchData = player:GetJoinData().LaunchData
	if type(launchData) == "string" then
		local party = byCode[string.upper(launchData)]
		if party and addMember(player, party) then
			notify(player, "You joined " .. party.host.DisplayName .. "'s crew from an invite.")
		end
	end
end

Players.PlayerAdded:Connect(onPlayerAdded)
for _, player in ipairs(Players:GetPlayers()) do
	task.spawn(onPlayerAdded, player)
end

Players.PlayerRemoving:Connect(function(player)
	removeMember(player)
	lastRequest[player] = nil
	PlayerData.release(player)
end)

game:BindToClose(PlayerData.saveAll)

-- Autosave so a crash loses at most a few minutes of progress
task.spawn(function()
	while true do
		task.wait(180)
		for _, player in ipairs(Players:GetPlayers()) do
			task.spawn(PlayerData.save, player)
		end
	end
end)
