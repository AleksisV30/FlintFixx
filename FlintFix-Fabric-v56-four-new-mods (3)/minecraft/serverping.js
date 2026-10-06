// Minecraft Java "Server List Ping": asks a server for its MOTD, player count,
// version and icon over the normal game port, exactly like the in-game server
// list does. Resolves _minecraft._tcp SRV records like the game.

const net = require("net");
const dns = require("dns").promises;

const DEFAULT_PORT = 25565;
const TIMEOUT_MS = 5000;

function writeVarInt(value) {
    const bytes = [];
    let v = value >>> 0;
    do {
        let byte = v & 0x7f;
        v >>>= 7;
        if (v !== 0) byte |= 0x80;
        bytes.push(byte);
    } while (v !== 0);
    return Buffer.from(bytes);
}

function readVarInt(buffer, offset) {
    let value = 0;
    let position = 0;
    let index = offset;
    while (true) {
        if (index >= buffer.length) return null;
        const byte = buffer[index++];
        value |= (byte & 0x7f) << position;
        if ((byte & 0x80) === 0) break;
        position += 7;
        if (position >= 35) throw new Error("VarInt is too big.");
    }
    return { value, size: index - offset };
}

function packet(id, ...parts) {
    const body = Buffer.concat([writeVarInt(id), ...parts]);
    return Buffer.concat([writeVarInt(body.length), body]);
}

function mcString(text) {
    const bytes = Buffer.from(text, "utf8");
    return Buffer.concat([writeVarInt(bytes.length), bytes]);
}

/** Splits "host:port" (or "[ipv6]:port"); port is null when not given. */
function parseAddress(address) {
    const value = String(address || "").trim();
    if (!value || value.length > 255) throw new Error("Enter a server address.");
    const bracket = value.match(/^\[([0-9a-fA-F:]+)\](?::(\d{1,5}))?$/);
    if (bracket) return { host: bracket[1], port: bracket[2] ? Number(bracket[2]) : null };
    const colons = (value.match(/:/g) || []).length;
    if (colons > 1 && net.isIPv6(value)) return { host: value, port: null };
    const [host, portText] = colons === 1 ? value.split(":") : [value, null];
    if (!/^[A-Za-z0-9.-]+$/.test(host) || (portText !== null && !/^\d{1,5}$/.test(portText))) {
        throw new Error("That doesn't look like a server address.");
    }
    const port = portText === null ? null : Number(portText);
    if (port !== null && (port < 1 || port > 65535)) throw new Error("The port must be between 1 and 65535.");
    return { host, port };
}

async function resolveTarget(address) {
    const { host, port } = parseAddress(address);
    if (port) return { host, port };
    if (net.isIP(host)) return { host, port: DEFAULT_PORT };
    try {
        const records = await dns.resolveSrv(`_minecraft._tcp.${host}`);
        if (records.length) return { host: records[0].name, port: records[0].port };
    } catch {
        // No SRV record: use the address as is.
    }
    return { host, port: DEFAULT_PORT };
}

/** Turns a chat component (string or JSON object) into plain text. */
function chatToText(component) {
    if (component == null) return "";
    if (typeof component === "string") return component;
    if (Array.isArray(component)) return component.map(chatToText).join("");
    let text = String(component.text || component.translate || "");
    if (Array.isArray(component.extra)) text += component.extra.map(chatToText).join("");
    return text;
}

function stripFormatting(text) {
    return String(text || "").replace(/§[0-9a-fk-orx]/gi, "");
}

function ping(address, protocol = 767) {
    return new Promise((resolve, reject) => {
        resolveTarget(address).then(({ host, port }) => {
            const socket = net.createConnection({ host, port });
            let received = Buffer.alloc(0);
            let statusJson = null;
            let pingSentAt = 0;
            let finished = false;
            const done = (error, result) => {
                if (finished) return;
                finished = true;
                socket.destroy();
                if (error) reject(error);
                else resolve(result);
            };
            socket.setTimeout(TIMEOUT_MS, () => done(new Error("The server did not answer in time.")));
            socket.on("error", error => done(new Error(error.code === "ENOTFOUND" ? "Server not found." : "Could not connect.")));
            socket.on("connect", () => {
                const port16 = Buffer.alloc(2);
                port16.writeUInt16BE(port);
                socket.write(packet(0x00, writeVarInt(protocol), mcString(host), port16, writeVarInt(1)));
                socket.write(packet(0x00));
            });
            socket.on("data", chunk => {
                received = Buffer.concat([received, chunk]);
                while (true) {
                    const length = readVarInt(received, 0);
                    if (!length || received.length < length.size + length.value) return;
                    const body = received.subarray(length.size, length.size + length.value);
                    received = received.subarray(length.size + length.value);
                    const id = readVarInt(body, 0);
                    if (!id) return;
                    if (id.value === 0x00 && !statusJson) {
                        const strLength = readVarInt(body, id.size);
                        const json = body.subarray(id.size + strLength.size, id.size + strLength.size + strLength.value).toString("utf8");
                        try {
                            statusJson = JSON.parse(json);
                        } catch {
                            done(new Error("The server sent an unreadable answer."));
                            return;
                        }
                        const payload = Buffer.alloc(8);
                        payload.writeBigInt64BE(BigInt(Date.now()));
                        pingSentAt = Date.now();
                        socket.write(packet(0x01, payload));
                    } else if (id.value === 0x01 && statusJson) {
                        done(null, summarize(statusJson, Date.now() - pingSentAt));
                    }
                }
            });
        }).catch(reject);
    });
}

function summarize(status, latency) {
    const motd = stripFormatting(chatToText(status.description)).replace(/\s+\n/g, "\n").trim();
    const icon = typeof status.favicon === "string" && status.favicon.startsWith("data:image/png;base64,")
        ? status.favicon : "";
    return {
        online: true,
        motd: motd.slice(0, 300),
        players: Number(status.players?.online) || 0,
        maxPlayers: Number(status.players?.max) || 0,
        version: stripFormatting(String(status.version?.name || "")).slice(0, 60),
        protocol: Number(status.version?.protocol) || 0,
        latency: Math.max(0, latency),
        icon
    };
}

module.exports = { ping, parseAddress };
