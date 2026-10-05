// Resource pack catalog (Modrinth) and management of the shared game's
// resourcepacks folder. Packs are installed into <root>/game/resourcepacks,
// the same game directory every FlintFix launch uses.

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { Readable } = require("stream");
const { pipeline } = require("stream/promises");

const API = "https://api.modrinth.com/v2";
const USER_AGENT = "FlintFix-Client/56 (resource packs)";
const MANIFEST_NAME = ".flintfix-packs.json";
const SORTS = ["relevance", "downloads", "follows", "newest", "updated"];

function createResourcePackManager({ getGameDir }) {
    const packsDir = () => path.join(getGameDir(), "resourcepacks");
    const optionsPath = () => path.join(getGameDir(), "options.txt");
    const manifestPath = () => path.join(packsDir(), MANIFEST_NAME);

    async function fetchJson(url) {
        const response = await fetch(url, { headers: { "User-Agent": USER_AGENT, "Accept": "application/json" } });
        if (!response.ok) throw new Error(`Modrinth request failed (${response.status}).`);
        return response.json();
    }

    function isSafePackName(name) {
        const value = String(name || "");
        return value === path.basename(value) && !value.startsWith(".") && !value.includes("..")
            && value.length > 0 && value.length <= 240;
    }

    function uniqueFileName(dir, original) {
        const base = path.basename(String(original || "resource-pack.zip")).replace(/[<>:"|?*\u0000-\u001f]/g, "_");
        const named = /\.zip$/i.test(base) ? base : `${base}.zip`;
        const ext = path.extname(named);
        const stem = path.basename(named, ext);
        let candidate = named;
        let index = 2;
        while (fs.existsSync(path.join(dir, candidate))) {
            candidate = `${stem} (${index})${ext}`;
            index += 1;
        }
        return candidate;
    }

    async function readManifest() {
        try {
            const parsed = JSON.parse(await fs.promises.readFile(manifestPath(), "utf8"));
            return parsed && typeof parsed.packs === "object" && parsed.packs ? parsed : { packs: {} };
        } catch {
            return { packs: {} };
        }
    }

    async function writeManifest(manifest) {
        await fs.promises.mkdir(packsDir(), { recursive: true });
        await fs.promises.writeFile(manifestPath(), JSON.stringify(manifest, null, 2), "utf8");
    }

    // ------------------------------------------------------------------
    // options.txt: the resourcePacks line decides which packs are active.
    // ------------------------------------------------------------------

    async function readOptionsLines() {
        try {
            return (await fs.promises.readFile(optionsPath(), "utf8")).split(/\r?\n/);
        } catch {
            return [];
        }
    }

    function parsePackList(lines, key) {
        const line = lines.find(item => item.startsWith(`${key}:`));
        if (!line) return null;
        try {
            const parsed = JSON.parse(line.slice(key.length + 1));
            return Array.isArray(parsed) ? parsed.map(String) : [];
        } catch {
            return [];
        }
    }

    function writeListLine(lines, key, list) {
        const line = `${key}:${JSON.stringify(list)}`;
        const index = lines.findIndex(item => item.startsWith(`${key}:`));
        if (index >= 0) {
            lines[index] = line;
        } else {
            const end = lines.length && lines[lines.length - 1] === "" ? lines.length - 1 : lines.length;
            lines.splice(end, 0, line);
        }
    }

    async function enabledPackIds() {
        return new Set(parsePackList(await readOptionsLines(), "resourcePacks") || []);
    }

    async function setPackEnabled(fileName, enabled) {
        if (!isSafePackName(fileName)) throw new Error("Invalid resource pack name.");
        if (!fs.existsSync(path.join(packsDir(), fileName))) throw new Error("Resource pack was not found.");
        const id = `file/${fileName}`;
        const lines = await readOptionsLines();
        let list = parsePackList(lines, "resourcePacks") || ["vanilla"];
        list = list.filter(item => item !== id);
        // The last entry has the highest priority, so new packs go on top.
        if (enabled) list.push(id);
        writeListLine(lines, "resourcePacks", list);
        // Minecraft skips an active pack made for another version unless it is
        // also listed here. It drops the entry again once the pack is compatible.
        const incompatible = (parsePackList(lines, "incompatibleResourcePacks") || []).filter(item => item !== id);
        if (enabled) incompatible.push(id);
        writeListLine(lines, "incompatibleResourcePacks", incompatible);
        await fs.promises.mkdir(getGameDir(), { recursive: true });
        await fs.promises.writeFile(optionsPath(), lines.join("\n"), "utf8");
        return { fileName, enabled: Boolean(enabled) };
    }

    // ------------------------------------------------------------------
    // Catalog
    // ------------------------------------------------------------------

    function packFromHit(hit) {
        const gallery = Array.isArray(hit.gallery) ? hit.gallery.filter(Boolean) : [];
        return {
            projectId: hit.project_id,
            slug: hit.slug || "",
            title: hit.title || hit.slug || "Resource pack",
            description: hit.description || "",
            author: hit.author || "",
            downloads: hit.downloads || 0,
            follows: hit.follows || 0,
            categories: Array.isArray(hit.display_categories) ? hit.display_categories : (hit.categories || []),
            iconUrl: hit.icon_url || "",
            previewUrl: hit.featured_gallery || gallery[0] || "",
            color: Number.isFinite(hit.color) ? hit.color : null,
            versions: Array.isArray(hit.versions) ? hit.versions : [],
            dateModified: hit.date_modified || "",
            pageUrl: `https://modrinth.com/resourcepack/${hit.slug || hit.project_id}`
        };
    }

    async function search(options = {}) {
        const query = String(options.query || "").trim().slice(0, 120);
        const version = String(options.version || "").trim();
        const category = String(options.category || "").trim().toLowerCase();
        const index = SORTS.includes(options.index) ? options.index : "downloads";
        const limit = Math.min(30, Math.max(6, Number(options.limit) || 12));
        const offset = Math.max(0, Number(options.offset) || 0);
        const facets = [["project_type:resourcepack"]];
        if (version) facets.push([`versions:${version}`]);
        if (category) facets.push([`categories:${category}`]);
        const params = new URLSearchParams({ query, index, limit: String(limit), offset: String(offset), facets: JSON.stringify(facets) });
        const data = await fetchJson(`${API}/search?${params}`);
        const packs = (Array.isArray(data?.hits) ? data.hits : []).map(packFromHit);
        return { packs, totalHits: Number(data?.total_hits) || packs.length, offset, limit };
    }

    async function details(projectId, options = {}) {
        const id = String(projectId || "").trim();
        if (!id) throw new Error("Missing project ID.");
        const project = await fetchJson(`${API}/project/${encodeURIComponent(id)}`);
        const version = String(options.version || "").trim();
        const versionsUrl = new URL(`${API}/project/${encodeURIComponent(id)}/version`);
        if (version) versionsUrl.searchParams.set("game_versions", JSON.stringify([version]));
        let versions = [];
        try {
            versions = await fetchJson(versionsUrl.toString());
        } catch {
            versions = [];
        }
        const gallery = (Array.isArray(project.gallery) ? project.gallery : [])
            .slice()
            .sort((a, b) => Number(Boolean(b.featured)) - Number(Boolean(a.featured)) || (a.ordering || 0) - (b.ordering || 0))
            .map(item => ({ url: item.url, rawUrl: item.raw_url || item.url, title: item.title || "", description: item.description || "" }))
            .filter(item => item.url);
        return {
            projectId: project.id,
            slug: project.slug || "",
            title: project.title || project.slug || project.id,
            description: project.description || "",
            body: project.body || "",
            iconUrl: project.icon_url || "",
            color: Number.isFinite(project.color) ? project.color : null,
            downloads: project.downloads || 0,
            followers: project.followers || 0,
            categories: [...(project.categories || []), ...(project.additional_categories || [])],
            gameVersions: Array.isArray(project.game_versions) ? project.game_versions : [],
            license: project.license?.name || project.license?.id || "Unknown",
            gallery,
            pageUrl: `https://modrinth.com/resourcepack/${project.slug || project.id}`,
            compatibleVersions: (Array.isArray(versions) ? versions : []).slice(0, 8).map(entry => ({
                id: entry.id,
                versionNumber: entry.version_number || entry.name,
                gameVersions: entry.game_versions || [],
                datePublished: entry.date_published
            }))
        };
    }

    // ------------------------------------------------------------------
    // Installed packs
    // ------------------------------------------------------------------

    async function list() {
        const dir = packsDir();
        await fs.promises.mkdir(dir, { recursive: true });
        const manifest = await readManifest();
        const enabled = await enabledPackIds();
        const entries = await fs.promises.readdir(dir, { withFileTypes: true });
        const packs = [];
        for (const entry of entries) {
            if (!isSafePackName(entry.name)) continue;
            const isZip = entry.isFile() && /\.zip$/i.test(entry.name);
            if (!isZip && !entry.isDirectory()) continue;
            const stat = await fs.promises.stat(path.join(dir, entry.name));
            const source = Object.entries(manifest.packs).find(([, info]) => info?.fileName === entry.name);
            packs.push({
                fileName: entry.name,
                title: source?.[1]?.title || entry.name.replace(/\.zip$/i, ""),
                projectId: source?.[0] || null,
                versionNumber: source?.[1]?.versionNumber || "",
                folder: entry.isDirectory(),
                size: stat.size,
                enabled: enabled.has(`file/${entry.name}`)
            });
        }
        packs.sort((a, b) => a.title.localeCompare(b.title, undefined, { sensitivity: "base" }));
        return { dir, packs };
    }

    async function install(options = {}, onProgress = () => {}) {
        const projectId = String(options.projectId || "").trim();
        if (!projectId) throw new Error("Missing project ID.");
        const version = String(options.version || "").trim();
        const versionsUrl = new URL(`${API}/project/${encodeURIComponent(projectId)}/version`);
        if (version) versionsUrl.searchParams.set("game_versions", JSON.stringify([version]));
        let versions = await fetchJson(versionsUrl.toString());
        let exactMatch = Array.isArray(versions) && versions.length > 0;
        if (!exactMatch) {
            versions = await fetchJson(`${API}/project/${encodeURIComponent(projectId)}/version`);
        }
        let chosen = null;
        let file = null;
        for (const entry of Array.isArray(versions) ? versions : []) {
            const files = Array.isArray(entry?.files) ? entry.files : [];
            const zip = files.find(item => item?.primary && /\.zip$/i.test(item.filename || ""))
                || files.find(item => /\.zip$/i.test(item?.filename || ""));
            if (zip?.url) {
                chosen = entry;
                file = zip;
                break;
            }
        }
        if (!file) throw new Error("This pack has no downloadable .zip file.");

        const dir = packsDir();
        await fs.promises.mkdir(dir, { recursive: true });
        const manifest = await readManifest();
        const previous = manifest.packs[projectId]?.fileName;
        if (previous && fs.existsSync(path.join(dir, previous))) {
            return { fileName: previous, alreadyInstalled: true, title: manifest.packs[projectId].title, exactMatch };
        }

        const fileName = uniqueFileName(dir, file.filename);
        const target = path.join(dir, fileName);
        const partial = `${target}.part`;
        const response = await fetch(file.url, { headers: { "User-Agent": USER_AGENT } });
        if (!response.ok || !response.body) throw new Error(`Download failed (${response.status}).`);
        const total = Number(response.headers.get("content-length")) || Number(file.size) || 0;
        const hash = crypto.createHash("sha1");
        let received = 0;
        let lastReport = 0;
        const body = Readable.fromWeb(response.body);
        body.on("data", chunk => {
            hash.update(chunk);
            received += chunk.length;
            const now = Date.now();
            if (now - lastReport > 120) {
                lastReport = now;
                onProgress({ projectId, received, total });
            }
        });
        try {
            await pipeline(body, fs.createWriteStream(partial));
            const expected = file.hashes?.sha1;
            if (expected && hash.digest("hex") !== expected) throw new Error("Downloaded file failed its checksum.");
            await fs.promises.rename(partial, target);
        } catch (error) {
            await fs.promises.rm(partial, { force: true });
            throw error;
        }
        onProgress({ projectId, received: total || received, total: total || received });

        manifest.packs[projectId] = {
            fileName,
            title: options.title || chosen?.name || fileName,
            versionNumber: chosen?.version_number || "",
            installedAt: new Date().toISOString()
        };
        await writeManifest(manifest);
        if (options.enable !== false) await setPackEnabled(fileName, true);
        return { fileName, title: manifest.packs[projectId].title, versionNumber: chosen?.version_number || "", exactMatch };
    }

    async function remove(fileName) {
        if (!isSafePackName(fileName)) throw new Error("Invalid resource pack name.");
        const target = path.join(packsDir(), fileName);
        try {
            await setPackEnabled(fileName, false);
        } catch {
            // Not listed in options.txt; nothing to disable.
        }
        await fs.promises.rm(target, { recursive: true, force: true });
        const manifest = await readManifest();
        for (const [id, info] of Object.entries(manifest.packs)) {
            if (info?.fileName === fileName) delete manifest.packs[id];
        }
        await writeManifest(manifest);
        return { fileName };
    }

    return { packsDir, search, details, list, install, remove, setPackEnabled };
}

module.exports = { createResourcePackManager };
