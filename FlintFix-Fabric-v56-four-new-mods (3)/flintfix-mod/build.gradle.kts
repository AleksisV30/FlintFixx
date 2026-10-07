import net.fabricmc.loom.api.LoomGradleExtensionAPI

fun prop(name: String) = project.property(name) as String

val mcVersion = stonecutter.current.version
// 26.1+ ships without obfuscation: plain Loom, no mappings and no jar remapping.
val unobfuscated = stonecutter.eval(mcVersion, ">=26.1")
val javaVersion = when {
    unobfuscated -> 25
    stonecutter.eval(mcVersion, ">=1.20.5") -> 21
    else -> 17
}

apply(plugin = if (unobfuscated) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")
val loom = the<LoomGradleExtensionAPI>()

version = "${prop("mod_version")}+$mcVersion"
group = prop("maven_group")
the<BasePluginExtension>().archivesName = prop("archives_base_name")

repositories { mavenCentral() }

dependencies {
    "minecraft"("com.mojang:minecraft:$mcVersion")
    if (unobfuscated) {
        "implementation"("net.fabricmc:fabric-loader:${prop("loader_version")}")
        "implementation"("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")
    } else {
        "mappings"(loom.officialMojangMappings())
        "modImplementation"("net.fabricmc:fabric-loader:${prop("loader_version")}")
        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")
    }
}

loom.runs {
    // ./gradlew :<mc>:runAudit starts the game, applies every mixin and quits (see FlintFixClient).
    register("audit") {
        inherit(getByName("client"))
        configName = "Mixin audit"
        vmArgs("-Dflintfix.auditMixins=true", "-Dmixin.debug.countInjections=true")
    }
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to project.version,
        "minecraft" to prop("mc_dep"),
        "java" to javaVersion,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "flintfix.mixins.json")) { expand(props) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000"))
}

configure<JavaPluginExtension> {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.register<Copy>("collectJar") {
    group = "build"
    from(tasks.named<AbstractArchiveTask>(if (unobfuscated) "jar" else "remapJar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/all"))
}
