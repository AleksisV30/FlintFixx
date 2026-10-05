const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("flintfix", {
    getMinecraftVersions: () => ipcRenderer.invoke("minecraft:getVersions"),
    getMinecraftVersionInfo: metadataUrl => ipcRenderer.invoke("minecraft:getVersionInfo", metadataUrl),

    installMinecraft: versionId => ipcRenderer.invoke("minecraft:install", versionId),
    launchMinecraft: (versionId, javaExecutable, loader = "vanilla", launchOptions = {}) =>
        ipcRenderer.invoke("minecraft:launch", versionId, javaExecutable, loader, launchOptions),
    launchMinecraftDev: (versionId, javaExecutable, username = "FlintFixDev", loader = "vanilla") =>
        ipcRenderer.invoke("minecraft:launchDev", versionId, javaExecutable, username, loader),

    onMinecraftProgress: callback => {
        const listener = (_event, data) => callback(data);
        ipcRenderer.on("minecraft:progress", listener);
        return () => ipcRenderer.removeListener("minecraft:progress", listener);
    },

    onMinecraftLog: callback => {
        const listener = (_event, data) => callback(data);
        ipcRenderer.on("minecraft:log", listener);
        return () => ipcRenderer.removeListener("minecraft:log", listener);
    },

    detectJava: () => ipcRenderer.invoke("java:detect"),
    getManagedJava: majorVersion => ipcRenderer.invoke("java:getManaged", majorVersion),
    installJava: majorVersion => ipcRenderer.invoke("java:install", majorVersion),

    onJavaProgress: callback => {
        const listener = (_event, data) => callback(data);
        ipcRenderer.on("java:progress", listener);
        return () => ipcRenderer.removeListener("java:progress", listener);
    },

    getAuthStatus: () => ipcRenderer.invoke("auth:status"),
    signInMicrosoft: () => ipcRenderer.invoke("auth:signIn"),
    listMicrosoftAccounts: () => ipcRenderer.invoke("auth:listAccounts"),
    switchMicrosoftAccount: homeAccountId => ipcRenderer.invoke("auth:switchAccount", homeAccountId),
    signOutMicrosoft: () => ipcRenderer.invoke("auth:signOut"),

    openExternal: url => ipcRenderer.invoke("app:openExternal", url),
    openMinecraftFolder: () => ipcRenderer.invoke("minecraft:openFolder"),
    openInstanceFolder: instanceId => ipcRenderer.invoke("instance:openFolder", instanceId),
    deleteInstanceData: instanceId => ipcRenderer.invoke("instance:deleteData", instanceId),
    listInstanceMods: instanceId => ipcRenderer.invoke("mods:list", instanceId),
    addInstanceMods: instanceId => ipcRenderer.invoke("mods:add", instanceId),
    toggleInstanceMod: (instanceId, fileName, enabled) => ipcRenderer.invoke("mods:toggle", instanceId, fileName, enabled),
    removeInstanceMod: (instanceId, fileName) => ipcRenderer.invoke("mods:remove", instanceId, fileName),
    openInstanceModsFolder: instanceId => ipcRenderer.invoke("mods:openFolder", instanceId),
    searchCatalogMods: options => ipcRenderer.invoke("mods:catalog:search", options),
    getCatalogModDetails: (projectId, options) => ipcRenderer.invoke("mods:catalog:details", projectId, options),
    installCatalogMod: (instanceId, options) => ipcRenderer.invoke("mods:catalog:install", instanceId, options),
    checkInstanceModUpdates: (instanceId, options) => ipcRenderer.invoke("mods:updates", instanceId, options),
    updateInstanceMod: (instanceId, fileName, options) => ipcRenderer.invoke("mods:update", instanceId, fileName, options),
    getMinecraftFlintIcon: versionId => ipcRenderer.invoke("minecraft:getFlintIcon", versionId),

    getDiscordPresenceStatus: () => ipcRenderer.invoke("discord:presence:status"),
    setDiscordPresenceEnabled: (enabled, context = {}) => ipcRenderer.invoke("discord:presence:setEnabled", enabled, context),
    updateDiscordLauncherPresence: (context = {}) => ipcRenderer.invoke("discord:presence:updateLauncher", context),
    getDiscordLinkStatus: () => ipcRenderer.invoke("discord:link:status"),
    startDiscordLink: () => ipcRenderer.invoke("discord:link:start"),
    getDiscordLinkChallengeStatus: challengeId => ipcRenderer.invoke("discord:link:challengeStatus", challengeId),
    unlinkDiscordAccount: () => ipcRenderer.invoke("discord:link:unlink"),
    openDiscordLinkManager: () => ipcRenderer.invoke("discord:link:manage"),

    syncSocial: options => ipcRenderer.invoke("social:sync", options),
    searchSocialUsers: query => ipcRenderer.invoke("social:search", query),
    sendFriendRequest: targetUuid => ipcRenderer.invoke("social:friendRequest", targetUuid),
    respondFriendRequest: (targetUuid, accept) => ipcRenderer.invoke("social:friendRespond", targetUuid, accept),
    removeSocialFriend: targetUuid => ipcRenderer.invoke("social:friendRemove", targetUuid),
    getSocialMessages: (targetUuid, beforeId = 0) => ipcRenderer.invoke("social:messages", targetUuid, beforeId),
    sendSocialMessage: (targetUuid, content) => ipcRenderer.invoke("social:messageSend", targetUuid, content),
    pushGameSocialNotification: notification => ipcRenderer.invoke("social:pushGameNotification", notification),
    getSocialGameState: () => ipcRenderer.invoke("social:gameState"),

    minimizeWindow: () => ipcRenderer.invoke("window:minimize"),
    toggleMaximizeWindow: () => ipcRenderer.invoke("window:toggleMaximize"),
    getWindowState: () => ipcRenderer.invoke("window:state"),
    onWindowMaximizedChanged: callback => {
        const listener = (_event, maximized) => callback(Boolean(maximized));
        ipcRenderer.on("window:maximized-changed", listener);
        return () => ipcRenderer.removeListener("window:maximized-changed", listener);
    },
    closeWindow: () => ipcRenderer.invoke("window:close")
});
