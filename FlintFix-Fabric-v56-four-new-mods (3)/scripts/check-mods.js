// Stops the installer build when the FlintFix mod jars are missing. Without them
// the installed client can't load its in-game features ("build-mod.ps1 is
// missing"), and electron-builder only warns about a missing extraResources folder.

const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const libs = path.join(root, "flintfix-mod", "build", "libs", "all");
const source = fs.readFileSync(path.join(root, "minecraft", "fabric.js"), "utf8");
const targetsBlock = source.match(/const FLINTFIX_TARGETS = \{([\s\S]*?)\};/);
if (!targetsBlock) {
    console.error("Could not read FLINTFIX_TARGETS from minecraft/fabric.js.");
    process.exit(1);
}
const targets = [...new Set([...targetsBlock[1].matchAll(/:\s*"([^"]+)"/g)].map(match => match[1]))];

const jars = fs.existsSync(libs) ? fs.readdirSync(libs).filter(name => name.endsWith(".jar") && !name.endsWith("-sources.jar")) : [];
const missing = targets.filter(target => !jars.some(name => name.endsWith(`+${target}.jar`)));
if (missing.length) {
    console.error(`FlintFix mod jars are missing for: ${missing.join(", ")}`);
    console.error("Build them first: cd flintfix-mod, then .\\gradlew.bat buildAll (needs JDK 25), then build the installer again.");
    process.exit(1);
}
console.log(`found FlintFix mod jars for all ${targets.length} Minecraft versions`);
