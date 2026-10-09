// FlintFix self-update. Packaged builds (electron-builder) download and install
// new releases through electron-updater; a copy started with "npm start" can't
// replace itself, so it only checks GitHub Releases and links to the new one.

const REPO = "AleksisV30/FlintFixx";

function compareVersions(a, b) {
    const parse = value => String(value || "").replace(/^v/i, "").split(/[.-]/).map(part => Number.parseInt(part, 10) || 0);
    const left = parse(a);
    const right = parse(b);
    for (let i = 0; i < Math.max(left.length, right.length); i++) {
        const diff = (left[i] || 0) - (right[i] || 0);
        if (diff !== 0) return diff;
    }
    return 0;
}

function createUpdater({ app, send }) {
    let autoUpdater = null;
    let state = { status: "idle", currentVersion: app.getVersion() };

    function setState(next) {
        state = { ...state, ...next, currentVersion: app.getVersion() };
        send(state);
    }

    if (app.isPackaged) {
        try {
            ({ autoUpdater } = require("electron-updater"));
            autoUpdater.autoDownload = true;
            autoUpdater.on("checking-for-update", () => setState({ status: "checking" }));
            autoUpdater.on("update-available", info => setState({ status: "downloading", version: info?.version, percent: 0 }));
            autoUpdater.on("update-not-available", () => setState({ status: "current" }));
            autoUpdater.on("download-progress", progress => setState({ status: "downloading", percent: Math.round(progress?.percent || 0) }));
            autoUpdater.on("update-downloaded", info => setState({ status: "ready", version: info?.version }));
            autoUpdater.on("error", error => {
                // Before the first GitHub release exists there is simply nothing to update to.
                const message = String(error?.message || error);
                setState(/no published versions/i.test(message) ? { status: "current" } : { status: "error", error: message });
            });
        } catch {
            autoUpdater = null;
        }
    }

    async function check() {
        if (autoUpdater) {
            try {
                await autoUpdater.checkForUpdates();
            } catch {
                // Reported through the "error" event above.
            }
            return state;
        }
        setState({ status: "checking" });
        try {
            const response = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
                headers: { "Accept": "application/vnd.github+json", "User-Agent": "FlintFix-Client" }
            });
            if (response.status === 404) {
                setState({ status: "current" });
                return state;
            }
            if (!response.ok) throw new Error(`GitHub answered ${response.status}.`);
            const release = await response.json();
            const latest = String(release.tag_name || release.name || "").replace(/^v/i, "");
            if (latest && compareVersions(latest, app.getVersion()) > 0) {
                setState({ status: "available", version: latest, url: release.html_url, notes: String(release.body || "").slice(0, 4000) });
            } else {
                setState({ status: "current" });
            }
        } catch (error) {
            setState({ status: "error", error: error.message });
        }
        return state;
    }

    function install() {
        if (autoUpdater && state.status === "ready") autoUpdater.quitAndInstall();
    }

    return { check, install, getState: () => state, canInstall: () => Boolean(autoUpdater) };
}

module.exports = { createUpdater, compareVersions, REPO };
