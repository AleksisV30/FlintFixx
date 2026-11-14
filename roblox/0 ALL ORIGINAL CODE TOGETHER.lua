-- ALL ORIGINAL CODE IN ONE FILE (your game exactly as it was before my changes).
-- There are 6 scripts below. Each one starts with a big ===== line that says where it goes in Studio.
-- Copy from under that line down to the next ===== line, and paste it into that script (Ctrl+A, Ctrl+V).

-- ==========================================================================================
-- SCRIPT 1 OF 6:  StarterPlayer > StarterPlayerScripts > LobbyClient
-- ==========================================================================================
-- The lobby menu: four tabs (Play, Inventory, Shop, Profile) over a blurred backdrop.
-- Play holds creating / joining a crew, and the party room with the 3D hideout, crew chat,
-- ready checks and the host's heist settings.
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local RunService = game:GetService("RunService")
local SocialService = game:GetService("SocialService")
local TextChatService = game:GetService("TextChatService")
local MarketplaceService = game:GetService("MarketplaceService")
local UserInputService = game:GetService("UserInputService")
local Lighting = game:GetService("Lighting")

if require(ReplicatedStorage:WaitForChild("GameMode")).get() ~= "Lobby" then
	return
end

local Config = require(ReplicatedStorage:WaitForChild("LobbyConfig"))
local SharedUI = ReplicatedStorage:WaitForChild("SharedUI")
local Kit = require(SharedUI:WaitForChild("Kit"))
local Overlays = require(SharedUI:WaitForChild("Overlays"))
local Hideout = require(script:WaitForChild("Hideout"))
local InventoryTab = require(script:WaitForChild("InventoryTab"))
local ShopTab = require(script:WaitForChild("ShopTab"))
local ProfileTab = require(script:WaitForChild("ProfileTab"))

local remotes = ReplicatedStorage:WaitForChild("LobbyRemotes")
local LobbyRequest = remotes:WaitForChild("LobbyRequest")
local LobbyUpdate = remotes:WaitForChild("LobbyUpdate")

local C = Kit.COLORS
local make, label, button, onClick = Kit.make, Kit.label, Kit.button, Kit.onClick
local fmt = Kit.formatNumber

local player = Players.LocalPlayer

-- Everything is laid out on one 1280x720 canvas that's scaled to fit the screen
local CANVAS = Vector2.new(1280, 720)
local CONTENT_TOP = 98
local CONTENT_SIZE = Vector2.new(1232, 600)
-- The party room's three columns
local COLUMNS = {
	{ x = 0, width = 400 },
	{ x = 416, width = 400 },
	{ x = 832, width = 400 },
}
local TABS = {
	{ id = "Play", icon = "🎮", text = "PLAY" },
	{ id = "Inventory", icon = "🎒", text = "INVENTORY" },
	{ id = "Shop", icon = "🛒", text = "SHOP" },
	{ id = "Profile", icon = "👤", text = "PROFILE" },
}
local TAB_WIDTH = 158
local MAP_CHOICES = { "Random" }
for _, map in ipairs(Config.MAPS) do
	table.insert(MAP_CHOICES, map.name)
end

---------------------------------------------------------------- state

local currentParty = nil
local profile = nil
local lastPublicList = {}

local function request(action, ...)
	local ok, result, extra = pcall(LobbyRequest.InvokeServer, LobbyRequest, action, ...)
	if not ok then
		Kit.toast("Couldn't reach the server.", true)
		return false
	end
	if result == false and extra == "NEEDS_MAP_PICKER" then
		if Config.MAP_PICKER_GAMEPASS_ID ~= 0 then
			MarketplaceService:PromptGamePassPurchase(player, Config.MAP_PICKER_GAMEPASS_ID)
		else
			Kit.toast("🔒 Picking a place needs the Map Picker pass (not on sale yet).", true)
		end
	elseif result == false and type(extra) == "string" then
		Kit.toast(extra, true)
	end
	return result, extra
end

local function isHost()
	return currentParty ~= nil and currentParty.hostUserId == player.UserId
end

local function cycle(list, current, delta)
	local index = table.find(list, current) or 1
	return list[(index - 1 + delta) % #list + 1]
end

---------------------------------------------------------------- backdrop and canvas

local gui = make("ScreenGui", {
	Name = "LobbyGui",
	ResetOnSpawn = false,
	IgnoreGuiInset = true,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
})
Kit.initToast(gui)

local blur = make("BlurEffect", { Name = "LobbyBlur", Size = 0, Parent = Lighting })

-- Studio: when the host starts, this test session turns into the heist. Clear the menu away.
if RunService:IsStudio() then
	workspace:GetAttributeChangedSignal("StudioTestMode"):Connect(function()
		if workspace:GetAttribute("StudioTestMode") == "Heist" then
			gui:Destroy()
			blur:Destroy()
		end
	end)
end
local backdrop = make("Frame", {
	Size = UDim2.fromScale(1, 1),
	BackgroundColor3 = Color3.new(1, 1, 1),
	BackgroundTransparency = 1,
	Parent = gui,
}, { Kit.gradient(Color3.fromRGB(24, 28, 46), C.backdrop) })
-- Soft colour glows behind the menu: robber orange, cop blue, a touch of purple
Kit.glow(backdrop, C.robber, UDim2.fromScale(0.1, 0.12), UDim2.fromScale(0.9, 0.9), 0.025)
Kit.glow(backdrop, C.cop, UDim2.fromScale(0.92, 0.95), UDim2.fromScale(1, 1), 0.03)
Kit.glow(backdrop, C.purple, UDim2.fromScale(0.62, 0.0), UDim2.fromScale(0.6, 0.6), 0.02)

local canvas = make("Frame", {
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Size = UDim2.fromOffset(CANVAS.X, CANVAS.Y),
	BackgroundTransparency = 1,
	Parent = gui,
})
local canvasScale = make("UIScale", { Parent = canvas })

-- `fit` shrinks the canvas onto small screens; `pop` is the little zoom when the menu opens
local fitScale, popScale = 1, 0.9
local popValue = make("NumberValue", { Value = popScale })
local function applyScale()
	canvasScale.Scale = fitScale * popScale
end
popValue.Changed:Connect(function(value)
	popScale = value
	applyScale()
end)
local function pop(from, info)
	popValue.Value = from
	Kit.tween(popValue, info or Kit.SMOOTH, { Value = 1 })
end

---------------------------------------------------------------- top bar

make("Frame", {
	Position = UDim2.fromOffset(24, 18),
	Size = UDim2.fromOffset(54, 54),
	BackgroundColor3 = Color3.new(1, 1, 1),
	Parent = canvas,
}, {
	Kit.corner(16),
	Kit.gradient(C.robber, C.robberDeep),
	Kit.stroke(C.gold, 0.5, 1.5),
	label({
		Text = "💰",
		TextSize = 28,
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.fromScale(1, 1),
	}),
})
local title = label({
	Text = "UNDERCOVER",
	Font = Kit.FONT_BLACK,
	TextSize = 24,
	Position = UDim2.fromOffset(90, 18),
	Size = UDim2.fromOffset(260, 26),
	Parent = canvas,
})
Kit.gradient(C.gold, C.robber).Parent = title
label({
	Text = "HEIST",
	Font = Kit.FONT_BLACK,
	TextSize = 24,
	TextColor3 = C.text,
	Position = UDim2.fromOffset(90, 44),
	Size = UDim2.fromOffset(260, 26),
	Parent = canvas,
})

-- Tab bar with a highlight that slides to the selected tab
local tabBar = make("Frame", {
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 18),
	Size = UDim2.fromOffset(#TABS * TAB_WIDTH + 8, 56),
	BackgroundColor3 = C.well,
	BackgroundTransparency = 0.1,
	Parent = canvas,
}, { Kit.round(), Kit.stroke(C.stroke, 0.35, 1.5) })
local tabHighlight = make("Frame", {
	Position = UDim2.fromOffset(4, 4),
	Size = UDim2.fromOffset(TAB_WIDTH, 48),
	BackgroundColor3 = Color3.new(1, 1, 1),
	Parent = tabBar,
}, { Kit.round(), Kit.gradient(C.robber, C.robberDeep), Kit.stroke(C.gold, 0.5, 1) })
local tabLabels = {}
local switchTab -- defined below
for i, tab in ipairs(TABS) do
	local tabButton = make("TextButton", {
		Text = "",
		AutoButtonColor = false,
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(4 + (i - 1) * TAB_WIDTH, 4),
		Size = UDim2.fromOffset(TAB_WIDTH, 48),
		Parent = tabBar,
	})
	tabLabels[tab.id] = label({
		Text = tab.icon .. "  " .. tab.text,
		Font = Kit.FONT_BLACK,
		TextSize = 15,
		TextColor3 = C.muted,
		TextTruncate = Enum.TextTruncate.None,
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.fromScale(1, 1),
		Parent = tabButton,
	})
	tabButton.MouseEnter:Connect(function()
		if tabLabels[tab.id].TextColor3 ~= C.text then
			Kit.tween(tabLabels[tab.id], Kit.FAST, { TextColor3 = Color3.fromRGB(205, 210, 228) })
		end
	end)
	tabButton.MouseLeave:Connect(function()
		if tabLabels[tab.id].TextColor3 ~= C.text then
			Kit.tween(tabLabels[tab.id], Kit.FAST, { TextColor3 = C.muted })
		end
	end)
	tabButton.Activated:Connect(function()
		switchTab(tab.id)
	end)
end
-- Q / E keycaps either side of the tab bar, for keyboard players
if UserInputService.KeyboardEnabled then
	for _, side in ipairs({ { "Q", -1 }, { "E", 1 } }) do
		make("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.new(0.5, side[2] * ((#TABS * TAB_WIDTH + 8) / 2 + 26), 0, 46),
			Size = UDim2.fromOffset(30, 30),
			BackgroundColor3 = C.well,
			Parent = canvas,
		}, {
			Kit.corner(8),
			Kit.stroke(C.stroke, 0.4),
			label({
				Text = side[1],
				Font = Kit.FONT_BLACK,
				TextSize = 13,
				TextColor3 = C.muted,
				TextXAlignment = Enum.TextXAlignment.Center,
				Size = UDim2.fromScale(1, 1),
			}),
		})
	end
end

-- Right side: help, your avatar (opens Profile) and your cash
local helpButton = Kit.makeRound(button("?", "ghost", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, -24, 0, 23),
	Size = UDim2.fromOffset(44, 44),
	Font = Kit.FONT_BLACK,
	TextSize = 18,
	Parent = canvas,
}))
local avatarButton = make("TextButton", {
	Text = "",
	AutoButtonColor = false,
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, -80, 0, 21),
	Size = UDim2.fromOffset(48, 48),
	Parent = canvas,
})
local topAvatar = Kit.avatar(player.UserId, player.DisplayName, 48, C.faint)
topAvatar.Parent = avatarButton
local topAvatarRing = topAvatar:FindFirstChildOfClass("UIStroke")
topAvatarRing.Thickness = 2.5
local cashPill = make("Frame", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, -142, 0, 25),
	Size = UDim2.fromOffset(0, 40),
	AutomaticSize = Enum.AutomaticSize.X,
	BackgroundColor3 = C.well,
	BackgroundTransparency = 0.1,
	Parent = canvas,
}, { Kit.round(), Kit.stroke(C.gold, 0.5), Kit.pad(0, 16) })
local cashText = label({
	Text = "💵  $0",
	Font = Kit.FONT_BLACK,
	TextSize = 16,
	TextColor3 = C.gold,
	TextTruncate = Enum.TextTruncate.None,
	Size = UDim2.fromScale(0, 1),
	AutomaticSize = Enum.AutomaticSize.X,
	Parent = cashPill,
})

make("Frame", {
	Position = UDim2.fromOffset(24, 86),
	Size = UDim2.fromOffset(CONTENT_SIZE.X, 1),
	BackgroundColor3 = Color3.new(1, 1, 1),
	BorderSizePixel = 0,
	Parent = canvas,
}, {
	make("UIGradient", {
		Color = ColorSequence.new(C.stroke),
		Transparency = NumberSequence.new({
			NumberSequenceKeypoint.new(0, 1),
			NumberSequenceKeypoint.new(0.5, 0.3),
			NumberSequenceKeypoint.new(1, 1),
		}),
	}),
})

-- One frame per tab; only the selected one is visible
local pagesHolder = make("Frame", {
	Position = UDim2.fromOffset(24, CONTENT_TOP),
	Size = UDim2.fromOffset(CONTENT_SIZE.X, CONTENT_SIZE.Y),
	BackgroundTransparency = 1,
	Parent = canvas,
})
local pages = {}
for _, tab in ipairs(TABS) do
	pages[tab.id] = make("Frame", {
		Name = tab.id,
		Size = UDim2.fromScale(1, 1),
		BackgroundTransparency = 1,
		Visible = false,
		Parent = pagesHolder,
	})
end

---------------------------------------------------------------- play: hub

local playPage = pages.Play
local hubView = make("Frame", { Size = UDim2.fromScale(1, 1), BackgroundTransparency = 1, Parent = playPage })

-- Hero banner with the main buttons
local hero = make("Frame", {
	Size = UDim2.new(1, 0, 0, 232),
	BackgroundColor3 = Color3.new(1, 1, 1),
	ClipsDescendants = true,
	Parent = hubView,
}, {
	Kit.corner(20),
	make("UIGradient", {
		Color = ColorSequence.new(Color3.fromRGB(70, 44, 112), Color3.fromRGB(20, 28, 56)),
		Rotation = 12,
	}),
	Kit.stroke(C.purple, 0.55, 1.5),
})
Kit.glow(hero, C.robber, UDim2.new(1, -230, 0.5, 0), UDim2.fromOffset(460, 460), 0.07)
Kit.glow(hero, C.cop, UDim2.new(1, -60, 1, 0), UDim2.fromOffset(300, 300), 0.06)
local heroMoney = label({
	Text = "💰",
	TextSize = 100,
	TextTruncate = Enum.TextTruncate.None,
	TextXAlignment = Enum.TextXAlignment.Center,
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.new(1, -230, 0.5, 0),
	Size = UDim2.fromOffset(150, 150),
	Parent = hero,
})
make("UIScale", { Scale = 1.5, Parent = heroMoney })
local heroSpy = label({
	Text = "🕵️",
	TextSize = 70,
	TextTruncate = Enum.TextTruncate.None,
	TextXAlignment = Enum.TextXAlignment.Center,
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.new(1, -90, 0.5, 40),
	Size = UDim2.fromOffset(100, 100),
	Rotation = 12,
	Parent = hero,
})
-- Gentle bobbing
RunService.RenderStepped:Connect(function()
	if not hubView.Visible or not playPage.Visible then
		return
	end
	local t = os.clock()
	heroMoney.Position = UDim2.new(1, -230, 0.5, math.sin(t * 1.4) * 8)
	heroMoney.Rotation = math.sin(t * 0.9) * 6
	heroSpy.Position = UDim2.new(1, -90, 0.5, 40 + math.sin(t * 1.7 + 1) * 6)
end)

label({
	Text = "🌙  TONIGHT'S JOB",
	Font = Kit.FONT_BLACK,
	TextSize = 13,
	TextColor3 = C.gold,
	Position = UDim2.fromOffset(34, 30),
	Size = UDim2.fromOffset(400, 16),
	Parent = hero,
})
label({
	Text = "READY FOR A HEIST?",
	Font = Kit.FONT_BLACK,
	TextSize = 46,
	TextTruncate = Enum.TextTruncate.None,
	Position = UDim2.fromOffset(34, 50),
	Size = UDim2.fromOffset(700, 54),
	Parent = hero,
})
label({
	Text = "Team up with your crew, crack the vault and get away. Careful: one of you is secretly a cop.",
	TextSize = 16,
	TextColor3 = Color3.fromRGB(205, 208, 230),
	TextWrapped = true,
	TextTruncate = Enum.TextTruncate.None,
	TextYAlignment = Enum.TextYAlignment.Top,
	Position = UDim2.fromOffset(34, 108),
	Size = UDim2.fromOffset(640, 44),
	Parent = hero,
})
local createButton = button("Create party", "primary", {
	Position = UDim2.fromOffset(34, 160),
	Size = UDim2.fromOffset(210, 52),
	Font = Kit.FONT_BLACK,
	TextSize = 18,
	Parent = hero,
})
local quickJoinButton = button("⚡  Quick join", "purple", {
	Position = UDim2.fromOffset(256, 160),
	Size = UDim2.fromOffset(180, 52),
	Font = Kit.FONT_BLACK,
	TextSize = 16,
	Parent = hero,
})
local codeField = make("Frame", {
	Position = UDim2.fromOffset(452, 160),
	Size = UDim2.fromOffset(170, 52),
	BackgroundColor3 = C.well,
	BackgroundTransparency = 0.1,
	Parent = hero,
}, { Kit.corner(12) })
local codeFieldStroke = Kit.stroke(C.stroke, 0.2, 1.5)
codeFieldStroke.Parent = codeField
local codeBox = make("TextBox", {
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Font = Kit.FONT_CODE,
	TextSize = 22,
	TextColor3 = C.gold,
	PlaceholderText = "CODE",
	PlaceholderColor3 = C.faint,
	Text = "",
	ClearTextOnFocus = false,
	Parent = codeField,
})
local joinCodeButton = button("Join", "blue", {
	Position = UDim2.fromOffset(630, 160),
	Size = UDim2.fromOffset(96, 52),
	Font = Kit.FONT_BLACK,
	TextSize = 16,
	Parent = hero,
})

-- Public crews
local publicCard = Kit.card({
	Position = UDim2.fromOffset(0, 248),
	Size = UDim2.fromOffset(808, 352),
	Parent = hubView,
})
Kit.pad(18).Parent = publicCard
Kit.sectionTitle("PUBLIC CREWS", { Size = UDim2.new(1, -100, 0, 30), Parent = publicCard })
local refreshButton = button("🔄  Refresh", "ghost", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.fromScale(1, 0),
	Size = UDim2.fromOffset(100, 30),
	TextSize = 13,
	Parent = publicCard,
})
local publicList = make("ScrollingFrame", {
	BackgroundTransparency = 1,
	BorderSizePixel = 0,
	Position = UDim2.fromOffset(0, 42),
	Size = UDim2.new(1, 0, 1, -42),
	CanvasSize = UDim2.new(),
	AutomaticCanvasSize = Enum.AutomaticSize.Y,
	ScrollingDirection = Enum.ScrollingDirection.Y,
	ScrollBarThickness = 4,
	ScrollBarImageColor3 = C.faint,
	Parent = publicCard,
}, {
	make("UIGridLayout", {
		CellSize = UDim2.new(0.5, -10, 0, 86),
		CellPadding = UDim2.fromOffset(12, 12),
		SortOrder = Enum.SortOrder.LayoutOrder,
	}),
})
local publicEmpty = make("Frame", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(0, 42),
	Size = UDim2.new(1, 0, 1, -42),
	Parent = publicCard,
})
label({
	Text = "🕵️",
	TextSize = 44,
	TextTruncate = Enum.TextTruncate.None,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(0, 50),
	Size = UDim2.new(1, 0, 0, 54),
	Parent = publicEmpty,
})
label({
	Text = "No public crews right now",
	Font = Kit.FONT_BLACK,
	TextSize = 17,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(0, 112),
	Size = UDim2.new(1, 0, 0, 22),
	Parent = publicEmpty,
})
label({
	Text = "Create one, or hit Quick join and we'll start one for you.",
	TextSize = 14,
	TextColor3 = C.muted,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(0, 138),
	Size = UDim2.new(1, 0, 0, 18),
	Parent = publicEmpty,
})

-- Places to rob
local placesCard = Kit.card({
	Position = UDim2.fromOffset(824, 248),
	Size = UDim2.fromOffset(408, 352),
	Parent = hubView,
})
Kit.pad(18).Parent = placesCard
Kit.sectionTitle("PLACES TO ROB", { Parent = placesCard })
local placesList = make("Frame", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(0, 28),
	Size = UDim2.new(1, 0, 1, -28),
	Parent = placesCard,
}, { Kit.listLayout(10) })
for i, map in ipairs(Config.MAPS) do
	local row = make("Frame", {
		BackgroundColor3 = C.cardRaised,
		Size = UDim2.new(1, 0, 0, 66),
		LayoutOrder = i,
		Parent = placesList,
	}, { Kit.corner(12), Kit.pad(10, 12), Kit.stroke(map.color, map.available and 0.35 or 0.75, 1.5) })
	make("Frame", {
		AnchorPoint = Vector2.new(0, 0.5),
		Position = UDim2.fromScale(0, 0.5),
		Size = UDim2.fromOffset(46, 46),
		BackgroundColor3 = C.well,
		Parent = row,
	}, {
		Kit.round(),
		label({
			Text = map.icon,
			TextSize = 24,
			TextTruncate = Enum.TextTruncate.None,
			TextXAlignment = Enum.TextXAlignment.Center,
			TextTransparency = map.available and 0 or 0.5,
			Size = UDim2.fromScale(1, 1),
		}),
	})
	label({
		Text = map.name,
		Font = Kit.FONT_BLACK,
		TextSize = 15,
		TextColor3 = map.available and C.text or C.muted,
		Position = UDim2.fromOffset(58, 4),
		Size = UDim2.new(1, -150, 0, 20),
		Parent = row,
	})
	label({
		RichText = true,
		Text = ('<font color="%s">%s</font><font color="%s">%s</font>   <font color="%s">%s</font>'):format(
			Kit.hex(C.robber),
			string.rep("●", map.difficulty),
			Kit.hex(C.faint),
			string.rep("●", 5 - map.difficulty),
			Kit.hex(C.gold),
			string.rep("$", map.loot)
		),
		Font = Kit.FONT_BOLD,
		TextSize = 12,
		Position = UDim2.fromOffset(58, 26),
		Size = UDim2.new(1, -150, 0, 16),
		Parent = row,
	})
	Kit.pill(map.available and "OPEN" or "🔒 SOON", map.available and C.good or C.muted, {
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.fromScale(1, 0.5),
		Parent = row,
	})
end

---------------------------------------------------------------- play: party room

local partyView = make("Frame", {
	Size = UDim2.fromScale(1, 1),
	BackgroundTransparency = 1,
	Visible = false,
	Parent = playPage,
})

-- Top row: code, invite, host
local codeCard = Kit.card({ Size = UDim2.fromOffset(400, 60), Parent = partyView })
Kit.pad(9, 16).Parent = codeCard
Kit.sectionTitle("PARTY CODE  ·  SHARE WITH FRIENDS", { TextSize = 10, Size = UDim2.new(1, -70, 0, 12), Parent = codeCard })
local codeChips = make("Frame", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(0, 16),
	Size = UDim2.new(1, -70, 0, 26),
	Parent = codeCard,
}, { Kit.listLayout(6, true) })
local chips = {}
for i = 1, Config.CODE_LENGTH do
	chips[i] = label({
		Font = Kit.FONT_CODE,
		TextSize = 19,
		TextColor3 = C.gold,
		TextXAlignment = Enum.TextXAlignment.Center,
		BackgroundTransparency = 0,
		BackgroundColor3 = C.well,
		Size = UDim2.fromOffset(30, 26),
		LayoutOrder = i,
		Parent = codeChips,
	})
	Kit.corner(6).Parent = chips[i]
end
local newCodeButton = button("New", "ghost", {
	AnchorPoint = Vector2.new(1, 0.5),
	Position = UDim2.fromScale(1, 0.5),
	Size = UDim2.fromOffset(60, 32),
	TextSize = 13,
	Parent = codeCard,
})

local inviteButton = button("💌  Invite friends", "blue", {
	Position = UDim2.fromOffset(COLUMNS[2].x, 0),
	Size = UDim2.fromOffset(230, 60),
	Font = Kit.FONT_BLACK,
	TextSize = 16,
	Parent = partyView,
})

local hostPill = make("Frame", {
	AnchorPoint = Vector2.new(1, 0.5),
	Position = UDim2.new(1, 0, 0, 30),
	Size = UDim2.fromOffset(0, 40),
	AutomaticSize = Enum.AutomaticSize.X,
	BackgroundColor3 = C.card,
	Parent = partyView,
}, { Kit.round(), Kit.stroke(C.gold, 0.55), Kit.pad(0, 18) })
local hostText = label({
	Font = Kit.FONT_BOLD,
	TextSize = 14,
	TextColor3 = C.gold,
	TextTruncate = Enum.TextTruncate.None,
	Size = UDim2.fromScale(0, 1),
	AutomaticSize = Enum.AutomaticSize.X,
	Parent = hostPill,
})

-- 3D hideout banner
local banner = make("Frame", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(0, 72),
	Size = UDim2.new(1, 0, 0, 210),
	Parent = partyView,
})
local hideout = Hideout.new(banner)
make("Frame", {
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Parent = banner,
}, { Kit.corner(14), Kit.stroke(C.stroke, 0.35, 1.5) })
label({
	Text = "THE HIDEOUT",
	Font = Kit.FONT_BLACK,
	TextSize = 12,
	TextTransparency = 0.2,
	TextStrokeTransparency = 0.6,
	Position = UDim2.fromOffset(16, 12),
	Size = UDim2.fromOffset(200, 14),
	Parent = banner,
})
local targetPill = make("Frame", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, -12, 0, 10),
	Size = UDim2.fromOffset(0, 30),
	AutomaticSize = Enum.AutomaticSize.X,
	BackgroundColor3 = C.well,
	BackgroundTransparency = 0.15,
	Parent = banner,
}, { Kit.round(), Kit.stroke(C.stroke, 0.4), Kit.pad(0, 14) })
local targetText = label({
	Font = Kit.FONT_BOLD,
	TextSize = 13,
	TextTruncate = Enum.TextTruncate.None,
	Size = UDim2.fromScale(0, 1),
	AutomaticSize = Enum.AutomaticSize.X,
	Parent = targetPill,
})

local ROW_TOP = 294
local ROW_HEIGHT = CONTENT_SIZE.Y - ROW_TOP - 64

local function columnCard(index, scrolling)
	local col = COLUMNS[index]
	return Kit.card({
		Position = UDim2.fromOffset(col.x, ROW_TOP),
		Size = UDim2.fromOffset(col.width, ROW_HEIGHT),
		Parent = partyView,
	}, scrolling)
end

-- Crew list
local crewCard = columnCard(1)
Kit.pad(14).Parent = crewCard
local crewTitle = Kit.sectionTitle("", { Parent = crewCard })
local membersList = Kit.scroller({
	Position = UDim2.fromOffset(0, 24),
	Size = UDim2.new(1, 0, 1, -24),
	Parent = crewCard,
}, 6)

-- Crew chat
local chatCard = columnCard(2)
Kit.pad(14).Parent = chatCard
Kit.sectionTitle("CREW CHAT", { Parent = chatCard })
local chatList = Kit.scroller({
	Position = UDim2.fromOffset(0, 24),
	Size = UDim2.new(1, 0, 1, -70),
	Parent = chatCard,
}, 4)
local chatEmpty = label({
	Text = "👋  Say hi and plan the job.",
	TextSize = 13,
	TextColor3 = C.faint,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(0, 24),
	Size = UDim2.new(1, 0, 1, -70),
	Parent = chatCard,
})
local chatField = make("Frame", {
	AnchorPoint = Vector2.new(0, 1),
	Position = UDim2.fromScale(0, 1),
	Size = UDim2.new(1, -74, 0, 38),
	BackgroundColor3 = C.well,
	Parent = chatCard,
}, { Kit.corner(10), Kit.stroke(C.stroke, 0.4) })
local chatBox = make("TextBox", {
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Font = Kit.FONT,
	TextSize = 14,
	TextColor3 = C.text,
	TextXAlignment = Enum.TextXAlignment.Left,
	TextTruncate = Enum.TextTruncate.AtEnd,
	PlaceholderText = "Message your crew...",
	PlaceholderColor3 = C.faint,
	Text = "",
	ClearTextOnFocus = false,
	Parent = chatField,
}, { Kit.pad(0, 12) })
local sendButton = button("Send", "blue", {
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.fromScale(1, 1),
	Size = UDim2.fromOffset(66, 38),
	TextSize = 14,
	Parent = chatCard,
})

-- Heist settings
local settingsCard = columnCard(3, true)
Kit.pad(14).Parent = settingsCard
Kit.listLayout(10).Parent = settingsCard
local settingsTitle = Kit.sectionTitle("", { LayoutOrder = 0, Parent = settingsCard })

local function settingRow(order, text)
	local row = make("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.new(1, -6, 0, 32),
		LayoutOrder = order,
		Parent = settingsCard,
	})
	label({ Text = text, TextSize = 13, Size = UDim2.new(1, -166, 1, 0), Parent = row })
	return row
end

local function control(row)
	return make("Frame", {
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.fromScale(1, 0.5),
		Size = UDim2.fromOffset(160, 32),
		BackgroundColor3 = C.well,
		Parent = row,
	}, { Kit.round(), Kit.stroke(C.stroke, 0.5) })
end

local function smallRoundButton(text, rightSide, parent)
	return Kit.makeRound(button(text, "ghost", {
		AnchorPoint = Vector2.new(rightSide and 1 or 0, 0),
		Position = rightSide and UDim2.new(1, -2, 0, 2) or UDim2.fromOffset(2, 2),
		Size = UDim2.fromOffset(28, 28),
		TextSize = 16,
		Parent = parent,
	}))
end

local function step(key, delta)
	if isHost() then
		request("UpdateSetting", key, currentParty.settings[key] + delta)
	end
end

-- ( - ) value ( + )
local function stepper(order, text, key)
	local box = control(settingRow(order, text))
	local minus = smallRoundButton("-", false, box)
	local value = label({
		Font = Kit.FONT_BLACK,
		TextSize = 16,
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.fromScale(1, 1),
		Parent = box,
	})
	local plus = smallRoundButton("+", true, box)
	onClick(minus, function()
		step(key, -1)
	end)
	onClick(plus, function()
		step(key, 1)
	end)
	return { minus = minus, value = value, plus = plus }
end

-- ( < ) value ( > ), for picking from a list
local function selector(order, text, onStep)
	local box = control(settingRow(order, text))
	local prev = smallRoundButton("<", false, box)
	local valueLabel = label({
		Font = Kit.FONT_BOLD,
		TextSize = 13,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(32, 0),
		Size = UDim2.new(1, -64, 1, 0),
		Parent = box,
	})
	local nextButton = smallRoundButton(">", true, box)
	onClick(prev, function()
		onStep(-1)
	end)
	onClick(nextButton, function()
		onStep(1)
	end)
	return function(value, editable)
		valueLabel.Text = value
		prev.Visible = editable
		nextButton.Visible = editable
	end
end

-- Two-choice pill switch
local function segmented(order, text, options, onPick)
	local box = control(settingRow(order, text))
	local segments = {}
	for i, option in ipairs(options) do
		local segment = make("TextButton", {
			Text = option.text,
			Font = Kit.FONT_BOLD,
			TextSize = 12,
			TextColor3 = C.muted,
			AutoButtonColor = false,
			BackgroundColor3 = C.cop,
			BackgroundTransparency = 1,
			Position = UDim2.new((i - 1) / #options, i == 1 and 2 or 0, 0, 2),
			Size = UDim2.new(1 / #options, -2, 1, -4),
			Parent = box,
		}, { Kit.round() })
		segment.Activated:Connect(function()
			if isHost() then
				onPick(option.value)
			end
		end)
		segments[i] = { button = segment, value = option.value }
	end
	return function(selected)
		for _, s in ipairs(segments) do
			local on = s.value == selected
			s.button.BackgroundTransparency = on and 0 or 1
			s.button.TextColor3 = on and C.text or C.muted
		end
	end
end

local function setSetting(key, value)
	if isHost() and currentParty.settings[key] ~= value then
		request("UpdateSetting", key, value)
	end
end

local sizeStepper = stepper(1, "Max players", "partySize")
local copsStepper = stepper(2, "Undercover cops", "copCount")

-- Robber / cop split bar
local splitRow = make("Frame", {
	BackgroundTransparency = 1,
	Size = UDim2.new(1, -6, 0, 30),
	LayoutOrder = 3,
	Parent = settingsCard,
})
local splitBar = make("Frame", {
	Size = UDim2.new(1, 0, 0, 8),
	BackgroundColor3 = Color3.new(1, 1, 1),
	Parent = splitRow,
}, { Kit.round(), Kit.gradient(C.cop, C.copDeep, 0) })
local robberFill = make("Frame", {
	Size = UDim2.fromScale(0.75, 1),
	BackgroundColor3 = Color3.new(1, 1, 1),
	Parent = splitBar,
}, { Kit.round(), Kit.gradient(C.robberDeep, C.robber, 0) })
local splitText = label({
	RichText = true,
	Font = Kit.FONT_BOLD,
	TextSize = 12,
	TextColor3 = C.muted,
	Position = UDim2.fromOffset(0, 12),
	Size = UDim2.new(1, 0, 0, 18),
	Parent = splitRow,
})

local setPrivacy = segmented(4, "Lobby", {
	{ text = "Public", value = false },
	{ text = "🔒 Private", value = true },
}, function(value)
	setSetting("isPrivate", value)
end)

local setPlace = selector(5, "Place to rob", function(delta)
	if isHost() then
		request("UpdateSetting", "map", cycle(MAP_CHOICES, currentParty.settings.map, delta))
	end
end)

-- Preview of the chosen place
local mapCard = make("Frame", {
	Size = UDim2.new(1, -6, 0, 72),
	BackgroundColor3 = C.cardRaised,
	LayoutOrder = 6,
	Parent = settingsCard,
}, { Kit.corner(10), Kit.pad(8, 10) })
local mapCardStroke = Kit.stroke(C.stroke, 0.3, 1.5)
mapCardStroke.Parent = mapCard
local mapIconWell = make("Frame", {
	AnchorPoint = Vector2.new(0, 0.5),
	Position = UDim2.fromScale(0, 0.5),
	Size = UDim2.fromOffset(46, 46),
	BackgroundColor3 = C.well,
	Parent = mapCard,
}, { Kit.round() })
local mapIcon = label({
	TextSize = 26,
	TextTruncate = Enum.TextTruncate.None,
	TextXAlignment = Enum.TextXAlignment.Center,
	Size = UDim2.fromScale(1, 1),
	Parent = mapIconWell,
})
local mapName = label({
	Font = Kit.FONT_BOLD,
	TextSize = 14,
	Position = UDim2.fromOffset(56, 0),
	Size = UDim2.new(1, -56, 0, 18),
	Parent = mapCard,
})
local mapMeta = label({
	RichText = true,
	Font = Kit.FONT_BOLD,
	TextSize = 11,
	TextColor3 = C.muted,
	Position = UDim2.fromOffset(56, 18),
	Size = UDim2.new(1, -56, 0, 14),
	Parent = mapCard,
})
local mapBlurb = label({
	TextSize = 11,
	TextColor3 = C.muted,
	TextWrapped = true,
	TextTruncate = Enum.TextTruncate.None,
	TextYAlignment = Enum.TextYAlignment.Top,
	Position = UDim2.fromOffset(56, 34),
	Size = UDim2.new(1, -56, 0, 24),
	Parent = mapCard,
})

local setDifficulty = selector(7, "Difficulty", function(delta)
	if isHost() then
		request("UpdateSetting", "difficulty", cycle(Config.DIFFICULTIES, currentParty.settings.difficulty, delta))
	end
end)
local setLength = selector(8, "Mission length", function(delta)
	if isHost() then
		request("UpdateSetting", "length", cycle(Config.LENGTHS, currentParty.settings.length, delta))
	end
end)
local setCopsKnow = segmented(9, "Cops know each other", {
	{ text = "Yes", value = true },
	{ text = "No", value = false },
}, function(value)
	setSetting("copsKnowEachOther", value)
end)
label({
	Text = "🎲 Random places are free. Picking one costs Robux.",
	TextSize = 11,
	TextColor3 = C.faint,
	TextWrapped = true,
	TextTruncate = Enum.TextTruncate.None,
	Size = UDim2.new(1, -6, 0, 16),
	LayoutOrder = 10,
	Parent = settingsCard,
})

-- Footer
local leaveButton = button("Leave", "danger", {
	AnchorPoint = Vector2.new(0, 1),
	Position = UDim2.fromScale(0, 1),
	Size = UDim2.fromOffset(140, 52),
	Font = Kit.FONT_BLACK,
	TextSize = 16,
	Parent = partyView,
})
local function footerButton(text, style)
	return button(text, style, {
		AnchorPoint = Vector2.new(1, 1),
		Position = UDim2.fromScale(1, 1),
		Size = UDim2.fromOffset(260, 52),
		Font = Kit.FONT_BLACK,
		TextSize = 19,
		Visible = false,
		Parent = partyView,
	})
end
local startButton = footerButton("START HEIST", "green")
local cancelButton = footerButton("CANCEL START", "danger")
local readyButton = footerButton("I'M READY", "green")
local unreadyButton = footerButton("✔ READY  ·  undo", "ghost")
unreadyButton.TextColor3 = C.good
unreadyButton.TextSize = 16
local footerHint = label({
	RichText = true,
	TextSize = 14,
	TextColor3 = C.muted,
	TextXAlignment = Enum.TextXAlignment.Right,
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.new(1, -276, 1, 0),
	Size = UDim2.new(1, -432, 0, 52),
	Parent = partyView,
})

---------------------------------------------------------------- other tabs

Overlays.init(gui, canvas)
local tabModules = {}
local ctx = {
	request = request,
	switchTab = function(id)
		switchTab(id)
	end,
}
tabModules.Inventory = InventoryTab.new(pages.Inventory, ctx)
tabModules.Shop = ShopTab.new(pages.Shop, ctx)
tabModules.Profile = ProfileTab.new(pages.Profile)

local currentTab = nil
local function tabIndex(id)
	for i, tab in ipairs(TABS) do
		if tab.id == id then
			return i
		end
	end
	return 1
end

-- Pages slide in from the side they're on in the tab bar
function switchTab(id)
	if id == currentTab then
		return
	end
	local fromIndex = currentTab and tabIndex(currentTab) or 0
	local toIndex = tabIndex(id)
	if currentTab then
		pages[currentTab].Visible = false
	end
	currentTab = id

	local page = pages[id]
	page.Visible = true
	page.Position = UDim2.fromOffset(toIndex > fromIndex and 50 or -50, 0)
	Kit.tween(page, Kit.SMOOTH, { Position = UDim2.new() })
	Kit.tween(tabHighlight, TweenInfo.new(0.35, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {
		Position = UDim2.fromOffset(4 + (toIndex - 1) * TAB_WIDTH, 4),
	})
	for tabId, tabLabel in pairs(tabLabels) do
		tabLabel.TextColor3 = tabId == id and C.text or C.muted
	end
	local module = tabModules[id]
	if module and module.onShown then
		module:onShown()
	end
end

UserInputService.InputBegan:Connect(function(input, processed)
	if processed or not gui.Enabled then
		return
	end
	if input.KeyCode == Enum.KeyCode.Q or input.KeyCode == Enum.KeyCode.E then
		local delta = input.KeyCode == Enum.KeyCode.Q and -1 or 1
		local index = math.clamp(tabIndex(currentTab) + delta, 1, #TABS)
		switchTab(TABS[index].id)
	end
end)

---------------------------------------------------------------- rendering

local function renderProfile()
	if not profile then
		return
	end
	local rank = Config.rankFor(profile.xp)
	cashText.Text = "💵  $" .. fmt(profile.cash)
	topAvatarRing.Color = rank.color
	for _, module in pairs(tabModules) do
		module:update(profile)
	end
end

local function renderPublicList(list)
	Kit.clearRows(publicList)
	publicEmpty.Visible = #list == 0
	for i, entry in ipairs(list) do
		local row = make("Frame", {
			BackgroundColor3 = C.cardRaised,
			LayoutOrder = i,
			Parent = publicList,
		}, { Kit.corner(12), Kit.pad(12, 14), Kit.stroke(C.stroke, 0.5) })
		local face = Kit.avatar(entry.hostUserId, entry.hostName, 48, C.gold)
		face.AnchorPoint = Vector2.new(0, 0.5)
		face.Position = UDim2.fromScale(0, 0.5)
		face.Parent = row
		label({
			Text = entry.hostName .. "'s crew",
			Font = Kit.FONT_BLACK,
			TextSize = 15,
			Position = UDim2.fromOffset(60, 0),
			Size = UDim2.new(1, -170, 0, 20),
			Parent = row,
		})
		local map = Config.getMap(entry.map)
		label({
			Text = ("%d cop%s  ·  %s"):format(
				entry.cops,
				entry.cops == 1 and "" or "s",
				map and (map.icon .. " " .. map.name) or "🎲 Random"
			),
			TextSize = 12,
			TextColor3 = C.muted,
			Position = UDim2.fromOffset(60, 22),
			Size = UDim2.new(1, -170, 0, 16),
			Parent = row,
		})
		-- How full the crew is
		local track = make("Frame", {
			Position = UDim2.fromOffset(60, 46),
			Size = UDim2.new(1, -230, 0, 6),
			BackgroundColor3 = C.well,
			Parent = row,
		}, { Kit.round() })
		make("Frame", {
			Size = UDim2.fromScale(entry.count / entry.partySize, 1),
			BackgroundColor3 = entry.count >= entry.partySize and C.bad or C.good,
			Parent = track,
		}, { Kit.round() })
		label({
			Text = entry.count .. "/" .. entry.partySize,
			Font = Kit.FONT_BLACK,
			TextSize = 12,
			TextXAlignment = Enum.TextXAlignment.Right,
			AnchorPoint = Vector2.new(1, 0),
			Position = UDim2.new(1, -110, 0, 40),
			Size = UDim2.fromOffset(50, 16),
			Parent = row,
		})

		local full = entry.count >= entry.partySize
		local joined = currentParty ~= nil and currentParty.id == entry.id
		local canJoin = not full and not joined
		local join = button(joined and "Joined" or full and "Full" or "Join", canJoin and "blue" or "ghost", {
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.fromScale(1, 0.5),
			Size = UDim2.fromOffset(92, 40),
			Font = Kit.FONT_BLACK,
			TextSize = 14,
			Parent = row,
		})
		if canJoin then
			onClick(join, function()
				request("JoinPublic", entry.id)
			end)
		else
			Kit.setEnabled(join, false)
		end
	end
end

local function memberRow(member, order, amHost)
	local row = make("Frame", {
		BackgroundColor3 = C.cardRaised,
		Size = UDim2.new(1, -6, 0, 48),
		LayoutOrder = order,
		Parent = membersList,
	}, { Kit.corner(10), Kit.pad(7, 8) })
	local ring = member.isHost and C.gold or member.isReady and C.good or nil
	local face = Kit.avatar(member.userId, member.displayName, 34, ring)
	face.AnchorPoint = Vector2.new(0, 0.5)
	face.Position = UDim2.fromScale(0, 0.5)
	face.Parent = row

	local isYou = member.userId == player.UserId
	local rightSpace = member.isHost and 72 or amHost and 96 or 6
	label({
		Text = (member.mask and member.mask .. " " or "") .. member.displayName .. (isYou and "  (you)" or ""),
		Font = Kit.FONT_BOLD,
		TextSize = 14,
		TextYAlignment = Enum.TextYAlignment.Bottom,
		Position = UDim2.fromOffset(44, 0),
		Size = UDim2.new(1, -(44 + rightSpace), 0.5, 0),
		Parent = row,
	})
	local rank = Config.rankFor(member.xp or 0)
	local status
	if member.isHost then
		status = "Host"
	elseif member.isReady then
		status = ('<font color="%s">Ready</font>'):format(Kit.hex(C.good))
	else
		status = "Not ready"
	end
	label({
		RichText = true,
		Text = ('<font color="%s">%s %s</font>  ·  %s'):format(
			Kit.hex(rank.color),
			Config.RANK_ICONS[rank.name] or "",
			rank.name,
			status
		),
		TextSize = 11,
		TextColor3 = C.muted,
		TextYAlignment = Enum.TextYAlignment.Top,
		Position = UDim2.new(0, 44, 0.5, 2),
		Size = UDim2.new(1, -(44 + rightSpace), 0.5, -2),
		Parent = row,
	})

	if member.isHost then
		Kit.pill("👑 HOST", C.gold, {
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.fromScale(1, 0.5),
			Parent = row,
		})
	elseif amHost then
		local kick = button("Kick", "danger", {
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.fromScale(1, 0.5),
			Size = UDim2.fromOffset(52, 30),
			TextSize = 12,
			Parent = row,
		})
		onClick(kick, function()
			request("Kick", member.userId)
		end)
		local makeHost = button("👑", "ghost", {
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.new(1, -58, 0.5, 0),
			Size = UDim2.fromOffset(34, 30),
			TextSize = 13,
			Parent = row,
		})
		onClick(makeHost, function()
			if request("TransferHost", member.userId) then
				Kit.toast(member.displayName .. " is the host now.")
			end
		end)
	end
end

local function emptySlot(order)
	local slot = make("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.new(1, -6, 0, 48),
		LayoutOrder = order,
		Parent = membersList,
	}, { Kit.corner(10), Kit.stroke(C.faint, 0.65) })
	make("Frame", {
		AnchorPoint = Vector2.new(0, 0.5),
		Position = UDim2.new(0, 8, 0.5, 0),
		Size = UDim2.fromOffset(34, 34),
		BackgroundColor3 = C.well,
		Parent = slot,
	}, {
		Kit.round(),
		Kit.stroke(C.faint, 0.6),
		label({
			Text = "+",
			Font = Kit.FONT_BOLD,
			TextSize = 16,
			TextColor3 = C.faint,
			TextXAlignment = Enum.TextXAlignment.Center,
			Size = UDim2.fromScale(1, 1),
		}),
	})
	label({
		Text = "Open slot",
		TextSize = 13,
		TextColor3 = C.faint,
		Position = UDim2.fromOffset(52, 0),
		Size = UDim2.new(1, -60, 1, 0),
		Parent = slot,
	})
end

local function setStepButton(b, visible, enabled)
	b.Visible = visible
	Kit.setEnabled(b, enabled)
end

local function renderSettings(host, settings, members)
	settingsTitle.Text = host and "HEIST SETTINGS" or "HEIST SETTINGS  ·  HOST ONLY"
	local maxCops = Config.maxCopsFor(settings.partySize)
	sizeStepper.value.Text = tostring(settings.partySize)
	copsStepper.value.Text = tostring(settings.copCount)
	setStepButton(sizeStepper.minus, host, settings.partySize > math.max(Config.MIN_PARTY_SIZE, #members))
	setStepButton(sizeStepper.plus, host, settings.partySize < Config.MAX_PARTY_SIZE)
	setStepButton(copsStepper.minus, host, settings.copCount > 1)
	setStepButton(copsStepper.plus, host, settings.copCount < maxCops)

	local robbers = settings.partySize - settings.copCount
	Kit.tween(robberFill, Kit.SMOOTH, { Size = UDim2.fromScale(robbers / settings.partySize, 1) })
	splitText.Text = ('<font color="%s">%d robber%s</font>   vs   <font color="%s">%d undercover cop%s</font>'):format(
		Kit.hex(C.robber),
		robbers,
		robbers == 1 and "" or "s",
		Kit.hex(C.cop),
		settings.copCount,
		settings.copCount == 1 and "" or "s"
	)

	setPrivacy(settings.isPrivate)
	setPlace(settings.map == "Random" and "🎲 Random" or settings.map, host)
	setDifficulty(settings.difficulty, host)
	setLength(("%s (%d min)"):format(settings.length, Config.LENGTH_MINUTES[settings.length]), host)
	setCopsKnow(settings.copsKnowEachOther)

	local map = Config.getMap(settings.map)
	if map then
		mapIcon.Text = map.icon
		mapName.Text = map.name
		mapBlurb.Text = map.blurb
		mapMeta.Text = ('Difficulty <font color="%s">%s</font>%s   Loot <font color="%s">%s</font>'):format(
			Kit.hex(C.robber),
			string.rep("●", map.difficulty),
			string.rep("○", 5 - map.difficulty),
			Kit.hex(C.gold),
			string.rep("$", map.loot)
		)
		mapCardStroke.Color = map.color
		targetText.Text = "🎯  Target: " .. map.icon .. " " .. map.name
	else
		mapIcon.Text = "❓"
		mapName.Text = "Random place"
		mapBlurb.Text = "A surprise target, picked when the heist starts."
		mapMeta.Text = ('Difficulty <font color="%s">???</font>   Loot <font color="%s">???</font>'):format(
			Kit.hex(C.robber),
			Kit.hex(C.gold)
		)
		mapCardStroke.Color = C.stroke
		targetText.Text = "🎯  Target: 🎲 Random"
	end
end

local function renderFooter(host, settings, members)
	local starting = currentParty.starting
	local me
	local notReady = {}
	for _, member in ipairs(members) do
		if member.userId == player.UserId then
			me = member
		end
		if not member.isReady then
			table.insert(notReady, member.displayName)
		end
	end
	local meReady = me ~= nil and me.isReady
	-- Studio tests can start with any crew size, even alone
	local studio = RunService:IsStudio()
	local missing = studio and 0 or Config.MIN_PLAYERS_TO_START - #members
	local tooManyCops = not studio and settings.copCount >= #members - settings.copCount

	startButton.Visible = host and not starting
	cancelButton.Visible = host and starting
	readyButton.Visible = not host and not meReady
	unreadyButton.Visible = not host and meReady
	Kit.setEnabled(startButton, missing <= 0 and #notReady == 0 and not tooManyCops)

	local hint
	if starting then
		hint = "Starting the heist..."
	elseif missing > 0 then
		hint = ("Need %d more player%s to start"):format(missing, missing == 1 and "" or "s")
	elseif tooManyCops then
		hint = "Too many cops for this crew. Lower the cop count."
	elseif #notReady > 0 then
		hint = "Waiting for: " .. table.concat(notReady, ", ")
	else
		hint = ('<font color="%s">Everyone is ready!</font>'):format(Kit.hex(C.good))
			.. (host and "" or "  Waiting for the host...")
	end
	footerHint.Text = hint
end

---------------------------------------------------------------- crew chat

local chatChannelName, chatChannel, chatConnection
local chatCount = 0

local function addChatLine(richText)
	chatCount += 1
	chatEmpty.Visible = false
	label({
		RichText = true,
		Text = richText,
		TextSize = 13,
		TextWrapped = true,
		TextTruncate = Enum.TextTruncate.None,
		TextYAlignment = Enum.TextYAlignment.Top,
		AutomaticSize = Enum.AutomaticSize.Y,
		Size = UDim2.new(1, -8, 0, 0),
		LayoutOrder = chatCount,
		Parent = chatList,
	})

	-- Keep the newest 60 lines
	local lines = {}
	for _, child in ipairs(chatList:GetChildren()) do
		if child:IsA("TextLabel") then
			table.insert(lines, child)
		end
	end
	if #lines > 60 then
		table.sort(lines, function(a, b)
			return a.LayoutOrder < b.LayoutOrder
		end)
		for i = 1, #lines - 60 do
			lines[i]:Destroy()
		end
	end
	task.delay(0.05, function()
		chatList.CanvasPosition = Vector2.new(0, 1e6)
	end)
end

local function addSystemLine(text)
	addChatLine(('<i><font color="%s">%s</font></i>'):format(Kit.hex(C.faint), Kit.escape(text)))
end

-- TextChatMessage.Text arrives already escaped for rich text; undo that so it's escaped once
local function unescape(text)
	return (
		text:gsub("&lt;", "<")
			:gsub("&gt;", ">")
			:gsub("&quot;", '"')
			:gsub("&apos;", "'")
			:gsub("&amp;", "&")
	)
end

local function onChatMessage(message)
	local source = message.TextSource
	if not source or message.Text == "" then
		return
	end
	local sender = Players:GetPlayerByUserId(source.UserId)
	local name = sender and sender.DisplayName or source.Name
	addChatLine(('<b><font color="%s">%s</font></b>  %s'):format(
		Kit.hex(Kit.nameColor(source.UserId)),
		Kit.escape(name),
		Kit.escape(unescape(message.Text))
	))
end

local function connectChat(channelName)
	if channelName == chatChannelName then
		return
	end
	chatChannelName = channelName
	if chatConnection then
		chatConnection:Disconnect()
		chatConnection = nil
	end
	chatChannel = nil
	Kit.clearRows(chatList)
	chatCount = 0
	chatEmpty.Visible = true
	if not channelName then
		return
	end
	task.spawn(function()
		local folder = TextChatService:WaitForChild("PartyChannels", 10)
		local channel = folder and folder:WaitForChild(channelName, 10)
		if not channel or chatChannelName ~= channelName then
			return
		end
		chatChannel = channel
		chatConnection = channel.MessageReceived:Connect(onChatMessage)
	end)
end

local function sendChat()
	local text = chatBox.Text
	if string.match(text, "^%s*$") then
		return
	end
	if not chatChannel then
		Kit.toast("Crew chat is still connecting...", true)
		return
	end
	chatBox.Text = ""
	local channel = chatChannel
	task.spawn(function()
		local ok = pcall(channel.SendAsync, channel, text)
		if not ok then
			Kit.toast("Your message didn't send.", true)
		end
	end)
end

---------------------------------------------------------------- party room

local function renderParty()
	local inParty = currentParty ~= nil
	if partyView.Visible ~= inParty then
		-- Joining or leaving a crew always brings you to the Play tab
		switchTab("Play")
		local view = inParty and partyView or hubView
		view.Position = UDim2.fromOffset(0, 24)
		Kit.tween(view, Kit.SMOOTH, { Position = UDim2.new() })
	end
	hubView.Visible = not inParty
	partyView.Visible = inParty
	tabLabels.Play.Text = inParty and "🎮  CREW" or "🎮  PLAY"
	connectChat(inParty and currentParty.channel or nil)
	if not inParty then
		return
	end

	local host = isHost()
	local settings = currentParty.settings
	local members = currentParty.members

	for i, chip in ipairs(chips) do
		chip.Text = string.sub(currentParty.code, i, i)
	end
	newCodeButton.Visible = host

	local hostName = ""
	for _, member in ipairs(members) do
		if member.isHost then
			hostName = member.displayName
		end
	end
	hostText.Text = host and "👑  You're the host" or ("👑  Host: " .. hostName)

	crewTitle.Text = ("CREW  %d/%d"):format(#members, settings.partySize)
	Kit.clearRows(membersList)
	for i = 1, settings.partySize do
		if members[i] then
			memberRow(members[i], i, host)
		else
			emptySlot(i)
		end
	end

	hideout:setMembers(members, settings.partySize)
	renderSettings(host, settings, members)
	renderFooter(host, settings, members)
end

---------------------------------------------------------------- input

codeBox:GetPropertyChangedSignal("Text"):Connect(function()
	local clean = string.sub(string.upper((string.gsub(codeBox.Text, "[^%w]", ""))), 1, Config.CODE_LENGTH)
	if clean ~= codeBox.Text then
		codeBox.Text = clean
	end
end)
codeBox.Focused:Connect(function()
	Kit.tween(codeFieldStroke, Kit.FAST, { Color = C.cop, Transparency = 0 })
end)

local function joinByCode()
	if #codeBox.Text == 0 then
		Kit.toast("Type a party code first.", true)
		return
	end
	if request("JoinByCode", codeBox.Text) then
		codeBox.Text = ""
	end
end

codeBox.FocusLost:Connect(function(enterPressed)
	Kit.tween(codeFieldStroke, Kit.FAST, { Color = C.stroke, Transparency = 0.2 })
	if enterPressed then
		joinByCode()
	end
end)
onClick(joinCodeButton, joinByCode)
onClick(createButton, function()
	request("Create")
end)
onClick(quickJoinButton, function()
	local ok, result = request("QuickJoin")
	if ok then
		Kit.toast(result == "CREATED" and "No open crews, so you started a new one." or "You joined a crew!")
	end
end)
onClick(refreshButton, function()
	local ok, list = request("ListPublic")
	if ok then
		lastPublicList = list
		renderPublicList(list)
	end
end)
onClick(helpButton, function()
	Overlays.showGuide()
end)
avatarButton.Activated:Connect(function()
	switchTab("Profile")
end)

onClick(leaveButton, function()
	request("Leave")
end)
onClick(newCodeButton, function()
	request("NewCode")
end)
onClick(startButton, function()
	request("Start")
end)
onClick(cancelButton, function()
	request("CancelStart")
end)
onClick(readyButton, function()
	request("SetReady", true)
end)
onClick(unreadyButton, function()
	request("SetReady", false)
end)
onClick(inviteButton, function()
	local ok, canInvite = pcall(SocialService.CanSendGameInviteAsync, SocialService, player)
	if not ok or not canInvite then
		Kit.toast("Invites work in the published game. For now, share the party code.", true)
		return
	end
	local options = Instance.new("ExperienceInviteOptions")
	options.PromptMessage = "Join my heist crew!"
	options.LaunchData = currentParty.code
	pcall(SocialService.PromptGameInvite, SocialService, player, options)
end)

onClick(sendButton, sendChat)
chatBox.FocusLost:Connect(function(enterPressed)
	if enterPressed then
		sendChat()
	end
end)

---------------------------------------------------------------- server updates

LobbyUpdate.OnClientEvent:Connect(function(kind, data)
	if kind == "Party" then
		currentParty = data
		renderParty()
		renderPublicList(lastPublicList)
	elseif kind == "PublicList" then
		lastPublicList = data
		renderPublicList(data)
	elseif kind == "Profile" then
		profile = data
		renderProfile()
	elseif kind == "Notice" then
		Kit.toast(data)
		if currentParty then
			addSystemLine(data)
		end
	elseif kind == "Countdown" then
		Overlays.countdown(data)
	elseif kind == "MapPick" then
		Overlays.mapPick(data)
	elseif kind == "Launching" then
		Overlays.launching(data)
	elseif kind == "Role" then
		Overlays.role(data)
	end
end)

---------------------------------------------------------------- start

local function fit()
	local screen = gui.AbsoluteSize
	if screen.X == 0 or screen.Y == 0 then
		return
	end
	fitScale = math.min(1.25, (screen.X - 16) / CANVAS.X, (screen.Y - 16) / CANVAS.Y)
	applyScale()
	Overlays.setFit(math.min(1, fitScale))
end
gui:GetPropertyChangedSignal("AbsoluteSize"):Connect(fit)

gui.Parent = player:WaitForChild("PlayerGui")
fit()
Kit.tween(blur, Kit.SMOOTH, { Size = 18 })
Kit.tween(backdrop, Kit.SMOOTH, { BackgroundTransparency = 0.08 })
pop(0.9, Kit.BOUNCE)
switchTab("Play")

renderParty()
renderPublicList(lastPublicList)
local ok, state = request("GetState")
if ok then
	profile = state.profile or profile
	lastPublicList = state.publicList
	currentParty = state.party
	renderProfile()
	renderParty()
	renderPublicList(lastPublicList)
end

-- ==========================================================================================
-- SCRIPT 2 OF 6:  ServerScriptService > LobbyService
-- ==========================================================================================
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

-- ==========================================================================================
-- SCRIPT 3 OF 6:  StarterPlayer > StarterPlayerScripts > LobbyClient > Hideout
-- ==========================================================================================
-- The hideout banner: the crew's avatars standing on pads in a garage, drawn in a ViewportFrame.
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Kit = require(ReplicatedStorage:WaitForChild("SharedUI"):WaitForChild("Kit"))
-- Real 3D models (van, cash, gold, workbench, crate), each with its pivot at its bottom-centre
local ASSETS = ReplicatedStorage:WaitForChild("HideoutAssets")

local C = Kit.COLORS

local SPACING = 4.4
local CAMERA_POSITION = Vector3.new(0, 4, 17)
local LOOK_AT = Vector3.new(0, 2.2, 0)
local FIELD_OF_VIEW = 32
local FLAT = CFrame.Angles(0, 0, math.rad(90)) -- turns a cylinder part on its side into a disc

local Hideout = {}
Hideout.__index = Hideout

local function part(props)
	local p = Instance.new("Part")
	p.Anchored = true
	p.CanCollide = false
	p.CastShadow = false
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	p.Material = Enum.Material.SmoothPlastic
	for key, value in pairs(props) do
		p[key] = value
	end
	return p
end

-- Places a copy of one of the HideoutAssets models; cframe is where its bottom-centre goes
local function prop(world, name, cframe, scale)
	local template = ASSETS:FindFirstChild(name)
	if not template then
		return nil
	end
	local model = template:Clone()
	if scale and scale ~= 1 then
		model:ScaleTo(scale)
	end
	model:PivotTo(cframe)
	model.Parent = world
	return model
end

local function buildRoom(world)
	-- Concrete floor with a painted bay line and a couple of oil stains
	part({
		Size = Vector3.new(110, 1, 44),
		CFrame = CFrame.new(0, -0.5, -6),
		Color = Color3.fromRGB(86, 86, 90),
		Material = Enum.Material.Concrete,
		Parent = world,
	})
	part({
		Size = Vector3.new(80, 0.04, 0.35),
		CFrame = CFrame.new(0, 0.02, -8.2),
		Color = Color3.fromRGB(214, 172, 40),
		Parent = world,
	})

	-- Brick back wall on a concrete plinth, with steel pillars
	part({
		Size = Vector3.new(110, 28, 1),
		CFrame = CFrame.new(0, 13.5, -10.5),
		Color = Color3.fromRGB(118, 74, 60),
		Material = Enum.Material.Brick,
		Parent = world,
	})
	part({
		Size = Vector3.new(110, 2.4, 0.5),
		CFrame = CFrame.new(0, 1.2, -9.9),
		Color = Color3.fromRGB(104, 106, 110),
		Material = Enum.Material.Concrete,
		Parent = world,
	})
	for _, x in ipairs({ -36, -16, 16, 36 }) do
		part({
			Size = Vector3.new(1.4, 28, 1.4),
			CFrame = CFrame.new(x, 13.5, -9.6),
			Color = Color3.fromRGB(46, 50, 58),
			Material = Enum.Material.Metal,
			Parent = world,
		})
	end

	-- Ribbed roller door behind the crew
	part({
		Size = Vector3.new(16, 9.4, 0.3),
		CFrame = CFrame.new(0, 4.7, -9.7),
		Color = Color3.fromRGB(134, 138, 146),
		Material = Enum.Material.Metal,
		Parent = world,
	})
	for i = 0, 17 do
		part({
			Size = Vector3.new(16, 0.12, 0.2),
			CFrame = CFrame.new(0, 0.35 + i * 0.52, -9.5),
			Color = Color3.fromRGB(100, 104, 112),
			Material = Enum.Material.Metal,
			Parent = world,
		})
	end
	for _, x in ipairs({ -8.4, 8.4 }) do
		part({
			Size = Vector3.new(0.8, 10, 0.8),
			CFrame = CFrame.new(x, 5, -9.5),
			Color = Color3.fromRGB(40, 42, 48),
			Material = Enum.Material.Metal,
			Parent = world,
		})
	end
	part({
		Size = Vector3.new(17.6, 1, 0.9),
		CFrame = CFrame.new(0, 10, -9.5),
		Color = Color3.fromRGB(40, 42, 48),
		Material = Enum.Material.Metal,
		Parent = world,
	})

	-- Fluorescent tube lights along the wall
	for _, x in ipairs({ -26, -8, 8, 26 }) do
		part({
			Size = Vector3.new(6.4, 0.45, 0.7),
			CFrame = CFrame.new(x, 11.6, -9.6),
			Color = Color3.fromRGB(36, 38, 44),
			Material = Enum.Material.Metal,
			Parent = world,
		})
		part({
			Size = Vector3.new(6, 0.22, 0.22),
			CFrame = CFrame.new(x, 11.3, -9.2),
			Color = Color3.fromRGB(226, 240, 255),
			Material = Enum.Material.Neon,
			Parent = world,
		})
	end

	-- Oil drums by the van
	for _, drum in ipairs({ { -17.6, -8.4, Color3.fromRGB(150, 38, 34) }, { -15.2, -8.7, Color3.fromRGB(38, 70, 140) } }) do
		part({
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(3.4, 2.3, 2.3),
			CFrame = CFrame.new(drum[1], 1.7, drum[2]) * FLAT,
			Color = drum[3],
			Material = Enum.Material.Metal,
			Parent = world,
		})
		for _, y in ipairs({ 0.9, 2.5 }) do
			part({
				Shape = Enum.PartType.Cylinder,
				Size = Vector3.new(0.18, 2.4, 2.4),
				CFrame = CFrame.new(drum[1], y, drum[2]) * FLAT,
				Color = drum[3]:Lerp(Color3.new(0, 0, 0), 0.35),
				Material = Enum.Material.Metal,
				Parent = world,
			})
		end
	end
end

-- ViewportFrames can't draw real shadows, so props get a soft dark patch on the floor instead:
-- three stacked, slightly different sized discs make a fake soft edge.
local function shadow(world, cframe, width, depth)
	for i, grow in ipairs({ 1.3, 1.12, 1 }) do
		part({
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.02, width * grow, depth * grow),
			CFrame = cframe * CFrame.new(0, 0.012 + i * 0.003, 0) * FLAT,
			Color = Color3.new(0, 0, 0),
			Transparency = 0.76,
			Parent = world,
		})
	end
end

-- A neat pyramid of cash bundles: `layers` high, 3 x 2 bundles at the bottom
local BUNDLE_SCALE = 0.62
local BUNDLE = { long = 1.86 * BUNDLE_SCALE, short = 0.9 * BUNDLE_SCALE, high = 0.3 * BUNDLE_SCALE }
local function cashPile(world, x, y, z, layers, rng)
	for layer = 0, layers - 1 do
		local across = math.max(1, 3 - math.floor(layer / 2))
		local deep = layer >= layers - 1 and 1 or 2
		for i = 1, across do
			for j = 1, deep do
				local offsetX = (i - (across + 1) / 2) * (BUNDLE.long + 0.03)
				local offsetZ = (j - (deep + 1) / 2) * (BUNDLE.short + 0.03)
				local wobble = CFrame.Angles(0, math.rad(rng:NextNumber(-4, 4)), 0)
				prop(world, "CashBrick", CFrame.new(x + offsetX, y + layer * BUNDLE.high, z + offsetZ) * CFrame.Angles(0, math.rad(90), 0) * wobble, BUNDLE_SCALE)
			end
		end
	end
end

-- Black duffel bag lying on the floor with cash spilling out of the open zip
local function duffelBag(world, cframe, rng)
	local fabric = Color3.fromRGB(26, 27, 31)
	part({
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(4.2, 1.9, 1.9),
		CFrame = cframe * CFrame.new(0, 0.95, 0),
		Color = fabric,
		Material = Enum.Material.Fabric,
		Parent = world,
	})
	for _, side in ipairs({ -2.12, 2.12 }) do
		part({
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.12, 1.75, 1.75),
			CFrame = cframe * CFrame.new(side, 0.95, 0),
			Color = Color3.fromRGB(14, 14, 16),
			Material = Enum.Material.Fabric,
			Parent = world,
		})
	end
	-- Handles and the open zip
	for _, x in ipairs({ -0.6, 0.6 }) do
		part({
			Size = Vector3.new(0.14, 0.7, 0.14),
			CFrame = cframe * CFrame.new(x, 2.1, 0.35) * CFrame.Angles(math.rad(20), 0, 0),
			Color = Color3.fromRGB(40, 40, 46),
			Parent = world,
		})
	end
	part({
		Size = Vector3.new(1.34, 0.14, 0.14),
		CFrame = cframe * CFrame.new(0, 2.45, 0.47),
		Color = Color3.fromRGB(40, 40, 46),
		Parent = world,
	})
	part({
		Size = Vector3.new(3.6, 0.06, 0.5),
		CFrame = cframe * CFrame.new(0, 1.9, -0.1),
		Color = Color3.fromRGB(120, 120, 128),
		Material = Enum.Material.Metal,
		Parent = world,
	})
	-- Bundles poking out of the top and spilled in front
	for i, spot in ipairs({
		{ -1.1, 1.92, -0.1, 8 },
		{ -0.1, 1.98, 0, -14 },
		{ 1, 1.92, -0.15, 22 },
		{ -0.4, 0, 1.6, 35 },
		{ 0.9, 0, 1.45, -20 },
		{ 0.3, BUNDLE.high, 1.55, 60 },
	}) do
		local tilt = i <= 3 and CFrame.Angles(math.rad(rng:NextNumber(-25, 25)), 0, math.rad(rng:NextNumber(-12, 12))) or CFrame.new()
		prop(world, "CashBrick", cframe * CFrame.new(spot[1], spot[2], spot[3]) * CFrame.Angles(0, math.rad(spot[4]), 0) * tilt, BUNDLE_SCALE)
	end
	shadow(world, cframe, 5.2, 3.6)
end

local function buildProps(world)
	local rng = Random.new(11)

	-- Getaway van parked side-on along the left wall, nose towards the crew
	local vanSpot = CFrame.new(-25, 0, -5.5) * CFrame.Angles(0, math.rad(84), 0)
	prop(world, "Van", vanSpot, 0.9)
	shadow(world, vanSpot, 6.4, 12.8)

	-- Crates stacked behind the drums
	local crateSpot = CFrame.new(-11.8, 0, -7.9) * CFrame.Angles(0, math.rad(8), 0)
	prop(world, "Crate", crateSpot, 0.55)
	shadow(world, crateSpot, 4, 4)
	prop(world, "Crate", CFrame.new(-11.6, 3.45, -8.1) * CFrame.Angles(0, math.rad(-14), 0), 0.38)
	for _, drum in ipairs({ { -17.6, -8.4 }, { -15.2, -8.7 } }) do
		shadow(world, CFrame.new(drum[1], 0, drum[2]), 2.9, 2.9)
	end

	-- Waist-high workbench on the right with two neat piles of cash
	local benchSpot = CFrame.new(19.5, 0, -6.4) * CFrame.Angles(0, math.rad(90), 0)
	local bench = prop(world, "Workbench", benchSpot, 0.72)
	shadow(world, CFrame.new(19.5, 0, -6.4), 7.2, 2.6)
	if bench then
		local box, size = bench:GetBoundingBox()
		local top = box.Position.Y + size.Y / 2
		cashPile(world, 17.9, top, -6.4, 5, rng)
		cashPile(world, 21.2, top, -6.4, 3, rng)
	end

	-- Gold bars on the floor between the crew and the bench
	local goldSpot = CFrame.new(12, 0, -6.6) * CFrame.Angles(0, math.rad(-8), 0)
	prop(world, "GoldStack", goldSpot, 0.42)
	shadow(world, goldSpot, 6.8, 3.4)

	-- Duffel bag full of cash on the floor in front of the bench
	duffelBag(world, CFrame.new(21, 0, -2.2) * CFrame.Angles(0, math.rad(-18), 0), rng)
end

-- The plan for the job, pinned up either side of the roller door
local function buildWallDecor(world)
	local wallZ = -9.95
	-- Cork board with photos, notes and red string
	local board = CFrame.new(12.2, 6.2, wallZ)
	part({
		Size = Vector3.new(6, 4, 0.16),
		CFrame = board,
		Color = Color3.fromRGB(178, 134, 88),
		Material = Enum.Material.Fabric,
		Parent = world,
	})
	for _, edge in ipairs({ { 0, 2.05, 6.3, 0.2 }, { 0, -2.05, 6.3, 0.2 }, { -3.05, 0, 0.2, 4.3 }, { 3.05, 0, 0.2, 4.3 } }) do
		part({
			Size = Vector3.new(edge[3], edge[4], 0.24),
			CFrame = board * CFrame.new(edge[1], edge[2], 0.04),
			Color = Color3.fromRGB(70, 46, 30),
			Material = Enum.Material.Wood,
			Parent = world,
		})
	end
	local pins = {}
	for _, note in ipairs({
		{ -2, 1, 1, 1.2, Color3.fromRGB(236, 236, 230), -6 },
		{ -0.4, 1.2, 1.2, 0.9, Color3.fromRGB(70, 74, 84), 4 },
		{ 1.6, 0.9, 1, 1.25, Color3.fromRGB(236, 236, 230), 8 },
		{ -1.6, -0.9, 1.4, 1, Color3.fromRGB(255, 226, 120), -3 },
		{ 0.6, -0.8, 1, 1.3, Color3.fromRGB(236, 236, 230), 5 },
		{ 2.2, -1, 0.9, 0.9, Color3.fromRGB(255, 160, 150), -9 },
	}) do
		part({
			Size = Vector3.new(note[3], note[4], 0.04),
			CFrame = board * CFrame.new(note[1], note[2], 0.11) * CFrame.Angles(0, 0, math.rad(note[6])),
			Color = note[5],
			Parent = world,
		})
		local pin = board * CFrame.new(note[1], note[2] + note[4] / 2 - 0.15, 0.16)
		part({
			Shape = Enum.PartType.Ball,
			Size = Vector3.new(0.16, 0.16, 0.16),
			CFrame = pin,
			Color = Color3.fromRGB(210, 40, 40),
			Parent = world,
		})
		table.insert(pins, pin.Position)
	end
	for _, link in ipairs({ { 1, 3 }, { 3, 5 }, { 5, 4 }, { 2, 6 }, { 4, 1 } }) do
		local a, b = pins[link[1]], pins[link[2]]
		part({
			Size = Vector3.new(0.04, 0.04, (b - a).Magnitude),
			CFrame = CFrame.lookAt((a + b) / 2, b),
			Color = Color3.fromRGB(220, 30, 40),
			Parent = world,
		})
	end

	-- Blueprint of the bank on the other side
	local blueprint = CFrame.new(-12.2, 6.2, wallZ) * CFrame.Angles(0, 0, math.rad(-2))
	part({
		Size = Vector3.new(5.2, 3.4, 0.06),
		CFrame = blueprint,
		Color = Color3.fromRGB(34, 86, 168),
		Parent = world,
	})
	local lineColor = Color3.fromRGB(190, 214, 255)
	for _, line in ipairs({
		{ 0, 1.3, 4.4, 0.05 }, { 0, -1.3, 4.4, 0.05 }, { -2.2, 0, 0.05, 2.65 }, { 2.2, 0, 0.05, 2.65 },
		{ -0.6, 0.2, 0.05, 2.2 }, { 0.9, -0.4, 2.6, 0.05 }, { 1.3, 0.6, 0.05, 1.4 }, { -1.4, -0.5, 1.6, 0.05 },
	}) do
		part({
			Size = Vector3.new(line[3], line[4], 0.02),
			CFrame = blueprint * CFrame.new(line[1], line[2], 0.04),
			Color = lineColor,
			Parent = world,
		})
	end
	part({
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(0.02, 0.7, 0.7),
		CFrame = blueprint * CFrame.new(0.3, -0.5, 0.05) * CFrame.Angles(0, math.rad(90), 0),
		Color = Color3.fromRGB(230, 50, 50),
		Parent = world,
	})
end

local function buildScene(world)
	buildRoom(world)
	buildWallDecor(world)
	buildProps(world)
end

function Hideout.new(parent)
	local self = setmetatable({}, Hideout)
	self.viewport = Kit.make("ViewportFrame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.fromRGB(14, 16, 24),
		Ambient = Color3.fromRGB(104, 102, 114),
		LightColor = Color3.fromRGB(255, 242, 224),
		LightDirection = Vector3.new(-0.35, -1, -0.45),
		Parent = parent,
	}, { Kit.corner(14) })
	self.world = Kit.make("WorldModel", { Parent = self.viewport })
	self.camera = Kit.make("Camera", {
		FieldOfView = FIELD_OF_VIEW,
		CFrame = CFrame.lookAt(CAMERA_POSITION, LOOK_AT),
		Parent = self.viewport,
	})
	self.viewport.CurrentCamera = self.camera
	buildScene(self.world)

	self.labels = Kit.make("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.fromScale(1, 1),
		Parent = parent,
	})
	self.slots = {}
	self.padCount = 0
	self.viewport:GetPropertyChangedSignal("AbsoluteSize"):Connect(function()
		self:_placeLabels()
	end)
	return self
end

-- Characters at the same depth line up linearly across the screen, so labels can be placed
-- under each pad with simple maths instead of projecting every frame.
function Hideout:_visibleWidth()
	local size = self.viewport.AbsoluteSize
	if size.X == 0 or size.Y == 0 then
		return nil
	end
	local depth = (LOOK_AT - CAMERA_POSITION).Magnitude
	return 2 * depth * math.tan(math.rad(FIELD_OF_VIEW / 2)) * size.X / size.Y
end

function Hideout:_placeLabels()
	local width = self:_visibleWidth()
	if not width then
		return
	end
	for _, slot in ipairs(self.slots) do
		slot.tag.Position = UDim2.new(0.5 + slot.x / width, 0, 1, -4)
		slot.tag.Size = UDim2.new(SPACING / width, -10, 0, 32)
	end
end

function Hideout:_buildPads(count)
	for _, slot in ipairs(self.slots) do
		if slot.model then
			slot.model:Destroy()
		end
		slot.ring:Destroy()
		slot.base:Destroy()
		slot.glow:Destroy()
		slot.feet:Destroy()
		slot.tag:Destroy()
	end
	table.clear(self.slots)
	self.padCount = count

	for i = 1, count do
		local x = (i - (count + 1) / 2) * SPACING
		-- A raised, lit platform: a soft glow on the floor, a neon rim and a light metal top
		local glow = part({
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.03, 4.4, 4.4),
			CFrame = CFrame.new(x, 0.02, 0) * FLAT,
			Color = C.faint,
			Material = Enum.Material.Neon,
			Transparency = 0.8,
			Parent = self.world,
		})
		local ring = part({
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.3, 3.3, 3.3),
			CFrame = CFrame.new(x, 0.15, 0) * FLAT,
			Color = C.faint,
			Material = Enum.Material.Neon,
			Parent = self.world,
		})
		local base = part({
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.34, 2.95, 2.95),
			CFrame = CFrame.new(x, 0.17, 0) * FLAT,
			Color = Color3.fromRGB(150, 156, 170),
			Material = Enum.Material.DiamondPlate,
			Parent = self.world,
		})
		-- Fake shadow under whoever stands here; only shown when the slot is filled
		local feet = part({
			Shape = Enum.PartType.Cylinder,
			Size = Vector3.new(0.02, 2, 1.4),
			CFrame = CFrame.new(x, 0.35, 0.1) * FLAT,
			Color = Color3.new(0, 0, 0),
			Transparency = 1,
			Parent = self.world,
		})
		local tag = Kit.make("Frame", {
			AnchorPoint = Vector2.new(0.5, 1),
			BackgroundColor3 = C.well,
			BackgroundTransparency = 0.3,
			Parent = self.labels,
		}, { Kit.corner(8) })
		local nameLabel = Kit.label({
			Font = Kit.FONT_BOLD,
			TextSize = 13,
			TextXAlignment = Enum.TextXAlignment.Center,
			Position = UDim2.fromOffset(4, 2),
			Size = UDim2.new(1, -8, 0, 16),
			Parent = tag,
		})
		local statusLabel = Kit.label({
			Font = Kit.FONT_BLACK,
			TextSize = 10,
			TextXAlignment = Enum.TextXAlignment.Center,
			Position = UDim2.fromOffset(4, 18),
			Size = UDim2.new(1, -8, 0, 12),
			Parent = tag,
		})
		table.insert(self.slots, {
			x = x,
			ring = ring,
			base = base,
			glow = glow,
			feet = feet,
			tag = tag,
			nameLabel = nameLabel,
			statusLabel = statusLabel,
		})
	end
	self:_placeLabels()
end

-- A copy of a player's live character for showing in a ViewportFrame: scripts and sounds
-- removed, name tag hidden, root anchored. Also returns their idle animation, if any.
-- Returns nil while the character hasn't spawned.
function Hideout.cloneCharacter(owner)
	local character = owner and owner.Character
	if not character or not character:FindFirstChildOfClass("Humanoid") then
		return nil
	end

	local wasArchivable = character.Archivable
	character.Archivable = true
	local clone = character:Clone()
	character.Archivable = wasArchivable
	if not clone then
		return nil
	end

	local animate = character:FindFirstChild("Animate")
	local idle = animate and animate:FindFirstChild("idle")
	local idleAnimation = idle and idle:FindFirstChildOfClass("Animation")

	for _, descendant in ipairs(clone:GetDescendants()) do
		if descendant:IsA("BaseScript") or descendant:IsA("Sound") or descendant:IsA("ForceField") then
			descendant:Destroy()
		end
	end
	local humanoid = clone:FindFirstChildOfClass("Humanoid")
	humanoid.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.None
	humanoid.HealthDisplayType = Enum.HumanoidHealthDisplayType.AlwaysOff
	local root = clone:FindFirstChild("HumanoidRootPart")
	if root then
		root.Anchored = true
	end
	return clone, idleAnimation
end

-- Stands a cloned character at `cframe` (facing its look direction) with its feet on y = floorY
function Hideout.standAt(clone, cframe, floorY)
	clone:PivotTo(cframe)
	local box, size = clone:GetBoundingBox()
	clone:PivotTo(clone:GetPivot() + Vector3.new(0, floorY - (box.Position.Y - size.Y / 2), 0))
end

-- Needs the clone to be parented to a WorldModel first
function Hideout.playIdle(clone, idleAnimation)
	local humanoid = clone:FindFirstChildOfClass("Humanoid")
	if not idleAnimation or not humanoid then
		return
	end
	task.spawn(function()
		pcall(function()
			local animator = humanoid:FindFirstChildOfClass("Animator") or Instance.new("Animator", humanoid)
			local track = animator:LoadAnimation(idleAnimation:Clone())
			track.Looped = true
			track:Play()
		end)
	end)
end

-- Copies the player's live character onto a pad, facing the camera, playing its idle animation
function Hideout:_spawnAvatar(userId, x)
	local clone, idleAnimation = Hideout.cloneCharacter(Players:GetPlayerByUserId(userId))
	if not clone then
		return nil
	end
	Hideout.standAt(clone, CFrame.new(x, 0, 0) * CFrame.Angles(0, math.pi, 0), 0.34)
	clone.Parent = self.world
	Hideout.playIdle(clone, idleAnimation)
	return clone
end

-- members: the party's member list from the server, in slot order
function Hideout:setMembers(members, partySize)
	if partySize ~= self.padCount then
		self:_buildPads(partySize)
	end

	local missingCharacter = false
	for i, slot in ipairs(self.slots) do
		local member = members[i]
		local userId = member and member.userId
		if slot.userId ~= userId or (userId and not slot.model) then
			if slot.model then
				slot.model:Destroy()
				slot.model = nil
			end
			slot.userId = userId
			if userId then
				slot.model = self:_spawnAvatar(userId, slot.x)
				missingCharacter = missingCharacter or slot.model == nil
			end
		end

		local color
		if not member then
			color = Color3.fromRGB(110, 120, 150)
		elseif member.isHost then
			color = C.gold
		elseif member.isReady then
			color = C.good
		else
			color = C.muted
		end
		slot.ring.Color = color
		slot.glow.Color = color
		slot.glow.Transparency = member and 0.7 or 0.85
		slot.feet.Transparency = slot.model and 0.55 or 1
		slot.tag.BackgroundTransparency = member and 0.25 or 0.5
		slot.nameLabel.Text = member and member.displayName or "Open slot"
		slot.nameLabel.TextColor3 = member and C.text or Color3.fromRGB(190, 198, 220)
		slot.statusLabel.TextColor3 = color
		if not member then
			slot.statusLabel.Text = ""
		elseif member.isHost then
			slot.statusLabel.Text = "👑 HOST"
		else
			slot.statusLabel.Text = member.isReady and "✔ READY" or "NOT READY"
		end
	end

	-- Characters that haven't spawned yet get another try shortly
	self.lastMembers, self.lastSize = members, partySize
	if missingCharacter and not self.retryScheduled then
		self.retryScheduled = true
		task.delay(2, function()
			self.retryScheduled = false
			if self.lastMembers then
				self:setMembers(self.lastMembers, self.lastSize)
			end
		end)
	end
end

return Hideout

-- ==========================================================================================
-- SCRIPT 4 OF 6:  ReplicatedStorage > LobbyConfig
-- ==========================================================================================
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

-- ==========================================================================================
-- SCRIPT 5 OF 6:  ServerScriptService > LobbyService > PlayerData
-- ==========================================================================================
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

-- ==========================================================================================
-- SCRIPT 6 OF 6:  ServerScriptService > HeistService
-- ==========================================================================================
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

-- ==========================================================================================
-- END
