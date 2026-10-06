# FlintFix multi-version plan (handoff)

Goal: ship the FlintFix in-game client (Fabric mod in
`FlintFix-Fabric-v56-four-new-mods (3)/flintfix-mod`) for the same Minecraft
versions as Lunar Client, and have the launcher use a prebuilt jar per version.

## Current state (branch `claude/clever-johnson-wumk7u`)
- Stonecutter 0.7.11 multi-version build (Gradle 8.14.3 wrapper, Loom 1.10.1,
  Kotlin DSL). One shared `src/`, per-version settings in
  `flintfix-mod/versions/<mc>/gradle.properties`. Active (editable) version: 1.21.1.
- Group 1 done except 1.21.5+: **1.20.1, 1.20.4, 1.20.6, 1.21.1, 1.21.4** all
  compile and pass a runtime mixin audit (`./gradlew :<mc>:runAudit` starts the
  game, force-applies every mixin, logs `FLINTFIX_AUDIT_OK`/`FAILED`, quits).
  1.20.1/1.20.4 build for Java 17, the rest for Java 21.
- Version differences live in `FlintFixCompat` (rendering/API shims),
  `FlintFixScreen` (Screen API: blur, scroll) and `//? if` blocks in the mixins.
  Not yet checked by hand in-game on every version: custom sky look on 1.21.4
  (new frame-graph sky pass), title glow on 1.21.4, motion blur on 1.21.4.
- Build: `./gradlew buildAll` -> `build/libs/all/flintfix-client-mod-<ver>+<mc>.jar`;
  one version: `./gradlew :1.20.1:build` or `build-mod.ps1 -MinecraftVersion 1.20.1`.
- Launcher (`minecraft/fabric.js`, `renderer.js`) now enables FlintFix for every
  version in its supported list and builds the matching jar locally.
- GitHub Actions workflow builds all versions (`buildAll`), but the user's GitHub
  account is billing-locked for Actions, so builds run in the Claude session.

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
