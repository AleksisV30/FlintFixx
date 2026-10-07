# FlintFix multi-version plan (handoff)

Goal: ship the FlintFix in-game client (Fabric mod in
`FlintFix-Fabric-v56-four-new-mods (3)/flintfix-mod`) for the same Minecraft
versions as Lunar Client, and have the launcher use a prebuilt jar per version.

## Current state (branch `claude/clever-johnson-wumk7u`)
- Stonecutter 0.7.11 multi-version build (Gradle 9.7.1 wrapper, Loom 1.18.3 via
  `net.fabricmc.fabric-loom-remap`, Mojang mappings, Kotlin DSL). Gradle itself
  must run on JDK 25. One shared `src/`, per-version settings in
  `flintfix-mod/versions/<mc>/gradle.properties`. Active (editable) version: 1.21.1.
- Group 1 done: **1.20.1, 1.20.2, 1.20.4, 1.20.6, 1.21.1, 1.21.3, 1.21.4,
  1.21.5, 1.21.6, 1.21.8, 1.21.10, 1.21.11** all compile and pass a runtime
  mixin audit (`./gradlew :<mc>:runAudit` starts the game, force-applies every
  mixin, logs `FLINTFIX_AUDIT_OK`/`FAILED`, quits). Each jar also covers its
  neighbours (1.21.8 jar = 1.21.7-1.21.8, 1.21.10 jar = 1.21.9-1.21.10, see
  `mc_dep`). 1.20.1/1.20.2/1.20.4 build for Java 17, the rest for Java 21.
- Version differences live in `FlintFixCompat` (rendering/API shims),
  `FlintFixScreen` (Screen API: blur, scroll, 1.21.9 input events) and `//? if`
  blocks in the mixins. 1.21.11 renames (ResourceLocation -> Identifier,
  GraphicsStatus -> GraphicsPreset) are Stonecutter string replacements in
  `stonecutter.gradle.kts`.
- Known gaps: motion blur is off on 1.21.5+ (no post-effect hook yet); the Show
  Hand option uses vanilla first-person transforms on 1.21.5+. Visuals (sky,
  title glow, outlines, chunk borders) are only audited, not playtested, on
  1.21.2+.
- Build: `./gradlew buildAll` -> `build/libs/all/flintfix-client-mod-<ver>+<mc>.jar`;
  one version: `./gradlew :1.20.1:build` or `build-mod.ps1 -MinecraftVersion 1.20.1`.
- Launcher (`minecraft/fabric.js` `FLINTFIX_TARGETS`, `renderer.js`
  `FLINTFIX_GAME_VERSIONS`) maps every supported game version to its jar.
- GitHub Actions workflow builds all versions (`buildAll`), but the user's GitHub
  account is billing-locked for Actions, so builds run in the Claude session.

## Next: 26.x
26.1+ ships unobfuscated: use the `net.fabricmc.fabric-loom` (no-remap) plugin,
no mappings line, `implementation` instead of `modImplementation`, Java 25.
Fabric API: 26.1.2 -> 0.155.3, 26.2 -> 0.161.0, 26.3 -> 0.162.0.

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
