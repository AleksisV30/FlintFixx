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
