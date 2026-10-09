// Builds the app icon (assets/flintfix-client-icon.ico and .png) from the real
// Minecraft flint texture, so the exe, installer, shortcuts and taskbar show it.
// The texture is read from a Minecraft client jar on this PC: the launcher's own
// Minecraft folder, the official .minecraft folder, or the jars Gradle/Loom
// downloaded while building the mod.

const fs = require("fs");
const os = require("os");
const path = require("path");
const yauzl = require("yauzl");
const { PNG } = require("pngjs");

const root = path.join(__dirname, "..");
const ENTRY = "assets/minecraft/textures/item/flint.png";
const ICO_SIZES = [16, 24, 32, 48, 64, 128, 256];

function* versionJars(minecraftDir) {
    const versionsDir = path.join(minecraftDir, "versions");
    if (!fs.existsSync(versionsDir)) return;
    for (const id of fs.readdirSync(versionsDir)) {
        const jar = path.join(versionsDir, id, `${id}.jar`);
        if (fs.existsSync(jar)) yield jar;
    }
}

function* loomJars(dir, depth = 0) {
    if (!fs.existsSync(dir) || depth > 3) return;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) yield* loomJars(full, depth + 1);
        else if (/client.*\.jar$/i.test(entry.name)) yield full;
    }
}

function* candidateJars() {
    const appData = process.env.APPDATA;
    if (appData) {
        yield* versionJars(path.join(appData, "FlintFix Client", "minecraft"));
        yield* versionJars(path.join(appData, ".minecraft"));
    }
    yield* versionJars(path.join(os.homedir(), ".minecraft"));
    const gradleHome = process.env.GRADLE_USER_HOME || path.join(os.homedir(), ".gradle");
    yield* loomJars(path.join(gradleHome, "caches", "fabric-loom"));
    yield* loomJars(path.join(root, "flintfix-mod", ".gradle", "loom-cache"));
}

function readEntry(jar) {
    return new Promise(resolve => {
        yauzl.open(jar, { lazyEntries: true }, (error, zip) => {
            if (error) return resolve(null);
            zip.on("entry", entry => {
                if (entry.fileName !== ENTRY) return zip.readEntry();
                zip.openReadStream(entry, (streamError, stream) => {
                    if (streamError) { zip.close(); return resolve(null); }
                    const chunks = [];
                    stream.on("data", chunk => chunks.push(chunk));
                    stream.on("end", () => { zip.close(); resolve(Buffer.concat(chunks)); });
                    stream.on("error", () => { zip.close(); resolve(null); });
                });
            });
            zip.on("end", () => resolve(null));
            zip.on("error", () => resolve(null));
            zip.readEntry();
        });
    });
}

// Nearest-neighbour scaling keeps the pixels sharp.
function scale(source, size) {
    const out = new PNG({ width: size, height: size });
    for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
            const sx = Math.floor(x * source.width / size);
            const sy = Math.floor(y * source.height / size);
            source.data.copy(out.data, (y * size + x) * 4, (sy * source.width + sx) * 4, (sy * source.width + sx) * 4 + 4);
        }
    }
    return PNG.sync.write(out);
}

// An .ico whose images are stored as PNGs (supported since Windows Vista).
function buildIco(images) {
    const header = Buffer.alloc(6 + 16 * images.length);
    header.writeUInt16LE(0, 0);
    header.writeUInt16LE(1, 2);
    header.writeUInt16LE(images.length, 4);
    let offset = header.length;
    images.forEach(({ size, data }, index) => {
        const at = 6 + 16 * index;
        header.writeUInt8(size >= 256 ? 0 : size, at);
        header.writeUInt8(size >= 256 ? 0 : size, at + 1);
        header.writeUInt16LE(1, at + 4);
        header.writeUInt16LE(32, at + 6);
        header.writeUInt32LE(data.length, at + 8);
        header.writeUInt32LE(offset, at + 12);
        offset += data.length;
    });
    return Buffer.concat([header, ...images.map(image => image.data)]);
}

(async () => {
    let texture = null;
    let sourceJar = null;
    for (const jar of candidateJars()) {
        texture = await readEntry(jar);
        if (texture) { sourceJar = jar; break; }
    }
    if (!texture) {
        console.error("Could not find the Minecraft flint texture. Start Minecraft once (in FlintFix or the official launcher)");
        console.error("or build the mod (flintfix-mod: gradlew buildAll) so a Minecraft client jar is on this PC, then build again.");
        process.exit(1);
    }

    const flint = PNG.sync.read(texture);
    const images = ICO_SIZES.map(size => ({ size, data: scale(flint, size) }));
    fs.writeFileSync(path.join(root, "assets", "flintfix-client-icon.ico"), buildIco(images));
    fs.writeFileSync(path.join(root, "assets", "flintfix-client-icon.png"), scale(flint, 512));
    console.log(`app icon made from the Minecraft flint in ${sourceJar}`);
})();
