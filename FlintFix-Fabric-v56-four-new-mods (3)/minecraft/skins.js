// Skin library and Minecraft skin changes. Saved skins live in
// <userData>/skins with an index.json; applying one uploads it to the signed-in
// Minecraft account through the official Minecraft services API.

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
const MAX_SKINS = 60;

function createSkinManager({ getUserDataDir, getAccessToken }) {
    const dir = () => path.join(getUserDataDir(), "skins");
    const indexPath = () => path.join(dir(), "index.json");

    async function readIndex() {
        try {
            const parsed = JSON.parse(await fs.promises.readFile(indexPath(), "utf8"));
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    async function writeIndex(entries) {
        await fs.promises.mkdir(dir(), { recursive: true });
        await fs.promises.writeFile(indexPath(), JSON.stringify(entries, null, 2), "utf8");
    }

    /** Width and height from a PNG header, or null when the bytes are not a PNG. */
    function pngSize(bytes) {
        const signature = "89504e470d0a1a0a";
        if (bytes.length < 24 || bytes.subarray(0, 8).toString("hex") !== signature) return null;
        return { width: bytes.readUInt32BE(16), height: bytes.readUInt32BE(20) };
    }

    function validateSkin(bytes) {
        const size = pngSize(bytes);
        if (!size) throw new Error("Skins must be PNG images.");
        if (size.width !== 64 || (size.height !== 64 && size.height !== 32)) {
            throw new Error(`Skins must be 64×64 (or old 64×32) pixels; this one is ${size.width}×${size.height}.`);
        }
        if (bytes.length > 1024 * 1024) throw new Error("That skin file is too large.");
    }

    function toDataUrl(bytes) {
        return `data:image/png;base64,${bytes.toString("base64")}`;
    }

    function safeId(id) {
        const value = String(id || "");
        if (!/^[a-f0-9]{16}$/.test(value)) throw new Error("Unknown skin.");
        return value;
    }

    async function list() {
        const entries = await readIndex();
        const result = [];
        for (const entry of entries) {
            try {
                const bytes = await fs.promises.readFile(path.join(dir(), `${safeId(entry.id)}.png`));
                result.push({ ...entry, dataUrl: toDataUrl(bytes) });
            } catch {
                // Missing file: drop it from the list.
            }
        }
        return result;
    }

    async function add(bytes, { name, variant }) {
        validateSkin(bytes);
        const entries = await readIndex();
        const hash = crypto.createHash("sha1").update(bytes).digest("hex").slice(0, 16);
        const existing = entries.find(entry => entry.id === hash);
        if (existing) return existing;
        if (entries.length >= MAX_SKINS) throw new Error(`The library holds up to ${MAX_SKINS} skins; delete one first.`);
        await fs.promises.mkdir(dir(), { recursive: true });
        await fs.promises.writeFile(path.join(dir(), `${hash}.png`), bytes);
        const entry = {
            id: hash,
            name: String(name || "Skin").replace(/\.png$/i, "").slice(0, 40) || "Skin",
            variant: variant === "slim" ? "slim" : "classic",
            addedAt: new Date().toISOString()
        };
        entries.unshift(entry);
        await writeIndex(entries);
        return entry;
    }

    async function importFile(filePath, variant) {
        const bytes = await fs.promises.readFile(filePath);
        return add(bytes, { name: path.basename(filePath), variant });
    }

    async function update(id, changes) {
        const entries = await readIndex();
        const entry = entries.find(item => item.id === safeId(id));
        if (!entry) throw new Error("Unknown skin.");
        if (changes.variant) entry.variant = changes.variant === "slim" ? "slim" : "classic";
        if (typeof changes.name === "string" && changes.name.trim()) entry.name = changes.name.trim().slice(0, 40);
        await writeIndex(entries);
        return entry;
    }

    async function remove(id) {
        const clean = safeId(id);
        const entries = (await readIndex()).filter(entry => entry.id !== clean);
        await writeIndex(entries);
        await fs.promises.rm(path.join(dir(), `${clean}.png`), { force: true });
    }

    async function authorizedFetch(url, options = {}) {
        const token = await getAccessToken();
        if (!token) throw new Error("Sign in with a Microsoft account to change your skin.");
        const response = await fetch(url, {
            ...options,
            headers: { ...(options.headers || {}), Authorization: `Bearer ${token}`, "User-Agent": "FlintFix-Client" }
        });
        if (response.status === 401) throw new Error("Your Minecraft session expired. Sign in again.");
        if (response.status === 429) throw new Error("Too many skin changes. Wait a minute and try again.");
        if (!response.ok) throw new Error(`Minecraft rejected the request (${response.status}).`);
        return response;
    }

    /** The account's active skin as a data URL, plus its model variant. */
    async function current() {
        const profile = await (await authorizedFetch(PROFILE_URL)).json();
        const active = (profile.skins || []).find(skin => skin.state === "ACTIVE") || (profile.skins || [])[0];
        if (!active?.url) return { dataUrl: "", variant: "classic", name: profile.name || "" };
        const url = String(active.url).replace(/^http:/, "https:");
        if (!/^https:\/\/textures\.minecraft\.net\//.test(url)) throw new Error("Unexpected skin location.");
        const bytes = Buffer.from(await (await fetch(url)).arrayBuffer());
        return {
            dataUrl: toDataUrl(bytes),
            variant: String(active.variant || "CLASSIC").toLowerCase() === "slim" ? "slim" : "classic",
            name: profile.name || ""
        };
    }

    async function apply(id) {
        const clean = safeId(id);
        const entry = (await readIndex()).find(item => item.id === clean);
        if (!entry) throw new Error("Unknown skin.");
        const bytes = await fs.promises.readFile(path.join(dir(), `${clean}.png`));
        const form = new FormData();
        form.append("variant", entry.variant === "slim" ? "slim" : "classic");
        form.append("file", new Blob([bytes], { type: "image/png" }), "skin.png");
        await authorizedFetch(`${PROFILE_URL}/skins`, { method: "POST", body: form });
        return entry;
    }

    async function reset() {
        await authorizedFetch(`${PROFILE_URL}/skins/active`, { method: "DELETE" });
    }

    return { list, add, importFile, update, remove, current, apply, reset, validateSkin };
}

module.exports = { createSkinManager };
