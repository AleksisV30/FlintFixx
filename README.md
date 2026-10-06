# FlintFix Client 1.0

- **Loading screen:** the launcher opens with an animated FlintFix splash that shows what it is loading, then fades into Home. The window only appears once the splash has painted, so there is no white flash.
- **Fabric first:** Fabric is the default loader and Minecraft 1.21.1 the default version. Fabric now installs on every Minecraft version Fabric supports, with the matching Fabric API. The FlintFix in-game client is built for 1.21.1, so other versions launch with Fabric only (the launcher says so). The FlintFix and Fabric API files of other versions are removed from the shared mods folder automatically.
- **Forge and NeoForge** are listed as "Coming soon" and can't be selected yet.
- **Launcher polish:** one accent color everywhere (buttons, toggles, radios, labels), a FLINTFIX tag on 1.21.1 in the version list, page fade-ins, and the loader card shows what each loader does.

## v59


- **Servers tab:** save favorite servers, see their message, players online and ping, and join with one click using the instance you pick. A few popular servers are listed too.
- **Skins tab:** a 3D preview (idle, walk, run), a skin library (import PNGs or save the skin you're wearing), Classic or Slim arms, and one click to put a skin on your Minecraft account or go back to a default skin.
- **What's new** on the Home page, read from `news.json` (from GitHub when online, the bundled copy otherwise).
- **Auto-update:** the launcher checks GitHub Releases. Installed builds download the update in the background and offer "Restart now"; a copy started with `npm start` shows a download link.
- **Performance mode** (Mods page): installs Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling and ModernFix, only builds made for the instance's Minecraft version.
- **Waypoints:** press B to drop a waypoint where you stand; manage them (color, show/hide, delete) from the Waypoints module. Waypoints show a light beam, a label with the distance through walls, and a marker on the compass bar. A "Death" waypoint is added where you die. Saved per world/server and dimension.
- **Server Profiles:** link a server to one of your module profiles; it becomes active when you join and the previous one comes back when you leave.
- **Motion blur** now works without shader files, by blending each frame with the previous one.
- **Show Hand:** the arm faces the right way, and the new Arm turn, Hand height and Hand depth options let you fine-tune it.

## Releasing an update

1. Raise `version` in `package.json` (and `mod_version` in `flintfix-mod/gradle.properties`) and add a post to `news.json`.
2. Create a GitHub token with `repo` access and set it: `$env:GH_TOKEN = "<token>"`.
3. In the project folder run `npm install`, then `npm run release`. This builds the Windows installer and publishes it as a GitHub Release, which every installed copy picks up automatically.

Use `npm run dist` to build the installer locally without publishing.

## v58

Ten new modules. Each has a card in the module dashboard (click to toggle, gear or right-click for options).

- **Potion Effects HUD:** active effects with their icon, level and time left; the time blinks red during the last ten seconds.
- **Speed Meter:** your speed in blocks per second or km/h; counts height too while gliding with an elytra.
- **Compass Bar:** a heading strip (N, NE, E...) at the top of the screen, with a red marker pointing at your last death.
- **Block Outline:** any color or rainbow, adjustable thickness, optional translucent fill.
- **Crosshair:** cross, dot, circle, cross with dot or X; size, gap, thickness, color, dark outline, hit marker, and the attack cooldown bar.
- **Low Overlays:** lowers the fire overlay and the shield, and shrinks the totem pop animation, each adjustable.
- **Damage Numbers:** red numbers float up from mobs and players when they take damage (bigger hits are larger and deeper red), green for healing.
- **No Weather:** hides rain, snow and thunder for you only.
- **Motion Blur:** cinematic frame blending with adjustable strength; the hand and HUD stay sharp.
- **Team Glow:** outlines your FlintFix friends (from the launcher's Chat) and scoreboard teammates in a color you pick.

Potion Effects, Speed Meter and Compass Bar can be moved and resized in the HUD editor.

## v57

Release-polish pass: fixes, a cinematic sky, a resource pack browser, and redesigned HUD widgets.

- **Resource packs in the launcher.** A new Packs tab searches popular Modrinth resource packs by Minecraft version, category and popularity. Each card shows the pack's screenshot; click it for a full preview with the image gallery, description and stats. Download puts the pack in your game and turns it on for the next launch; the Installed view turns packs on/off or removes them.
- **Show Hand holds the item.** The arm is now drawn in the item's own frame, so the fist closes around the handle and follows every swing, bow draw and bite.
- **New Item Inspect animation.** The hand lifts the item into view, flips it over in the fingers to show the back, tilts it to show the edge, then flips it back while lowering it.
- **Sun always visible.** The sun has a solid core with soft bloom on top, the sky around it never brightens to white, and it sinks smoothly below the horizon instead of disappearing or showing through the ground.
- **More cinematic Custom Sky.** Smoother horizon gradients, a scattering halo around the sun, warm twilight on the sun's side with a pink "Belt of Venus" and Earth shadow opposite, stars and the Milky Way that fade near the horizon, and aurora curtains with vertical rays.
- **Video Settings stay in sync.** Changes made on the FlintFix Video Settings page now show up on Minecraft's own Video Settings page, which previously kept showing (and could save back) the old values.
- **HUD widgets redesigned.** FPS, CPS, Coordinates, Ping, Keystrokes and Armor are measured from the real text size, so text never overlaps at any GUI scale. They have consistent padding, an accent edge, status colors, and text shadows when the background is off. Keystrokes adds a space bar.
- **Armor HUD rebuilt.** Each row shows the item, its remaining durability with a percentage and a bar, includes the off hand, and pulses red when an item is about to break.
- **Trajectory preview.** A smooth glowing arc with flowing dashes, a pulsing ring lying flat on the block face it will hit, entity hit detection with a red highlight, and support for snowballs, eggs, splash/lingering potions and charged tridents.
- **Light theme redesigned.** White cards on a cool off-white window, soft borders, an indigo accent, and status colors deepened for readability.
- The launcher now always loads the newest built mod JAR after an update.

## v56

Performance + in-game UI polish pass.

- In-game UI redesign: every FlintFix screen now uses one shared component set in `FlintFixUi` (window frame, header, buttons, switches, sliders, search fields, badges) drawn from the selected theme's colors, so all screens recolor smoothly when the theme changes.
- Larger, more readable module dashboard. Each card shows an icon, an animated switch, a name and a short description. Click a card to toggle it; use the gear or right-click for options. HUD Layout and Themes now live in the sidebar.
- The Right Shift home screen is now a proper window with Open Modules plus HUD Layout, Themes and Profiles shortcuts.
- Custom Sky module with six presets (Aurora, Golden Hour, Cosmic, Crystal Day, Pastel Dream, Eternal Night). The procedural overworld sky follows the time of day and the weather, and the fog is tinted to match. The vanilla sky comes back underwater, in the Nether and End, and with blindness. Pick a preset from the module's options or from Video Settings.
- The Custom Sky screen has an Effects list for switching each effect on or off: stars, Milky Way, shooting stars, aurora, nebula clouds, sun and moon glow, sun rays, moon phases, horizon glow and matching fog. Two new presets were added: Synthwave and Blood Moon.
- HUD widgets (FPS, CPS, Coordinates, Ping, Keystrokes, Armor) were restyled to follow the selected theme. Keys animate when pressed, and FPS, CPS and Armor show colored status bars and dots.
- Show Hand module draws your arm holding items in first person. During Item Inspect the arm stays on screen when Show Hand is on; otherwise it slides in from below the screen and back out.
- Fullbright module lights every block fully. Your saved Brightness option is never changed.
- The Custom Sky screen has a Sky time option (Follow world / Day / Sunset / Night) that changes only how the sky looks, so night effects can be previewed at any time.
- Video Settings use sliders for render distance, simulation distance, max frame rate, entity distance, field of view and brightness. A tick on each slider marks the suggested value.
- UI text is snapped to whole screen pixels so it renders sharp instead of soft.
- Item Inspect: press I (rebindable under Controls → FlintFix Client) to spin and show off the item in your main hand. It stops when you switch items, attack or use the item.
- Redesigned Video Settings: a PC check card, one-click Performance / Balanced / Quality presets, stepper and switch controls with "matches suggestion" hints, scroll-to-change, and a Custom Sky row.
- New title screen: an animated night background with drifting glows and stars, a glowing FlintFix logo in place of the Minecraft logo, and glassy buttons.
- New distinct Lucide-style icons for Freecam, Hitboxes, Zoom, Look Around, Trajectory, Chunks and Shulkers. Their sources are under `icons-source/`.

- Faster first-run preparation with higher safe download concurrency, keep-alive HTTPS, parallel Fabric preparation, and reuse of an existing JDK 21 when available.
- Launcher console now logs preparation stages and shows an adaptive approximate launch ETA.
- Launch duration is remembered per Minecraft version/loader to improve later ETA estimates.
- In-game FlintFix panels use a more compact size.
- Module and navigation icons use Lucide assets; the original SVGs and license are included under `flintfix-mod/src/main/resources/assets/flintfix/icons-source/`.
- The original custom font and rounded-panel artwork are retained.
- The game stays available when Minecraft launches, and the vanilla title buttons have more vertical spacing.
- The in-game dashboard uses a translucent, cleaner three-column layout with visible sidebar labels.
- Added a FlintFix-themed video settings panel from Minecraft's Video Settings page, with performance and visual controls that follow the selected FlintFix theme.
- The PC check estimates a performance profile from CPU thread count, Minecraft's Java heap cap, and the active graphics renderer; applying its FPS recommendations is optional.
- Added a draggable armor points and durability HUD widget.
- Added an optional projectile trajectory preview for held ender pearls, drawn bows, XP bottles, and loaded crossbows; the path predicts block impacts and marks the estimated hit point.
- Added four in-game modules: entity hitboxes, press-and-hold camera zoom with scroll-wheel adjustment, independent third-person look-around, and a 27-slot shulker inventory preview with item icons and counts on hover.
- Zoom and Look Around can be rebound in Minecraft Controls → FlintFix Client (defaults C and V). Their camera changes end automatically when the key is released.
- The FlintFix client key is registered in Minecraft's Controls menu under FlintFix Client.
- Freecam is available from the in-game module cards or its Controls keybind (default G). Its Options card lets you rebind the same key for enabling and disabling it. It shows a “Use at your own risk!” notice before activation; servers may prohibit it.
- The installer reuses the desktop app's Java 21 build JDK when present and uses resumable `curl.exe` downloads on Windows, avoiding repeat JDK downloads and PowerShell transfer overhead.

## Build and install on Windows

The ZIP contains the Fabric mod source. To build it and install it into the default Minecraft `mods` folder, open PowerShell in this folder and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\install-flintfix.ps1
```

The script uses an existing Java 21 JDK or downloads a portable one for the build, backs up older FlintFix JARs, and installs the new JAR into the standard Minecraft folder and FlintFix Client's managed game folder. For a different launcher instance, pass its `mods` folder with `-ModsDirectory`.
