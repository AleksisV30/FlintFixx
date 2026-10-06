# FlintFix multi-version plan (handoff)

Goal: ship the FlintFix in-game client (Fabric mod in
`FlintFix-Fabric-v56-four-new-mods (3)/flintfix-mod`) for the same Minecraft
versions as Lunar Client, and have the launcher use a prebuilt jar per version.

## Current state (branch `claude/clever-johnson-wumk7u`)
- Mod targets Fabric 1.21.1 only (yarn 1.21.1+build.3, loader 0.16.14,
  Fabric API 0.116.16+1.21.1, Loom 1.10.1, Gradle 8.14.3, Java 21).
  ~59 Java files, ~10k lines, 18 mixins (see `flintfix.mixins.json`).
- The mod has never been compiled outside the user's PC. First step: build it
  as-is and fix any compile errors.
- Launcher (`minecraft/fabric.js`) installs Fabric + matching Fabric API on any
  version; it copies the FlintFix jar only for 1.21.1 (`MC_VERSION`). The jar is
  built locally via `build-mod.ps1` on first launch.
- GitHub Actions workflow `.github/workflows/build-mod.yml` exists but the
  user's GitHub account is billing-locked for Actions, so builds must run in
  the Claude session (network allow-list now includes Fabric/Mojang/Gradle hosts).

## Target versions (Lunar's list)
1. Group 1 (first): 1.20.1, 1.20.4, 1.20.6, 1.21.1, 1.21.4, then 1.21.5+
   (1.21.5/1.21.6 changed GUI rendering a lot; treat as its own step).
2. Group 2: 1.16.5, 1.17.1, 1.18.2, 1.19.4 (pre-DrawContext rendering:
   MatrixStack + DrawableHelper; needs a small rendering adapter).
3. Group 3: 1.7.10, 1.8.9, 1.12.2 need Legacy Fabric (Java 8, LWJGL2, very
   different code). Effectively a second client; discuss with the user before
   starting, likely 1.8.9 only.

## Approach
- Use Stonecutter (maven.kikugie.dev) for one codebase -> one jar per version.
- Keep version-independent logic (config, profiles, waypoints storage, HUD
  layout, theme) free of Minecraft calls; put Minecraft calls behind small
  adapters so each version only touches those.
- Build every version locally in the session after each change; never push code
  that doesn't compile.
- Launcher: ship/download the jar matching the selected version instead of
  building on the player's PC; update `fabric.js` (`MC_VERSION` check) to a
  supported-versions map.
