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
local InventoryTab = require(script:WaitForChild("InventoryTab"))
local ShopTab = require(script:WaitForChild("ShopTab"))
local ProfileTab = require(script:WaitForChild("ProfileTab"))

-- The 3D hideout (crew, silly poses, getaway car). It used to be the Hideout ModuleScript;
-- it lives in this script now so the whole lobby is one file.
local Hideout = (function()
	-- The hideout: the crew's avatars striking silly poses on pads in a garage, next to the
	-- getaway car, drawn in a full-screen ViewportFrame behind the party room's controls.
	local Players = game:GetService("Players")
	local ReplicatedStorage = game:GetService("ReplicatedStorage")

	local Kit = require(ReplicatedStorage:WaitForChild("SharedUI"):WaitForChild("Kit"))
	local Config = require(ReplicatedStorage:WaitForChild("LobbyConfig"))
	-- Real 3D models (cash, gold, workbench, crate), each with its pivot at its bottom-centre
	local ASSETS = ReplicatedStorage:WaitForChild("HideoutAssets")

	local C = Kit.COLORS

	local SPACING = 4.4
	local FIELD_OF_VIEW = 34
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
		-- Concrete floor (deep enough for the camera to pull back) with a painted bay line
		part({
			Size = Vector3.new(140, 1, 90),
			CFrame = CFrame.new(0, -0.5, 34),
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

		-- Oil drums in the back corner
		for _, drum in ipairs({ { -28.6, -8.4, Color3.fromRGB(150, 38, 34) }, { -26.2, -8.7, Color3.fromRGB(38, 70, 140) } }) do
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

		-- (The getaway car is built separately, see buildCar, and parks next to the crew)

		-- Crates stacked by the roller door
		local crateSpot = CFrame.new(-10.6, 0, -8.2) * CFrame.Angles(0, math.rad(8), 0)
		prop(world, "Crate", crateSpot, 0.55)
		shadow(world, crateSpot, 4, 4)
		prop(world, "Crate", CFrame.new(-10.4, 3.45, -8.4) * CFrame.Angles(0, math.rad(-14), 0), 0.38)
		for _, drum in ipairs({ { -28.6, -8.4 }, { -26.2, -8.7 } }) do
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

	-- A modern getaway supercar built from parts: low wide body, glass canopy, LED light bars,
	-- a big rear wing, spoked rims and a purple underglow. Built facing -Z with its bottom
	-- centre at the origin, inside a Model so it can be moved around with PivotTo.
	local CAR_BODY = Color3.fromRGB(22, 22, 28)
	local CAR_TRIM = Color3.fromRGB(10, 10, 14)
	local CAR_ACCENT = C.robber
	local CAR_GLOW = C.purple
	local CAR_GLASS = Color3.fromRGB(20, 30, 46)

	local function buildCar(world)
		local car = Instance.new("Model")
		car.Name = "GetawayCar"
		car.WorldPivot = CFrame.new()

		local function piece(size, cframe, color, props)
			local p = part({
				Size = size,
				CFrame = cframe,
				Color = color,
				Parent = car,
			})
			for key, value in pairs(props or {}) do
				p[key] = value
			end
			return p
		end
		local function tilt(x, y, z, degrees)
			return CFrame.new(x, y, z) * CFrame.Angles(math.rad(degrees), 0, 0)
		end
		local paint = { Material = Enum.Material.SmoothPlastic, Reflectance = 0.12 }
		local neon = { Material = Enum.Material.Neon }
		local glass = { Material = Enum.Material.Glass, Transparency = 0.12, Reflectance = 0.3 }

		-- Body
		piece(Vector3.new(4.9, 0.9, 10.8), CFrame.new(0, 0.95, 0), CAR_BODY, paint)
		piece(Vector3.new(4.9, 0.55, 0.7), CFrame.new(0, 0.72, -5.6), CAR_TRIM)
		piece(Vector3.new(5, 0.08, 0.9), CFrame.new(0, 0.45, -5.7), CAR_ACCENT)
		piece(Vector3.new(4.6, 0.3, 3.6), tilt(0, 1.42, -3.6, -7), CAR_BODY, paint)
		for _, x in ipairs({ -0.45, 0.45 }) do
			piece(Vector3.new(0.5, 0.02, 3.6), tilt(x, 1.585, -3.6, -7), CAR_ACCENT)
		end
		piece(Vector3.new(3.9, 0.3, 4.8), CFrame.new(0, 1.55, 0.6), CAR_BODY, paint)
		piece(Vector3.new(3.7, 1, 3.4), CFrame.new(0, 1.95, 0.6), CAR_TRIM)
		piece(Vector3.new(4, 0.1, 2.2), tilt(0, 2, -1.35, -28), CAR_GLASS, glass)
		piece(Vector3.new(3.8, 0.16, 2.2), CFrame.new(0, 2.56, 0.7), CAR_BODY, paint)
		piece(Vector3.new(3.9, 0.1, 2.6), tilt(0, 2.05, 3, 22), CAR_GLASS, glass)
		for _, x in ipairs({ -1.95, 1.95 }) do
			piece(Vector3.new(0.1, 0.75, 3.4), CFrame.new(x, 2.05, 0.5), CAR_GLASS, glass)
		end
		piece(Vector3.new(4.7, 0.3, 1.4), CFrame.new(0, 1.5, 4.7), CAR_BODY, paint)
		piece(Vector3.new(4.6, 0.4, 0.5), CFrame.new(0, 0.7, 5.55), CAR_TRIM)

		-- Rear wing
		piece(Vector3.new(5, 0.12, 0.9), CFrame.new(0, 2.3, 5), CAR_ACCENT)
		for _, x in ipairs({ -2.5, 2.5 }) do
			piece(Vector3.new(0.1, 0.5, 0.9), CFrame.new(x, 2.15, 5), CAR_BODY, paint)
		end
		for _, x in ipairs({ -1.4, 1.4 }) do
			piece(Vector3.new(0.14, 0.75, 0.25), CFrame.new(x, 1.9, 5), CAR_BODY, paint)
		end

		-- Lights: LED strips at the front, one red bar across the back
		for _, side in ipairs({ -1, 1 }) do
			piece(Vector3.new(1.3, 0.14, 0.1), CFrame.new(side * 1.55, 1.25, -5.42), Color3.fromRGB(235, 245, 255), neon)
			piece(Vector3.new(0.12, 0.4, 0.1), CFrame.new(side * 2.15, 1.08, -5.42), Color3.fromRGB(235, 245, 255), neon)
		end
		piece(Vector3.new(2, 0.35, 0.06), CFrame.new(0, 0.75, -5.97), Color3.fromRGB(6, 6, 8))
		piece(Vector3.new(4.5, 0.12, 0.08), CFrame.new(0, 1.3, 5.42), Color3.fromRGB(255, 40, 60), neon)
		piece(Vector3.new(1.3, 0.35, 0.04), CFrame.new(0, 1, 5.42), Color3.fromRGB(235, 235, 230))

		-- Side stripes, mirrors and exhausts
		for _, side in ipairs({ -1, 1 }) do
			piece(Vector3.new(0.08, 0.14, 8), CFrame.new(side * 2.46, 0.62, 0), CAR_ACCENT, neon)
			piece(Vector3.new(0.45, 0.22, 0.4), CFrame.new(side * 2.3, 1.85, -0.9), CAR_BODY, paint)
			piece(Vector3.new(0.2, 0.12, 0.12), CFrame.new(side * 2.1, 1.75, -0.9), CAR_TRIM)
			piece(Vector3.new(0.5, 0.32, 0.32), CFrame.new(side * 0.8, 0.72, 5.8) * CFrame.Angles(0, math.rad(90), 0), Color3.fromRGB(180, 182, 190), {
				Shape = Enum.PartType.Cylinder,
				Material = Enum.Material.Metal,
			})
		end

		-- Wheels: fat tyres, silver rims with spokes, orange brake callipers, flared fenders
		for _, z in ipairs({ -3.4, 3.5 }) do
			for _, side in ipairs({ -1, 1 }) do
				local x = side * 2.3
				local cylinder = { Shape = Enum.PartType.Cylinder }
				piece(Vector3.new(0.85, 1.7, 1.7), CFrame.new(x, 0.85, z), Color3.fromRGB(14, 14, 16), cylinder)
				piece(Vector3.new(0.88, 1.15, 1.15), CFrame.new(x, 0.85, z), Color3.fromRGB(196, 200, 210), {
					Shape = Enum.PartType.Cylinder,
					Material = Enum.Material.Metal,
				})
				for k = 0, 4 do
					piece(Vector3.new(0.9, 1.05, 0.12), CFrame.new(x, 0.85, z) * CFrame.Angles(math.rad(k * 36), 0, 0), Color3.fromRGB(40, 40, 46))
				end
				piece(Vector3.new(0.92, 0.4, 0.4), CFrame.new(x, 0.85, z), CAR_ACCENT, cylinder)
				piece(Vector3.new(0.86, 0.35, 0.5), CFrame.new(x, 1.08, z + 0.2), CAR_ACCENT)
				piece(Vector3.new(0.5, 0.4, 2.3), CFrame.new(x, 1.5, z), CAR_BODY, paint)
			end
		end

		-- Underglow and a soft shadow on the floor
		piece(Vector3.new(4.4, 0.05, 9.6), CFrame.new(0, 0.42, 0), CAR_GLOW, neon)
		piece(Vector3.new(0.02, 7.5, 13.5), CFrame.new(0, 0.03, 0) * FLAT, CAR_GLOW, {
			Shape = Enum.PartType.Cylinder,
			Material = Enum.Material.Neon,
			Transparency = 0.72,
		})
		piece(Vector3.new(0.02, 6, 11.8), CFrame.new(0, 0.04, 0) * FLAT, Color3.new(0, 0, 0), {
			Shape = Enum.PartType.Cylinder,
			Transparency = 0.55,
		})

		car.Parent = world
		return car
	end

	local function buildScene(world)
		buildRoom(world)
		buildWallDecor(world)
		buildProps(world)
	end

	---------------------------------------------------------------- silly poses
	-- Each avatar strikes a silly pose. The pose bends the R15 joints (Motor6D.C0) before the
	-- avatar is placed, so standAt still puts its feet on the pad; the idle animation plays on
	-- top of the pose. Angles are degrees around the X, Y and Z axes of each joint.
	-- R6 avatars keep their normal idle.
	local POSES = {
		{ -- The dab
			RightShoulder = { 0, 0, 135 },
			LeftShoulder = { 100, 0, 40 },
			LeftElbow = { 100, 0, 0 },
			Neck = { -25, 0, -20 },
			Waist = { 0, -15, 0 },
		},
		{ -- Muscle flex
			RightShoulder = { 0, 0, 90 },
			RightElbow = { 0, 0, 100 },
			LeftShoulder = { 0, 0, -90 },
			LeftElbow = { 0, 0, -100 },
			Neck = { 10, 0, 0 },
		},
		{ -- Flamingo
			RightHip = { 80, 0, 0 },
			RightKnee = { -110, 0, 0 },
			RightShoulder = { 0, 0, 110 },
			LeftShoulder = { 0, 0, -110 },
			Neck = { 15, 0, 0 },
		},
		{ -- Zombie
			RightShoulder = { 90, 0, 0 },
			LeftShoulder = { 90, 0, 0 },
			Neck = { 0, 0, 30 },
			Waist = { -10, 0, 0 },
		},
		{ -- Hooray, with a little kick
			RightShoulder = { 0, 0, 155 },
			LeftShoulder = { 0, 0, -155 },
			LeftHip = { 0, 0, -25 },
			Neck = { 20, 0, 0 },
		},
		{ -- Squat
			RightHip = { 90, 0, 0 },
			LeftHip = { 90, 0, 0 },
			RightKnee = { -90, 0, 0 },
			LeftKnee = { -90, 0, 0 },
			RightShoulder = { 60, 0, 0 },
			LeftShoulder = { 60, 0, 0 },
		},
		{ -- Disco
			RightShoulder = { 0, 0, 150 },
			LeftShoulder = { 0, 0, -30 },
			LeftElbow = { 0, 0, 110 },
			Waist = { 0, 0, -10 },
			Neck = { 0, 0, 10 },
		},
		{ -- Chicken
			RightShoulder = { 0, 0, 50 },
			RightElbow = { 0, 0, -120 },
			LeftShoulder = { 0, 0, -50 },
			LeftElbow = { 0, 0, 120 },
			Neck = { -25, 0, 0 },
			RightKnee = { -20, 0, 0 },
		},
		{ -- Superhero
			RightShoulder = { 0, 0, 30 },
			RightElbow = { 0, 0, -110 },
			LeftShoulder = { 0, 0, -30 },
			LeftElbow = { 0, 0, 110 },
			Neck = { 15, 25, 0 },
			Waist = { 8, 0, 0 },
		},
		{ -- T-pose
			RightShoulder = { 0, 0, 90 },
			LeftShoulder = { 0, 0, -90 },
			Neck = { 0, 0, 15 },
		},
	}

	local function applyPose(clone, pose)
		if not clone:FindFirstChild("UpperTorso") then
			return
		end
		local motors = {}
		for _, descendant in ipairs(clone:GetDescendants()) do
			if descendant:IsA("Motor6D") then
				motors[descendant.Name] = descendant
			end
		end
		for jointName, angles in pairs(pose) do
			local motor = motors[jointName]
			if motor then
				motor.C0 = motor.C0 * CFrame.Angles(math.rad(angles[1]), math.rad(angles[2]), math.rad(angles[3]))
			end
		end
	end

	---------------------------------------------------------------- the hideout view

	-- callbacks.kick(userId) and callbacks.makeHost(userId, displayName) power the host's
	-- buttons on the name tags
	function Hideout.new(parent, callbacks)
		local self = setmetatable({}, Hideout)
		self.callbacks = callbacks or {}
		self.poseOffset = math.random(1, #POSES)
		self.viewport = Kit.make("ViewportFrame", {
			Size = UDim2.fromScale(1, 1),
			BackgroundColor3 = Color3.fromRGB(14, 16, 24),
			Ambient = Color3.fromRGB(104, 102, 114),
			LightColor = Color3.fromRGB(255, 242, 224),
			LightDirection = Vector3.new(-0.35, -1, -0.45),
			ZIndex = 1,
			Parent = parent,
		})
		self.world = Kit.make("WorldModel", { Parent = self.viewport })
		self.camera = Kit.make("Camera", {
			FieldOfView = FIELD_OF_VIEW,
			CFrame = CFrame.lookAt(Vector3.new(0, 4, 30), Vector3.new(0, 2, 0)),
			Parent = self.viewport,
		})
		self.viewport.CurrentCamera = self.camera
		buildScene(self.world)
		self.car = buildCar(self.world)

		-- Name tags are drawn in design pixels (see setUiScale), on top of the shading
		self.labels = Kit.make("Frame", {
			BackgroundTransparency = 1,
			Size = UDim2.fromScale(1, 1),
			ZIndex = 3,
			Parent = parent,
		})
		self.labelScale = Kit.make("UIScale", { Parent = self.labels })
		self.slots = {}
		self.padCount = 0
		self.amHost = false
		self.viewport:GetPropertyChangedSignal("AbsoluteSize"):Connect(function()
			self:_frameCamera()
			self:_placeLabels()
		end)
		return self
	end

	-- Same scale as the menu, so the name tags grow and shrink with it
	function Hideout:setUiScale(scale)
		self.labels.Size = UDim2.fromScale(1 / scale, 1 / scale)
		self.labelScale.Scale = scale
	end

	-- Where the car parks: just left of the crew, whatever the crew size
	function Hideout:_carX()
		return -(self.padCount * SPACING / 2 + 7.5)
	end

	-- Frames the crew and the car so they fill most of the screen width
	function Hideout:_frameCamera()
		local size = self.viewport.AbsoluteSize
		if size.X == 0 or size.Y == 0 then
			return
		end
		local aspect = math.max(size.X / size.Y, 1)
		local left = self:_carX() - 6
		local right = math.max(self.padCount, 3) * SPACING / 2 + 3
		local centreX = (left + right) / 2
		local tanHalf = math.tan(math.rad(FIELD_OF_VIEW / 2))
		local distance = math.clamp((right - left) / (0.92 * 2 * tanHalf * aspect), 20, 46)
		local lookAt = Vector3.new(centreX, 2.2, 0)
		self.camera.CFrame = CFrame.lookAt(lookAt + Vector3.new(0, 1.5 + distance * 0.08, distance), lookAt)
	end

	-- Projects a point in the hideout onto the viewport: returns its position (0..1 on each
	-- axis) and how many studs fit across the screen at that depth
	function Hideout:_project(point)
		local size = self.viewport.AbsoluteSize
		if size.X == 0 or size.Y == 0 then
			return nil
		end
		local aspect = size.X / size.Y
		local tanHalf = math.tan(math.rad(FIELD_OF_VIEW / 2))
		local relative = self.camera.CFrame:PointToObjectSpace(point)
		local depth = -relative.Z
		if depth <= 0 then
			return nil
		end
		local x = 0.5 + relative.X / (depth * tanHalf * aspect) / 2
		local y = 0.5 - relative.Y / (depth * tanHalf) / 2
		return x, y, 2 * depth * tanHalf * aspect
	end

	function Hideout:_placeLabels()
		for _, slot in ipairs(self.slots) do
			local x, y, studsAcross = self:_project(Vector3.new(slot.x, 0, 1.9))
			if x then
				slot.tag.Position = UDim2.fromScale(x, y)
				slot.tag.Size = UDim2.new(SPACING / studsAcross, -12, 0, slot.tag.Size.Y.Offset)
			end
		end
	end

	local function tagButton(text, style, parent, position)
		return Kit.button(text, style, {
			Position = position,
			Size = UDim2.new(0.5, -3, 0, 24),
			TextSize = 12,
			Parent = parent,
		})
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

			-- Name tag under the pad, with Kick / Make host buttons for the host
			local tag = Kit.make("Frame", {
				AnchorPoint = Vector2.new(0.5, 0),
				Size = UDim2.fromOffset(150, 44),
				BackgroundColor3 = C.well,
				BackgroundTransparency = 0.25,
				Parent = self.labels,
			}, { Kit.corner(10), Kit.pad(4, 6) })
			local tagStroke = Kit.stroke(C.faint, 0.4, 1.5)
			tagStroke.Parent = tag
			local nameLabel = Kit.label({
				Font = Kit.FONT_BOLD,
				TextSize = 15,
				TextXAlignment = Enum.TextXAlignment.Center,
				Size = UDim2.new(1, 0, 0, 18),
				Parent = tag,
			})
			local statusLabel = Kit.label({
				RichText = true,
				Font = Kit.FONT_BLACK,
				TextSize = 11,
				TextXAlignment = Enum.TextXAlignment.Center,
				Position = UDim2.fromOffset(0, 19),
				Size = UDim2.new(1, 0, 0, 14),
				Parent = tag,
			})
			local slot = {
				x = x,
				ring = ring,
				base = base,
				glow = glow,
				feet = feet,
				tag = tag,
				tagStroke = tagStroke,
				nameLabel = nameLabel,
				statusLabel = statusLabel,
			}
			slot.kickButton = tagButton("Kick", "danger", tag, UDim2.fromOffset(0, 38))
			slot.hostButton = tagButton("👑 Host", "ghost", tag, UDim2.new(0.5, 3, 0, 38))
			Kit.onClick(slot.kickButton, function()
				if slot.member and self.callbacks.kick then
					self.callbacks.kick(slot.member.userId)
				end
			end)
			Kit.onClick(slot.hostButton, function()
				if slot.member and self.callbacks.makeHost then
					self.callbacks.makeHost(slot.member.userId, slot.member.displayName)
				end
			end)
			table.insert(self.slots, slot)
		end

		self.car:PivotTo(CFrame.new(self:_carX(), 0, -3.5) * CFrame.Angles(0, math.rad(-150), 0))
		self:_frameCamera()
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

	-- Copies the player's live character onto a pad in a silly pose, facing the camera
	function Hideout:_spawnAvatar(userId, x, slotIndex)
		local clone, idleAnimation = Hideout.cloneCharacter(Players:GetPlayerByUserId(userId))
		if not clone then
			return nil
		end
		applyPose(clone, POSES[(slotIndex + self.poseOffset) % #POSES + 1])
		Hideout.standAt(clone, CFrame.new(x, 0, 0) * CFrame.Angles(0, math.pi, 0), 0.34)
		clone.Parent = self.world
		Hideout.playIdle(clone, idleAnimation)
		return clone
	end

	-- members: the party's member list from the server, in slot order.
	-- amHost: whether the local player is the host (shows Kick / Make host on the tags).
	function Hideout:setMembers(members, partySize, amHost)
		if partySize ~= self.padCount then
			self:_buildPads(partySize)
		end
		self.amHost = amHost == true
		local localUserId = Players.LocalPlayer.UserId

		local missingCharacter = false
		for i, slot in ipairs(self.slots) do
			local member = members[i]
			local userId = member and member.userId
			slot.member = member
			if slot.userId ~= userId or (userId and not slot.model) then
				if slot.model then
					slot.model:Destroy()
					slot.model = nil
				end
				slot.userId = userId
				if userId then
					slot.model = self:_spawnAvatar(userId, slot.x, i)
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
			slot.tag.BackgroundTransparency = member and 0.2 or 0.55
			slot.tagStroke.Color = color
			slot.nameLabel.Text = member and ((member.mask and member.mask .. " " or "") .. member.displayName) or "Open slot"
			slot.nameLabel.TextColor3 = member and C.text or Color3.fromRGB(190, 198, 220)
			if not member then
				slot.statusLabel.Text = ""
			else
				local rank = Config.rankFor(member.xp or 0)
				local status
				if member.isHost then
					status = ('<font color="%s">👑 HOST</font>'):format(Kit.hex(C.gold))
				elseif member.isReady then
					status = ('<font color="%s">✔ READY</font>'):format(Kit.hex(C.good))
				else
					status = ('<font color="%s">NOT READY</font>'):format(Kit.hex(C.muted))
				end
				slot.statusLabel.Text = ('<font color="%s">%s %s</font>  ·  %s'):format(
					Kit.hex(rank.color),
					Config.RANK_ICONS[rank.name] or "",
					string.upper(rank.name),
					status
				)
			end

			local canManage = self.amHost and member ~= nil and not member.isHost and member.userId ~= localUserId
			slot.kickButton.Visible = canManage
			slot.hostButton.Visible = canManage
			slot.tag.Size = UDim2.new(slot.tag.Size.X.Scale, slot.tag.Size.X.Offset, 0, canManage and 72 or 44)
		end

		-- Characters that haven't spawned yet get another try shortly
		self.lastMembers, self.lastSize, self.lastHost = members, partySize, amHost
		if missingCharacter and not self.retryScheduled then
			self.retryScheduled = true
			task.delay(2, function()
				self.retryScheduled = false
				if self.lastMembers then
					self:setMembers(self.lastMembers, self.lastSize, self.lastHost)
				end
			end)
		end
	end

	return Hideout
end)()

local remotes = ReplicatedStorage:WaitForChild("LobbyRemotes")
local LobbyRequest = remotes:WaitForChild("LobbyRequest")
local LobbyUpdate = remotes:WaitForChild("LobbyUpdate")

local C = Kit.COLORS
local make, label, button, onClick = Kit.make, Kit.label, Kit.button, Kit.onClick
local fmt = Kit.formatNumber

local player = Players.LocalPlayer
-- Shows up in Studio's Output window, so you can check this version of the script is the one running
print("[Lobby] LobbyClient v5 starting: full-screen party room, quests, heist settings tab")

-- Everything is laid out on one 1280x720 canvas that's scaled to fit the screen
local CANVAS = Vector2.new(1280, 720)
local CONTENT_TOP = 98
local CONTENT_SIZE = Vector2.new(1232, 600)
-- The party room is drawn a little bigger than the other tabs when the screen has room
local MENU_SCALE_CAP = 1.25
local PARTY_SCALE_CAP = 1.4
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
local fit -- defined at the bottom; re-run when entering or leaving the party room
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
local hubQuestsButton = button("📜  Quests", "gold", {
	Position = UDim2.fromOffset(742, 160),
	Size = UDim2.fromOffset(150, 52),
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
-- The party room fills the whole screen. The 3D hideout is the background (the crew stands
-- in the middle with their name tags), and the controls sit around the edges:
--   top left      Leave crew, Quests
--   top middle    host, crew count and target
--   top right     party code, invite
--   bottom left   crew chat (see-through, no box behind the text)
--   bottom right  Heist settings tab + Start / Ready

-- Empty stand-in inside the Play page, so the hub hides while you're in a crew
local partyView = make("Frame", {
	Size = UDim2.fromScale(1, 1),
	BackgroundTransparency = 1,
	Visible = false,
	Parent = playPage,
})

-- Draw order on the screen: backdrop, 3D hideout, party controls, then the top bar on top
backdrop.ZIndex = 0
canvas.ZIndex = 3

local PARTY_EDGE = 24
local SETTINGS_WIDTH = 400

-- Layers drawn in "design pixels": the frame is 1/s of the screen and a UIScale of s blows
-- it back up, so everything inside grows and shrinks with the rest of the menu.
local scaledLayers = {}
local function scaledLayer(props)
	local frame = make("Frame", props)
	local uiScale = make("UIScale", { Parent = frame })
	table.insert(scaledLayers, { frame = frame, scale = uiScale })
	return frame
end

-- Full-screen 3D hideout
local hideoutLayer = make("Frame", {
	Name = "HideoutLayer",
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Visible = false,
	ZIndex = 1,
	Parent = gui,
})
-- Soft dark shading at the top and bottom so the menu stays readable over the 3D scene
for _, shade in ipairs({ { 0, 0, 90 }, { 1, 1, -90 } }) do
	make("Frame", {
		AnchorPoint = Vector2.new(0, shade[1]),
		Position = UDim2.fromScale(0, shade[2]),
		Size = UDim2.fromScale(1, 0.3),
		BackgroundColor3 = Color3.new(0, 0, 0),
		BorderSizePixel = 0,
		ZIndex = 2,
		Parent = hideoutLayer,
	}, {
		make("UIGradient", {
			Rotation = shade[3],
			Transparency = NumberSequence.new(1, 0.45),
		}),
	})
end

local partyLayer = scaledLayer({
	Name = "PartyLayer",
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Visible = false,
	ZIndex = 2,
	Parent = gui,
})
-- Everything below the top bar; laid out again whenever the screen size changes
local partyBody = make("Frame", {
	BackgroundTransparency = 1,
	Parent = partyLayer,
})

-- Readable text over the 3D scene: white with a dark outline
local function outlined(props)
	props.TextStrokeTransparency = props.TextStrokeTransparency or 0.35
	props.TextStrokeColor3 = Color3.new(0, 0, 0)
	return label(props)
end

-- Little red counter in the corner of a button
local function makeBadge(parent)
	local badge = make("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.new(1, -4, 0, 4),
		Size = UDim2.fromOffset(24, 24),
		BackgroundColor3 = C.bad,
		Visible = false,
		ZIndex = 5,
		Parent = parent,
	}, { Kit.round(), Kit.stroke(Color3.new(1, 1, 1), 0.2, 2) })
	local text = label({
		Font = Kit.FONT_BLACK,
		TextSize = 13,
		TextXAlignment = Enum.TextXAlignment.Center,
		TextTruncate = Enum.TextTruncate.None,
		Size = UDim2.fromScale(1, 1),
		ZIndex = 6,
		Parent = badge,
	})
	return function(count)
		badge.Visible = count > 0
		text.Text = tostring(count)
	end
end

-- Top left: leave and quests
local leaveButton = button("🚪  Leave crew", "danger", {
	Size = UDim2.fromOffset(170, 48),
	Font = Kit.FONT_BLACK,
	TextSize = 15,
	Parent = partyBody,
})
local partyQuestsButton = button("📜  Quests", "gold", {
	Position = UDim2.fromOffset(182, 0),
	Size = UDim2.fromOffset(150, 48),
	Font = Kit.FONT_BLACK,
	TextSize = 15,
	Parent = partyBody,
})
local setPartyQuestBadge = makeBadge(partyQuestsButton)

-- Top middle: host and target
local infoRow = make("Frame", {
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 4),
	Size = UDim2.fromOffset(0, 40),
	AutomaticSize = Enum.AutomaticSize.X,
	BackgroundTransparency = 1,
	Parent = partyBody,
}, { Kit.listLayout(10, true) })
local function infoPill(order, strokeColor)
	local pill = make("Frame", {
		Size = UDim2.fromOffset(0, 40),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = C.well,
		BackgroundTransparency = 0.2,
		LayoutOrder = order,
		Parent = infoRow,
	}, { Kit.round(), Kit.stroke(strokeColor, 0.5), Kit.pad(0, 18) })
	return label({
		Font = Kit.FONT_BOLD,
		TextSize = 14,
		TextTruncate = Enum.TextTruncate.None,
		Size = UDim2.fromScale(0, 1),
		AutomaticSize = Enum.AutomaticSize.X,
		Parent = pill,
	})
end
local hostText = infoPill(1, C.gold)
hostText.TextColor3 = C.gold
local targetText = infoPill(2, C.stroke)

-- Top right: party code and invite
local codeCard = make("Frame", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.fromScale(1, 0),
	Size = UDim2.fromOffset(320, 66),
	BackgroundColor3 = C.card,
	BackgroundTransparency = 0.1,
	Parent = partyBody,
}, { Kit.corner(14), Kit.stroke(C.stroke, 0.4), Kit.pad(9, 14) })
Kit.sectionTitle("PARTY CODE  ·  SHARE WITH FRIENDS", { TextSize = 10, Size = UDim2.new(1, -70, 0, 12), Parent = codeCard })
local codeChips = make("Frame", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(0, 18),
	Size = UDim2.new(1, -70, 0, 28),
	Parent = codeCard,
}, { Kit.listLayout(6, true) })
local chips = {}
for i = 1, Config.CODE_LENGTH do
	chips[i] = label({
		Font = Kit.FONT_CODE,
		TextSize = 20,
		TextColor3 = C.gold,
		TextXAlignment = Enum.TextXAlignment.Center,
		BackgroundTransparency = 0,
		BackgroundColor3 = C.well,
		Size = UDim2.fromOffset(32, 28),
		LayoutOrder = i,
		Parent = codeChips,
	})
	Kit.corner(6).Parent = chips[i]
end
local newCodeButton = button("New", "ghost", {
	AnchorPoint = Vector2.new(1, 0.5),
	Position = UDim2.fromScale(1, 0.5),
	Size = UDim2.fromOffset(60, 34),
	TextSize = 13,
	Parent = codeCard,
})
local inviteButton = button("💌  Invite friends", "blue", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, 0, 0, 76),
	Size = UDim2.fromOffset(320, 46),
	Font = Kit.FONT_BLACK,
	TextSize = 16,
	Parent = partyBody,
})

-- The 3D hideout itself. Hosts get Kick / Make host buttons on each name tag.
-- If the 3D scene fails for any reason, the rest of the lobby still works without it.
local hideoutOk, hideout = pcall(Hideout.new, hideoutLayer, {
	kick = function(userId)
		request("Kick", userId)
	end,
	makeHost = function(userId, displayName)
		if request("TransferHost", userId) then
			Kit.toast(displayName .. " is the host now.")
		end
	end,
})
if not hideoutOk then
	warn("[Lobby] The 3D hideout failed to build (please send this line):", hideout)
	hideout = nil
end

---------------------------------------------------------------- party room: crew chat
-- Bottom left, see-through: just outlined text over the hideout, no box behind it.

local chatFrame = make("Frame", {
	AnchorPoint = Vector2.new(0, 1),
	Position = UDim2.fromScale(0, 1),
	Size = UDim2.fromOffset(460, 290),
	BackgroundTransparency = 1,
	Parent = partyBody,
})
outlined({
	Text = "💬  CREW CHAT",
	Font = Kit.FONT_BLACK,
	TextSize = 13,
	TextColor3 = Color3.fromRGB(220, 226, 240),
	Size = UDim2.new(1, 0, 0, 16),
	Parent = chatFrame,
})
local chatList = Kit.scroller({
	Position = UDim2.fromOffset(0, 22),
	Size = UDim2.new(1, 0, 1, -72),
	ScrollBarThickness = 3,
	ScrollBarImageTransparency = 0.5,
	Parent = chatFrame,
}, 4)
-- Newest messages sit at the bottom, just above the typing line
chatList:FindFirstChildOfClass("UIListLayout").VerticalAlignment = Enum.VerticalAlignment.Bottom
local chatEmpty = outlined({
	Text = "👋  Say hi and plan the job.",
	TextSize = 15,
	TextColor3 = Color3.fromRGB(205, 210, 228),
	TextYAlignment = Enum.TextYAlignment.Bottom,
	Position = UDim2.fromOffset(0, 22),
	Size = UDim2.new(1, 0, 1, -76),
	Parent = chatFrame,
})
-- The typing line: only a thin outline, no filled box
local chatField = make("Frame", {
	AnchorPoint = Vector2.new(0, 1),
	Position = UDim2.fromScale(0, 1),
	Size = UDim2.new(1, -52, 0, 42),
	BackgroundTransparency = 1,
	Parent = chatFrame,
}, { Kit.round() })
local chatFieldStroke = Kit.stroke(Color3.new(1, 1, 1), 0.6, 1.5)
chatFieldStroke.Parent = chatField
local chatBox = make("TextBox", {
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Font = Kit.FONT_BOLD,
	TextSize = 16,
	TextColor3 = Color3.new(1, 1, 1),
	TextStrokeTransparency = 0.35,
	TextXAlignment = Enum.TextXAlignment.Left,
	TextTruncate = Enum.TextTruncate.AtEnd,
	PlaceholderText = "Click here to message your crew...",
	PlaceholderColor3 = Color3.fromRGB(200, 205, 220),
	Text = "",
	ClearTextOnFocus = false,
	Parent = chatField,
}, { Kit.pad(0, 16) })
chatBox.Focused:Connect(function()
	Kit.tween(chatFieldStroke, Kit.FAST, { Transparency = 0.1 })
end)
chatBox.FocusLost:Connect(function()
	Kit.tween(chatFieldStroke, Kit.FAST, { Transparency = 0.6 })
end)
local sendButton = Kit.makeRound(button("➤", "blue", {
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.fromScale(1, 1),
	Size = UDim2.fromOffset(42, 42),
	TextSize = 16,
	Parent = chatFrame,
}))

---------------------------------------------------------------- party room: heist settings tab
-- A pop-up panel above the "Heist settings" button, so it doesn't cover the hideout

local settingsPanel = make("Frame", {
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.new(1, 0, 1, -112),
	Size = UDim2.fromOffset(SETTINGS_WIDTH, 460),
	BackgroundTransparency = 1,
	Visible = false,
	Parent = partyBody,
})
local settingsPanelScale = make("UIScale", { Parent = settingsPanel })
local settingsCard = Kit.card({
	Size = UDim2.fromScale(1, 1),
	BackgroundColor3 = C.window,
	BackgroundTransparency = 0.04,
	Parent = settingsPanel,
}, true)
Kit.pad(14).Parent = settingsCard
Kit.listLayout(10).Parent = settingsCard
local settingsTitle = Kit.sectionTitle("", { LayoutOrder = 0, Parent = settingsCard })
local closeSettingsButton = Kit.makeRound(button("✕", "ghost", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, -10, 0, 8),
	Size = UDim2.fromOffset(30, 30),
	TextSize = 13,
	ZIndex = 2,
	Parent = settingsPanel,
}))

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

---------------------------------------------------------------- party room: bottom right

local ACTION_HEIGHT = 58
local settingsTabButton = button("⚙  HEIST SETTINGS", "ghost", {
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.new(1, -272, 1, 0),
	Size = UDim2.fromOffset(230, ACTION_HEIGHT),
	Font = Kit.FONT_BLACK,
	TextSize = 15,
	Parent = partyBody,
})
local function footerButton(text, style)
	return button(text, style, {
		AnchorPoint = Vector2.new(1, 1),
		Position = UDim2.fromScale(1, 1),
		Size = UDim2.fromOffset(260, ACTION_HEIGHT),
		Font = Kit.FONT_BLACK,
		TextSize = 19,
		Visible = false,
		Parent = partyBody,
	})
end
local startButton = footerButton("START HEIST", "green")
local cancelButton = footerButton("CANCEL START", "danger")
local readyButton = footerButton("I'M READY", "green")
local unreadyButton = footerButton("✔ READY  ·  undo", "ghost")
unreadyButton.TextColor3 = C.good
unreadyButton.TextSize = 16
local footerHint = outlined({
	RichText = true,
	Font = Kit.FONT_BOLD,
	TextSize = 15,
	TextXAlignment = Enum.TextXAlignment.Right,
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.new(1, 0, 1, -(ACTION_HEIGHT + 12)),
	Size = UDim2.fromOffset(560, 24),
	Parent = partyBody,
})

local settingsOpen = false
local function setSettingsOpen(open)
	settingsOpen = open
	settingsTabButton.Text = open and "⚙  HEIST SETTINGS  ▼" or "⚙  HEIST SETTINGS  ▲"
	footerHint.Visible = not open
	if open then
		settingsPanel.Visible = true
		settingsPanelScale.Scale = 0.92
		Kit.tween(settingsPanelScale, Kit.BOUNCE, { Scale = 1 })
	else
		settingsPanel.Visible = false
	end
end
setSettingsOpen(false)
onClick(settingsTabButton, function()
	setSettingsOpen(not settingsOpen)
end)
onClick(closeSettingsButton, function()
	setSettingsOpen(false)
end)

---------------------------------------------------------------- quests

-- Full-screen pop-up with every quest, its progress and a Claim button
local questsOverlay = make("Frame", {
	Name = "Quests",
	Size = UDim2.fromScale(1, 1),
	BackgroundColor3 = C.backdrop,
	BackgroundTransparency = 0.3,
	Active = true,
	Visible = false,
	ZIndex = 20,
	Parent = gui,
})
local questsPanel = make("Frame", {
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Size = UDim2.fromOffset(700, 560),
	BackgroundColor3 = Color3.new(1, 1, 1),
	Parent = questsOverlay,
}, {
	Kit.corner(18),
	Kit.gradient(C.windowTop, C.window),
	Kit.stroke(C.gold, 0.5, 1.5),
	Kit.pad(22, 24),
})
local questsScale = make("UIScale", { Parent = questsPanel })
label({
	Text = "📜  QUESTS",
	Font = Kit.FONT_BLACK,
	TextSize = 26,
	Size = UDim2.new(1, -60, 0, 30),
	Parent = questsPanel,
})
label({
	Text = "Finish quests to earn cash and XP. Every quest has a few tiers, each one bigger.",
	TextSize = 14,
	TextColor3 = C.muted,
	Position = UDim2.fromOffset(0, 34),
	Size = UDim2.new(1, -60, 0, 18),
	Parent = questsPanel,
})
local closeQuestsButton = Kit.makeRound(button("✕", "ghost", {
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.fromScale(1, 0),
	Size = UDim2.fromOffset(40, 40),
	TextSize = 15,
	Parent = questsPanel,
}))
local questsList = Kit.scroller({
	Position = UDim2.fromOffset(0, 68),
	Size = UDim2.new(1, 0, 1, -68),
	Parent = questsPanel,
}, 10)

local questBadgeSetters = { setPartyQuestBadge }

-- The server's quest list: { id, icon, title, tiers, claimed, value } per quest
local questsState = nil

local function questRow(quest, order)
	local claimed = quest.claimed or 0
	local tier = quest.tiers[claimed + 1]
	local value = quest.value or 0
	local done = tier == nil
	local ready = not done and value >= tier.goal

	local row = make("Frame", {
		BackgroundColor3 = C.cardRaised,
		Size = UDim2.new(1, -8, 0, 84),
		LayoutOrder = order,
		Parent = questsList,
	}, { Kit.corner(12), Kit.pad(12, 14), Kit.stroke(ready and C.gold or C.stroke, ready and 0.2 or 0.5, ready and 2 or 1) })
	make("Frame", {
		AnchorPoint = Vector2.new(0, 0.5),
		Position = UDim2.fromScale(0, 0.5),
		Size = UDim2.fromOffset(54, 54),
		BackgroundColor3 = C.well,
		Parent = row,
	}, {
		Kit.round(),
		label({
			Text = quest.icon,
			TextSize = 28,
			TextTruncate = Enum.TextTruncate.None,
			TextXAlignment = Enum.TextXAlignment.Center,
			Size = UDim2.fromScale(1, 1),
		}),
	})
	label({
		RichText = true,
		Text = ('%s   <font color="%s" size="11">TIER %d/%d</font>'):format(
			Kit.escape(quest.title),
			Kit.hex(C.muted),
			math.min(claimed + 1, #quest.tiers),
			#quest.tiers
		),
		Font = Kit.FONT_BOLD,
		TextSize = 16,
		Position = UDim2.fromOffset(68, 0),
		Size = UDim2.new(1, -200, 0, 20),
		Parent = row,
	})

	local goal = done and quest.tiers[#quest.tiers].goal or tier.goal
	local bar = make("Frame", {
		Position = UDim2.fromOffset(68, 28),
		Size = UDim2.new(1, -270, 0, 10),
		BackgroundColor3 = C.well,
		Parent = row,
	}, { Kit.round() })
	make("Frame", {
		Size = UDim2.fromScale(math.clamp(value / goal, 0, 1), 1),
		BackgroundColor3 = Color3.new(1, 1, 1),
		Parent = bar,
	}, { Kit.round(), Kit.gradient(C.gold, C.robber, 0) })
	label({
		Text = ("%s / %s"):format(fmt(math.min(value, goal)), fmt(goal)),
		Font = Kit.FONT_BOLD,
		TextSize = 12,
		TextColor3 = C.muted,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -130, 0, 25),
		Size = UDim2.fromOffset(64, 16),
		TextXAlignment = Enum.TextXAlignment.Right,
		Parent = row,
	})
	label({
		RichText = true,
		Text = done and "All tiers done. Nice work!"
			or ('Reward: <font color="%s">💵 $%s</font>   <font color="%s">⭐ %s XP</font>'):format(
				Kit.hex(C.gold),
				fmt(tier.cash),
				Kit.hex(C.cop),
				fmt(tier.xp)
			),
		TextSize = 13,
		TextColor3 = C.muted,
		Position = UDim2.fromOffset(68, 44),
		Size = UDim2.new(1, -200, 0, 16),
		Parent = row,
	})

	if done then
		Kit.pill("✔ COMPLETE", C.good, {
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.fromScale(1, 0.5),
			Parent = row,
		})
	else
		local claim = button(ready and "CLAIM" or "In progress", ready and "green" or "ghost", {
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.fromScale(1, 0.5),
			Size = UDim2.fromOffset(116, 42),
			Font = Kit.FONT_BLACK,
			TextSize = ready and 15 or 12,
			Parent = row,
		})
		if ready then
			onClick(claim, function()
				local ok, reward = request("ClaimQuest", quest.id)
				if ok and type(reward) == "table" then
					Kit.toast(("%s Quest done! +$%s and +%s XP"):format(quest.icon, fmt(reward.cash), fmt(reward.xp)))
				end
			end)
		else
			Kit.setEnabled(claim, false)
		end
	end
	return ready
end

local function renderQuests()
	if not questsState then
		return
	end
	Kit.clearRows(questsList)
	local claimable = 0
	for i, quest in ipairs(questsState) do
		if questRow(quest, i) then
			claimable += 1
		end
	end
	for _, setBadge in ipairs(questBadgeSetters) do
		setBadge(claimable)
	end
end

local function showQuests(open)
	questsOverlay.Visible = open
	if open then
		renderQuests()
		task.spawn(function()
			local ok, list = request("GetQuests")
			if ok and type(list) == "table" then
				questsState = list
				renderQuests()
			end
		end)
		local base = questsScale:GetAttribute("Fit") or 1
		questsScale.Scale = base * 0.9
		Kit.tween(questsScale, Kit.BOUNCE, { Scale = base })
	end
end
onClick(partyQuestsButton, function()
	showQuests(true)
end)
onClick(closeQuestsButton, function()
	showQuests(false)
end)

---------------------------------------------------------------- party room: screen layout

-- Called from fit() whenever the screen size or the menu scale changes
local function layoutParty(screen, scale)
	for _, layer in ipairs(scaledLayers) do
		layer.frame.Size = UDim2.fromScale(1 / scale, 1 / scale)
		layer.scale.Scale = scale
	end
	if hideout then
		hideout:setUiScale(scale)
	end
	local height = screen.Y / scale
	-- Just under the top bar (the canvas is centred on the screen)
	local top = math.floor((height - CANVAS.Y) / 2 + 96)
	partyBody.Position = UDim2.fromOffset(PARTY_EDGE, top)
	partyBody.Size = UDim2.new(1, -2 * PARTY_EDGE, 1, -(top + PARTY_EDGE))
	local bodyHeight = height - top - PARTY_EDGE
	-- The settings panel opens above the buttons and stops below the invite button
	settingsPanel.Size = UDim2.fromOffset(SETTINGS_WIDTH, math.clamp(bodyHeight - (ACTION_HEIGHT + 12) - 140, 220, 560))
	questsScale:SetAttribute("Fit", math.min(scale, 1.2))
	questsScale.Scale = math.min(scale, 1.2)
end

table.insert(questBadgeSetters, makeBadge(hubQuestsButton))
onClick(hubQuestsButton, function()
	showQuests(true)
end)

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

-- The full-screen party room only shows on the Play tab while you're in a crew
local function updatePartyLayers()
	local show = currentParty ~= nil and currentTab == "Play"
	hideoutLayer.Visible = show
	partyLayer.Visible = show
end
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
	updatePartyLayers()

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
	renderQuests()
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
		targetText.Text = ("👥  %d/%d   ·   🎯  Target: %s %s"):format(#members, settings.partySize, map.icon, map.name)
	else
		mapIcon.Text = "❓"
		mapName.Text = "Random place"
		mapBlurb.Text = "A surprise target, picked when the heist starts."
		mapMeta.Text = ('Difficulty <font color="%s">???</font>   Loot <font color="%s">???</font>'):format(
			Kit.hex(C.robber),
			Kit.hex(C.gold)
		)
		mapCardStroke.Color = C.stroke
		targetText.Text = ("👥  %d/%d   ·   🎯  Target: 🎲 Random"):format(#members, settings.partySize)
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
		Font = Kit.FONT_BOLD,
		TextSize = 16,
		TextColor3 = Color3.new(1, 1, 1),
		TextStrokeTransparency = 0.3,
		TextStrokeColor3 = Color3.new(0, 0, 0),
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
		if inParty then
			setSettingsOpen(false)
		else
			hubView.Position = UDim2.fromOffset(0, 24)
			Kit.tween(hubView, Kit.SMOOTH, { Position = UDim2.new() })
		end
	end
	hubView.Visible = not inParty
	partyView.Visible = inParty
	updatePartyLayers()
	if fit then
		fit()
	end
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

	if hideout then
		local ok, err = pcall(hideout.setMembers, hideout, members, settings.partySize, host)
		if not ok then
			warn("[Lobby] The 3D hideout failed to show the crew (please send this line):", err)
		end
	end
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
	elseif kind == "Quests" then
		questsState = data
		renderQuests()
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

function fit()
	local screen = gui.AbsoluteSize
	if screen.X == 0 or screen.Y == 0 then
		return
	end
	local cap = currentParty ~= nil and PARTY_SCALE_CAP or MENU_SCALE_CAP
	fitScale = math.min(cap, (screen.X - 16) / CANVAS.X, (screen.Y - 16) / CANVAS.Y)
	applyScale()
	Overlays.setFit(math.min(1, fitScale))
	layoutParty(screen, fitScale)
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

print("[Lobby] LobbyClient v5 ready")
