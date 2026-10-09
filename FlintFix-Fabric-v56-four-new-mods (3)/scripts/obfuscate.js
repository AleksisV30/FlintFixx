// Writes obfuscated copies of the launcher's own JavaScript to build-obf/.
// electron-builder packs those instead of the readable sources (see "files"
// in package.json), so the installed app doesn't ship plain source code.

const fs = require("fs");
const path = require("path");
const JavaScriptObfuscator = require("javascript-obfuscator");

const root = path.join(__dirname, "..");
const out = path.join(root, "build-obf");

const sources = [
    { file: "main.js", target: "node" },
    { file: "preload.js", target: "node" },
    { file: "renderer.js", target: "browser" },
    ...["auth", "minecraft", "discord"].flatMap(dir =>
        fs.readdirSync(path.join(root, dir))
            .filter(name => name.endsWith(".js"))
            .map(name => ({ file: `${dir}/${name}`, target: "node" })))
];

fs.rmSync(out, { recursive: true, force: true });

for (const { file, target } of sources) {
    const code = fs.readFileSync(path.join(root, file), "utf8");
    const result = JavaScriptObfuscator.obfuscate(code, {
        target,
        compact: true,
        // Top-level names stay: renderer.js shares globals with index.html.
        renameGlobals: false,
        identifierNamesGenerator: "hexadecimal",
        stringArray: true,
        stringArrayEncoding: ["base64"],
        stringArrayThreshold: 0.75,
        splitStrings: true,
        splitStringsChunkLength: 10,
        transformObjectKeys: false,
        // Control-flow flattening would slow the 5000-line renderer noticeably.
        controlFlowFlattening: false,
        deadCodeInjection: false,
        selfDefending: false,
        sourceMap: false,
        unicodeEscapeSequence: false
    });
    const dest = path.join(out, file);
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.writeFileSync(dest, result.getObfuscatedCode());
    console.log(`obfuscated ${file}`);
}
