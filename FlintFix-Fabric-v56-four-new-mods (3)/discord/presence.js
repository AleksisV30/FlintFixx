class FlintFixDiscordPresence {
    constructor({ clientId = "", largeImageKey = "", discordInvite = "https://discord.gg/flintfix" } = {}) {
        this.clientId = String(clientId || "").trim();
        this.largeImageKey = String(largeImageKey || "").trim();
        this.discordInvite = String(discordInvite || "https://discord.gg/flintfix").trim();
        this.client = null;
        this.ready = false;
        this.connecting = null;
        this.enabled = false;
        this.launcherStartedAt = new Date();
        this.gameStartedAt = null;
        this.lastError = null;
        this.configSource = this.isConfigured() ? "local" : "none";
    }

    isConfigured() {
        return /^\d{15,24}$/.test(this.clientId);
    }

    async configure({ clientId = null, largeImageKey = null, discordInvite = null, source = null } = {}) {
        const nextClientId = clientId == null ? this.clientId : String(clientId || "").trim();
        const changedClientId = nextClientId !== this.clientId;

        if (changedClientId && this.client) {
            await this.destroy();
        }

        this.clientId = nextClientId;
        if (largeImageKey != null) this.largeImageKey = String(largeImageKey || "").trim();
        if (discordInvite != null) this.discordInvite = String(discordInvite || "https://discord.gg/flintfix").trim();
        if (source) this.configSource = source;
        else if (this.isConfigured() && this.configSource === "none") this.configSource = "local";

        return this.status();
    }

    async connect() {
        if (!this.enabled || !this.isConfigured()) return false;
        if (this.ready && this.client?.user) return true;
        if (this.connecting) return this.connecting;

        this.connecting = (async () => {
            try {
                const { Client } = await import("@xhayper/discord-rpc");
                const client = new Client({ clientId: this.clientId });
                this.client = client;
                this.lastError = null;

                await new Promise((resolve, reject) => {
                    let settled = false;
                    const finish = (fn, value) => {
                        if (settled) return;
                        settled = true;
                        clearTimeout(timer);
                        fn(value);
                    };
                    const timer = setTimeout(
                        () => finish(reject, new Error("Discord desktop client was not detected.")),
                        9000
                    );

                    client.once("ready", () => {
                        this.ready = true;
                        finish(resolve, true);
                    });
                    client.once("error", error => finish(reject, error));

                    Promise.resolve(client.login()).catch(error => finish(reject, error));
                });

                return true;
            } catch (error) {
                this.lastError = error?.message || "Discord Rich Presence could not connect.";
                this.ready = false;
                try { await this.client?.destroy?.(); } catch {}
                this.client = null;
                throw error;
            } finally {
                this.connecting = null;
            }
        })();

        return this.connecting;
    }

    buildBaseActivity(startTimestamp) {
        const activity = {
            startTimestamp: startTimestamp || this.launcherStartedAt,
            buttons: []
        };

        if (this.discordInvite) {
            activity.buttons.push({ label: "Join FlintFix Discord", url: this.discordInvite });
        }

        if (!activity.buttons.length) delete activity.buttons;

        if (this.largeImageKey) {
            activity.largeImageKey = this.largeImageKey;
            activity.largeImageText = "FlintFix Client";
        }

        return activity;
    }

    async setEnabled(enabled) {
        this.enabled = Boolean(enabled);
        if (!this.enabled) {
            await this.destroy();
            return { success: true, enabled: false, configured: this.isConfigured(), connected: false, configSource: this.configSource };
        }

        if (!this.isConfigured()) {
            return {
                success: false,
                enabled: true,
                configured: false,
                connected: false,
                configSource: this.configSource,
                error: "Discord Rich Presence is not configured yet."
            };
        }

        try {
            const connected = await this.connect();
            return { success: connected, enabled: true, configured: true, connected, configSource: this.configSource };
        } catch (error) {
            return {
                success: false,
                enabled: true,
                configured: true,
                connected: false,
                configSource: this.configSource,
                error: error?.message || "Discord Rich Presence could not connect."
            };
        }
    }

    async setLauncher({ username = null, version = null, loader = null } = {}) {
        if (!this.enabled) return false;
        try {
            if (!(await this.connect()) || !this.client?.user) return false;
            this.gameStartedAt = null;
            await this.client.user.setActivity({
                ...this.buildBaseActivity(this.launcherStartedAt),
                details: "FlintFix Client",
                state: username
                    ? `${username} • Ready to play`
                    : version
                        ? `Preparing Minecraft ${version}`
                        : "Browsing the launcher"
            });
            return true;
        } catch (error) {
            this.lastError = error?.message || "Could not update Discord Rich Presence.";
            return false;
        }
    }

    async setPlaying({ version = null, loader = "Vanilla", username = null } = {}) {
        if (!this.enabled) return false;
        try {
            if (!(await this.connect()) || !this.client?.user) return false;
            this.gameStartedAt = new Date();
            await this.client.user.setActivity({
                ...this.buildBaseActivity(this.gameStartedAt),
                details: version ? `Playing Minecraft ${version}` : "Playing Minecraft",
                state: [loader, username].filter(Boolean).join(" • ") || "FlintFix Client"
            });
            return true;
        } catch (error) {
            this.lastError = error?.message || "Could not update Discord Rich Presence.";
            return false;
        }
    }

    async clear() {
        try {
            await this.client?.user?.clearActivity?.();
        } catch {}
    }

    async destroy() {
        await this.clear();
        try { await this.client?.destroy?.(); } catch {}
        this.client = null;
        this.ready = false;
        this.connecting = null;
        this.gameStartedAt = null;
    }

    status() {
        return {
            enabled: this.enabled,
            configured: this.isConfigured(),
            connected: Boolean(this.ready && this.client?.user),
            clientId: this.isConfigured() ? this.clientId : null,
            configSource: this.configSource,
            lastError: this.lastError
        };
    }
}

module.exports = { FlintFixDiscordPresence };
