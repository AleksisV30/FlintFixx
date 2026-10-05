const fs = require("fs");
const path = require("path");
const https = require("https");
const { execFile } = require("child_process");

const MC_VERSION = "1.21.1";
const FABRIC_API_VERSION = "0.116.16+1.21.1";
const FABRIC_DOWNLOAD_CONCURRENCY = 8;
const FABRIC_AGENT = new https.Agent({ keepAlive: true, maxSockets: 16, maxFreeSockets: 8, timeout: 30_000 });

function getJson(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { agent: FABRIC_AGENT, headers: { "User-Agent": "FlintFix-Client/0.56" } }, response => {
            if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
                response.resume();
                return getJson(new URL(response.headers.location, url).toString()).then(resolve, reject);
            }
            let data = "";
            response.setEncoding("utf8");
            response.on("data", chunk => data += chunk);
            response.on("end", () => {
                if (response.statusCode < 200 || response.statusCode >= 300) {
                    reject(new Error(`HTTP ${response.statusCode} while requesting ${url}`));
                    return;
                }
                try { resolve(JSON.parse(data)); } catch (error) { reject(error); }
            });
        }).on("error", reject);
    });
}

function download(url, destination) {
    return new Promise((resolve, reject) => {
        fs.mkdirSync(path.dirname(destination), { recursive: true });
        if (fs.existsSync(destination) && fs.statSync(destination).size > 0) return resolve(destination);
        const temp = `${destination}.part`;
        const request = currentUrl => {
            https.get(currentUrl, { agent: FABRIC_AGENT, headers: { "User-Agent": "FlintFix-Client/0.56" } }, response => {
                if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
                    response.resume();
                    request(new URL(response.headers.location, currentUrl).toString());
                    return;
                }
                if (response.statusCode !== 200) {
                    response.resume();
                    reject(new Error(`Download failed (${response.statusCode}): ${currentUrl}`));
                    return;
                }
                const output = fs.createWriteStream(temp);
                response.pipe(output);
                output.on("finish", () => output.close(() => {
                    fs.renameSync(temp, destination);
                    resolve(destination);
                }));
                output.on("error", reject);
            }).on("error", reject);
        };
        request(url);
    });
}

async function runWorkerPool(items, concurrency, worker) {
    let next = 0;
    async function run() {
        while (true) {
            const index = next++;
            if (index >= items.length) return;
            await worker(items[index], index);
        }
    }
    const count = Math.min(Math.max(1, concurrency), Math.max(1, items.length));
    await Promise.all(Array.from({ length: count }, () => run()));
}

function mavenArtifact(name) {
    const parts = name.split(":");
    if (parts.length < 3) throw new Error(`Unsupported Maven coordinate: ${name}`);
    const [group, artifact, version, classifier] = parts;
    const groupPath = group.replace(/\./g, "/");
    const file = `${artifact}-${version}${classifier ? `-${classifier}` : ""}.jar`;
    return `${groupPath}/${artifact}/${version}/${file}`;
}

function runPowerShell(scriptPath, cwd, javaExecutable) {
    return new Promise((resolve, reject) => {
        if (typeof javaExecutable !== "string" || !javaExecutable) {
            reject(new Error("Managed Java executable was not supplied to the FlintFix mod builder."));
            return;
        }

        const javaBin = path.dirname(javaExecutable);
        const javaHome = path.dirname(javaBin);

        execFile("powershell.exe", ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptPath],
            {
                cwd,
                windowsHide: true,
                maxBuffer: 16 * 1024 * 1024,
                env: {
                    ...process.env,
                    JAVA_HOME: javaHome,
                    PATH: `${javaBin};${process.env.PATH || ""}`
                }
            },
            (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(`FlintFix mod build failed. ${stderr || stdout || error.message}`));
                    return;
                }
                resolve(stdout);
            });
    });
}

async function ensureModJar(projectRoot, javaExecutable, emit) {
    const modRoot = path.join(projectRoot, "flintfix-mod");
    const libs = path.join(modRoot, "build", "libs");
    const sourceRoot = path.join(modRoot, "src");
    const newestSource = (() => {
        let newest = 0;
        const walk = dir => {
            if (!fs.existsSync(dir)) return;
            for (const name of fs.readdirSync(dir)) {
                const full = path.join(dir, name);
                const stat = fs.statSync(full);
                if (stat.isDirectory()) walk(full); else newest = Math.max(newest, stat.mtimeMs);
            }
        };
        walk(sourceRoot);
        for (const name of ["build.gradle", "gradle.properties", "settings.gradle"]) {
            const full = path.join(modRoot, name);
            if (fs.existsSync(full)) newest = Math.max(newest, fs.statSync(full).mtimeMs);
        }
        return newest;
    })();
    const cached = fs.existsSync(libs)
        ? fs.readdirSync(libs).filter(file => /^flintfix-client-mod-.*\.jar$/i.test(file) && !/-sources\.jar$/i.test(file))
            .map(file => path.join(libs, file)).sort((a,b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs)[0]
        : null;
    if (cached && fs.statSync(cached).mtimeMs >= newestSource) return cached;

    emit?.({ stage: "fabric", message: "Building FlintFix in-game client..." });
    const script = path.join(modRoot, "build-mod.ps1");
    if (!fs.existsSync(script)) throw new Error("flintfix-mod/build-mod.ps1 is missing.");
    await runPowerShell(script, modRoot, javaExecutable);

    const candidate = fs.existsSync(libs)
        ? fs.readdirSync(libs).find(file => /^flintfix-client-mod-.*\.jar$/i.test(file) && !/-sources\.jar$/i.test(file))
        : null;
    if (!candidate) throw new Error("The FlintFix mod build completed but no mod JAR was produced.");
    return path.join(libs, candidate);
}

async function installFabric({ rootDir, projectRoot, minecraftVersion, javaExecutable, emit }) {
    if (minecraftVersion !== MC_VERSION) {
        throw new Error(`FlintFix Fabric DEV currently supports Minecraft ${MC_VERSION}. Select ${MC_VERSION}.`);
    }

    emit?.({ stage: "fabric", message: "Finding Fabric Loader..." });
    const loaders = await getJson(`https://meta.fabricmc.net/v2/versions/loader/${encodeURIComponent(MC_VERSION)}`);
    const stable = loaders.find(item => item.loader?.stable) || loaders[0];
    const loaderVersion = stable?.loader?.version;
    if (!loaderVersion) throw new Error("Fabric Meta did not return a loader version.");

    const profile = await getJson(`https://meta.fabricmc.net/v2/versions/loader/${encodeURIComponent(MC_VERSION)}/${encodeURIComponent(loaderVersion)}/profile/json`);
    const fabricVersionId = `fabric-loader-${loaderVersion}-${MC_VERSION}`;
    const baseDir = path.join(rootDir, "versions", MC_VERSION);
    const fabricDir = path.join(rootDir, "versions", fabricVersionId);
    const librariesDir = path.join(rootDir, "libraries");
    const modsDir = path.join(rootDir, "game", "mods");

    const baseJsonPath = path.join(baseDir, `${MC_VERSION}.json`);
    const baseJarPath = path.join(baseDir, `${MC_VERSION}.jar`);
    if (!fs.existsSync(baseJsonPath) || !fs.existsSync(baseJarPath)) {
        throw new Error(`Minecraft ${MC_VERSION} must be installed before Fabric.`);
    }

    const base = JSON.parse(fs.readFileSync(baseJsonPath, "utf8"));
    const fabricLibraries = (profile.libraries || []).map(library => {
        const artifactPath = mavenArtifact(library.name);
        const baseUrl = (library.url || "https://maven.fabricmc.net/").replace(/\/?$/, "/");
        return {
            library,
            artifactPath,
            url: `${baseUrl}${artifactPath}`,
            destination: path.join(librariesDir, ...artifactPath.split("/"))
        };
    });
    const normalizedFabricLibraries = new Array(fabricLibraries.length);

    // The FlintFix mod build is the slowest first-run step. Start it while the
    // Fabric libraries and Fabric API are downloading instead of waiting for
    // each step serially.
    emit?.({ stage: "fabric", phase: "parallel", message: "Preparing Fabric components in parallel..." });
    const modJarPromise = ensureModJar(projectRoot, javaExecutable, emit);
    // Attach handlers immediately so a fast failure is still reported through
    // the awaited launch path instead of becoming an unhandled rejection.
    modJarPromise.catch(() => {});
    const apiName = `fabric-api-${FABRIC_API_VERSION}.jar`;
    const apiUrl = `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/${encodeURIComponent(FABRIC_API_VERSION)}/${apiName}`;
    const apiPromise = download(apiUrl, path.join(modsDir, apiName));
    apiPromise.catch(() => {});

    let completedFabricLibraries = 0;
    await runWorkerPool(fabricLibraries, FABRIC_DOWNLOAD_CONCURRENCY, async (item, index) => {
        await download(item.url, item.destination);
        normalizedFabricLibraries[index] = {
            ...item.library,
            downloads: { artifact: { path: item.artifactPath } }
        };
        completedFabricLibraries++;
        emit?.({
            stage: "fabric",
            phase: "libraries",
            current: completedFabricLibraries,
            total: fabricLibraries.length,
            message: `Fabric libraries ${completedFabricLibraries}/${fabricLibraries.length}`
        });
    });

    fs.mkdirSync(fabricDir, { recursive: true });
    fs.copyFileSync(baseJarPath, path.join(fabricDir, `${fabricVersionId}.jar`));

    const baseNatives = path.join(baseDir, "natives");
    const fabricNatives = path.join(fabricDir, "natives");
    if (fs.existsSync(baseNatives)) {
        fs.cpSync(baseNatives, fabricNatives, { recursive: true, force: true });
    }

    // Fabric launcher profiles normally use `inheritsFrom`, so their own
    // `arguments.game` array can be empty. FlintFix flattens the profile into a
    // standalone version JSON, therefore we must MERGE the Fabric arguments
    // with vanilla's arguments instead of replacing them. Replacing them drops
    // --username/--uuid/--accessToken/--userType and creates an offline/invalid
    // session even after Microsoft authentication succeeded.
    const baseArguments = base.arguments || {};
    const fabricArguments = profile.arguments || {};
    const merged = {
        ...base,
        id: fabricVersionId,
        mainClass: profile.mainClass,
        type: "release",
        libraries: [...(base.libraries || []), ...normalizedFabricLibraries],
        arguments: {
            ...baseArguments,
            ...fabricArguments,
            game: [
                ...(baseArguments.game || []),
                ...(fabricArguments.game || [])
            ],
            jvm: [
                ...(baseArguments.jvm || []),
                ...(fabricArguments.jvm || [])
            ]
        }
    };
    fs.writeFileSync(path.join(fabricDir, `${fabricVersionId}.json`), JSON.stringify(merged, null, 2));

    fs.mkdirSync(modsDir, { recursive: true });
    emit?.({ stage: "fabric", phase: "api", message: `Installing Fabric API ${FABRIC_API_VERSION}...` });
    await apiPromise;

    const modJar = await modJarPromise;
    const targetMod = path.join(modsDir, "flintfix-client-mod.jar");
    fs.copyFileSync(modJar, targetMod);

    emit?.({ stage: "fabric", message: `FlintFix Client ready with Fabric Loader ${loaderVersion}.` });
    return { versionId: fabricVersionId, loaderVersion, modsDir, modJar: targetMod };
}

module.exports = { installFabric };
