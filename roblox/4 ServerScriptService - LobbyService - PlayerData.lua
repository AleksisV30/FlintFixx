-- Saved player progress: cash, XP, heist stats, and owned / equipped shop items.
local DataStoreService = game:GetService("DataStoreService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Config = require(ReplicatedStorage:WaitForChild("LobbyConfig"))

local PlayerData = {}

local profiles = {} -- [Player] = data
-- Only data that really loaded gets saved, so a failed load can never overwrite a save
local loaded = {} -- [Player] = true

local storeOk, store = pcall(DataStoreService.GetDataStore, DataStoreService, "PlayerData_v1")
if not storeOk then
	store = nil
end

local function key(player)
	return "player_" .. player.UserId
end

local function defaults()
	return {
		cash = Config.STARTING_CASH,
		xp = 0,
		heists = 0,
		robberWins = 0,
		copWins = 0,
		-- Quest stats (see Config.QUESTS)
		bags = 0,
		sabotages = 0,
		copsCaught = 0,
		evidence = 0,
		-- [quest id] = how many tiers have been claimed
		quests = {},
		owned = {},
		equipped = {},
	}
end

function PlayerData.load(player)
	local data = defaults()
	-- Studio test players have negative ids and nothing saved
	if store and player.UserId > 0 then
		local ok, saved = pcall(store.GetAsync, store, key(player))
		if ok then
			loaded[player] = true
			if type(saved) == "table" then
				for field, value in pairs(saved) do
					data[field] = value
				end
			end
		else
			warn("Couldn't load data for " .. player.Name .. " (progress won't save this session):", saved)
		end
	end

	-- Free items are always owned, and every category has something equipped
	for _, item in ipairs(Config.SHOP) do
		if item.cash == 0 then
			data.owned[item.id] = true
			if not data.equipped[item.category] then
				data.equipped[item.category] = item.id
			end
		end
	end

	profiles[player] = data
	return data
end

function PlayerData.get(player)
	return profiles[player]
end

-- A copy that is safe to send to the client
function PlayerData.profile(player)
	local data = profiles[player]
	if not data then
		return nil
	end
	return {
		cash = data.cash,
		xp = data.xp,
		heists = data.heists,
		robberWins = data.robberWins,
		copWins = data.copWins,
		bags = data.bags,
		sabotages = data.sabotages,
		copsCaught = data.copsCaught,
		evidence = data.evidence,
		quests = table.clone(data.quests),
		owned = table.clone(data.owned),
		equipped = table.clone(data.equipped),
	}
end

-- Returns true when the data is safely stored
function PlayerData.save(player)
	local data = profiles[player]
	if not store or not data or not loaded[player] then
		return false
	end
	local ok, err = pcall(store.SetAsync, store, key(player), data, { player.UserId })
	if not ok then
		warn("Couldn't save data for " .. player.Name .. ":", err)
	end
	return ok
end

function PlayerData.release(player)
	PlayerData.save(player)
	profiles[player] = nil
	loaded[player] = nil
end

function PlayerData.saveAll()
	local pending = 0
	for player in pairs(profiles) do
		pending += 1
		task.spawn(function()
			PlayerData.save(player)
			pending -= 1
		end)
	end
	local deadline = os.clock() + 25
	while pending > 0 and os.clock() < deadline do
		task.wait(0.1)
	end
end

return PlayerData
