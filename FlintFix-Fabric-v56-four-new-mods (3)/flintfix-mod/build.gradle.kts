plugins {
    id("net.fabricmc.fabric-loom-remap")
}

fun prop(name: String) = project.property(name) as String

val mcVersion = stonecutter.current.version
val javaVersion = if (stonecutter.eval(mcVersion, ">=1.20.5")) 21 else 17

version = "${prop("mod_version")}+$mcVersion"
group = prop("maven_group")
base { archivesName = prop("archives_base_name") }

repositories { mavenCentral() }

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${prop("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")
}

loom {
    runs {
        // ./gradlew :<mc>:runAudit starts the game, applies every mixin and quits (see FlintFixClient).
        register("audit") {
            inherit(getByName("client"))
            configName = "Mixin audit"
            vmArgs("-Dflintfix.auditMixins=true", "-Dmixin.debug.countInjections=true")
        }
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "minecraft" to prop("mc_dep"),
        "java" to javaVersion,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "flintfix.mixins.json")) { expand(props) }
}

tasks.withType<JavaCompile>().configureEach { options.release = javaVersion }

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.register<Copy>("collectJar") {
    group = "build"
    from(tasks.remapJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/all"))
}
