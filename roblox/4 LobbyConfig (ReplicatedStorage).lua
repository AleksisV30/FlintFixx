-- Shared settings for the lobby. Read by both the server and the client.
local Config = {}

-- Party size the host can pick, and the size a new party starts with
Config.MIN_PARTY_SIZE = 3
Config.MAX_PARTY_SIZE = 8
Config.DEFAULT_PARTY_SIZE = 4
Config.DEFAULT_COPS = 1

-- Fewest players in the party before the host can start (1 cop + 2 robbers)
Config.MIN_PLAYERS_TO_START = 3

Config.CODE_LENGTH = 6
Config.COUNTDOWN_SECONDS = 5
-- How long the map spin animation runs before players are sent to the heist
Config.MAP_PICK_SECONDS = 6

-- Fill this in once the game pass exists on the Creator Dashboard (0 = not set up yet)
Config.MAP_PICKER_GAMEPASS_ID = 0

-- Places that can be robbed. "Random" is free; picking one needs the Map Picker game pass.
-- Only `available` places can be played; the rest show up as teasers in the map spin.
-- An available place needs a model with the same name in ServerStorage.Maps.
Config.MAPS = {
	{
		name = "Bank",
		available = true,
		icon = "🏦",
		difficulty = 3,
		loot = 4,
		blurb = "Crack the vault before the guards wake up.",
		color = Color3.fromRGB(64, 190, 120),
	},
	{
		name = "Jewelry Store",
		icon = "💎",
		difficulty = 2,
		loot = 3,
		blurb = "Smash, grab and get out in under a minute.",
		color = Color3.fromRGB(90, 170, 255),
	},
	{
		name = "Museum",
		icon = "🏛️",
		difficulty = 4,
		loot = 4,
		blurb = "Lasers, cameras and one priceless painting.",
		color = Color3.fromRGB(180, 130, 255),
	},
	{
		name = "Casino",
		icon = "🎰",
		difficulty = 5,
		loot = 5,
		blurb = "The house always wins. Prove it wrong.",
		color = Color3.fromRGB(255, 90, 120),
	},
}

function Config.availableMaps()
	local list = {}
	for _, map in ipairs(Config.MAPS) do
		if map.available then
			table.insert(list, map)
		end
	end
	return list
end

function Config.getMap(name)
	for _, map in ipairs(Config.MAPS) do
		if map.name == name then
			return map
		end
	end
	return nil
end

Config.DIFFICULTIES = { "Easy", "Normal", "Hard" }
Config.LENGTHS = { "Short", "Normal", "Long" }
Config.LENGTH_MINUTES = { Short = 5, Normal = 10, Long = 15 }

-- Cops must always be outnumbered by robbers
function Config.maxCopsFor(partySize)
	return math.max(1, math.floor((partySize - 1) / 2))
end

-- Ranks unlock with XP, which the heist place hands out after each round
Config.RANKS = {
	{ name = "Rookie", xp = 0, color = Color3.fromRGB(165, 172, 194) },
	{ name = "Lookout", xp = 100, color = Color3.fromRGB(100, 210, 140) },
	{ name = "Safecracker", xp = 300, color = Color3.fromRGB(80, 152, 255) },
	{ name = "Pro", xp = 700, color = Color3.fromRGB(185, 120, 255) },
	{ name = "Mastermind", xp = 1500, color = Color3.fromRGB(255, 208, 86) },
}

-- Returns the rank for this much XP, and the next rank (nil at the top)
function Config.rankFor(xp)
	local current, nextRank = Config.RANKS[1], Config.RANKS[2]
	for i, rank in ipairs(Config.RANKS) do
		if xp >= rank.xp then
			current, nextRank = rank, Config.RANKS[i + 1]
		end
	end
	return current, nextRank
end

Config.STARTING_CASH = 500

-- Shop items. `cash` items are bought with in-game cash (0 = free and owned by everyone);
-- `robux` items are developer products, sold once their productId is filled in.
Config.SHOP_CATEGORIES = { "Mask", "Outfit", "Getaway" }
Config.SHOP = {
	{ id = "mask_plain", category = "Mask", name = "Plain Mask", icon = "😶", cash = 0 },
	{ id = "mask_clown", category = "Mask", name = "Clown Mask", icon = "🤡", cash = 250 },
	{ id = "mask_skull", category = "Mask", name = "Skull Mask", icon = "💀", cash = 400 },
	{ id = "mask_alien", category = "Mask", name = "Alien Mask", icon = "👽", robux = 49, productId = 0 },
	{ id = "outfit_hoodie", category = "Outfit", name = "Black Hoodie", icon = "🧥", cash = 0 },
	{ id = "outfit_vest", category = "Outfit", name = "Work Vest", icon = "🦺", cash = 300 },
	{ id = "outfit_suit", category = "Outfit", name = "Sharp Suit", icon = "🕴️", cash = 600 },
	{ id = "outfit_tux", category = "Outfit", name = "Gold Tuxedo", icon = "🤵", robux = 99, productId = 0 },
	{ id = "car_van", category = "Getaway", name = "Getaway Van", icon = "🚐", cash = 0 },
	{ id = "car_bike", category = "Getaway", name = "Motorbike", icon = "🏍️", cash = 500 },
	{ id = "car_sports", category = "Getaway", name = "Sports Car", icon = "🏎️", cash = 900 },
	{ id = "car_heli", category = "Getaway", name = "Helicopter", icon = "🚁", robux = 199, productId = 0 },
}

-- Shown in the shop's featured banner and on item cards
Config.ITEM_BLURBS = {
	mask_plain = "Simple, cheap, does the job.",
	mask_clown = "Nobody suspects the clown. Probably.",
	mask_skull = "For when you want the guards to panic.",
	mask_alien = "Out of this world. And out of the vault.",
	outfit_hoodie = "Blend into the shadows.",
	outfit_vest = "Looks like you're here to fix the lights.",
	outfit_suit = "Rob the bank like you own it.",
	outfit_tux = "The most expensive way to commit a crime.",
	car_van = "Room for the whole crew and the loot.",
	car_bike = "Weave through traffic, lose the cops.",
	car_sports = "Zero to getaway in three seconds.",
	car_heli = "Escape over the rooftops in style.",
}

Config.RARITY_COLORS = {
	Common = Color3.fromRGB(165, 172, 194),
	Rare = Color3.fromRGB(80, 152, 255),
	Epic = Color3.fromRGB(185, 120, 255),
	Legendary = Color3.fromRGB(255, 196, 60),
}

-- Rarity comes from the price, so new items slot in without extra setup
function Config.rarityFor(item)
	if item.robux then
		return "Legendary"
	elseif item.cash == 0 then
		return "Common"
	elseif item.cash <= 300 then
		return "Rare"
	elseif item.cash <= 600 then
		return "Epic"
	end
	return "Legendary"
end

Config.RANK_ICONS = {
	Rookie = "🔰",
	Lookout = "👀",
	Safecracker = "🔐",
	Pro = "💼",
	Mastermind = "👑",
}

function Config.getItem(id)
	for _, item in ipairs(Config.SHOP) do
		if item.id == id then
			return item
		end
	end
	return nil
end

function Config.getItemByProduct(productId)
	for _, item in ipairs(Config.SHOP) do
		if item.productId and item.productId ~= 0 and item.productId == productId then
			return item
		end
	end
	return nil
end

return Config
