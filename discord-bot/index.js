// FlintFix Client Discord bot.
// Slash commands: /download, /status, /versions, /report, /help.
// Also posts an announcement whenever a new GitHub release of the client is published.

const fs = require("node:fs");
const path = require("node:path");
const {
    Client,
    EmbedBuilder,
    Events,
    GatewayIntentBits,
    MessageFlags,
    REST,
    Routes,
    SlashCommandBuilder
} = require("discord.js");

// ---------------------------------------------------------------------------
// Configuration (.env next to this file)
// ---------------------------------------------------------------------------

const ENV_FILE = path.join(__dirname, ".env");
if (fs.existsSync(ENV_FILE)) process.loadEnvFile(ENV_FILE);

const config = {
    token: process.env.DISCORD_TOKEN,
    clientId: process.env.DISCORD_CLIENT_ID,
    guildId: process.env.DISCORD_GUILD_ID,
    announceChannelId: process.env.ANNOUNCE_CHANNEL_ID || "",
    reportsChannelId: process.env.REPORTS_CHANNEL_ID || process.env.ANNOUNCE_CHANNEL_ID || "",
    githubRepo: process.env.GITHUB_REPO || "AleksisV30/FlintFixx",
    githubToken: process.env.GITHUB_TOKEN || "",
    backendUrl: process.env.BACKEND_URL || "",
    websiteUrl: process.env.WEBSITE_URL || "https://github.com/AleksisV30/FlintFixx/releases"
};

for (const key of ["token", "clientId", "guildId"]) {
    if (!config[key]) {
        console.error(`Missing ${key} in .env (see .env.example).`);
        process.exit(1);
    }
}

const BRAND_COLOR = 0x8a72ff;
const RELEASE_CHECK_MINUTES = 10;
const REPORT_COOLDOWN_MS = 5 * 60 * 1000;
const STATE_FILE = path.join(__dirname, "state.json");

// Minecraft versions the client supports, and features that are not available on some of them yet.
const SUPPORTED_VERSIONS = [
    { range: "1.20.1 – 1.20.6", note: null },
    { range: "1.21 – 1.21.11", note: null },
    { range: "26.1 – 26.1.2", note: null },
    { range: "26.2", note: "In-world overlays (hitboxes, trajectory, waypoint beams, damage numbers, block outline) and the custom sky are not available yet." },
    { range: "26.3", note: "Same as 26.2, and Show Hand is not available yet." }
];

// ---------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------

function loadState() {
    try {
        return JSON.parse(fs.readFileSync(STATE_FILE, "utf8"));
    } catch {
        return {};
    }
}

function saveState(state) {
    fs.writeFileSync(STATE_FILE, JSON.stringify(state, null, 2));
}

async function fetchWithTimeout(url, options = {}, timeoutMs = 8000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
        return await fetch(url, { ...options, signal: controller.signal });
    } finally {
        clearTimeout(timer);
    }
}

/** Latest published GitHub release, or null when there is none (or GitHub is unreachable). */
async function getLatestRelease() {
    const headers = { "Accept": "application/vnd.github+json", "User-Agent": "FlintFix-Discord-Bot" };
    if (config.githubToken) headers.Authorization = `Bearer ${config.githubToken}`;
    const response = await fetchWithTimeout(`https://api.github.com/repos/${config.githubRepo}/releases/latest`, { headers });
    if (response.status === 404) return null;
    if (!response.ok) throw new Error(`GitHub returned HTTP ${response.status}`);
    const release = await response.json();
    const setup = (release.assets || []).find(asset => /\.exe$/i.test(asset.name));
    return {
        tag: release.tag_name,
        name: release.name || release.tag_name,
        url: release.html_url,
        body: release.body || "",
        publishedAt: release.published_at,
        setupName: setup?.name || null,
        setupUrl: setup?.browser_download_url || null,
        setupSize: setup?.size || 0
    };
}

/** True when the FlintFix backend answers at all within a few seconds. */
async function isBackendUp() {
    if (!config.backendUrl) return null;
    try {
        await fetchWithTimeout(config.backendUrl, {}, 5000);
        return true;
    } catch {
        return false;
    }
}

function formatSize(bytes) {
    if (!bytes) return "";
    return `${(bytes / (1024 * 1024)).toFixed(0)} MB`;
}

function trimText(text, max) {
    const clean = String(text || "").trim();
    return clean.length > max ? `${clean.slice(0, max - 1)}…` : clean;
}

function releaseEmbed(release, title) {
    const embed = new EmbedBuilder()
        .setColor(BRAND_COLOR)
        .setTitle(title)
        .setURL(release.url)
        .setDescription(trimText(release.body, 1500) || "A new version of FlintFix Client is out.")
        .addFields({ name: "Version", value: release.tag, inline: true });
    if (release.setupUrl) {
        embed.addFields({
            name: "Download",
            value: `[${release.setupName}](${release.setupUrl}) ${formatSize(release.setupSize)}`.trim(),
            inline: true
        });
    }
    if (release.publishedAt) embed.setTimestamp(new Date(release.publishedAt));
    return embed;
}

// ---------------------------------------------------------------------------
// Slash commands
// ---------------------------------------------------------------------------

const commands = [
    new SlashCommandBuilder().setName("download").setDescription("Get the latest FlintFix Client installer."),
    new SlashCommandBuilder().setName("status").setDescription("Check the FlintFix services and latest version."),
    new SlashCommandBuilder().setName("versions").setDescription("List the Minecraft versions FlintFix supports."),
    new SlashCommandBuilder()
        .setName("report")
        .setDescription("Report a bug in FlintFix Client.")
        .addStringOption(option => option
            .setName("problem")
            .setDescription("What went wrong?")
            .setRequired(true)
            .setMaxLength(1000))
        .addStringOption(option => option
            .setName("minecraft_version")
            .setDescription("Which Minecraft version were you playing? (e.g. 1.21.11)")
            .setMaxLength(20))
        .addStringOption(option => option
            .setName("steps")
            .setDescription("How can we make it happen again?")
            .setMaxLength(1000)),
    new SlashCommandBuilder().setName("help").setDescription("Show what this bot can do.")
].map(command => command.toJSON());

const handlers = {
    async download(interaction) {
        await interaction.deferReply();
        const release = await getLatestRelease().catch(() => null);
        if (!release) {
            await interaction.editReply(`No release is published yet. Check ${config.websiteUrl}`);
            return;
        }
        const embed = releaseEmbed(release, `Download FlintFix Client ${release.tag}`)
            .setDescription("Run the setup, then pick your Minecraft version in the launcher.\n"
                + "If Windows shows \"Windows protected your PC\", click **More info → Run anyway**.");
        await interaction.editReply({ embeds: [embed] });
    },

    async status(interaction) {
        await interaction.deferReply();
        const [release, backendUp] = await Promise.all([getLatestRelease().catch(() => undefined), isBackendUp()]);
        const latest = release === undefined ? "Could not reach GitHub" : (release ? release.tag : "No release yet");
        const backend = backendUp === null ? "Not configured" : (backendUp ? "🟢 Online" : "🔴 Offline");
        const embed = new EmbedBuilder()
            .setColor(backendUp === false ? 0xe5484d : BRAND_COLOR)
            .setTitle("FlintFix status")
            .addFields(
                { name: "Latest version", value: latest, inline: true },
                { name: "Friends & chat", value: backend, inline: true },
                { name: "Bot latency", value: `${Math.max(0, Math.round(interaction.client.ws.ping))} ms`, inline: true }
            )
            .setTimestamp();
        await interaction.editReply({ embeds: [embed] });
    },

    async versions(interaction) {
        const lines = SUPPORTED_VERSIONS.map(version => version.note
            ? `**${version.range}** — ${version.note}`
            : `**${version.range}** — everything works`);
        const embed = new EmbedBuilder()
            .setColor(BRAND_COLOR)
            .setTitle("Supported Minecraft versions")
            .setDescription(lines.join("\n"));
        await interaction.reply({ embeds: [embed] });
    },

    async report(interaction) {
        const state = loadState();
        state.reportCooldowns ??= {};
        const last = state.reportCooldowns[interaction.user.id] || 0;
        const waitMs = last + REPORT_COOLDOWN_MS - Date.now();
        if (waitMs > 0) {
            await interaction.reply({
                content: `Please wait ${Math.ceil(waitMs / 60000)} more minute(s) before sending another report.`,
                flags: MessageFlags.Ephemeral
            });
            return;
        }

        const channel = config.reportsChannelId
            ? await interaction.client.channels.fetch(config.reportsChannelId).catch(() => null)
            : null;
        if (!channel?.isTextBased()) {
            await interaction.reply({ content: "Bug reports are not set up on this server yet.", flags: MessageFlags.Ephemeral });
            return;
        }

        const embed = new EmbedBuilder()
            .setColor(0xf5a623)
            .setTitle("Bug report")
            .setAuthor({ name: interaction.user.tag, iconURL: interaction.user.displayAvatarURL() })
            .addFields({ name: "Problem", value: interaction.options.getString("problem", true) })
            .setFooter({ text: `User ID ${interaction.user.id}` })
            .setTimestamp();
        const version = interaction.options.getString("minecraft_version");
        const steps = interaction.options.getString("steps");
        if (version) embed.addFields({ name: "Minecraft version", value: version, inline: true });
        if (steps) embed.addFields({ name: "Steps", value: steps });

        // allowedMentions: none, so a report can't ping @everyone or roles.
        await channel.send({ embeds: [embed], allowedMentions: { parse: [] } });
        state.reportCooldowns[interaction.user.id] = Date.now();
        saveState(state);
        await interaction.reply({ content: "Thanks! Your report was sent to the FlintFix team.", flags: MessageFlags.Ephemeral });
    },

    async help(interaction) {
        const embed = new EmbedBuilder()
            .setColor(BRAND_COLOR)
            .setTitle("FlintFix bot")
            .setDescription([
                "`/download` — latest FlintFix Client installer",
                "`/status` — latest version and whether friends & chat are online",
                "`/versions` — supported Minecraft versions",
                "`/report` — send a bug report to the team"
            ].join("\n"));
        await interaction.reply({ embeds: [embed], flags: MessageFlags.Ephemeral });
    }
};

// ---------------------------------------------------------------------------
// Release announcements
// ---------------------------------------------------------------------------

async function checkForNewRelease(client) {
    if (!config.announceChannelId) return;
    let release;
    try {
        release = await getLatestRelease();
    } catch (error) {
        console.warn(`Release check failed: ${error.message}`);
        return;
    }
    if (!release) return;

    const state = loadState();
    if (!state.lastAnnouncedTag) {
        // First run: remember the current release without announcing an old one.
        state.lastAnnouncedTag = release.tag;
        saveState(state);
        return;
    }
    if (state.lastAnnouncedTag === release.tag) return;

    const channel = await client.channels.fetch(config.announceChannelId).catch(() => null);
    if (!channel?.isTextBased()) {
        console.warn("ANNOUNCE_CHANNEL_ID is not a text channel the bot can see.");
        return;
    }
    await channel.send({ embeds: [releaseEmbed(release, `FlintFix Client ${release.tag} is out!`)] });
    state.lastAnnouncedTag = release.tag;
    saveState(state);
    console.log(`Announced release ${release.tag}`);
}

// ---------------------------------------------------------------------------
// Startup
// ---------------------------------------------------------------------------

async function registerCommands() {
    const rest = new REST().setToken(config.token);
    await rest.put(Routes.applicationGuildCommands(config.clientId, config.guildId), { body: commands });
    console.log(`Registered ${commands.length} slash commands.`);
}

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

client.once(Events.ClientReady, async readyClient => {
    console.log(`Logged in as ${readyClient.user.tag}`);
    await checkForNewRelease(readyClient);
    setInterval(() => void checkForNewRelease(readyClient), RELEASE_CHECK_MINUTES * 60 * 1000);
});

client.on(Events.InteractionCreate, async interaction => {
    if (!interaction.isChatInputCommand()) return;
    const handler = handlers[interaction.commandName];
    if (!handler) return;
    try {
        await handler(interaction);
    } catch (error) {
        console.error(`/${interaction.commandName} failed:`, error);
        const reply = { content: "Something went wrong. Please try again in a moment.", flags: MessageFlags.Ephemeral };
        if (interaction.deferred || interaction.replied) await interaction.editReply(reply).catch(() => {});
        else await interaction.reply(reply).catch(() => {});
    }
});

registerCommands()
    .then(() => client.login(config.token))
    .catch(error => {
        console.error("Startup failed:", error);
        process.exit(1);
    });
