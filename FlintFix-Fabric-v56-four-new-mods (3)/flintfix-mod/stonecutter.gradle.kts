plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18.3" apply false
    id("net.fabricmc.fabric-loom") version "1.18.3" apply false
}

stonecutter active "1.21.1" /* [SC] DO NOT EDIT */

// Copies every version's jar into build/libs/all so the launcher can pick one per Minecraft version.
tasks.register("buildAll") {
    group = "build"
    dependsOn(stonecutter.tasks.named("collectJar"))
}

stonecutter parameters {
    // 1.21.11 renamed ResourceLocation to Identifier and turned the graphics
    // mode into a preset (same FAST/FANCY values; setting one applies it).
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("ResourceLocation", "Identifier")
    }
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("GraphicsStatus", "GraphicsPreset")
    }
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("graphicsMode()", "graphicsPreset()")
    }
}
