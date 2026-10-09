const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");

function isRuleAllowed(rules = [], features = {}) {
    if (!rules || rules.length === 0) return true;

    let allowed = false;

    for (const rule of rules) {
        let matches = true;

        if (rule.os) {
            if (rule.os.name && rule.os.name !== "windows") matches = false;

            if (rule.os.arch) {
                const arch =
                    process.arch === "x64"
                        ? "x86_64"
                        : process.arch === "ia32"
                            ? "x86"
                            : process.arch;

                if (rule.os.arch !== arch) matches = false;
            }

            if (rule.os.version) {
                try {
                    if (!new RegExp(rule.os.version).test(require("os").release())) {
                        matches = false;
                    }
                } catch {
                    matches = false;
                }
            }
        }

        if (rule.features) {
            for (const [name, expected] of Object.entries(rule.features)) {
                if (Boolean(features[name]) !== Boolean(expected)) {
                    matches = false;
                    break;
                }
            }
        }

        if (matches) {
            allowed = rule.action === "allow";
        }
    }

    return allowed;
}

function substitute(value, variables) {
    return String(value).replace(/\$\{([^}]+)\}/g, (_match, key) => {
        return Object.prototype.hasOwnProperty.call(variables, key)
            ? String(variables[key])
            : "";
    });
}

function expandArguments(entries, variables, features) {
    const result = [];

    for (const entry of entries || []) {
        if (typeof entry === "string") {
            result.push(substitute(entry, variables));
            continue;
        }

        if (!entry || typeof entry !== "object") continue;
        if (!isRuleAllowed(entry.rules || [], features)) continue;

        const values = Array.isArray(entry.value)
            ? entry.value
            : [entry.value];

        for (const value of values) {
            if (typeof value === "string") {
                result.push(substitute(value, variables));
            }
        }
    }

    return result;
}

function getLibraryArtifactPaths(metadata, librariesDir) {
    const paths = [];

    for (const library of metadata.libraries || []) {
        if (!isRuleAllowed(library.rules || [], {})) continue;

        const artifact = library.downloads?.artifact;
        if (!artifact?.path) continue;

        const file = path.join(
            librariesDir,
            ...artifact.path.split("/")
        );

        if (fs.existsSync(file)) {
            paths.push(file);
        }
    }

    return paths;
}

function normalizeJavaExecutable(javaExecutable) {
    if (!javaExecutable) {
        throw new Error("No Java executable was provided.");
    }

    if (!fs.existsSync(javaExecutable)) {
        throw new Error(`Java executable does not exist: ${javaExecutable}`);
    }

    // javaw.exe is ideal for a release launcher. During development,
    // stdout/stderr pipes still work with Node spawn.
    return javaExecutable;
}

function launchMinecraft({
    rootDir,
    versionId,
    javaExecutable,
    session,
    launchOptions = {},
    onLog,
    onExit
}) {
    if (!/^[A-Za-z0-9._+-]+$/.test(versionId)) {
        throw new Error("Invalid Minecraft version ID.");
    }

    if (!session?.minecraft?.username || !session?.minecraft?.uuid) {
        throw new Error("A Minecraft launch profile is required.");
    }

    const versionDir = path.join(rootDir, "versions", versionId);
    const metadataPath = path.join(versionDir, `${versionId}.json`);
    const clientJar = path.join(versionDir, `${versionId}.jar`);
    const nativesDir = path.join(versionDir, "natives");
    const librariesDir = path.join(rootDir, "libraries");
    const assetsDir = path.join(rootDir, "assets");
    const gameDir = path.join(rootDir, "game");

    if (!fs.existsSync(metadataPath)) {
        throw new Error(`Minecraft ${versionId} is not installed.`);
    }

    if (!fs.existsSync(clientJar)) {
        throw new Error(`Minecraft client JAR is missing for ${versionId}.`);
    }

    fs.mkdirSync(gameDir, { recursive: true });

    const metadata = JSON.parse(
        fs.readFileSync(metadataPath, "utf8")
    );

    const classpathEntries = [
        ...getLibraryArtifactPaths(metadata, librariesDir),
        clientJar
    ];

    const classpath = classpathEntries.join(path.delimiter);

    const username = session.minecraft.username;
    const uuid = session.minecraft.uuid;
    const accessToken = session.minecraft.accessToken;

    const requestedWidth = Number(launchOptions.width);
    const requestedHeight = Number(launchOptions.height);
    const resolutionWidth = Number.isFinite(requestedWidth)
        ? Math.max(320, Math.min(7680, Math.round(requestedWidth)))
        : 1280;
    const resolutionHeight = Number.isFinite(requestedHeight)
        ? Math.max(240, Math.min(4320, Math.round(requestedHeight)))
        : 720;
    const fullscreen = Boolean(launchOptions.fullscreen);

    const variables = {
        natives_directory: nativesDir,
        launcher_name: "FlintFix",
        launcher_version: "0.1",
        classpath,
        classpath_separator: path.delimiter,
        library_directory: librariesDir,

        auth_player_name: username,
        version_name: versionId,
        game_directory: gameDir,
        assets_root: assetsDir,
        assets_index_name: metadata.assetIndex?.id || metadata.assets || "",
        auth_uuid: uuid,
        auth_access_token: accessToken,
        clientid: session.minecraft.clientId || "",
        auth_xuid: session.minecraft.xuid || "",
        user_type: session.minecraft.offline ? "legacy" : "msa",
        version_type: metadata.type || "release",
        resolution_width: String(resolutionWidth),
        resolution_height: String(resolutionHeight)
    };

    const features = {
        has_custom_resolution: !fullscreen,
        is_demo_user: false,
        has_quick_plays_support: false,
        is_quick_play_singleplayer: false,
        is_quick_play_multiplayer: false,
        is_quick_play_realms: false
    };

    let jvmArgs = [];

    if (metadata.arguments?.jvm) {
        jvmArgs = expandArguments(
            metadata.arguments.jvm,
            variables,
            features
        );
    } else {
        jvmArgs = [
            `-Djava.library.path=${nativesDir}`,
            "-cp",
            classpath
        ];
    }

    // Ensure modern metadata that omits an explicit classpath still gets one.
    if (!jvmArgs.includes("-cp") && !jvmArgs.includes("-classpath")) {
        jvmArgs.push("-cp", classpath);
    }

    // Give Minecraft a sensible default heap while keeping it modest.
    // User-configurable memory can replace this later.
    if (!jvmArgs.some(arg => /^-Xmx/i.test(arg))) {
        jvmArgs.unshift("-Xmx4G");
    }

    if (!jvmArgs.some(arg => /^-Xms/i.test(arg))) {
        jvmArgs.unshift("-Xms512M");
    }

    let gameArgs = [];

    if (metadata.arguments?.game) {
        gameArgs = expandArguments(
            metadata.arguments.game,
            variables,
            features
        );
    } else if (metadata.minecraftArguments) {
        // Legacy Mojang metadata uses a simple argument string.
        gameArgs = metadata.minecraftArguments
            .split(/\s+/)
            .filter(Boolean)
            .map(arg => substitute(arg, variables));
    }

    // Authentication arguments are mandatory for joining online-mode servers.
    // Fabric profiles are inheritance-based and some custom profile merges can
    // accidentally omit the vanilla game arguments, so enforce these values at
    // the final launch boundary as a second line of defense.
    const ensureGameArg = (flag, value) => {
        if (value === undefined || value === null) return;
        const index = gameArgs.indexOf(flag);
        if (index >= 0) {
            if (index + 1 < gameArgs.length) gameArgs[index + 1] = String(value);
            else gameArgs.push(String(value));
            return;
        }
        gameArgs.push(flag, String(value));
    };

    ensureGameArg("--username", username);
    ensureGameArg("--uuid", uuid);
    ensureGameArg("--accessToken", accessToken || "0");
    ensureGameArg("--userType", session.minecraft.offline ? "legacy" : "msa");

    const removeFlagWithValue = flag => {
        let index = gameArgs.indexOf(flag);
        while (index >= 0) {
            gameArgs.splice(index, Math.min(2, gameArgs.length - index));
            index = gameArgs.indexOf(flag);
        }
    };

    if (fullscreen) {
        removeFlagWithValue("--width");
        removeFlagWithValue("--height");
        if (!gameArgs.includes("--fullscreen")) gameArgs.push("--fullscreen");
    } else {
        const fullscreenIndex = gameArgs.indexOf("--fullscreen");
        if (fullscreenIndex >= 0) gameArgs.splice(fullscreenIndex, 1);
        ensureGameArg("--width", resolutionWidth);
        ensureGameArg("--height", resolutionHeight);
    }

    // Join a server straight from the launcher's server list (Minecraft 1.20+).
    if (launchOptions.joinServer) {
        removeFlagWithValue("--quickPlayMultiplayer");
        gameArgs.push("--quickPlayMultiplayer", String(launchOptions.joinServer));
    }

    if (onLog) {
        onLog({
            source: "AUTH",
            message: session.minecraft.offline
                ? `Launching offline profile ${username}`
                : `Launching authenticated profile ${username} (${uuid}) with Microsoft session arguments.`
        });
    }

    const args = [
        ...jvmArgs,
        metadata.mainClass,
        ...gameArgs
    ];

    const executable = normalizeJavaExecutable(javaExecutable);

    const child = spawn(
        executable,
        args,
        {
            cwd: gameDir,
            windowsHide: true,
            stdio: ["ignore", "pipe", "pipe"]
        }
    );

    const emitLines = (source, chunk) => {
        const text = String(chunk || "");
        for (const line of text.split(/\r?\n/)) {
            if (line.trim() && onLog) {
                onLog({
                    source,
                    message: line
                });
            }
        }
    };

    child.stdout?.on("data", chunk => emitLines("GAME", chunk));
    child.stderr?.on("data", chunk => emitLines("GAME", chunk));

    child.on("error", error => {
        if (onLog) {
            onLog({
                source: "ERROR",
                message: `Minecraft process error: ${error.message}`
            });
        }
    });

    child.on("exit", (code, signal) => {
        if (onExit) {
            onExit({ code, signal });
        }
    });

    return {
        pid: child.pid,
        gameDirectory: gameDir
    };
}

module.exports = {
    launchMinecraft
};
