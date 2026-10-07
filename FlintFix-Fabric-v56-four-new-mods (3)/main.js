const { app, BrowserWindow, ipcMain, shell, screen, nativeImage, dialog } = require("electron");

const path = require("path");
const crypto = require("crypto");
const fs = require("fs");
const https = require("https");
const http = require("http");

const { execFile } = require("child_process");

const extract = require("extract-zip");
const { installMinecraft } = require("./minecraft/installer");
const microsoftAuth = require("./auth/microsoft");
const { launchMinecraft } = require("./minecraft/launcher");
const { installFabric } = require("./minecraft/fabric");
const { FlintFixDiscordPresence } = require("./discord/presence");
const { createResourcePackManager } = require("./minecraft/resourcepacks");
const serverPing = require("./minecraft/serverping");
const { createSkinManager } = require("./minecraft/skins");
const { createUpdater, REPO: UPDATE_REPO } = require("./minecraft/updates");

function loadDiscordPresenceConfig() {
    const configPath = path.join(__dirname, "discord", "config.json");
    let config = {};
    try {
        config = JSON.parse(fs.readFileSync(configPath, "utf8"));
    } catch {
        config = {};
    }
    return {
        clientId: process.env.FLINTFIX_DISCORD_APP_ID || config.clientId || "",
        largeImageKey: config.largeImageKey || "",
        discordInvite: config.discordInvite || "https://discord.gg/flintfix",
        linkBackendUrl: (process.env.FLINTFIX_DISCORD_LINK_BACKEND || config.linkBackendUrl || "").replace(/\/$/, "")
    };
}

const discordConfig = loadDiscordPresenceConfig();
const discordPresence = new FlintFixDiscordPresence(discordConfig);
const discordLinkBackendUrl = discordConfig.linkBackendUrl;

// Keep the freshly authenticated Minecraft session in the main process.
// This avoids running the Microsoft -> Xbox -> XSTS -> Minecraft chain twice
// between the Sign In button and the actual game launch.
let activeMinecraftSession = null;
let activeMinecraftSessionExpiresAt = 0;

function rememberMinecraftSession(session) {
    activeMinecraftSession = session || null;

    if (!session?.minecraft?.accessToken) {
        activeMinecraftSessionExpiresAt = 0;
        return;
    }

    const reportedSeconds = Number(session.minecraft.expiresIn);
    const usableSeconds = Number.isFinite(reportedSeconds) && reportedSeconds > 180
        ? reportedSeconds - 120
        : 3300;

    activeMinecraftSessionExpiresAt = Date.now() + usableSeconds * 1000;
}

function clearMinecraftSession() {
    activeMinecraftSession = null;
    activeMinecraftSessionExpiresAt = 0;
}

function hasUsableMinecraftSession() {
    return Boolean(
        activeMinecraftSession?.minecraft?.accessToken &&
        activeMinecraftSession?.minecraft?.username &&
        activeMinecraftSession?.minecraft?.uuid &&
        Date.now() < activeMinecraftSessionExpiresAt
    );
}

async function getMinecraftSessionForLaunch() {
    if (hasUsableMinecraftSession()) {
        return activeMinecraftSession;
    }

    const session = await microsoftAuth.createMinecraftSession(null, false);
    rememberMinecraftSession(session);
    return session;
}

function getCachedMinecraftFlintIconPath() {
    try {
        const cacheDir = path.join(app.getPath("userData"), "cache");
        if (!fs.existsSync(cacheDir)) return null;
        const candidates = fs.readdirSync(cacheDir)
            .filter(name => /^minecraft-flint-[A-Za-z0-9._+-]+\.png$/i.test(name))
            .map(name => {
                const fullPath = path.join(cacheDir, name);
                return { fullPath, mtime: fs.statSync(fullPath).mtimeMs };
            })
            .sort((a, b) => b.mtime - a.mtime);
        return candidates[0]?.fullPath || null;
    } catch {
        return null;
    }
}


function normalizeInstanceId(instanceId) {
    const value = String(instanceId || "");
    if (!/^[A-Za-z0-9._-]{1,120}$/.test(value)) {
        throw new Error("Invalid instance ID.");
    }
    return value;
}

function getInstanceRoot(instanceId) {
    const safeId = normalizeInstanceId(instanceId);
    return path.join(app.getPath("userData"), "minecraft", "instances", safeId);
}

function getGameDir() {
    return path.join(app.getPath("userData"), "minecraft", "game");
}

const resourcePacks = createResourcePackManager({ getGameDir });

const skins = createSkinManager({
    getUserDataDir: () => app.getPath("userData"),
    getAccessToken: async () => {
        const session = await getMinecraftSessionForLaunch();
        return session?.minecraft?.offline ? null : session?.minecraft?.accessToken || null;
    }
});

const updater = createUpdater({
    app,
    send: state => {
        for (const window of BrowserWindow.getAllWindows()) {
            if (!window.isDestroyed()) window.webContents.send("update:status", state);
        }
    }
});

// Mods installed by Performance mode, by Modrinth slug. "match" finds an
// already installed copy by file name.
const PERFORMANCE_MODS = [
    { slug: "sodium", title: "Sodium", match: /sodium/i },
    { slug: "lithium", title: "Lithium", match: /lithium/i },
    { slug: "ferrite-core", title: "FerriteCore", match: /ferrite/i },
    { slug: "immediatelyfast", title: "ImmediatelyFast", match: /immediatelyfast/i },
    { slug: "entityculling", title: "Entity Culling", match: /entityculling/i },
    { slug: "modernfix", title: "ModernFix", match: /modernfix/i }
];

function getInstanceModsDir(instanceId) {
    return path.join(getInstanceRoot(instanceId), "mods");
}

function isSafeModFileName(fileName) {
    const value = String(fileName || "");
    return value === path.basename(value) && /\.jar(?:\.disabled)?$/i.test(value) && !value.includes("..") && value.length <= 240;
}

async function listInstanceMods(instanceId) {
    const modsDir = getInstanceModsDir(instanceId);
    await fs.promises.mkdir(modsDir, { recursive: true });
    const entries = await fs.promises.readdir(modsDir, { withFileTypes: true });
    const mods = [];
    for (const entry of entries) {
        if (!entry.isFile() || !isSafeModFileName(entry.name)) continue;
        const full = path.join(modsDir, entry.name);
        const stat = await fs.promises.stat(full);
        const enabled = !entry.name.toLowerCase().endsWith(".disabled");
        const displayFileName = enabled ? entry.name : entry.name.slice(0, -".disabled".length);
        const displayName = displayFileName
            .replace(/\.jar$/i, "")
            .replace(/[-_]+/g, " ")
            .replace(/\s+/g, " ")
            .trim();
        const sha1 = crypto.createHash("sha1").update(await fs.promises.readFile(full)).digest("hex");
        mods.push({
            fileName: entry.name,
            displayFileName,
            displayName: displayName || displayFileName,
            enabled,
            size: stat.size,
            modifiedAt: stat.mtime.toISOString(),
            sha1
        });
    }
    mods.sort((a, b) => a.displayName.localeCompare(b.displayName, undefined, { sensitivity: "base" }));
    return { modsDir, mods };
}

async function copyModIntoInstance(instanceId, sourcePath) {
    const modsDir = getInstanceModsDir(instanceId);
    await fs.promises.mkdir(modsDir, { recursive: true });
    const original = path.basename(sourcePath);
    if (!/\.jar$/i.test(original)) throw new Error("Only .jar Minecraft mods can be added.");
    if (original.toLowerCase() === "flintfix-client-mod.jar" || /^fabric-api-.*\.jar$/i.test(original)) {
        throw new Error("FlintFix manages Fabric API and the FlintFix client mod automatically.");
    }
    const ext = path.extname(original);
    const stem = path.basename(original, ext);
    let candidate = original;
    let index = 2;
    while (fs.existsSync(path.join(modsDir, candidate)) || fs.existsSync(path.join(modsDir, `${candidate}.disabled`))) {
        candidate = `${stem} (${index})${ext}`;
        index += 1;
    }
    await fs.promises.copyFile(sourcePath, path.join(modsDir, candidate));
    return candidate;
}

function buildUniqueModFileName(modsDir, originalName) {
    const original = path.basename(String(originalName || "downloaded-mod.jar"));
    if (!/\.jar$/i.test(original)) throw new Error("Only .jar Minecraft mods can be installed.");
    const ext = path.extname(original);
    const stem = path.basename(original, ext);
    let candidate = original;
    let index = 2;
    while (fs.existsSync(path.join(modsDir, candidate)) || fs.existsSync(path.join(modsDir, `${candidate}.disabled`))) {
        candidate = `${stem} (${index})${ext}`;
        index += 1;
    }
    return candidate;
}

async function fetchJson(url) {
    const response = await fetch(url, {
        headers: {
            "User-Agent": "FlintFix-Client/1.0",
            "Accept": "application/json"
        }
    });
    if (!response.ok) {
        throw new Error(`Request failed (${response.status}).`);
    }
    return response.json();
}

async function searchCatalogMods(options = {}) {
    const query = String(options.query || "").trim();
    const version = String(options.version || "").trim();
    const loader = String(options.loader || "fabric").trim().toLowerCase();
    const category = String(options.category || "").trim().toLowerCase();
    const index = ["relevance", "downloads", "follows", "newest", "updated"].includes(String(options.index || "relevance"))
        ? String(options.index || "relevance")
        : "relevance";
    const limit = Math.min(24, Math.max(6, Number(options.limit) || 12));
    const offset = Math.max(0, Number(options.offset) || 0);
    const params = new URLSearchParams({
        limit: String(limit),
        offset: String(offset),
        query,
        index
    });
    const facets = [["project_type:mod"]];
    if (loader && loader !== "vanilla") facets.push([`categories:${loader}`]);
    if (version) facets.push([`versions:${version}`]);
    if (category) facets.push([`categories:${category}`]);
    params.set("facets", JSON.stringify(facets));
    const data = await fetchJson(`https://api.modrinth.com/v2/search?${params.toString()}`);
    const hits = Array.isArray(data?.hits) ? data.hits : [];
    const mods = hits.map(hit => ({
        projectId: hit.project_id,
        slug: hit.slug,
        title: hit.title,
        description: hit.description,
        author: hit.author || "",
        downloads: hit.downloads || 0,
        follows: hit.follows || 0,
        categories: Array.isArray(hit.display_categories) ? hit.display_categories : (Array.isArray(hit.categories) ? hit.categories : []),
        iconUrl: hit.icon_url || "",
        latestVersion: hit.latest_version || "",
        dateModified: hit.date_modified || "",
        pageUrl: hit.slug ? `https://modrinth.com/mod/${hit.slug}` : ""
    }));
    return {
        mods,
        totalHits: Number(data?.total_hits) || mods.length,
        offset: Number(data?.offset) || offset,
        limit: Number(data?.limit) || limit
    };
}

async function getCatalogModDetails(projectId, options = {}) {
    const id = String(projectId || "").trim();
    if (!id) throw new Error("Missing project ID.");
    const project = await fetchJson(`https://api.modrinth.com/v2/project/${encodeURIComponent(id)}`);
    const version = String(options.version || "").trim();
    const loader = String(options.loader || "").trim().toLowerCase();
    const versionsUrl = new URL(`https://api.modrinth.com/v2/project/${encodeURIComponent(id)}/version`);
    if (loader && loader !== "vanilla") versionsUrl.searchParams.set("loaders", JSON.stringify([loader]));
    if (version) versionsUrl.searchParams.set("game_versions", JSON.stringify([version]));
    versionsUrl.searchParams.set("include_changelog", "false");
    let versions = [];
    try {
        versions = await fetchJson(versionsUrl.toString());
    } catch {
        versions = [];
    }
    return {
        projectId: project.id,
        slug: project.slug || "",
        title: project.title || project.slug || project.id,
        description: project.description || "",
        body: project.body || "",
        iconUrl: project.icon_url || "",
        downloads: project.downloads || 0,
        followers: project.followers || 0,
        categories: Array.isArray(project.categories) ? project.categories : [],
        gallery: Array.isArray(project.gallery) ? project.gallery.map(item => item?.url).filter(Boolean) : [],
        license: project.license?.name || project.license?.id || "Unknown",
        sourceUrl: project.source_url || "",
        issuesUrl: project.issues_url || "",
        wikiUrl: project.wiki_url || "",
        pageUrl: project.slug ? `https://modrinth.com/mod/${project.slug}` : `https://modrinth.com/mod/${project.id}`,
        compatibleVersions: Array.isArray(versions) ? versions.slice(0, 12).map(entry => ({
            id: entry.id,
            name: entry.name,
            versionNumber: entry.version_number,
            versionType: entry.version_type,
            gameVersions: entry.game_versions,
            loaders: entry.loaders,
            datePublished: entry.date_published
        })) : []
    };
}

async function postJson(url, body) {
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "User-Agent": "FlintFix-Client/1.0",
            "Accept": "application/json",
            "Content-Type": "application/json"
        },
        body: JSON.stringify(body || {})
    });
    if (!response.ok) {
        const error = new Error(`Request failed (${response.status}).`);
        error.status = response.status;
        throw error;
    }
    return response.json();
}

async function getCompatibleModUpdate(mod, instanceVersion, loader) {
    if (!mod?.sha1) return null;
    const url = `https://api.modrinth.com/v2/version_file/${encodeURIComponent(mod.sha1)}/update?algorithm=sha1`;
    try {
        const latest = await postJson(url, {
            loaders: loader && loader !== "vanilla" ? [loader] : [],
            game_versions: instanceVersion ? [instanceVersion] : [],
            version_types: ["release", "beta"]
        });
        const files = Array.isArray(latest?.files) ? latest.files : [];
        const primary = files.find(file => file?.primary && /\.jar$/i.test(file.filename || "")) || files.find(file => /\.jar$/i.test(file?.filename || ""));
        if (!primary) return null;
        const latestHash = primary?.hashes?.sha1 || "";
        return {
            projectId: latest.project_id || "",
            versionId: latest.id || "",
            versionNumber: latest.version_number || latest.name || "",
            fileName: primary.filename || "",
            fileUrl: primary.url || "",
            sha1: latestHash,
            updateAvailable: Boolean(latestHash && latestHash !== mod.sha1)
        };
    } catch (error) {
        if (error?.status === 404) return null;
        throw error;
    }
}

async function checkInstanceModUpdates(instanceId, options = {}) {
    const { mods } = await listInstanceMods(instanceId);
    const version = String(options.version || "").trim();
    const loader = String(options.loader || "fabric").trim().toLowerCase();
    const checked = [];
    for (const mod of mods) {
        const update = await getCompatibleModUpdate(mod, version, loader);
        checked.push({ fileName: mod.fileName, sha1: mod.sha1, update });
    }
    return checked;
}

async function applyInstanceModUpdate(instanceId, fileName, options = {}) {
    if (!isSafeModFileName(fileName)) throw new Error("Invalid mod file name.");
    const { mods } = await listInstanceMods(instanceId);
    const mod = mods.find(item => item.fileName === fileName);
    if (!mod) throw new Error("Mod file was not found.");
    const update = await getCompatibleModUpdate(mod, String(options.version || ""), String(options.loader || "fabric"));
    if (!update?.updateAvailable || !update.fileUrl) return { updated: false, reason: "Already up to date." };
    const response = await fetch(update.fileUrl, { headers: { "User-Agent": "FlintFix-Client/1.0" } });
    if (!response.ok) throw new Error(`Download failed (${response.status}).`);
    const bytes = Buffer.from(await response.arrayBuffer());
    const modsDir = getInstanceModsDir(instanceId);
    const enabledTargetName = buildUniqueModFileName(modsDir, update.fileName || mod.displayFileName);
    const finalName = mod.enabled ? enabledTargetName : `${enabledTargetName}.disabled`;
    await fs.promises.writeFile(path.join(modsDir, finalName), bytes);
    await fs.promises.rm(path.join(modsDir, mod.fileName), { force: true });
    return { updated: true, oldFileName: mod.fileName, fileName: finalName, versionNumber: update.versionNumber };
}

async function installCatalogMod(instanceId, options = {}) {
    normalizeInstanceId(instanceId);
    const modsDir = getInstanceModsDir(instanceId);
    await fs.promises.mkdir(modsDir, { recursive: true });
    const projectId = String(options.projectId || "").trim();
    if (!projectId) throw new Error("Missing project ID.");
    const loader = String(options.loader || "fabric").trim().toLowerCase();
    const version = String(options.version || "").trim();
    const versionsUrl = new URL(`https://api.modrinth.com/v2/project/${encodeURIComponent(projectId)}/version`);
    if (loader && loader !== "vanilla") versionsUrl.searchParams.set("loaders", JSON.stringify([loader]));
    if (version) versionsUrl.searchParams.set("game_versions", JSON.stringify([version]));
    let versions = await fetchJson(versionsUrl.toString());
    if (!Array.isArray(versions) || !versions.length) {
        // Strict installs (Performance mode) never fall back to a build for another version.
        if (options.strict) throw new Error(`No ${loader} build for Minecraft ${version || "this version"} yet.`);
        versions = await fetchJson(`https://api.modrinth.com/v2/project/${encodeURIComponent(projectId)}/version`);
    }
    if (!Array.isArray(versions) || !versions.length) throw new Error("No downloadable versions were found for this mod.");
    let chosen = null;
    let chosenFile = null;
    for (const entry of versions) {
        const files = Array.isArray(entry?.files) ? entry.files : [];
        const file = files.find(item => item?.primary && /\.jar$/i.test(item.filename || "")) || files.find(item => /\.jar$/i.test(item?.filename || ""));
        if (file) {
            chosen = entry;
            chosenFile = file;
            break;
        }
    }
    if (!chosenFile?.url) throw new Error("This mod does not expose a downloadable .jar file.");
    const response = await fetch(chosenFile.url, { headers: { "User-Agent": "FlintFix-Client/1.0" } });
    if (!response.ok) throw new Error(`Download failed (${response.status}).`);
    const arrayBuffer = await response.arrayBuffer();
    const finalName = buildUniqueModFileName(modsDir, chosenFile.filename || `${projectId}.jar`);
    if (finalName.toLowerCase() === "flintfix-client-mod.jar" || /^fabric-api-.*\.jar$/i.test(finalName)) {
        throw new Error("FlintFix manages Fabric API and the FlintFix client mod automatically.");
    }
    await fs.promises.writeFile(path.join(modsDir, finalName), Buffer.from(arrayBuffer));
    return {
        fileName: finalName,
        versionNumber: chosen?.version_number || "",
        projectId,
        title: chosen?.name || projectId
    };
}

async function syncInstanceModsToGame(instanceId, gameModsDir) {
    if (!instanceId) return;
    const { mods } = await listInstanceMods(instanceId);
    await fs.promises.mkdir(gameModsDir, { recursive: true });

    // Remove only files FlintFix copied during the previous instance launch.
    // Never delete unrelated/manual mods from the shared game directory.
    const manifestPath = path.join(gameModsDir, ".flintfix-instance-mods.json");
    let previous = [];
    try {
        const parsed = JSON.parse(await fs.promises.readFile(manifestPath, "utf8"));
        previous = Array.isArray(parsed?.files) ? parsed.files : [];
    } catch {
        previous = [];
    }
    for (const fileName of previous) {
        if (!isSafeModFileName(fileName)) continue;
        await fs.promises.rm(path.join(gameModsDir, fileName), { force: true });
    }

    const sourceDir = getInstanceModsDir(instanceId);
    const copied = [];
    for (const mod of mods) {
        if (!mod.enabled) continue;
        const source = path.join(sourceDir, mod.fileName);
        const targetName = `ffinst-${instanceId.slice(0, 24)}-${mod.displayFileName}`;
        const target = path.join(gameModsDir, targetName);
        await fs.promises.copyFile(source, target);
        copied.push(targetName);
    }
    await fs.promises.writeFile(
        manifestPath,
        JSON.stringify({ instanceId, files: copied, syncedAt: new Date().toISOString() }, null, 2),
        "utf8"
    );
}

function createWindow() {
    const cachedFlintIcon = getCachedMinecraftFlintIconPath();
    const fallbackIcon = path.join(
        __dirname,
        "assets",
        process.platform === "win32" ? "flintfix-client-icon.ico" : "flintfix-client-icon.png"
    );

    const win = new BrowserWindow({
        title: "FlintFix Client",
        width: 1200,
        height: 760,
        minWidth: 900,
        minHeight: 600,
        backgroundColor: "#07080b",
        frame: false,
        // Shown once the splash screen has painted, so the window never flashes white.
        show: false,
        autoHideMenuBar: true,
        icon: cachedFlintIcon || fallbackIcon,
        webPreferences: {
            preload: path.join(__dirname, "preload.js"),
            contextIsolation: true,
            nodeIntegration: false,
            backgroundThrottling: false
        }
    });

    win.setMenuBarVisibility(false);
    if (typeof win.removeMenu === "function") {
        win.removeMenu();
    }

    win.on("maximize", () => {
        if (!win.webContents.isDestroyed()) {
            win.webContents.send("window:maximized-changed", true);
        }
    });
    win.on("unmaximize", () => {
        if (!win.webContents.isDestroyed()) {
            win.webContents.send("window:maximized-changed", false);
        }
    });
    win.on("restore", () => {
        try { win.setOpacity(1); } catch {}
    });
    win.on("show", () => {
        try { win.setOpacity(1); } catch {}
    });

    let shown = false;
    const showWindow = () => {
        if (shown || win.isDestroyed()) return;
        shown = true;
        win.show();
    };
    win.once("ready-to-show", showWindow);
    setTimeout(showWindow, 4000);

    win.loadFile("index.html");
}


// ========================================
// HTTP JSON helper
// ========================================

function requestJson(urlString, { method = "GET", headers = {}, body = null, timeoutMs = 15000 } = {}) {
    return new Promise((resolve, reject) => {
        let target;
        try {
            target = new URL(urlString);
        } catch {
            reject(new Error("Invalid backend URL."));
            return;
        }
        const transport = target.protocol === "http:" ? http : https;
        const payload = body == null ? null : Buffer.from(JSON.stringify(body), "utf8");
        const requestHeaders = {
            Accept: "application/json",
            ...headers
        };
        if (payload) {
            requestHeaders["Content-Type"] = "application/json";
            requestHeaders["Content-Length"] = String(payload.length);
        }
        const req = transport.request(target, { method, headers: requestHeaders }, response => {
            let data = "";
            response.setEncoding("utf8");
            response.on("data", chunk => { data += chunk; });
            response.on("end", () => {
                let parsed = null;
                if (data) {
                    try { parsed = JSON.parse(data); } catch { parsed = null; }
                }
                if (response.statusCode < 200 || response.statusCode >= 300) {
                    const message = parsed?.error || parsed?.message || data || `Request failed with status ${response.statusCode}`;
                    reject(new Error(String(message).slice(0, 500)));
                    return;
                }
                resolve(parsed || {});
            });
        });
        req.setTimeout(timeoutMs, () => req.destroy(new Error("Request timed out.")));
        req.on("error", reject);
        if (payload) req.write(payload);
        req.end();
    });
}

function requireDiscordLinkBackend() {
    if (!discordLinkBackendUrl) {
        throw new Error("Discord linking is not configured. Add linkBackendUrl to discord/config.json.");
    }
    return discordLinkBackendUrl;
}

function requireFlintFixBackend() {
    if (!discordLinkBackendUrl) {
        throw new Error("FlintFix backend is not configured. Add linkBackendUrl to discord/config.json.");
    }
    return discordLinkBackendUrl;
}

async function getSessionForDiscordLink() {
    const session = await getMinecraftSessionForLaunch();
    if (!session?.minecraft?.accessToken) {
        throw new Error("Sign in to your Minecraft account first.");
    }
    return session;
}

function loadDiscordLinkPublicKey() {
    const keyPath = path.join(__dirname, "discord", "link-public.pem");
    try {
        return fs.readFileSync(keyPath, "utf8");
    } catch {
        throw new Error("Discord linking public key is missing from the FlintFix installation.");
    }
}

function createEncryptedDiscordLinkProof(session, action, extra = {}, returnKey = false) {
    if (!session?.minecraft?.accessToken) {
        throw new Error("A valid Minecraft session is required.");
    }

    const plaintext = Buffer.from(JSON.stringify({
        ...extra,
        action,
        accessToken: session.minecraft.accessToken,
        ts: Math.floor(Date.now() / 1000),
        nonce: crypto.randomBytes(24).toString("base64url")
    }), "utf8");

    const aesKey = crypto.randomBytes(32);
    const iv = crypto.randomBytes(12);
    const cipher = crypto.createCipheriv("aes-256-gcm", aesKey, iv);
    const encrypted = Buffer.concat([cipher.update(plaintext), cipher.final()]);
    const tag = cipher.getAuthTag();

    const encryptedKey = crypto.publicEncrypt({
        key: loadDiscordLinkPublicKey(),
        padding: crypto.constants.RSA_PKCS1_OAEP_PADDING,
        oaepHash: "sha256"
    }, aesKey);

    const body = {
        key: encryptedKey.toString("base64"),
        iv: iv.toString("base64"),
        tag: tag.toString("base64"),
        data: encrypted.toString("base64")
    };
    return returnKey ? { body, aesKey } : body;
}

function verifySignedDiscordEnvelope(envelope) {
    if (!envelope || typeof envelope.payload !== "string" || typeof envelope.signature !== "string") {
        throw new Error("Discord service returned an unsigned response.");
    }

    let signature;
    try {
        signature = Buffer.from(envelope.signature, "base64");
    } catch {
        throw new Error("Discord service returned an invalid signature.");
    }

    const verified = crypto.verify(
        "sha256",
        Buffer.from(envelope.payload, "utf8"),
        {
            key: loadDiscordLinkPublicKey(),
            padding: crypto.constants.RSA_PKCS1_PSS_PADDING,
            saltLength: 32
        },
        signature
    );

    if (!verified) {
        throw new Error("Discord service response could not be verified.");
    }

    try {
        return JSON.parse(envelope.payload);
    } catch {
        throw new Error("Discord service returned invalid signed data.");
    }
}

async function requestSignedDiscordJson(url, options = {}) {
    return verifySignedDiscordEnvelope(await requestJson(url, options));
}

async function requestEncryptedSocialJson(url, session, action, extra = {}) {
    const request = createEncryptedDiscordLinkProof(session, action, extra, true);
    const envelope = await requestJson(url, { method: "POST", body: request.body });
    if (!envelope?.encrypted || typeof envelope.iv !== "string" || typeof envelope.data !== "string" || typeof envelope.tag !== "string") {
        throw new Error("FlintFix Social returned an invalid encrypted response.");
    }
    try {
        const decipher = crypto.createDecipheriv("aes-256-gcm", request.aesKey, Buffer.from(envelope.iv, "base64"));
        decipher.setAuthTag(Buffer.from(envelope.tag, "base64"));
        const plaintext = Buffer.concat([
            decipher.update(Buffer.from(envelope.data, "base64")),
            decipher.final()
        ]).toString("utf8");
        return verifySignedDiscordEnvelope(JSON.parse(plaintext));
    } catch (error) {
        if (error?.message?.includes("verified") || error?.message?.includes("signed")) throw error;
        throw new Error("FlintFix Social response could not be decrypted.");
    }
}

function getJSON(url) {
    return new Promise((resolve, reject) => {
        https.get(url, response => {
            if (response.statusCode !== 200) {
                reject(
                    new Error(
                        `Request failed with status ${response.statusCode}`
                    )
                );

                response.resume();
                return;
            }

            let data = "";

            response.on("data", chunk => {
                data += chunk;
            });

            response.on("end", () => {
                try {
                    resolve(JSON.parse(data));
                } catch (error) {
                    reject(error);
                }
            });
        }).on("error", reject);
    });
}


let discordPresenceRemoteConfigLoaded = false;
let discordPresenceRemoteConfigError = null;

async function ensureDiscordPresenceConfigured({ force = false } = {}) {
    if (discordPresenceRemoteConfigLoaded && !force) {
        return discordPresence.status();
    }

    if (!discordLinkBackendUrl) {
        discordPresenceRemoteConfigLoaded = true;
        return discordPresence.status();
    }

    try {
        const result = await requestSignedDiscordJson(`${discordLinkBackendUrl}/api/presence/config`);
        if (result?.success && result?.clientId) {
            await discordPresence.configure({
                clientId: result.clientId,
                largeImageKey: result.largeImageKey || discordConfig.largeImageKey || "",
                discordInvite: result.discordInvite || discordConfig.discordInvite,
                source: "FlintFix Discord bot"
            });
        }
        discordPresenceRemoteConfigError = null;
    } catch (error) {
        discordPresenceRemoteConfigError = error?.message || "Could not load Discord Rich Presence configuration.";
    } finally {
        discordPresenceRemoteConfigLoaded = true;
    }

    return {
        ...discordPresence.status(),
        configError: discordPresenceRemoteConfigError
    };
}

// ========================================
// Minecraft versions
// ========================================

ipcMain.handle("minecraft:getVersions", async () => {
    try {
        const manifest = await getJSON(
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        );

        return {
            success: true,

            latest: manifest.latest,

            versions: manifest.versions.map(version => ({
                id: version.id,
                type: version.type,
                url: version.url,
                releaseTime: version.releaseTime,
                sha1: version.sha1
            }))
        };
    } catch (error) {
        console.error(
            "Failed to get Minecraft versions:",
            error
        );

        return {
            success: false,
            error: error.message
        };
    }
});



// ========================================
// Minecraft version metadata
// ========================================

ipcMain.handle(
    "minecraft:getVersionInfo",
    async (event, metadataUrl) => {
        try {
            // Only allow Mojang's metadata servers.
            const parsedUrl = new URL(metadataUrl);

            const allowedHosts = [
                "piston-meta.mojang.com",
                "launchermeta.mojang.com"
            ];

            if (!allowedHosts.includes(parsedUrl.hostname)) {
                throw new Error(
                    "Invalid Minecraft metadata URL."
                );
            }

            const metadata = await getJSON(metadataUrl);

            return {
                success: true,

                id: metadata.id,

                javaVersion: metadata.javaVersion
                    ? {
                        component:
                            metadata.javaVersion.component,

                        majorVersion:
                            metadata.javaVersion.majorVersion
                    }
                    : null
            };

        } catch (error) {
            console.error(
                "Failed to load Minecraft metadata:",
                error
            );

            return {
                success: false,
                error: error.message
            };
        }
    }
);

// ========================================
// Java detection
// ========================================

function runJava(javaExecutable) {
    return new Promise((resolve, reject) => {
        execFile(
            javaExecutable,
            ["-version"],

            {
                windowsHide: true
            },

            (error, stdout, stderr) => {
                // Java normally prints -version to stderr.
                const output =
                    `${stdout || ""}\n${stderr || ""}`.trim();

                if (error && !output) {
                    reject(error);
                    return;
                }

                const versionMatch =
                    output.match(/version\s+"([^"]+)"/i);

                const runtimeMatch =
                    output.match(
                        /(?:Runtime Environment|OpenJDK Runtime Environment).*$/im
                    );

                resolve({
                    executable: javaExecutable,

                    version: versionMatch
                        ? versionMatch[1]
                        : "Unknown",

                    runtime: runtimeMatch
                        ? runtimeMatch[0].trim()
                        : "Java Runtime",

                    raw: output
                });
            }
        );
    });
}


function findJavaOnPath() {
    return new Promise(resolve => {
        execFile(
            "where.exe",
            ["java"],

            {
                windowsHide: true
            },

            (error, stdout) => {
                if (error || !stdout) {
                    resolve([]);
                    return;
                }

                const paths = stdout
                    .split(/\r?\n/)
                    .map(line => line.trim())
                    .filter(Boolean);

                resolve([...new Set(paths)]);
            }
        );
    });
}


ipcMain.handle("java:detect", async () => {
    try {
        const javaPaths = await findJavaOnPath();

        // First try Java installations found in PATH.
        for (const javaPath of javaPaths) {
            try {
                const java = await runJava(javaPath);

                return {
                    success: true,
                    found: true,
                    ...java
                };
            } catch (error) {
                console.log(
                    `Java candidate failed: ${javaPath}`
                );
            }
        }

        // Also try the normal "java" command directly.
        try {
            const java = await runJava("java");

            return {
                success: true,
                found: true,
                ...java
            };
        } catch (error) {
            // Continue to not-found response.
        }

        return {
            success: true,
            found: false
        };
    } catch (error) {
        console.error(
            "Java detection failed:",
            error
        );

        return {
            success: false,
            found: false,
            error: error.message
        };
    }
});



// ========================================
// FLINTFIX MANAGED BUILD JDK
// ========================================

// The mod build (Gradle 9 + Fabric Loom 1.18) runs on JDK 25; it still compiles
// Java 17/21 jars for older Minecraft versions.
const BUILD_JDK_MAJOR = 25;

function findJavacExecutable(directory) {
    if (!fs.existsSync(directory)) return null;
    const stack = [directory];
    while (stack.length) {
        const current = stack.pop();
        for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
            const full = path.join(current, entry.name);
            if (entry.isDirectory()) stack.push(full);
            else if (entry.isFile() && entry.name.toLowerCase() === "javac.exe") return full;
        }
    }
    return null;
}

function findJavacOnPath() {
    return new Promise(resolve => {
        execFile("where.exe", ["javac"], { windowsHide: true }, (error, stdout) => {
            if (error || !stdout) return resolve([]);
            resolve([...new Set(stdout.split(/\r?\n/).map(line => line.trim()).filter(Boolean))]);
        });
    });
}

async function findSystemBuildJdk() {
    for (const javac of await findJavacOnPath()) {
        const javaExecutable = path.join(path.dirname(javac), "java.exe");
        if (!fs.existsSync(javaExecutable)) continue;
        try {
            const java = await runJava(javaExecutable);
            const major = Number(String(java.version || "").split(/[._]/)[0]);
            if (major === BUILD_JDK_MAJOR) return javaExecutable;
        } catch (_) {}
    }
    return null;
}

async function ensureBuildJdk(emit) {
    // Installed builds ship prebuilt mod jars (see minecraft/fabric.js), so no compiler is needed.
    if (app.isPackaged && fs.existsSync(path.join(process.resourcesPath, "flintfix-mods"))) return null;
    const systemJdk = await findSystemBuildJdk();
    if (systemJdk) {
        emit?.({ stage: "fabric", phase: "jdk", message: `Using existing system JDK ${BUILD_JDK_MAJOR}.` });
        return systemJdk;
    }

    const jdkRoot = path.join(app.getPath("userData"), "runtime", `jdk-${BUILD_JDK_MAJOR}-build`);
    const existingJavac = findJavacExecutable(jdkRoot);
    if (existingJavac) {
        const javaExecutable = path.join(path.dirname(existingJavac), "java.exe");
        if (fs.existsSync(javaExecutable)) return javaExecutable;
    }

    if (process.platform !== "win32" || process.arch !== "x64") {
        throw new Error("Automatic FlintFix build JDK installation currently supports Windows x64 only.");
    }

    emit?.({ stage: "fabric", message: `Installing managed JDK ${BUILD_JDK_MAJOR} for FlintFix mod compilation...` });
    const zipPath = path.join(app.getPath("temp"), `flintfix-jdk-${BUILD_JDK_MAJOR}-build.zip`);
    const downloadUrl = `https://api.adoptium.net/v3/binary/latest/${BUILD_JDK_MAJOR}/ga/windows/x64/jdk/hotspot/normal/eclipse`;

    await downloadFile(downloadUrl, zipPath, percent => {
        emit?.({
            stage: "fabric",
            phase: "jdk-download",
            current: percent,
            total: 100,
            message: `Downloading managed JDK ${BUILD_JDK_MAJOR}... ${percent}%`
        });
    });

    fs.rmSync(jdkRoot, { recursive: true, force: true });
    fs.mkdirSync(jdkRoot, { recursive: true });
    await extract(zipPath, { dir: jdkRoot });
    try { fs.rmSync(zipPath, { force: true }); } catch (_) {}

    const javac = findJavacExecutable(jdkRoot);
    if (!javac) throw new Error(`Managed JDK ${BUILD_JDK_MAJOR} downloaded, but javac.exe was not found.`);
    const javaExecutable = path.join(path.dirname(javac), "java.exe");
    if (!fs.existsSync(javaExecutable)) throw new Error(`Managed JDK ${BUILD_JDK_MAJOR} downloaded, but java.exe was not found next to javac.exe.`);

    emit?.({ stage: "fabric", message: `Managed JDK ${BUILD_JDK_MAJOR} compiler ready.` });
    return javaExecutable;
}

// ========================================
// FLINTFIX MANAGED JAVA
// ========================================

function getRuntimeDirectory(majorVersion) {

    return path.join(
        app.getPath("userData"),
        "runtime",
        `java-${majorVersion}`
    );
}


function findJavaExecutable(directory) {

    if (!fs.existsSync(directory)) {
        return null;
    }


    const directJava = path.join(
        directory,
        "bin",
        "java.exe"
    );


    const directJavaw = path.join(
        directory,
        "bin",
        "javaw.exe"
    );


    if (fs.existsSync(directJavaw)) {
        return directJavaw;
    }


    if (fs.existsSync(directJava)) {
        return directJava;
    }


    // Temurin ZIP normally contains a root folder,
    // for example:
    //
    // jdk-21.0.12+7/
    //     bin/
    //         java.exe

    const entries = fs.readdirSync(
        directory,
        {
            withFileTypes: true
        }
    );


    for (const entry of entries) {

        if (!entry.isDirectory()) {
            continue;
        }


        const javaw = path.join(
            directory,
            entry.name,
            "bin",
            "javaw.exe"
        );


        const java = path.join(
            directory,
            entry.name,
            "bin",
            "java.exe"
        );


        if (fs.existsSync(javaw)) {
            return javaw;
        }


        if (fs.existsSync(java)) {
            return java;
        }
    }


    return null;
}


// ========================================
// DOWNLOAD WITH REDIRECT SUPPORT
// ========================================

function downloadFile(
    url,
    destination,
    onProgress,
    redirectCount = 0
) {

    return new Promise((resolve, reject) => {

        if (redirectCount > 10) {

            reject(
                new Error(
                    "Too many download redirects."
                )
            );

            return;
        }


        const request = https.get(
            url,

            {
                headers: {
                    "User-Agent":
                        "FlintFix-Client/0.1"
                }
            },

            response => {

                // Follow redirects.

                if (
                    response.statusCode >= 300 &&
                    response.statusCode < 400 &&
                    response.headers.location
                ) {

                    response.resume();


                    const redirectUrl =
                        new URL(
                            response.headers.location,
                            url
                        ).toString();


                    downloadFile(
                        redirectUrl,
                        destination,
                        onProgress,
                        redirectCount + 1
                    )
                        .then(resolve)
                        .catch(reject);


                    return;
                }


                if (response.statusCode !== 200) {

                    response.resume();

                    reject(
                        new Error(
                            `Download failed with HTTP ${response.statusCode}`
                        )
                    );

                    return;
                }


                const total =
                    Number(
                        response.headers[
                            "content-length"
                        ]
                    ) || 0;


                let downloaded = 0;


                const file =
                    fs.createWriteStream(
                        destination
                    );


                response.on(
                    "data",
                    chunk => {

                        downloaded +=
                            chunk.length;


                        if (
                            total > 0 &&
                            onProgress
                        ) {

                            const percent =
                                Math.round(
                                    (
                                        downloaded /
                                        total
                                    ) * 100
                                );


                            onProgress(
                                percent,
                                downloaded,
                                total
                            );
                        }
                    }
                );


                response.pipe(file);


                file.on(
                    "finish",
                    () => {

                        file.close(() => {
                            resolve();
                        });
                    }
                );


                file.on(
                    "error",
                    error => {

                        file.close();

                        try {
                            fs.unlinkSync(
                                destination
                            );
                        } catch {
                            // Ignore cleanup errors.
                        }


                        reject(error);
                    }
                );
            }
        );


        request.on(
            "error",
            error => {

                try {
                    fs.unlinkSync(
                        destination
                    );
                } catch {
                    // Ignore cleanup errors.
                }


                reject(error);
            }
        );
    });
}


// ========================================
// CHECK MANAGED JAVA
// ========================================

ipcMain.handle(
    "java:getManaged",

    async (event, majorVersion) => {

        try {

            const major =
                Number(majorVersion);


            if (
                !Number.isInteger(major) ||
                major < 8 ||
                major > 30
            ) {

                throw new Error(
                    "Invalid Java version."
                );
            }


            const runtimeDirectory =
                getRuntimeDirectory(
                    major
                );


            const executable =
                findJavaExecutable(
                    runtimeDirectory
                );


            if (!executable) {

                return {
                    success: true,
                    found: false
                };
            }


            const java =
                await runJava(
                    executable
                );


            return {
                success: true,
                found: true,

                managed: true,

                executable:
                    executable,

                version:
                    java.version,

                runtime:
                    java.runtime
            };


        } catch (error) {

            console.error(
                "Managed Java check failed:",
                error
            );


            return {
                success: false,
                found: false,
                error: error.message
            };
        }
    }
);


// ========================================
// INSTALL MANAGED JAVA
// ========================================

ipcMain.handle(
    "java:install",

    async (event, majorVersion) => {

        let zipPath = null;


        try {

            const major =
                Number(majorVersion);


            if (
                !Number.isInteger(major) ||
                major < 8 ||
                major > 30
            ) {

                throw new Error(
                    "Invalid Java version."
                );
            }


            // FlintFix is currently Windows-only.

            if (process.platform !== "win32") {

                throw new Error(
                    "Automatic Java installation currently supports Windows only."
                );
            }


            if (process.arch !== "x64") {

                throw new Error(
                    `Unsupported Windows architecture: ${process.arch}`
                );
            }


            const runtimeDirectory =
                getRuntimeDirectory(
                    major
                );


            fs.mkdirSync(
                runtimeDirectory,
                {
                    recursive: true
                }
            );


            // Check if it is already installed.

            const existing =
                findJavaExecutable(
                    runtimeDirectory
                );


            if (existing) {

                const java =
                    await runJava(
                        existing
                    );


                return {
                    success: true,
                    alreadyInstalled: true,

                    executable:
                        existing,

                    version:
                        java.version
                };
            }


            zipPath =
                path.join(
                    app.getPath("temp"),
                    `flintfix-java-${major}.zip`
                );


            // Eclipse Adoptium API:
            //
            // latest Java <major>
            // GA
            // Windows
            // x64
            // JRE
            // HotSpot

            const downloadUrl =
                `https://api.adoptium.net/v3/binary/latest/${major}/ga/windows/x64/jre/hotspot/normal/eclipse`;


            event.sender.send(
                "java:progress",
                {
                    stage: "download",
                    percent: 0,
                    majorVersion: major
                }
            );


            await downloadFile(

                downloadUrl,

                zipPath,

                percent => {

                    if (
                        !event.sender.isDestroyed()
                    ) {

                        event.sender.send(
                            "java:progress",
                            {
                                stage:
                                    "download",

                                percent:
                                    percent,

                                majorVersion:
                                    major
                            }
                        );
                    }
                }
            );


            event.sender.send(
                "java:progress",
                {
                    stage: "extract",
                    percent: 100,
                    majorVersion: major
                }
            );


            // Remove an incomplete previous
            // runtime if one exists.

            fs.rmSync(
                runtimeDirectory,
                {
                    recursive: true,
                    force: true
                }
            );


            fs.mkdirSync(
                runtimeDirectory,
                {
                    recursive: true
                }
            );


            await extract(
                zipPath,
                {
                    dir:
                        runtimeDirectory
                }
            );


            const executable =
                findJavaExecutable(
                    runtimeDirectory
                );


            if (!executable) {

                throw new Error(
                    "Java downloaded, but java.exe could not be found."
                );
            }


            const java =
                await runJava(
                    executable
                );


            event.sender.send(
                "java:progress",
                {
                    stage: "complete",
                    percent: 100,
                    majorVersion: major
                }
            );


            return {
                success: true,
                alreadyInstalled: false,

                executable:
                    executable,

                version:
                    java.version,

                runtime:
                    java.runtime
            };


        } catch (error) {

            console.error(
                "Java installation failed:",
                error
            );


            return {
                success: false,
                error: error.message
            };


        } finally {

            if (
                zipPath &&
                fs.existsSync(zipPath)
            ) {

                try {

                    fs.unlinkSync(
                        zipPath
                    );

                } catch (error) {

                    console.warn(
                        "Couldn't remove Java ZIP:",
                        error
                    );
                }
            }
        }
    }
);

// ========================================
// MINECRAFT VANILLA INSTALLER
// ========================================

ipcMain.handle("minecraft:install", async (event, versionId) => {
    try {
        if (typeof versionId !== "string" || !/^[A-Za-z0-9._+-]+$/.test(versionId)) {
            throw new Error("Invalid Minecraft version ID.");
        }

        const manifest = await getJSON(
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        );

        const version = manifest.versions.find(item => item.id === versionId);
        if (!version) {
            throw new Error(`Minecraft ${versionId} was not found in Mojang's manifest.`);
        }

        const minecraftRoot = path.join(app.getPath("userData"), "minecraft");
        const emit = data => {
            if (!event.sender.isDestroyed()) {
                event.sender.send("minecraft:progress", data);
            }
        };

        const installation = await installMinecraft({
            rootDir: minecraftRoot,
            versionId: version.id,
            metadataUrl: version.url,
            metadataSha1: version.sha1,
            emit
        });

        return { success: true, installation };
    } catch (error) {
        console.error("Minecraft installation failed:", error);
        return { success: false, error: error.message };
    }
});


// ========================================
// MICROSOFT / MINECRAFT AUTHENTICATION
// ========================================

ipcMain.handle("auth:status", async () => {
    try {
        const status = await microsoftAuth.getStatus();

        if (status.signedIn && !hasUsableMinecraftSession()) {
            try {
                const session = await microsoftAuth.createMinecraftSession(null, false);
                rememberMinecraftSession(session);
            } catch (error) {
                console.warn("Cached Microsoft account could not refresh Minecraft profile:", error.message);
            }
        }

        return {
            success: true,
            ...status,
            activeProfile: activeMinecraftSession?.minecraft ? {
                name: activeMinecraftSession.minecraft.username,
                id: activeMinecraftSession.minecraft.uuid,
                skinUrl: activeMinecraftSession.minecraft.skinUrl || null,
                microsoftUsername: activeMinecraftSession.account?.username || null,
                homeAccountId: activeMinecraftSession.account?.homeAccountId || null
            } : null
        };
    } catch (error) {
        return { success: false, signedIn: false, error: error.message };
    }
});

ipcMain.handle("auth:signIn", async event => {
    try {
        const session = await microsoftAuth.createMinecraftSession(
            null,
            true
        );

        rememberMinecraftSession(session);

        return {
            success: true,
            profile: {
                name: session.minecraft.username,
                id: session.minecraft.uuid,
                skinUrl: session.minecraft.skinUrl || null,
                microsoftUsername: session.account?.username || null,
                homeAccountId: session.account?.homeAccountId || null
            }
        };
    } catch (error) {
        console.error("Microsoft/Minecraft sign-in failed:", error);

        return {
            success: false,
            code: error.code || null,
            error: error.message
        };
    }
});

ipcMain.handle("auth:listAccounts", async () => {
    try {
        const accounts = await microsoftAuth.listMinecraftAccounts();
        const activeHomeAccountId = activeMinecraftSession?.account?.homeAccountId || null;
        return {
            success: true,
            activeHomeAccountId,
            accounts
        };
    } catch (error) {
        return { success: false, accounts: [], error: error.message };
    }
});

ipcMain.handle("auth:switchAccount", async (_event, homeAccountId) => {
    try {
        if (typeof homeAccountId !== "string" || !homeAccountId) {
            throw new Error("Invalid Microsoft account selection.");
        }

        const session = await microsoftAuth.createMinecraftSessionForAccount(homeAccountId);
        rememberMinecraftSession(session);

        return {
            success: true,
            profile: {
                name: session.minecraft.username,
                id: session.minecraft.uuid,
                skinUrl: session.minecraft.skinUrl || null,
                microsoftUsername: session.account?.username || null,
                homeAccountId: session.account?.homeAccountId || null
            }
        };
    } catch (error) {
        return {
            success: false,
            code: error.code || null,
            error: error.message
        };
    }
});

ipcMain.handle("auth:signOut", async () => {
    try {
        clearMinecraftSession();
        await microsoftAuth.signOut();
        return { success: true };
    } catch (error) {
        return { success: false, error: error.message };
    }
});


// ========================================
// DEV / OFFLINE MINECRAFT LAUNCH
// ========================================

function createOfflineUuid(username) {
    // Minecraft-compatible deterministic offline UUID (UUID v3 style).
    const digest = crypto
        .createHash("md5")
        .update(`OfflinePlayer:${username}`, "utf8")
        .digest();

    digest[6] = (digest[6] & 0x0f) | 0x30;
    digest[8] = (digest[8] & 0x3f) | 0x80;

    const hex = digest.toString("hex");
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

ipcMain.handle(
    "minecraft:launchDev",
    async (event, versionId, javaExecutable, requestedUsername, loader = "vanilla") => {
        try {
            if (typeof versionId !== "string" || !/^[A-Za-z0-9._+-]+$/.test(versionId)) {
                throw new Error("Invalid Minecraft version ID.");
            }

            if (typeof javaExecutable !== "string" || javaExecutable.length > 1000) {
                throw new Error("Invalid Java executable.");
            }

            const username =
                typeof requestedUsername === "string" && /^[A-Za-z0-9_]{3,16}$/.test(requestedUsername)
                    ? requestedUsername
                    : "FlintFixDev";

            const minecraftRoot = path.join(app.getPath("userData"), "minecraft");
            const session = {
                minecraft: {
                    username,
                    uuid: createOfflineUuid(username),
                    accessToken: "0",
                    offline: true
                }
            };

            let launchVersionId = versionId;
            if (loader === "fabric") {
                const fabricEmit = data => {
                    if (!event.sender.isDestroyed()) event.sender.send("minecraft:progress", data);
                };
                const buildJavaExecutable = await ensureBuildJdk(fabricEmit);
                const fabric = await installFabric({
                    rootDir: minecraftRoot,
                    projectRoot: __dirname,
                    minecraftVersion: versionId,
                    javaExecutable: buildJavaExecutable,
                    emit: fabricEmit
                });
                launchVersionId = fabric.versionId;
            } else if (loader !== "vanilla") {
                throw new Error("DEV mode currently supports Vanilla or Fabric only.");
            }

            const launched = launchMinecraft({
                rootDir: minecraftRoot,
                versionId: launchVersionId,
                javaExecutable,
                session,
                onLog: data => {
                    if (!event.sender.isDestroyed()) event.sender.send("minecraft:log", data);
                },
                onExit: data => {
                    if (!event.sender.isDestroyed()) {
                        event.sender.send("minecraft:log", {
                            source: "GAME",
                            message: `Minecraft exited with code ${data.code ?? "unknown"}` +
                                (data.signal ? ` (${data.signal})` : "")
                        });
                    }
                }
            });

            return {
                success: true,
                pid: launched.pid,
                profile: { name: username, id: session.minecraft.uuid, offline: true }
            };
        } catch (error) {
            console.error("DEV Minecraft launch failed:", error);
            return { success: false, error: error.message };
        }
    }
);

// ========================================
// MINECRAFT LAUNCH
// ========================================

ipcMain.handle(
    "minecraft:launch",
    async (event, versionId, javaExecutable, loader = "vanilla", launchOptions = {}) => {
        let launcherWindow = null;
        let keepLauncherVisible = false;
        let preventLaunchMinimize = null;
        try {
            if (
                typeof versionId !== "string" ||
                !/^[A-Za-z0-9._+-]+$/.test(versionId)
            ) {
                throw new Error("Invalid Minecraft version ID.");
            }

            if (
                typeof javaExecutable !== "string" ||
                javaExecutable.length > 1000
            ) {
                throw new Error("Invalid Java executable.");
            }

            if (loader !== "vanilla" && loader !== "fabric") {
                throw new Error("Microsoft mode currently supports Vanilla or Fabric only.");
            }

            const normalizedLaunchOptions = {
                width: Math.max(320, Math.min(7680, Math.round(Number(launchOptions?.width) || 1280))),
                height: Math.max(240, Math.min(4320, Math.round(Number(launchOptions?.height) || 720))),
                fullscreen: Boolean(launchOptions?.fullscreen),
                launcherVisibility: "keep",
                discordRichPresence: Boolean(launchOptions?.discordRichPresence),
                instanceId: launchOptions?.instanceId ? normalizeInstanceId(launchOptions.instanceId) : null,
                joinServer: (() => {
                    if (!launchOptions?.joinServer) return null;
                    try {
                        serverPing.parseAddress(launchOptions.joinServer);
                        return String(launchOptions.joinServer).trim();
                    } catch {
                        return null;
                    }
                })()
            };
            // Keep the launcher visible from the moment Play is pressed. On
            // Windows, Electron can receive a minimize event as Minecraft
            // takes focus; restore it so it remains available behind the game.
            keepLauncherVisible = true;

            launcherWindow = BrowserWindow.fromWebContents(event.sender);
            preventLaunchMinimize = () => {
                if (!keepLauncherVisible || !launcherWindow || launcherWindow.isDestroyed()) return;
                setTimeout(() => {
                    if (!keepLauncherVisible || launcherWindow.isDestroyed()) return;
                    if (launcherWindow.isMinimized()) launcherWindow.restore();
                    if (!launcherWindow.isVisible() && typeof launcherWindow.showInactive === "function") {
                        launcherWindow.showInactive();
                    }
                    launcherWindow.blur();
                }, 0);
            };
            if (launcherWindow && !launcherWindow.isDestroyed()) {
                launcherWindow.on("minimize", preventLaunchMinimize);
            }
            const restoreLauncherWindow = () => {
                if (!launcherWindow || launcherWindow.isDestroyed()) return;
                keepLauncherVisible = false;
                launcherWindow.removeListener("minimize", preventLaunchMinimize);
                launcherWindow.setSkipTaskbar(false);
                if (typeof launcherWindow.setFocusable === "function") launcherWindow.setFocusable(true);
                if (launcherWindow.isMinimized()) launcherWindow.restore();
                if (!launcherWindow.isVisible()) launcherWindow.show();
            };

            const minecraftRoot =
                path.join(
                    app.getPath("userData"),
                    "minecraft"
                );

            const emitLaunchProgress = data => {
                if (!event.sender.isDestroyed()) {
                    event.sender.send("minecraft:progress", data);
                }
            };

            emitLaunchProgress({
                stage: "launch",
                phase: "session",
                message: "Checking Minecraft session..."
            });

            // Reuse the session that was just proven valid by Sign In.
            // Only refresh if there is no usable in-memory session.
            const session = await getMinecraftSessionForLaunch();
            emitLaunchProgress({
                stage: "launch",
                phase: "session-ready",
                message: `Minecraft session ready for ${session.minecraft.username}.`
            });

            let launchVersionId = versionId;
            if (loader === "fabric") {
                const fabricEmit = data => emitLaunchProgress(data);

                emitLaunchProgress({ stage: "fabric", phase: "jdk", message: "Checking Fabric build runtime..." });
                const buildJavaExecutable = await ensureBuildJdk(fabricEmit);
                const fabric = await installFabric({
                    rootDir: minecraftRoot,
                    projectRoot: __dirname,
                    minecraftVersion: versionId,
                    javaExecutable: buildJavaExecutable,
                    emit: fabricEmit
                });

                if (normalizedLaunchOptions.instanceId) {
                    emitLaunchProgress({ stage: "launch", phase: "mods", message: "Syncing instance mods..." });
                    await syncInstanceModsToGame(normalizedLaunchOptions.instanceId, fabric.modsDir);
                }
                launchVersionId = fabric.versionId;
            }

            emitLaunchProgress({ stage: "launch", phase: "process", message: "Starting Minecraft Java process..." });

            const launched =
                launchMinecraft({
                    rootDir: minecraftRoot,
                    versionId: launchVersionId,
                    javaExecutable,
                    session,
                    launchOptions: normalizedLaunchOptions,

                    onLog: data => {
                        if (!event.sender.isDestroyed()) {
                            event.sender.send(
                                "minecraft:log",
                                data
                            );
                        }
                    },

                    onExit: data => {
                        restoreLauncherWindow();
                        if (normalizedLaunchOptions.discordRichPresence) {
                            void discordPresence.setLauncher({
                                username: session.minecraft.username,
                                version: versionId,
                                loader: loader === "fabric" ? "Fabric" : "Vanilla"
                            });
                        }
                        if (!event.sender.isDestroyed()) {
                            event.sender.send(
                                "minecraft:log",
                                {
                                    source: "GAME",
                                    message:
                                        `Minecraft exited with code ${data.code ?? "unknown"}` +
                                        (data.signal ? ` (${data.signal})` : "")
                                }
                            );
                        }
                    }
                });

            emitLaunchProgress({
                stage: "started",
                phase: "process-started",
                message: "Minecraft process started.",
                pid: launched.pid
            });

            if (normalizedLaunchOptions.discordRichPresence) {
                await ensureDiscordPresenceConfigured();
                const presenceResult = await discordPresence.setEnabled(true);
                if (presenceResult.success) {
                    await discordPresence.setPlaying({
                        version: versionId,
                        loader: loader === "fabric" ? "Fabric" : "Vanilla",
                        username: session.minecraft.username
                    });
                }
            } else {
                await discordPresence.setEnabled(false);
            }

            if (launcherWindow && !launcherWindow.isDestroyed()) {
                // Keep open must remain a real visible Windows window while
                // Minecraft owns focus. Do not remove it from the taskbar.
                launcherWindow.setSkipTaskbar(false);
                if (launcherWindow.isMinimized()) {
                    launcherWindow.restore();
                }
                if (!launcherWindow.isVisible()) {
                    if (typeof launcherWindow.showInactive === "function") {
                        launcherWindow.showInactive();
                    } else {
                        launcherWindow.show();
                    }
                }

                // Keep the launcher behind the game without making Windows
                // treat it like a hidden/background-only utility window.
                if (typeof launcherWindow.setFocusable === "function") {
                    launcherWindow.setFocusable(true);
                }
                launcherWindow.blur();
            }

            return {
                success: true,
                pid: launched.pid,
                profile: {
                    name: session.minecraft.username,
                    id: session.minecraft.uuid,
                    skinUrl: session.minecraft.skinUrl || null,
                    microsoftUsername: session.account?.username || null,
                    homeAccountId: session.account?.homeAccountId || null
                }
            };
        } catch (error) {
            keepLauncherVisible = false;
            if (launcherWindow && !launcherWindow.isDestroyed()) {
                if (preventLaunchMinimize) launcherWindow.removeListener("minimize", preventLaunchMinimize);
                launcherWindow.setSkipTaskbar(false);
                if (typeof launcherWindow.setFocusable === "function") launcherWindow.setFocusable(true);
                if (launcherWindow.isMinimized()) launcherWindow.restore();
                if (!launcherWindow.isVisible()) launcherWindow.show();
            }
            console.error(
                "Minecraft launch failed:",
                error
            );

            return {
                success: false,
                code: error.code || null,
                error: error.message
            };
        }
    }
);


// ========================================
// DISCORD RICH PRESENCE
// ========================================

ipcMain.handle("discord:presence:status", async () => {
    const configStatus = await ensureDiscordPresenceConfigured({
        force: !discordPresence.isConfigured()
    });
    return {
        success: true,
        ...configStatus
    };
});

ipcMain.handle("discord:presence:setEnabled", async (_event, enabled, context = {}) => {
    const wantsEnabled = Boolean(enabled);
    await ensureDiscordPresenceConfigured({
        force: wantsEnabled && !discordPresence.isConfigured()
    });
    const result = await discordPresence.setEnabled(wantsEnabled);
    if (Boolean(enabled) && result.success) {
        await discordPresence.setLauncher({
            username: context?.username || null,
            version: context?.version || null,
            loader: context?.loader || null
        });
    }
    return {
        ...result,
        configError: discordPresenceRemoteConfigError
    };
});

ipcMain.handle("discord:presence:updateLauncher", async (_event, context = {}) => {
    await ensureDiscordPresenceConfigured();
    const success = await discordPresence.setLauncher({
        username: context?.username || null,
        version: context?.version || null,
        loader: context?.loader || null
    });
    return { success, ...discordPresence.status(), configError: discordPresenceRemoteConfigError };
});


function getSocialGameState() {
    const statePath = path.join(app.getPath("userData"), "minecraft", "game", "flintfix-social-state.json");
    try {
        const stat = fs.statSync(statePath);
        if (Date.now() - stat.mtimeMs > 15000) return { state: "launcher", server: null };
        const parsed = JSON.parse(fs.readFileSync(statePath, "utf8"));
        const updatedAt = Number(parsed?.updatedAt || 0);
        if (!updatedAt || Date.now() - updatedAt > 15000) return { state: "launcher", server: null };
        return {
            state: parsed?.state === "in_game" ? "in_game" : "launcher",
            server: parsed?.state === "in_game" && typeof parsed?.server === "string" ? parsed.server.slice(0, 160) : null
        };
    } catch {
        return { state: "launcher", server: null };
    }
}

function getSocialNotificationPath() {
    return path.join(app.getPath("userData"), "minecraft", "game", "flintfix-social-notifications.json");
}

async function queueInGameSocialNotification(notification) {
    const filePath = getSocialNotificationPath();
    await fs.promises.mkdir(path.dirname(filePath), { recursive: true });
    let queue = [];
    try {
        const parsed = JSON.parse(await fs.promises.readFile(filePath, "utf8"));
        queue = Array.isArray(parsed) ? parsed : [];
    } catch {
        queue = [];
    }
    const id = Number(notification?.id || Date.now());
    const sender = String(notification?.sender || "FlintFix Friend").slice(0, 80);
    const message = String(notification?.message || "").slice(0, 300);
    if (!message) return;
    queue.push({ id, sender, message, createdAt: Date.now() });
    queue = queue.slice(-20);
    await fs.promises.writeFile(filePath, JSON.stringify(queue, null, 2), "utf8");
}

async function callSocialApi(pathname, action, extra = {}) {
    const baseUrl = requireFlintFixBackend();
    const session = await getSessionForDiscordLink();
    return requestEncryptedSocialJson(`${baseUrl}${pathname}`, session, action, extra);
}

// ========================================
// VERIFIED DISCORD ACCOUNT LINKING
// ========================================

ipcMain.handle("discord:link:status", async () => {
    try {
        const baseUrl = requireDiscordLinkBackend();
        const session = await getSessionForDiscordLink();
        return await requestSignedDiscordJson(`${baseUrl}/api/account/status`, {
            method: "POST",
            body: createEncryptedDiscordLinkProof(session, "status")
        });
    } catch (error) {
        return { success: false, linked: false, error: error.message };
    }
});

ipcMain.handle("discord:link:start", async () => {
    try {
        const baseUrl = requireDiscordLinkBackend();
        const session = await getSessionForDiscordLink();
        const result = await requestSignedDiscordJson(`${baseUrl}/api/link/challenge`, {
            method: "POST",
            body: createEncryptedDiscordLinkProof(session, "link")
        });
        return { success: true, ...result };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("discord:link:challengeStatus", async (_event, challengeId) => {
    try {
        const baseUrl = requireDiscordLinkBackend();
        if (typeof challengeId !== "string" || challengeId.length < 16 || challengeId.length > 200) {
            throw new Error("Invalid Discord link challenge.");
        }
        return await requestSignedDiscordJson(`${baseUrl}/api/link/challenge/${encodeURIComponent(challengeId)}`);
    } catch (error) {
        return { success: false, linked: false, error: error.message };
    }
});

ipcMain.handle("discord:link:unlink", async () => {
    try {
        const baseUrl = requireDiscordLinkBackend();
        const session = await getSessionForDiscordLink();
        return await requestSignedDiscordJson(`${baseUrl}/api/unlink`, {
            method: "POST",
            body: createEncryptedDiscordLinkProof(session, "unlink")
        });
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("discord:link:manage", async () => ({
    success: false,
    error: "FlintFix now uses one-time Discord link codes instead of a website."
}));

// ========================================
// FLINTFIX SOCIAL / CHAT
// ========================================

// The in-game Team Glow module reads friends' Minecraft UUIDs from this file.
let lastSocialFriendsJson = "";
async function writeSocialFriendsFile(friends) {
    const list = (Array.isArray(friends) ? friends : [])
        .map(friend => ({
            uuid: String(friend?.minecraft?.uuid || "").replace(/[^0-9a-fA-F]/g, "").slice(0, 32),
            name: String(friend?.minecraft?.username || friend?.minecraft?.name || "").slice(0, 32)
        }))
        .filter(friend => friend.uuid.length === 32);
    const json = JSON.stringify({ friends: list, updatedAt: Date.now() }, null, 2);
    const withoutTime = JSON.stringify(list);
    if (withoutTime === lastSocialFriendsJson) return;
    lastSocialFriendsJson = withoutTime;
    const filePath = path.join(app.getPath("userData"), "minecraft", "game", "flintfix-social-friends.json");
    await fs.promises.mkdir(path.dirname(filePath), { recursive: true });
    await fs.promises.writeFile(filePath, json, "utf8");
}

ipcMain.handle("social:sync", async (_event, options = {}) => {
    try {
        const game = getSocialGameState();
        const result = await callSocialApi("/api/social/sync", "social_sync", {
            state: game.state,
            server: options?.showServer === false ? null : game.server,
            lastMessageId: Number(options?.lastMessageId || 0)
        });
        if (result?.success && Array.isArray(result.friends)) {
            writeSocialFriendsFile(result.friends).catch(() => {});
        }
        return result;
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("social:search", async (_event, query) => {
    try {
        return await callSocialApi("/api/social/search", "social_search", { query: String(query || "").slice(0, 80) });
    } catch (error) {
        return { success: false, results: [], error: error.message };
    }
});

ipcMain.handle("social:friendRequest", async (_event, targetUuid) => {
    try {
        return await callSocialApi("/api/social/friend/request", "social_friend_request", { targetUuid });
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("social:friendRespond", async (_event, targetUuid, accept) => {
    try {
        return await callSocialApi("/api/social/friend/respond", "social_friend_respond", { targetUuid, accept: Boolean(accept) });
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("social:friendRemove", async (_event, targetUuid) => {
    try {
        return await callSocialApi("/api/social/friend/remove", "social_friend_remove", { targetUuid });
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("social:messages", async (_event, targetUuid, beforeId = 0) => {
    try {
        return await callSocialApi("/api/social/messages", "social_messages", { targetUuid, beforeId: Number(beforeId || 0) });
    } catch (error) {
        return { success: false, messages: [], error: error.message };
    }
});

ipcMain.handle("social:messageSend", async (_event, targetUuid, content) => {
    try {
        return await callSocialApi("/api/social/message/send", "social_message_send", {
            targetUuid,
            content: String(content || "").slice(0, 500)
        });
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("social:pushGameNotification", async (_event, notification = {}) => {
    try {
        await queueInGameSocialNotification(notification);
        return { success: true };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("social:gameState", async () => ({ success: true, ...getSocialGameState() }));

// ========================================
// LAUNCHER SETTINGS HELPERS
// ========================================

const ALLOWED_EXTERNAL_URL_PREFIXES = [
    "https://www.minecraft.net/en-us/msaprofile/mygames/editprofile",
    "https://account.live.com/names/manage",
    "https://account.live.com/password/change",
    "https://account.microsoft.com/security",
    "https://discord.gg/flintfix"
];

ipcMain.handle("app:openExternal", async (_event, requestedUrl) => {
    try {
        const url = String(requestedUrl || "");
        if (!ALLOWED_EXTERNAL_URL_PREFIXES.some(prefix => url.startsWith(prefix))) {
            throw new Error("That external page is not allowed by FlintFix.");
        }
        await shell.openExternal(url);
        return { success: true };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("minecraft:openFolder", async () => {
    try {
        const minecraftRoot = path.join(app.getPath("userData"), "minecraft");
        await fs.promises.mkdir(minecraftRoot, { recursive: true });
        const result = await shell.openPath(minecraftRoot);
        if (result) throw new Error(result);
        return { success: true, path: minecraftRoot };
    } catch (error) {
        return { success: false, error: error.message };
    }
});


ipcMain.handle("instance:openFolder", async (_event, instanceId) => {
    try {
        const instanceRoot = getInstanceRoot(instanceId);
        await fs.promises.mkdir(instanceRoot, { recursive: true });
        const result = await shell.openPath(instanceRoot);
        if (result) throw new Error(result);
        return { success: true, path: instanceRoot };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("instance:deleteData", async (_event, instanceId) => {
    try {
        const instanceRoot = getInstanceRoot(instanceId);
        await fs.promises.rm(instanceRoot, { recursive: true, force: true });
        return { success: true };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("mods:list", async (_event, instanceId) => {
    try {
        const result = await listInstanceMods(instanceId);
        return { success: true, ...result };
    } catch (error) {
        return { success: false, mods: [], error: error.message };
    }
});

ipcMain.handle("mods:add", async (event, instanceId) => {
    try {
        normalizeInstanceId(instanceId);
        const owner = BrowserWindow.fromWebContents(event.sender);
        const picked = await dialog.showOpenDialog(owner || undefined, {
            title: "Add Minecraft mods",
            properties: ["openFile", "multiSelections"],
            filters: [{ name: "Minecraft mods", extensions: ["jar"] }]
        });
        if (picked.canceled || !picked.filePaths?.length) {
            return { success: true, canceled: true, added: [] };
        }
        const added = [];
        for (const filePath of picked.filePaths) {
            added.push(await copyModIntoInstance(instanceId, filePath));
        }
        return { success: true, canceled: false, added };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("mods:catalog:search", async (_event, options) => {
    try {
        const result = await searchCatalogMods(options || {});
        return { success: true, ...result };
    } catch (error) {
        return { success: false, mods: [], totalHits: 0, offset: 0, limit: 12, error: error.message };
    }
});

ipcMain.handle("mods:catalog:details", async (_event, projectId, options) => {
    try {
        const details = await getCatalogModDetails(projectId, options || {});
        return { success: true, details };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("mods:updates", async (_event, instanceId, options) => {
    try {
        const updates = await checkInstanceModUpdates(instanceId, options || {});
        return { success: true, updates };
    } catch (error) {
        return { success: false, updates: [], error: error.message };
    }
});

ipcMain.handle("mods:update", async (_event, instanceId, fileName, options) => {
    try {
        const result = await applyInstanceModUpdate(instanceId, fileName, options || {});
        return { success: true, ...result };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("mods:catalog:install", async (_event, instanceId, options) => {
    try {
        const result = await installCatalogMod(instanceId, options || {});
        return { success: true, ...result };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("mods:toggle", async (_event, instanceId, fileName, enabled) => {
    try {
        if (!isSafeModFileName(fileName)) throw new Error("Invalid mod file name.");
        const modsDir = getInstanceModsDir(instanceId);
        const source = path.join(modsDir, fileName);
        if (!fs.existsSync(source)) throw new Error("Mod file was not found.");
        let targetName = fileName;
        if (enabled && fileName.toLowerCase().endsWith(".disabled")) {
            targetName = fileName.slice(0, -".disabled".length);
        } else if (!enabled && !fileName.toLowerCase().endsWith(".disabled")) {
            targetName = `${fileName}.disabled`;
        }
        if (targetName !== fileName) {
            const target = path.join(modsDir, targetName);
            if (fs.existsSync(target)) throw new Error("A mod with that name already exists.");
            await fs.promises.rename(source, target);
        }
        return { success: true, fileName: targetName, enabled: Boolean(enabled) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("mods:remove", async (_event, instanceId, fileName) => {
    try {
        if (!isSafeModFileName(fileName)) throw new Error("Invalid mod file name.");
        const filePath = path.join(getInstanceModsDir(instanceId), fileName);
        await fs.promises.rm(filePath, { force: true });
        return { success: true };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("mods:openFolder", async (_event, instanceId) => {
    try {
        const modsDir = getInstanceModsDir(instanceId);
        await fs.promises.mkdir(modsDir, { recursive: true });
        const result = await shell.openPath(modsDir);
        if (result) throw new Error(result);
        return { success: true, path: modsDir };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("packs:search", async (_event, options) => {
    try {
        return { success: true, ...(await resourcePacks.search(options || {})) };
    } catch (error) {
        return { success: false, packs: [], totalHits: 0, error: error.message };
    }
});

ipcMain.handle("packs:details", async (_event, projectId, options) => {
    try {
        return { success: true, details: await resourcePacks.details(projectId, options || {}) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("packs:list", async () => {
    try {
        return { success: true, ...(await resourcePacks.list()) };
    } catch (error) {
        return { success: false, packs: [], error: error.message };
    }
});

ipcMain.handle("packs:install", async (event, options) => {
    try {
        const result = await resourcePacks.install(options || {}, progress => {
            if (!event.sender.isDestroyed()) event.sender.send("packs:progress", progress);
        });
        return { success: true, ...result };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("packs:setEnabled", async (_event, fileName, enabled) => {
    try {
        return { success: true, ...(await resourcePacks.setPackEnabled(fileName, Boolean(enabled))) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("packs:remove", async (_event, fileName) => {
    try {
        return { success: true, ...(await resourcePacks.remove(fileName)) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("packs:openFolder", async () => {
    try {
        const dir = resourcePacks.packsDir();
        await fs.promises.mkdir(dir, { recursive: true });
        const result = await shell.openPath(dir);
        if (result) throw new Error(result);
        return { success: true, path: dir };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("servers:ping", async (_event, address) => {
    try {
        return { success: true, ...(await serverPing.ping(address)) };
    } catch (error) {
        return { success: false, online: false, error: error.message };
    }
});

ipcMain.handle("skins:list", async () => {
    try {
        return { success: true, skins: await skins.list() };
    } catch (error) {
        return { success: false, skins: [], error: error.message };
    }
});

ipcMain.handle("skins:import", async (event, variant) => {
    try {
        const owner = BrowserWindow.fromWebContents(event.sender);
        const picked = await dialog.showOpenDialog(owner || undefined, {
            title: "Add Minecraft skins",
            properties: ["openFile", "multiSelections"],
            filters: [{ name: "Minecraft skins", extensions: ["png"] }]
        });
        if (picked.canceled || !picked.filePaths?.length) return { success: true, canceled: true, added: [] };
        const added = [];
        for (const filePath of picked.filePaths) added.push(await skins.importFile(filePath, variant));
        return { success: true, canceled: false, added };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("skins:current", async () => {
    try {
        return { success: true, ...(await skins.current()) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("skins:saveCurrent", async () => {
    try {
        const current = await skins.current();
        if (!current.dataUrl) throw new Error("This account uses a default skin.");
        const bytes = Buffer.from(current.dataUrl.split(",")[1], "base64");
        return { success: true, skin: await skins.add(bytes, { name: `${current.name || "My"} skin`, variant: current.variant }) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("skins:apply", async (_event, id) => {
    try {
        return { success: true, skin: await skins.apply(id) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("skins:update", async (_event, id, changes) => {
    try {
        return { success: true, skin: await skins.update(id, changes || {}) };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("skins:delete", async (_event, id) => {
    try {
        await skins.remove(id);
        return { success: true };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("skins:reset", async () => {
    try {
        await skins.reset();
        return { success: true };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("news:get", async () => {
    const local = () => {
        try {
            return JSON.parse(fs.readFileSync(path.join(__dirname, "news.json"), "utf8"));
        } catch {
            return { posts: [] };
        }
    };
    try {
        // news.json sits in the project folder inside the repository.
        const url = `https://raw.githubusercontent.com/${UPDATE_REPO}/main/${encodeURIComponent(path.basename(__dirname))}/news.json`;
        const response = await fetch(url, { headers: { "User-Agent": "FlintFix-Client" } });
        if (!response.ok) throw new Error(String(response.status));
        const remote = await response.json();
        if (!Array.isArray(remote?.posts)) throw new Error("Invalid news file.");
        return { success: true, source: "online", posts: remote.posts.slice(0, 20) };
    } catch {
        return { success: true, source: "bundled", posts: (local().posts || []).slice(0, 20) };
    }
});

ipcMain.handle("app:version", () => app.getVersion());

ipcMain.handle("update:check", async () => {
    try {
        return { success: true, ...(await updater.check()), canInstall: updater.canInstall() };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

ipcMain.handle("update:install", async () => {
    updater.install();
    return { success: true };
});

ipcMain.handle("mods:performance", async (event, instanceId, options = {}) => {
    try {
        const { mods } = await listInstanceMods(instanceId);
        const results = [];
        for (const mod of PERFORMANCE_MODS) {
            if (mods.some(installed => mod.match.test(installed.fileName))) {
                results.push({ title: mod.title, status: "already" });
                continue;
            }
            if (!event.sender.isDestroyed()) event.sender.send("mods:performance:progress", { title: mod.title });
            try {
                await installCatalogMod(instanceId, { projectId: mod.slug, version: options.version, loader: "fabric", strict: true });
                results.push({ title: mod.title, status: "installed" });
            } catch (error) {
                results.push({ title: mod.title, status: "failed", error: error.message });
            }
        }
        return { success: true, results };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

function runPowerShellCommand(command) {
    return new Promise((resolve, reject) => {
        execFile(
            "powershell.exe",
            ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command],
            { windowsHide: true, maxBuffer: 4 * 1024 * 1024 },
            (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(String(stderr || stdout || error.message).trim()));
                    return;
                }
                resolve(String(stdout || "").trim());
            }
        );
    });
}

function psQuote(value) {
    return `'${String(value).replace(/'/g, "''")}'`;
}

ipcMain.handle("minecraft:getFlintIcon", async (_event, versionId) => {
    try {
        if (typeof versionId !== "string" || !/^[A-Za-z0-9._+-]+$/.test(versionId)) {
            throw new Error("Invalid Minecraft version ID.");
        }

        const minecraftRoot = path.join(app.getPath("userData"), "minecraft");
        const clientJar = path.join(minecraftRoot, "versions", versionId, `${versionId}.jar`);
        if (!fs.existsSync(clientJar)) {
            return { success: false, error: "Minecraft client JAR is not installed yet." };
        }

        const cacheDir = path.join(app.getPath("userData"), "cache");
        fs.mkdirSync(cacheDir, { recursive: true });
        const outputFile = path.join(cacheDir, `minecraft-flint-${versionId}.png`);

        if (!fs.existsSync(outputFile)) {
            const command = [
                "Add-Type -AssemblyName System.IO.Compression.FileSystem;",
                `$zip=[System.IO.Compression.ZipFile]::OpenRead(${psQuote(clientJar)});`,
                "$entry=$zip.GetEntry('assets/minecraft/textures/item/flint.png');",
                "if ($null -eq $entry) { $zip.Dispose(); throw 'Flint texture was not found in the Minecraft client JAR.' };",
                `$stream=$entry.Open(); $file=[System.IO.File]::Create(${psQuote(outputFile)});`,
                "$stream.CopyTo($file); $file.Dispose(); $stream.Dispose(); $zip.Dispose();"
            ].join(" ");
            await runPowerShellCommand(command);
        }

        const bytes = fs.readFileSync(outputFile);

        // Use the real Minecraft flint texture as the native FlintFix window/taskbar icon
        // as soon as an installed Minecraft client JAR is available.
        const senderWindow = BrowserWindow.fromWebContents(_event.sender);
        if (senderWindow && !senderWindow.isDestroyed()) {
            try {
                const flintIcon = nativeImage.createFromPath(outputFile);
                if (!flintIcon.isEmpty()) senderWindow.setIcon(flintIcon);
            } catch {
                // Keep the bundled fallback icon if Windows rejects a runtime icon update.
            }
        }

        return { success: true, dataUrl: `data:image/png;base64,${bytes.toString("base64")}` };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

// ========================================
// CUSTOM WINDOW CONTROLS
// ========================================

function getSenderWindow(event) {
    return BrowserWindow.fromWebContents(event.sender);
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function easeInOutQuint(t) {
    return t < 0.5
        ? 16 * Math.pow(t, 5)
        : 1 - Math.pow(-2 * t + 2, 5) / 2;
}

function easeOutQuint(t) {
    return 1 - Math.pow(1 - t, 5);
}

function lerp(start, end, t) {
    return Math.round(start + (end - start) * t);
}

async function animateWindowOpacity(win, from, to, duration = 190) {
    if (!win || win.isDestroyed()) return;
    const frameMs = 10;
    const steps = Math.max(12, Math.round(duration / frameMs));
    for (let i = 0; i <= steps; i += 1) {
        if (win.isDestroyed()) return;
        const t = easeOutQuint(i / steps);
        const value = from + (to - from) * t;
        try { win.setOpacity(Math.max(0.02, Math.min(1, value))); } catch { return; }
        await delay(frameMs);
    }
}

async function animateWindowBounds(win, from, to, duration = 300, opacityFrom = null, opacityTo = null) {
    if (!win || win.isDestroyed()) return;
    const frameMs = 10;
    const steps = Math.max(18, Math.round(duration / frameMs));
    for (let i = 0; i <= steps; i += 1) {
        if (win.isDestroyed()) return;
        const t = easeInOutQuint(i / steps);
        win.setBounds({
            x: lerp(from.x, to.x, t),
            y: lerp(from.y, to.y, t),
            width: lerp(from.width, to.width, t),
            height: lerp(from.height, to.height, t)
        });
        if (opacityFrom !== null && opacityTo !== null) {
            try {
                const opacityT = easeOutQuint(i / steps);
                win.setOpacity(Math.max(0.02, Math.min(1, opacityFrom + (opacityTo - opacityFrom) * opacityT)));
            } catch {}
        }
        await delay(frameMs);
    }
}

function markWindowTransition(win, active) {
    if (!win || win.isDestroyed()) return;
    win.__flintfixWindowTransition = Boolean(active);
}

ipcMain.handle("window:minimize", async event => {
    const win = getSenderWindow(event);
    if (!win || win.isDestroyed()) return { success: false };
    if (win.__flintfixWindowTransition) return { success: false, busy: true };

    markWindowTransition(win, true);
    try {
        // Animate the actual native window, not only the titlebar icon.
        await animateWindowOpacity(win, 1, 0.06, 210);
        if (!win.isDestroyed()) win.minimize();
        return { success: true };
    } finally {
        if (!win.isDestroyed()) {
            try { win.setOpacity(1); } catch {}
            markWindowTransition(win, false);
        }
    }
});

ipcMain.handle("window:toggleMaximize", async event => {
    const win = getSenderWindow(event);
    if (!win || win.isDestroyed()) return { success: false, maximized: false };
    if (win.__flintfixWindowTransition) {
        return { success: false, busy: true, maximized: win.isMaximized() };
    }

    markWindowTransition(win, true);
    try {
        if (!win.isMaximized()) {
            const start = win.getBounds();
            win.__flintfixRestoreBounds = { ...start };
            const display = screen.getDisplayMatching(start);
            const target = { ...display.workArea };

            await animateWindowBounds(win, start, target, 300);
            if (!win.isDestroyed()) win.maximize();
            return { success: true, maximized: true };
        }

        const start = win.getBounds();
        const normal = win.__flintfixRestoreBounds || win.getNormalBounds();

        // Prevent the native unmaximize jump from flashing on screen. We briefly hide
        // the window, put it back at the maximized bounds, then animate to restore size.
        try { win.setOpacity(0.08); } catch {}
        win.unmaximize();
        await delay(12);
        if (win.isDestroyed()) return { success: false, maximized: false };
        win.setBounds(start);
        await animateWindowBounds(win, start, normal, 300, 0.08, 1);
        return { success: true, maximized: false };
    } finally {
        markWindowTransition(win, false);
    }
});

ipcMain.handle("window:state", event => {
    const win = getSenderWindow(event);
    if (!win || win.isDestroyed()) return { success: false, maximized: false };
    return { success: true, maximized: win.isMaximized() };
});

ipcMain.handle("window:close", async event => {
    const win = getSenderWindow(event);
    if (!win || win.isDestroyed()) return { success: false };
    if (win.__flintfixWindowTransition) return { success: false, busy: true };

    markWindowTransition(win, true);
    try {
        await animateWindowOpacity(win, 1, 0.05, 190);
        if (!win.isDestroyed()) win.close();
        return { success: true };
    } finally {
        if (!win.isDestroyed()) markWindowTransition(win, false);
    }
});

// ========================================
// Electron
// ========================================

app.whenReady().then(async () => {
    await microsoftAuth.initialize(app.getPath("userData"));
    createWindow();

    app.on("activate", () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});


app.on("before-quit", () => {
    void discordPresence.destroy();
});

app.on("window-all-closed", () => {
    if (process.platform !== "darwin") {
        app.quit();
    }
});
