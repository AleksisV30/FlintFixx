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
