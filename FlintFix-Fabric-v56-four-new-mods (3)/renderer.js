const dropdown = document.getElementById("versionDropdown");
const trigger = document.getElementById("versionTrigger");
const triggerValue = document.getElementById("versionTriggerValue");
const searchInput = document.getElementById("versionSearch");
const versionList = document.getElementById("versionList");
const loaderDropdown = document.getElementById("loaderDropdown");
const loaderTrigger = document.getElementById("loaderTrigger");
const loaderTriggerValue = document.getElementById("loaderTriggerValue");
const loaderList = document.getElementById("loaderList");
const javaStatus = document.getElementById("javaStatus");
const javaPath = document.getElementById("javaPath");
const javaBadge = document.getElementById("javaBadge");
const javaInstallButton = document.getElementById("javaInstallButton");
const playButton = document.getElementById("playButton");
const playButtonText = document.getElementById("playButtonText");
const consoleElement = document.getElementById("console");
const profileDescription = document.getElementById("profileDescription");
const profileStatus = document.getElementById("profileStatus");
const launcherReady = document.getElementById("launcherReady");
const launcherReadyText = document.getElementById("launcherReadyText");
const loginButton = document.getElementById("loginButton");
const accountArea = document.getElementById("accountArea");
const accountButton = document.getElementById("accountButton");
const accountDropdown = document.getElementById("accountDropdown");
const changeAccountButton = document.getElementById("changeAccountButton");
const userSettingsButton = document.getElementById("userSettingsButton");
const signOutButton = document.getElementById("signOutButton");
const accountNameMini = document.getElementById("accountNameMini");
const sidebar = document.getElementById("sidebar");
const sidebarToggle = document.getElementById("sidebarToggle");
const brandHomeButton = document.getElementById("brandHomeButton");
const pageName = document.getElementById("pageName");
const pageIcon = document.getElementById("pageIcon");
const quickLaunchButton = document.getElementById("quickLaunchButton");
const playerSkin = document.getElementById("playerSkin");
const playerUsername = document.getElementById("playerUsername");
const playerMeta = document.getElementById("playerMeta");
const accountAvatar = document.getElementById("accountAvatar");
const dropdownAvatar = document.getElementById("dropdownAvatar");
const dropdownAccountName = document.getElementById("dropdownAccountName");
const dropdownAccountEmail = document.getElementById("dropdownAccountEmail");
const accountModalBackdrop = document.getElementById("accountModalBackdrop");
const accountModalClose = document.getElementById("accountModalClose");
const accountList = document.getElementById("accountList");
const addAccountButton = document.getElementById("addAccountButton");
const toastContainer = document.getElementById("toastContainer");
const consolePlaceholder = document.getElementById("consolePlaceholder");
const launchEta = document.getElementById("launchEta");
const windowMinimize = document.getElementById("windowMinimize");
const windowMaximize = document.getElementById("windowMaximize");
const windowClose = document.getElementById("windowClose");
const brandIcon = document.getElementById("brandIcon");
const settingsModalBackdrop = document.getElementById("settingsModalBackdrop");
const settingsModalClose = document.getElementById("settingsModalClose");
const settingsSearch = document.getElementById("settingsSearch");
const settingsModalTitle = document.getElementById("settingsModalTitle");
const settingsModalSubtitle = document.getElementById("settingsModalSubtitle");
const settingsSearchEmpty = document.getElementById("settingsSearchEmpty");
const settingsTabs = Array.from(document.querySelectorAll(".settings-tab"));
const settingsPanes = Array.from(document.querySelectorAll(".settings-pane"));
const resolutionWidth = document.getElementById("resolutionWidth");
const resolutionHeight = document.getElementById("resolutionHeight");
const launchFullscreen = document.getElementById("launchFullscreen");
const lockAspectRatio = document.getElementById("lockAspectRatio");
const launcherVisibilityRadios = Array.from(document.querySelectorAll('input[name="launcherVisibility"]'));
const settingsAccountAvatar = document.getElementById("settingsAccountAvatar");
const settingsAccountName = document.getElementById("settingsAccountName");
const settingsAccountState = document.getElementById("settingsAccountState");
const accountUsernameValue = document.getElementById("accountUsernameValue");
const accountEmailValue = document.getElementById("accountEmailValue");
const changeMinecraftUsername = document.getElementById("changeMinecraftUsername");
const changeAccountEmail = document.getElementById("changeAccountEmail");
const changeAccountPassword = document.getElementById("changeAccountPassword");
const manageTwoStep = document.getElementById("manageTwoStep");
const settingsLogOut = document.getElementById("settingsLogOut");
const discordRichPresence = document.getElementById("discordRichPresence");
const discordRichPresenceRow = document.getElementById("discordRichPresenceRow");
const discordPresenceStatus = document.getElementById("discordPresenceStatus");
const connectDiscord = document.getElementById("connectDiscord");
const discordLinkCard = document.getElementById("discordLinkCard");
const discordLinkAvatar = document.getElementById("discordLinkAvatar");
const discordLinkTitle = document.getElementById("discordLinkTitle");
const discordLinkDescription = document.getElementById("discordLinkDescription");
const discordLinkVerification = document.getElementById("discordLinkVerification");
const discordLinkCodePanel = document.getElementById("discordLinkCodePanel");
const discordLinkCommand = document.getElementById("discordLinkCommand");
const copyDiscordLinkCode = document.getElementById("copyDiscordLinkCode");
const manageDiscordLink = document.getElementById("manageDiscordLink");
const unlinkDiscord = document.getElementById("unlinkDiscord");
const navItems = Array.from(document.querySelectorAll(".nav-item"));
const appPages = Array.from(document.querySelectorAll(".app-page"));
const instancesPage = document.getElementById("instancesPage");
const instancesGrid = document.getElementById("instancesGrid");
const instancesEmpty = document.getElementById("instancesEmpty");
const instancesSearch = document.getElementById("instancesSearch");
const createInstanceButton = document.getElementById("createInstanceButton");
const instancesEmptyCreate = document.getElementById("instancesEmptyCreate");
const instanceTotalCount = document.getElementById("instanceTotalCount");
const instanceInGameCount = document.getElementById("instanceInGameCount");
const instanceReadyCount = document.getElementById("instanceReadyCount");
const instanceNotReadyCount = document.getElementById("instanceNotReadyCount");
const instanceModalBackdrop = document.getElementById("instanceModalBackdrop");
const instanceModalClose = document.getElementById("instanceModalClose");
const instanceModalTitle = document.getElementById("instanceModalTitle");
const instanceModalSubtitle = document.getElementById("instanceModalSubtitle");
const instanceNameInput = document.getElementById("instanceNameInput");
const instancePreviewVersion = document.getElementById("instancePreviewVersion");
const instancePreviewLoader = document.getElementById("instancePreviewLoader");
const instanceModalNote = document.getElementById("instanceModalNote");
const instanceColorSwatches = Array.from(document.querySelectorAll("[data-instance-color]"));
const instanceUseCurrentButton = document.getElementById("instanceUseCurrentButton");
const instanceSaveButton = document.getElementById("instanceSaveButton");
const deleteInstanceBackdrop = document.getElementById("deleteInstanceBackdrop");
const deleteInstanceTitle = document.getElementById("deleteInstanceTitle");
const deleteInstanceDescription = document.getElementById("deleteInstanceDescription");
const deleteInstanceCancel = document.getElementById("deleteInstanceCancel");
const deleteInstanceConfirm = document.getElementById("deleteInstanceConfirm");
const modsPage = document.getElementById("modsPage");
const modsInstancePicker = document.getElementById("modsInstancePicker");
const modsTargetName = document.getElementById("modsTargetName");
const modsTargetMeta = document.getElementById("modsTargetMeta");
const modsInstalledCount = document.getElementById("modsInstalledCount");
const modsEnabledCount = document.getElementById("modsEnabledCount");
const modsDisabledCount = document.getElementById("modsDisabledCount");
const modsLoaderValue = document.getElementById("modsLoaderValue");
const modsSearch = document.getElementById("modsSearch");
const modsList = document.getElementById("modsList");
const modsEmpty = document.getElementById("modsEmpty");
const modsEmptyAdd = document.getElementById("modsEmptyAdd");
const addModButton = document.getElementById("addModButton");
const openModsFolderButton = document.getElementById("openModsFolderButton");
const modsNotice = document.getElementById("modsNotice");
const modsVersionSelect = document.getElementById("modsVersionSelect");
const modsLoaderSelect = document.getElementById("modsLoaderSelect");
const modsConfigSave = document.getElementById("modsConfigSave");
const checkModUpdatesButton = document.getElementById("checkModUpdatesButton");
const explorePage = document.getElementById("explorePage");
const exploreInstanceSelect = document.getElementById("exploreInstanceSelect");
const exploreTargetName = document.getElementById("exploreTargetName");
const exploreTargetMeta = document.getElementById("exploreTargetMeta");
const modsExploreSearch = document.getElementById("modsExploreSearch");
const modsExploreGrid = document.getElementById("modsExploreGrid");
const modsExploreStatus = document.getElementById("modsExploreStatus");
const modsExploreRefresh = document.getElementById("modsExploreRefresh");
const exploreSortSelect = document.getElementById("exploreSortSelect");
const explorePagination = document.getElementById("explorePagination");
const explorePrevPage = document.getElementById("explorePrevPage");
const exploreNextPage = document.getElementById("exploreNextPage");
const explorePageNumbers = document.getElementById("explorePageNumbers");
const exploreCategoryButtons = Array.from(document.querySelectorAll(".explore-category"));
const modDetailsBackdrop = document.getElementById("modDetailsBackdrop");
const modDetailsClose = document.getElementById("modDetailsClose");
const modDetailsIcon = document.getElementById("modDetailsIcon");
const modDetailsTitle = document.getElementById("modDetailsTitle");
const modDetailsSubtitle = document.getElementById("modDetailsSubtitle");
const modDetailsBody = document.getElementById("modDetailsBody");
const modDetailsDownloads = document.getElementById("modDetailsDownloads");
const modDetailsFollowers = document.getElementById("modDetailsFollowers");
const modDetailsLicense = document.getElementById("modDetailsLicense");
const modDetailsCompatible = document.getElementById("modDetailsCompatible");
const modDetailsWeb = document.getElementById("modDetailsWeb");
const modDetailsInstall = document.getElementById("modDetailsInstall");
const chatPage = document.getElementById("chatPage");
const chatNavUnread = document.getElementById("chatNavUnread");
const chatSettingsButton = document.getElementById("chatSettingsButton");
const chatSettingsDrawer = document.getElementById("chatSettingsDrawer");
const chatSettingsClose = document.getElementById("chatSettingsClose");
const chatLauncherNotifications = document.getElementById("chatLauncherNotifications");
const chatInGameNotifications = document.getElementById("chatInGameNotifications");
const chatShowServer = document.getElementById("chatShowServer");
const chatUserSearch = document.getElementById("chatUserSearch");
const chatAddFriendButton = document.getElementById("chatAddFriendButton");
const chatSearchResults = document.getElementById("chatSearchResults");
const chatRequestCount = document.getElementById("chatRequestCount");
const chatRequestList = document.getElementById("chatRequestList");
const chatFriendCount = document.getElementById("chatFriendCount");
const chatFriendList = document.getElementById("chatFriendList");
const chatEmptyState = document.getElementById("chatEmptyState");
const chatConversationActive = document.getElementById("chatConversationActive");
const chatPeerAvatar = document.getElementById("chatPeerAvatar");
const chatPeerDot = document.getElementById("chatPeerDot");
const chatPeerName = document.getElementById("chatPeerName");
const chatPeerStatus = document.getElementById("chatPeerStatus");
const chatRemoveFriend = document.getElementById("chatRemoveFriend");
const chatMessages = document.getElementById("chatMessages");
const chatComposer = document.getElementById("chatComposer");
const chatMessageInput = document.getElementById("chatMessageInput");
const chatSendButton = document.getElementById("chatSendButton");
const chatInfoAvatar = document.getElementById("chatInfoAvatar");
const chatInfoName = document.getElementById("chatInfoName");
const chatInfoDiscord = document.getElementById("chatInfoDiscord");
const chatInfoDot = document.getElementById("chatInfoDot");
const chatInfoPresence = document.getElementById("chatInfoPresence");
const chatInfoServer = document.getElementById("chatInfoServer");

let minecraftVersions = [];
let latestRelease = null;
let selectedVersion = null;
let installedJava = null;
let managedJava = null;
let requiredJava = null;
let javaInstallInProgress = false;
let minecraftActionInProgress = false;
let javaCheckId = 0;

let microsoftSignedIn = false;
let minecraftProfile = null;
let cachedMicrosoftUsername = null;
let consoleLoggingEnabled = false;
let launchTelemetry = null;
let lastProgressLogKey = "";

// FlintFix's in-game client runs on Fabric; it is the default loader.
const FLINTFIX_GAME_VERSION = "1.21.1";
const loaderOptions = [
    { value: "fabric", label: "Fabric", subtitle: "FlintFix in-game client" },
    { value: "vanilla", label: "Vanilla", subtitle: "Unmodded Minecraft" },
    { value: "forge", label: "Forge", subtitle: "Coming soon", unavailable: true },
    { value: "neoforge", label: "NeoForge", subtitle: "Coming soon", unavailable: true }
];
let selectedLoader = loaderOptions[0];
let activeSettingsTab = "game";
let minecraftFlintDataUrl = null;
let discordLinkStatus = { linked: false };
let discordLinkPollTimer = null;
let activeDiscordLinkChallengeId = null;
let activeDiscordLinkCommand = null;
let instances = [];
let activeInstanceId = null;
let runningInstanceId = null;
let editingInstanceId = null;
let instanceDraftVersion = null;
let instanceDraftLoader = null;
let instanceDraftColor = "#7d6cf2";
let pendingDeleteInstanceId = null;
let selectedModsInstanceId = null;
let currentInstanceMods = [];
let modsLoading = false;
let modsExploreResults = [];
let modsExploreLoading = false;
let exploreCategory = "";
let explorePageNumber = 1;
let exploreTotalHits = 0;
const EXPLORE_PAGE_SIZE = 12;
let modUpdateMap = new Map();
let activeModDetails = null;
let activeContentPage = "home";
let socialState = { friends: [], incomingRequests: [], outgoingRequests: [], unreadTotal: 0, recentIncoming: [] };
let selectedChatFriendUuid = null;
let currentChatMessages = [];
let socialPollTimer = null;
let socialSyncRunning = false;
let socialSearchTimer = null;
let lastSocialMessageId = Number(localStorage.getItem("flintfix.social.lastMessageId")) || 0;
const chatPreferences = {
    launcherNotifications: localStorage.getItem("flintfix.chat.launcherNotifications") !== "false",
    inGameNotifications: localStorage.getItem("flintfix.chat.inGameNotifications") !== "false",
    showServer: localStorage.getItem("flintfix.chat.showServer") !== "false"
};
const pageHeaderIcons = {
    home: "home",
    instances: "dashboard",
    mods: "extension",
    explore: "travel_explore",
    packs: "palette",
    servers: "dns",
    skins: "checkroom",
    chat: "chat_bubble",
    settings: "settings"
};

const launchPreferences = {
    width: Number(localStorage.getItem("flintfix.launch.width")) || 1280,
    height: Number(localStorage.getItem("flintfix.launch.height")) || 720,
    fullscreen: localStorage.getItem("flintfix.launch.fullscreen") === "true",
    lockAspect: localStorage.getItem("flintfix.launch.lockAspect") !== "false",
    launcherVisibility: "keep",
    discordRichPresence: localStorage.getItem("flintfix.discord.richPresence") === "true"
};
let lockedAspectRatio = launchPreferences.height > 0 ? launchPreferences.width / launchPreferences.height : (16 / 9);

function saveChatPreferences() {
    localStorage.setItem("flintfix.chat.launcherNotifications", String(chatPreferences.launcherNotifications));
    localStorage.setItem("flintfix.chat.inGameNotifications", String(chatPreferences.inGameNotifications));
    localStorage.setItem("flintfix.chat.showServer", String(chatPreferences.showServer));
}

function syncChatPreferenceControls() {
    if (chatLauncherNotifications) chatLauncherNotifications.checked = chatPreferences.launcherNotifications;
    if (chatInGameNotifications) chatInGameNotifications.checked = chatPreferences.inGameNotifications;
    if (chatShowServer) chatShowServer.checked = chatPreferences.showServer;
}

function socialUuid(user) {
    return String(user?.minecraft?.uuid || "").replace(/-/g, "");
}

function getSocialFriend(uuid) {
    const clean = String(uuid || "").replace(/-/g, "");
    return socialState.friends.find(friend => socialUuid(friend) === clean) || null;
}

function socialPresenceText(user) {
    const presence = user?.presence || {};
    if (presence.state === "in_game") {
        return presence.server ? `In game • ${presence.server}` : "In game";
    }
    if (presence.state === "launcher") return "Online • In launcher";
    return "Offline";
}

function setPresenceDot(element, user) {
    if (!element) return;
    const state = user?.presence?.state || "offline";
    element.classList.toggle("online", state === "launcher");
    element.classList.toggle("in-game", state === "in_game");
    element.classList.toggle("offline", state === "offline");
}

function renderChatNavUnread() {
    if (!chatNavUnread) return;
    const count = Number(socialState.unreadTotal || 0);
    chatNavUnread.hidden = count <= 0;
    chatNavUnread.textContent = count > 99 ? "99+" : String(count);
}

function renderSocialSearchResults(_results = []) {
    if (chatSearchResults) {
        chatSearchResults.innerHTML = "";
        chatSearchResults.hidden = true;
    }
}


function renderFriendRequests() {
    if (!chatRequestList || !chatRequestCount) return;
    const incoming = Array.isArray(socialState.incomingRequests) ? socialState.incomingRequests : [];
    chatRequestCount.textContent = String(incoming.length);
    chatRequestList.innerHTML = "";
    if (!incoming.length) {
        chatRequestList.innerHTML = '<div class="chat-muted-row">No pending requests.</div>';
        return;
    }
    for (const user of incoming) {
        const row = document.createElement("div");
        row.className = "chat-request-row";
        const face = document.createElement("span");
        face.className = "skin-face chat-list-face";
        setSkinFace(face, user.minecraft?.skinUrl || null);
        const copy = document.createElement("div");
        copy.className = "chat-list-copy";
        const name = document.createElement("strong");
        name.textContent = user.minecraft?.name || "FlintFix user";
        const sub = document.createElement("span");
        sub.textContent = "Sent you a friend request";
        copy.append(name, sub);
        const actions = document.createElement("div");
        actions.className = "chat-request-actions";
        for (const [label, accept, iconName] of [["Accept", true, "check"], ["Decline", false, "close"]]) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = accept ? "accept" : "decline";
            button.innerHTML = `<span class="material-symbols-rounded">${iconName}</span>`;
            button.title = label;
            button.addEventListener("click", async () => {
                button.disabled = true;
                const response = await window.flintfix.respondFriendRequest(socialUuid(user), accept);
                if (!response?.success) showToast(response?.error || "Could not update request.", "error");
                await refreshSocial({ force: true });
            });
            actions.appendChild(button);
        }
        row.append(face, copy, actions);
        chatRequestList.appendChild(row);
    }
}

function renderFriendList() {
    if (!chatFriendList || !chatFriendCount) return;
    const friends = Array.isArray(socialState.friends) ? socialState.friends : [];
    chatFriendCount.textContent = String(friends.length);
    chatFriendList.innerHTML = "";
    if (!friends.length) {
        chatFriendList.innerHTML = '<div class="chat-muted-row">Your friends will appear here.</div>';
        return;
    }
    for (const friend of friends) {
        const uuid = socialUuid(friend);
        const button = document.createElement("button");
        button.type = "button";
        button.className = `chat-friend-row${selectedChatFriendUuid === uuid ? " active" : ""}`;
        const faceWrap = document.createElement("span");
        faceWrap.className = "chat-face-wrap";
        const face = document.createElement("span");
        face.className = "skin-face chat-list-face";
        setSkinFace(face, friend.minecraft?.skinUrl || null);
        const dot = document.createElement("span");
        dot.className = "presence-dot offline";
        setPresenceDot(dot, friend);
        faceWrap.append(face, dot);
        const copy = document.createElement("span");
        copy.className = "chat-list-copy";
        const name = document.createElement("strong");
        name.textContent = friend.minecraft?.name || "Friend";
        const status = document.createElement("span");
        status.textContent = socialPresenceText(friend);
        copy.append(name, status);
        button.append(faceWrap, copy);
        const unread = Number(friend.unread || 0);
        if (unread > 0) {
            const badge = document.createElement("span");
            badge.className = "chat-row-unread";
            badge.textContent = unread > 99 ? "99+" : String(unread);
            button.appendChild(badge);
        }
        button.addEventListener("click", () => selectChatFriend(uuid));
        chatFriendList.appendChild(button);
    }
}

function renderChatFriendInfo(friend) {
    if (!friend) {
        if (chatInfoName) chatInfoName.textContent = "FlintFix Social";
        if (chatInfoDiscord) chatInfoDiscord.textContent = "Verified Minecraft friends";
        if (chatInfoPresence) chatInfoPresence.textContent = "Offline";
        if (chatInfoServer) chatInfoServer.textContent = "Select a friend to see their status.";
        setSkinFace(chatInfoAvatar, null);
        setPresenceDot(chatInfoDot, null);
        return;
    }
    setSkinFace(chatInfoAvatar, friend.minecraft?.skinUrl || null);
    if (chatInfoName) chatInfoName.textContent = friend.minecraft?.name || "Friend";
    if (chatInfoDiscord) chatInfoDiscord.textContent = friend.discord?.displayName
        ? `Minecraft verified • Discord ${friend.discord.displayName}`
        : "Verified Minecraft account";
    if (chatInfoPresence) chatInfoPresence.textContent = friend.presence?.state === "in_game" ? "In game" : (friend.presence?.state === "launcher" ? "In launcher" : "Offline");
    if (chatInfoServer) chatInfoServer.textContent = friend.presence?.state === "in_game" ? (friend.presence?.server || "Server hidden") : socialPresenceText(friend);
    setPresenceDot(chatInfoDot, friend);
}

function renderChatConversationHeader(friend) {
    const active = Boolean(friend);
    if (chatEmptyState) chatEmptyState.hidden = active;
    if (chatConversationActive) chatConversationActive.hidden = !active;
    if (!friend) return;
    setSkinFace(chatPeerAvatar, friend.minecraft?.skinUrl || null);
    setPresenceDot(chatPeerDot, friend);
    if (chatPeerName) chatPeerName.textContent = friend.minecraft?.name || "Friend";
    if (chatPeerStatus) chatPeerStatus.textContent = socialPresenceText(friend);
}

function renderChatMessages() {
    if (!chatMessages) return;
    chatMessages.innerHTML = "";
    const meUuid = String(socialState.me?.minecraft?.uuid || minecraftProfile?.id || "").replace(/-/g, "");
    if (!currentChatMessages.length) {
        chatMessages.innerHTML = '<div class="chat-conversation-placeholder">No messages yet. Say hello.</div>';
        return;
    }
    for (const message of currentChatMessages) {
        const mine = String(message.senderUuid || "").replace(/-/g, "") === meUuid;
        const bubble = document.createElement("div");
        bubble.className = `chat-message ${mine ? "mine" : "theirs"}`;
        const content = document.createElement("div");
        content.className = "chat-message-content";
        content.textContent = message.content || "";
        const time = document.createElement("span");
        time.className = "chat-message-time";
        const timestamp = Number(message.createdAt || 0) * 1000;
        time.textContent = timestamp ? new Intl.DateTimeFormat(undefined, { hour: "numeric", minute: "2-digit" }).format(new Date(timestamp)) : "";
        bubble.append(content, time);
        chatMessages.appendChild(bubble);
    }
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

async function loadChatMessages() {
    if (!selectedChatFriendUuid) return;
    const result = await window.flintfix.getSocialMessages(selectedChatFriendUuid);
    if (!result?.success) {
        if (activeContentPage === "chat") showToast(result?.error || "Could not load messages.", "error");
        return;
    }
    currentChatMessages = Array.isArray(result.messages) ? result.messages : [];
    renderChatMessages();
}

async function selectChatFriend(uuid) {
    selectedChatFriendUuid = String(uuid || "").replace(/-/g, "");
    const friend = getSocialFriend(selectedChatFriendUuid);
    renderFriendList();
    renderChatConversationHeader(friend);
    renderChatFriendInfo(friend);
    await loadChatMessages();
    await refreshSocial({ force: true, suppressNotifications: true });
}

function renderChatPage() {
    renderChatNavUnread();
    renderFriendRequests();
    renderFriendList();
    const friend = getSocialFriend(selectedChatFriendUuid);
    if (selectedChatFriendUuid && !friend) {
        selectedChatFriendUuid = null;
        currentChatMessages = [];
    }
    renderChatConversationHeader(friend || null);
    renderChatFriendInfo(friend || null);
}

async function addFriendByUsername() {
    const query = String(chatUserSearch?.value || "").trim();
    if (!query) {
        showToast("Enter a Minecraft username first.", "error");
        chatUserSearch?.focus();
        return;
    }
    if (!/^[A-Za-z0-9_]{3,16}$/.test(query)) {
        showToast("Enter a valid Minecraft username.", "error");
        chatUserSearch?.focus();
        return;
    }
    if (!minecraftProfile) {
        showToast("Sign in to Minecraft before adding friends.", "error");
        return;
    }
    if (chatAddFriendButton) {
        chatAddFriendButton.disabled = true;
        chatAddFriendButton.classList.add("loading");
    }
    try {
        const result = await window.flintfix.searchSocialUsers(query);
        if (!result?.success) {
            showToast(result?.error || "Could not search FlintFix users.", "error");
            return;
        }
        const results = Array.isArray(result.results) ? result.results : [];
        const user = results.find(item => String(item?.minecraft?.name || "").toLowerCase() === query.toLowerCase());
        if (!user) {
            showToast(`No FlintFix user named ${query} was found. They need to run FlintFix v41 while signed into Minecraft at least once.`, "error");
            return;
        }
        const relationship = user.relationship || "none";
        if (relationship === "friends") {
            showToast(`${user.minecraft?.name || query} is already your friend.`, "info");
            return;
        }
        if (relationship === "outgoing") {
            showToast(`You already sent ${user.minecraft?.name || query} a friend request.`, "info");
            return;
        }
        if (relationship === "incoming") {
            showToast(`${user.minecraft?.name || query} already sent you a request. Accept it under Friend requests.`, "info");
            return;
        }
        const response = await window.flintfix.sendFriendRequest(socialUuid(user));
        if (!response?.success) {
            showToast(response?.error || "Could not send friend request.", "error");
            return;
        }
        if (chatUserSearch) chatUserSearch.value = "";
        showToast(`Friend request sent to ${user.minecraft?.name || query}.`, "success");
        await refreshSocial({ force: true, suppressNotifications: true });
    } finally {
        if (chatAddFriendButton) {
            chatAddFriendButton.disabled = false;
            chatAddFriendButton.classList.remove("loading");
        }
    }
}


async function handleIncomingSocialNotifications(messages, suppress = false) {
    if (!Array.isArray(messages) || !messages.length) return;
    const gameState = await window.flintfix.getSocialGameState().catch(() => ({ state: "launcher" }));
    for (const item of messages) {
        const id = Number(item.id || 0);
        if (id > lastSocialMessageId) lastSocialMessageId = id;
        if (suppress) continue;
        const senderName = item.sender?.minecraft?.name || "FlintFix friend";
        if (chatPreferences.launcherNotifications && (activeContentPage !== "chat" || selectedChatFriendUuid !== socialUuid(item.sender))) {
            showToast(`${senderName}: ${item.content}`, "info");
        }
        if (chatPreferences.inGameNotifications && gameState?.state === "in_game") {
            void window.flintfix.pushGameSocialNotification({ id, sender: senderName, message: item.content });
        }
    }
    localStorage.setItem("flintfix.social.lastMessageId", String(lastSocialMessageId));
}

async function refreshSocial(options = {}) {
    if (socialSyncRunning) return;
    if (!minecraftProfile) {
        socialState = { friends: [], incomingRequests: [], outgoingRequests: [], unreadTotal: 0, recentIncoming: [] };
        renderChatPage();
        return;
    }
    socialSyncRunning = true;
    try {
        const result = await window.flintfix.syncSocial({
            lastMessageId: lastSocialMessageId,
            showServer: chatPreferences.showServer
        });
        if (!result?.success) {
            if (activeContentPage === "chat" && options.force) showToast(result?.error || "FlintFix Social is unavailable.", "error");
            return;
        }
        socialState = result;
        await handleIncomingSocialNotifications(result.recentIncoming || [], Boolean(options.suppressNotifications));
        renderChatPage();
        if (selectedChatFriendUuid && activeContentPage === "chat") await loadChatMessages();
    } finally {
        socialSyncRunning = false;
    }
}

function startSocialPolling() {
    if (socialPollTimer) return;
    socialPollTimer = setInterval(() => void refreshSocial(), 5000);
    void refreshSocial({ force: true, suppressNotifications: true });
}

function stopSocialPolling() {
    if (socialPollTimer) clearInterval(socialPollTimer);
    socialPollTimer = null;
}

function openChatSettings() {
    if (!chatSettingsDrawer) return;
    syncChatPreferenceControls();
    chatSettingsDrawer.hidden = false;
    requestAnimationFrame(() => chatSettingsDrawer.classList.add("open"));
}

function closeChatSettings() {
    if (!chatSettingsDrawer) return;
    chatSettingsDrawer.classList.remove("open");
    setTimeout(() => { if (!chatSettingsDrawer.classList.contains("open")) chatSettingsDrawer.hidden = true; }, 180);
}

function clearConsoleForLaunch() {
    if (!consoleElement) return;
    consoleElement.innerHTML = "";
}

function addConsoleLog(source, message) {
    if (!consoleElement || !consoleLoggingEnabled) return;
    const normalizedSource = String(source || "GAME").toUpperCase();
    if (!["LAUNCHER", "AUTH", "GAME", "ERROR", "MINECRAFT", "MICROSOFT", "JAVA", "FABRIC", "FLINTFIX", "DOWNLOAD"].includes(normalizedSource)) return;
    const line = document.createElement("div");
    line.className = "console-line";
    const sourceElement = document.createElement("span");
    sourceElement.className = "console-time";
    sourceElement.textContent = normalizedSource;
    line.appendChild(sourceElement);
    line.appendChild(document.createTextNode(` ${message}`));
    consoleElement.appendChild(line);
    consoleElement.scrollTop = consoleElement.scrollHeight;
}

function formatEtaSeconds(seconds) {
    const value = Math.max(0, Math.round(Number(seconds) || 0));
    if (value < 60) return `${value}s`;
    const minutes = Math.floor(value / 60);
    const rest = value % 60;
    return rest ? `${minutes}m ${rest}s` : `${minutes}m`;
}

function setLaunchEta(label, complete = false) {
    if (!launchEta) return;
    launchEta.hidden = false;
    launchEta.textContent = label;
    launchEta.classList.toggle("complete", Boolean(complete));
}

function getLaunchHistoryKey() {
    const loader = selectedLoader?.value || "vanilla";
    return `flintfix.launch.average.${selectedVersion || "unknown"}.${loader}`;
}

function startLaunchTelemetry() {
    const history = Number(localStorage.getItem(getLaunchHistoryKey()) || 0);
    launchTelemetry = {
        startedAt: performance.now(),
        stageStartedAt: performance.now(),
        stage: "prepare",
        smoothEta: Number.isFinite(history) && history > 0 ? history / 1000 : null,
        historicalMs: Number.isFinite(history) && history > 0 ? history : null,
        milestones: {}
    };
    lastProgressLogKey = "";
    if (launchTelemetry.historicalMs) {
        setLaunchEta(`ETA ~${formatEtaSeconds(launchTelemetry.historicalMs / 1000)}`);
    } else {
        setLaunchEta("FIRST LAUNCH ~2–5 MIN");
    }
}

function progressFraction(data) {
    const ratio = data?.total ? Math.max(0, Math.min(1, Number(data.current || 0) / Number(data.total))) :
        (data?.bytesTotal ? Math.max(0, Math.min(1, Number(data.received || 0) / Number(data.bytesTotal))) : 0);
    switch (data?.stage) {
        case "cache": return 0.72;
        case "metadata": return 0.04 + ratio * 0.03;
        case "client": return 0.07 + ratio * 0.12;
        case "libraries": return 0.19 + ratio * 0.18;
        case "assets-index": return 0.38;
        case "assets": return 0.39 + ratio * 0.34;
        case "natives": return 0.74 + ratio * 0.05;
        case "complete": return 0.80;
        case "fabric": {
            if (data?.phase === "jdk") return 0.81;
            if (data?.phase === "jdk-download") return 0.81 + ratio * 0.04;
            if (data?.phase === "parallel") return 0.85;
            if (data?.phase === "libraries") return 0.86 + ratio * 0.05;
            if (data?.phase === "api") return 0.92;
            const message = String(data?.message || "").toLowerCase();
            if (message.includes("building flintfix")) return 0.91;
            if (message.includes("client ready")) return 0.96;
            return 0.86;
        }
        case "launch": {
            if (data?.phase === "session") return 0.81;
            if (data?.phase === "session-ready") return 0.83;
            if (data?.phase === "mods") return 0.96;
            if (data?.phase === "process") return 0.98;
            return 0.94;
        }
        case "started": return 1;
        default: return 0;
    }
}

function updateLaunchTelemetry(data) {
    if (!launchTelemetry) return;
    const now = performance.now();
    if (data?.stage && data.stage !== launchTelemetry.stage) {
        launchTelemetry.stage = data.stage;
        launchTelemetry.stageStartedAt = now;
    }

    const message = String(data?.message || "").trim();
    const hasCountProgress = Number(data?.total) > 0 && Number.isFinite(Number(data?.current));
    const isDynamicPercentMessage = /\.\.\.\s*\d+%$/.test(message);
    if (message && !isDynamicPercentMessage) {
        const key = `${data.stage || "progress"}:${message}`;
        if (key !== lastProgressLogKey) {
            lastProgressLogKey = key;
            addConsoleLog("Launcher", message);
        }
    }

    if (hasCountProgress && Number(data.total) > 1) {
        const ratio = Math.max(0, Math.min(1, Number(data.current || 0) / Number(data.total)));
        const step = data.stage === "client" ? 10 : 25;
        const milestone = Math.min(100, Math.floor((ratio * 100) / step) * step);
        const key = `${data.stage || "progress"}:${data.phase || ""}`;
        const previous = Number(launchTelemetry.milestones[key] || 0);
        if (milestone >= step && milestone > previous && milestone < 100) {
            launchTelemetry.milestones[key] = milestone;
            const label = data.stage === "fabric" && data.phase === "jdk-download"
                ? "Managed JDK"
                : String(data.stage || "Progress").replace(/(^|-)([a-z])/g, (_m, sep, char) => `${sep ? " " : ""}${char.toUpperCase()}`);
            addConsoleLog("Launcher", `${label} ${milestone}% (${data.current}/${data.total})`);
        }
    }

    const fraction = progressFraction(data);
    const elapsedSeconds = Math.max(0.25, (now - launchTelemetry.startedAt) / 1000);
    let etaSeconds = null;

    if (fraction >= 1) {
        setLaunchEta("PROCESS STARTED", true);
        return;
    }

    if (fraction > 0.06) {
        etaSeconds = elapsedSeconds * (1 - fraction) / fraction;
    }
    if (launchTelemetry.historicalMs) {
        const historicalRemaining = Math.max(0, launchTelemetry.historicalMs / 1000 - elapsedSeconds);
        etaSeconds = etaSeconds == null ? historicalRemaining : (etaSeconds * 0.55 + historicalRemaining * 0.45);
    }
    if (etaSeconds != null && Number.isFinite(etaSeconds)) {
        etaSeconds = Math.max(3, Math.min(300, etaSeconds));
        launchTelemetry.smoothEta = launchTelemetry.smoothEta == null
            ? etaSeconds
            : launchTelemetry.smoothEta * 0.72 + etaSeconds * 0.28;
        setLaunchEta(`ETA ~${formatEtaSeconds(launchTelemetry.smoothEta)}`);
    }
}

function finishLaunchTelemetry(success) {
    if (!launchTelemetry) return;
    const durationMs = Math.max(1, performance.now() - launchTelemetry.startedAt);
    if (success) {
        const old = Number(localStorage.getItem(getLaunchHistoryKey()) || 0);
        const next = old > 0 ? Math.round(old * 0.7 + durationMs * 0.3) : Math.round(durationMs);
        localStorage.setItem(getLaunchHistoryKey(), String(next));
        setLaunchEta(`STARTED IN ${formatEtaSeconds(durationMs / 1000)}`, true);
    } else if (launchEta) {
        launchEta.hidden = true;
        launchEta.classList.remove("complete");
    }
    launchTelemetry = null;
}

function isDevMode() {
    return false;
}

function setPlayText(text) {
    if (playButtonText) playButtonText.textContent = text;
    else if (playButton) playButton.textContent = text;
}

function setSkinFace(element, skinUrl = null) {
    if (!element) return;
    if (skinUrl) {
        const safeUrl = String(skinUrl).replace(/"/g, "%22");
        element.classList.remove("fallback-face");
        element.style.backgroundImage = `url("${safeUrl}"), url("${safeUrl}")`;
        element.style.backgroundSize = "800% 800%, 800% 800%";
        element.style.backgroundPosition = "71.4286% 14.2857%, 14.2857% 14.2857%";
        element.style.backgroundRepeat = "no-repeat, no-repeat";
    } else {
        element.classList.add("fallback-face");
        element.style.backgroundImage = `url("${minecraftFlintDataUrl || 'assets/flintfix-client-icon.png'}")`;
        element.style.backgroundSize = "cover";
        element.style.backgroundPosition = "center";
        element.style.backgroundRepeat = "no-repeat";
    }
}

function getPrimaryAccountName() {
    if (minecraftProfile?.name) return minecraftProfile.name;
    if (cachedMicrosoftUsername) return cachedMicrosoftUsername.split("@")[0];
    return "Not signed in";
}

function getSecondaryAccountText() {
    if (minecraftProfile) return "Minecraft account connected";
    if (cachedMicrosoftUsername) return "Minecraft profile needs to reconnect";
    return "Sign in to a Minecraft account to play.";
}

function showToast(message, type = "success") {
    if (!toastContainer) return;
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    const dot = document.createElement("span");
    dot.className = "toast-dot";
    const text = document.createElement("span");
    text.textContent = String(message);
    toast.append(dot, text);
    toastContainer.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add("show"));
    setTimeout(() => {
        toast.classList.remove("show");
        setTimeout(() => toast.remove(), 220);
    }, 2700);
}


function maskEmail(email) {
    const value = String(email || "");
    const at = value.indexOf("@");
    if (at <= 0) return value ? "••••••" : "Not connected";
    const local = value.slice(0, at);
    const domain = value.slice(at + 1);
    const visibleLocal = local.length <= 2 ? local[0] || "" : `${local.slice(0, 2)}${"*".repeat(Math.min(6, Math.max(2, local.length - 2)))}`;
    const domainParts = domain.split(".");
    const domainName = domainParts.shift() || "";
    const domainTail = domainParts.length ? `.${domainParts.join(".")}` : "";
    const visibleDomain = domainName.length <= 2 ? domainName : `${domainName[0]}${"*".repeat(Math.min(5, Math.max(2, domainName.length - 2)))}${domainName.slice(-1)}`;
    return `${visibleLocal}@${visibleDomain}${domainTail}`;
}

function persistLaunchPreferences() {
    localStorage.setItem("flintfix.launch.width", String(launchPreferences.width));
    localStorage.setItem("flintfix.launch.height", String(launchPreferences.height));
    localStorage.setItem("flintfix.launch.fullscreen", String(launchPreferences.fullscreen));
    localStorage.setItem("flintfix.launch.lockAspect", String(launchPreferences.lockAspect));
    localStorage.setItem("flintfix.launch.visibility", "keep");
    localStorage.setItem("flintfix.discord.richPresence", String(launchPreferences.discordRichPresence));
}

function clampNumber(value, min, max, fallback) {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return fallback;
    return Math.max(min, Math.min(max, Math.round(parsed)));
}

function syncSettingsControls() {
    if (resolutionWidth) resolutionWidth.value = String(launchPreferences.width);
    if (resolutionHeight) resolutionHeight.value = String(launchPreferences.height);
    if (launchFullscreen) launchFullscreen.checked = launchPreferences.fullscreen;
    if (lockAspectRatio) lockAspectRatio.checked = launchPreferences.lockAspect;
    for (const radio of launcherVisibilityRadios) {
        radio.checked = radio.value === launchPreferences.launcherVisibility;
    }
    if (discordRichPresence) discordRichPresence.checked = launchPreferences.discordRichPresence;
    syncSettingsAccountUi();
}

function getDiscordPresenceContext() {
    return {
        username: minecraftProfile?.name || null,
        version: selectedVersion || latestRelease || null,
        loader: selectedLoader?.label || "Vanilla"
    };
}

function renderDiscordPresenceStatus(result = {}) {
    if (!discordPresenceStatus) return;
    discordPresenceStatus.classList.remove("connected", "warning", "error");

    if (!launchPreferences.discordRichPresence) {
        discordPresenceStatus.textContent = "Off";
        return;
    }
    if (result.configured === false) {
        discordPresenceStatus.textContent = result.configError
            ? `Could not load Rich Presence configuration: ${result.configError}`
            : "Rich Presence configuration is not available from the FlintFix Discord bot yet.";
        discordPresenceStatus.classList.add("warning");
        return;
    }
    if (result.connected) {
        discordPresenceStatus.textContent = "Connected — Discord activity will update automatically.";
        discordPresenceStatus.classList.add("connected");
        return;
    }
    if (result.error || result.lastError) {
        discordPresenceStatus.textContent = result.error || result.lastError;
        discordPresenceStatus.classList.add("error");
        return;
    }
    discordPresenceStatus.textContent = "Enabled — open the Discord desktop app to show your activity.";
    discordPresenceStatus.classList.add("warning");
}

async function syncDiscordPresencePreference({ notify = false } = {}) {
    try {
        const result = await window.flintfix.setDiscordPresenceEnabled(
            launchPreferences.discordRichPresence,
            getDiscordPresenceContext()
        );
        renderDiscordPresenceStatus(result);
        if (notify) {
            if (!launchPreferences.discordRichPresence) {
                showToast("Discord Rich Presence disabled.", "neutral");
            } else if (result.success) {
                showToast("Discord Rich Presence enabled.", "success");
            } else {
                showToast(result.error || "Discord Rich Presence could not connect.", "error");
            }
        }
        return result;
    } catch (error) {
        renderDiscordPresenceStatus({ error: error.message });
        if (notify) showToast(error.message || "Discord Rich Presence failed.", "error");
        return { success: false, error: error.message };
    }
}

function renderDiscordLinkUi(result = {}) {
    discordLinkStatus = result && typeof result === "object" ? result : { linked: false };
    const linked = Boolean(discordLinkStatus.linked && discordLinkStatus.discord);
    const signedIn = Boolean(minecraftProfile);

    if (discordLinkAvatar) {
        discordLinkAvatar.classList.toggle("linked", linked);
        discordLinkAvatar.innerHTML = "";
        if (linked && discordLinkStatus.discord.avatarUrl) {
            const img = document.createElement("img");
            img.alt = "Discord avatar";
            img.src = discordLinkStatus.discord.avatarUrl;
            img.onerror = () => {
                discordLinkAvatar.classList.remove("linked");
                discordLinkAvatar.textContent = "D";
            };
            discordLinkAvatar.appendChild(img);
        } else {
            discordLinkAvatar.textContent = "D";
        }
    }

    if (linked) {
        activeDiscordLinkChallengeId = null;
        activeDiscordLinkCommand = null;
        if (discordLinkCodePanel) discordLinkCodePanel.hidden = true;
        const displayName = discordLinkStatus.discord.displayName || discordLinkStatus.discord.username || "Discord account";
        const username = discordLinkStatus.discord.username ? `@${discordLinkStatus.discord.username}` : "Discord account";
        if (discordLinkTitle) discordLinkTitle.textContent = displayName;
        if (discordLinkDescription) discordLinkDescription.textContent = `${username} is linked to ${discordLinkStatus.minecraft?.name || minecraftProfile?.name || "this Minecraft account"}.`;
        if (discordLinkVerification) discordLinkVerification.textContent = "Verified with a one-time FlintFix link code.";
        if (connectDiscord) connectDiscord.hidden = true;
        if (manageDiscordLink) manageDiscordLink.hidden = true;
        if (unlinkDiscord) unlinkDiscord.hidden = false;
        return;
    }

    if (discordLinkTitle) discordLinkTitle.textContent = "Link Discord account";
    if (discordLinkDescription) {
        discordLinkDescription.textContent = signedIn
            ? `Link Discord to ${minecraftProfile.name} without opening a website.`
            : "Sign in to Minecraft first, then link your Discord account.";
    }
    if (discordLinkVerification) {
        discordLinkVerification.textContent = signedIn
            ? "Press Link account, then run the one-time /link command in the FlintFix Discord."
            : "A Minecraft account must be signed in before Discord linking is available.";
    }
    if (discordLinkCodePanel) discordLinkCodePanel.hidden = !activeDiscordLinkCommand;
    if (discordLinkCommand && activeDiscordLinkCommand) discordLinkCommand.textContent = activeDiscordLinkCommand;
    if (connectDiscord) {
        connectDiscord.hidden = false;
        connectDiscord.disabled = !signedIn;
        connectDiscord.textContent = activeDiscordLinkCommand ? "New code" : (signedIn ? "Link account" : "Sign in first");
    }
    if (manageDiscordLink) manageDiscordLink.hidden = true;
    if (unlinkDiscord) unlinkDiscord.hidden = true;
}

async function refreshDiscordLinkStatus({ silent = true } = {}) {
    if (!minecraftProfile) {
        renderDiscordLinkUi({ linked: false });
        return { success: false, linked: false, error: "Minecraft account is not signed in." };
    }
    try {
        const result = await window.flintfix.getDiscordLinkStatus();
        if (result.success === false) {
            renderDiscordLinkUi({ linked: false });
            if (!silent) showToast(result.error || "Could not check Discord link status.", "error");
            return result;
        }
        renderDiscordLinkUi(result);
        return result;
    } catch (error) {
        renderDiscordLinkUi({ linked: false });
        if (!silent) showToast(error.message || "Could not check Discord link status.", "error");
        return { success: false, linked: false, error: error.message };
    }
}

function stopDiscordLinkPolling() {
    if (discordLinkPollTimer) {
        clearTimeout(discordLinkPollTimer);
        discordLinkPollTimer = null;
    }
}

async function pollForDiscordLink(challengeId, attempt = 0) {
    stopDiscordLinkPolling();
    if (!minecraftProfile || !challengeId || attempt > 150) return;
    try {
        const result = await window.flintfix.getDiscordLinkChallengeStatus(challengeId);
        if (!result.success) {
            if (attempt < 3) {
                discordLinkPollTimer = setTimeout(() => pollForDiscordLink(challengeId, attempt + 1), 2000);
                return;
            }
            showToast(result.error || "Could not check Discord link status.", "error");
            return;
        }
        if (result.status === "linked" && result.linked) {
            activeDiscordLinkChallengeId = null;
            activeDiscordLinkCommand = null;
            renderDiscordLinkUi(result);
            showToast("Your Minecraft account was successfully linked to Discord.", "success");
            return;
        }
        if (result.status === "expired") {
            activeDiscordLinkChallengeId = null;
            activeDiscordLinkCommand = null;
            renderDiscordLinkUi({ linked: false });
            showToast("Discord link code expired. Generate a new code.", "neutral");
            return;
        }
        discordLinkPollTimer = setTimeout(() => pollForDiscordLink(challengeId, attempt + 1), 2000);
    } catch (error) {
        if (attempt < 3) {
            discordLinkPollTimer = setTimeout(() => pollForDiscordLink(challengeId, attempt + 1), 2000);
            return;
        }
        showToast(error.message || "Could not check Discord link status.", "error");
    }
}

async function beginDiscordLink() {
    if (!minecraftProfile) {
        showToast("Sign in to Minecraft before linking Discord.", "error");
        return;
    }
    if (connectDiscord) connectDiscord.disabled = true;
    try {
        const result = await window.flintfix.startDiscordLink();
        if (!result.success) throw new Error(result.error || "Could not create a Discord link code.");

        if (result.alreadyLinked && result.linked) {
            renderDiscordLinkUi(result);
            showToast("This Minecraft account is already linked to Discord.", "success");
            return;
        }

        if (!result.challengeId || !result.command) {
            throw new Error("The Discord link service did not return a valid one-time code.");
        }

        activeDiscordLinkChallengeId = result.challengeId;
        activeDiscordLinkCommand = result.command;
        renderDiscordLinkUi({ linked: false });
        if (discordLinkCodePanel) discordLinkCodePanel.hidden = false;
        if (discordLinkCommand) discordLinkCommand.textContent = result.command;
        if (discordLinkVerification) discordLinkVerification.textContent = "Code expires in 5 minutes. Run it in the FlintFix Discord server.";
        showToast(`Discord link code created: ${result.code}`, "success");
        pollForDiscordLink(result.challengeId, 0);
    } catch (error) {
        showToast(error.message || "Discord linking failed.", "error");
    } finally {
        if (connectDiscord && !discordLinkStatus.linked) connectDiscord.disabled = false;
    }
}

async function unlinkDiscordAccount() {
    if (!discordLinkStatus.linked) return;
    const displayName = discordLinkStatus.discord?.displayName || discordLinkStatus.discord?.username || "this Discord account";
    if (!window.confirm(`Unlink ${displayName} from ${minecraftProfile?.name || "this Minecraft account"}?`)) return;
    if (unlinkDiscord) unlinkDiscord.disabled = true;
    try {
        const result = await window.flintfix.unlinkDiscordAccount();
        if (!result.success) throw new Error(result.error || "Could not unlink Discord account.");
        activeDiscordLinkChallengeId = null;
        activeDiscordLinkCommand = null;
        renderDiscordLinkUi({ linked: false });
        showToast("Discord account unlinked.", "neutral");
    } catch (error) {
        showToast(error.message || "Could not unlink Discord account.", "error");
    } finally {
        if (unlinkDiscord) unlinkDiscord.disabled = false;
    }
}

function syncSettingsAccountUi() {
    const name = minecraftProfile?.name || "Not signed in";
    const email = minecraftProfile?.microsoftUsername || cachedMicrosoftUsername || "";
    if (settingsAccountName) settingsAccountName.textContent = name;
    if (settingsAccountState) settingsAccountState.textContent = minecraftProfile ? "Minecraft account connected" : "Connect a Minecraft account to manage it.";
    if (accountUsernameValue) accountUsernameValue.textContent = minecraftProfile?.name || "Not connected";
    if (accountEmailValue) accountEmailValue.textContent = maskEmail(email);
    setSkinFace(settingsAccountAvatar, minecraftProfile?.skinUrl || null);
}

function setSettingsTab(name) {
    activeSettingsTab = name;
    settingsTabs.forEach(tab => tab.classList.toggle("active", tab.dataset.settingsTab === name));
    settingsPanes.forEach(pane => pane.classList.toggle("active", pane.dataset.settingsPane === name));

    if (settingsModalTitle && settingsModalSubtitle) {
        if (name === "account") {
            settingsModalTitle.textContent = "Account settings";
            settingsModalSubtitle.textContent = "Manage your Minecraft and Microsoft account links.";
        } else if (name === "discord") {
            settingsModalTitle.textContent = "Discord settings";
            settingsModalSubtitle.textContent = "Configure Rich Presence and your verified Discord account link.";
            void refreshDiscordLinkStatus({ silent: true });
        } else {
            settingsModalTitle.textContent = "Game settings";
            settingsModalSubtitle.textContent = "Configure how Minecraft opens from FlintFix.";
        }
    }

    applySettingsSearch();
}

function openSettingsModal(tab = "game") {
    closeAccountDropdown();
    if (!settingsModalBackdrop) return;
    navItems.forEach(item => item.classList.toggle("active", item.dataset.nav === "settings"));
    updatePageHeader("settings");
    syncSettingsControls();
    setSettingsTab(tab);
    if (settingsSearch) settingsSearch.value = "";
    applySettingsSearch();
    settingsModalBackdrop.hidden = false;
    requestAnimationFrame(() => settingsModalBackdrop.classList.add("open"));
    setTimeout(() => settingsSearch?.focus(), 170);
}

function closeSettingsModal() {
    if (!settingsModalBackdrop) return;
    settingsModalBackdrop.classList.remove("open");
    setTimeout(() => {
        if (!settingsModalBackdrop.classList.contains("open")) settingsModalBackdrop.hidden = true;
    }, 180);
    navItems.forEach(item => item.classList.toggle("active", item.dataset.nav === activeContentPage));
    updatePageHeader(activeContentPage);
}

function applySettingsSearch() {
    if (!settingsSearch) return;
    const query = settingsSearch.value.trim().toLowerCase();
    if (!query) {
        document.querySelectorAll(".settings-searchable").forEach(el => el.classList.remove("settings-search-hidden"));
        if (settingsSearchEmpty) settingsSearchEmpty.hidden = true;
        return;
    }

    const candidates = Array.from(document.querySelectorAll(".settings-searchable"));
    let firstMatchingPane = null;
    let totalMatches = 0;

    for (const el of candidates) {
        const haystack = `${el.dataset.search || ""} ${el.textContent || ""}`.toLowerCase();
        const matched = haystack.includes(query);
        el.classList.toggle("settings-search-hidden", !matched);
        if (matched) {
            totalMatches += 1;
            const pane = el.closest(".settings-pane");
            if (!firstMatchingPane && pane) firstMatchingPane = pane.dataset.settingsPane;
        }
    }

    if (firstMatchingPane && firstMatchingPane !== activeSettingsTab) {
        activeSettingsTab = firstMatchingPane;
        settingsTabs.forEach(tab => tab.classList.toggle("active", tab.dataset.settingsTab === activeSettingsTab));
        settingsPanes.forEach(pane => pane.classList.toggle("active", pane.dataset.settingsPane === activeSettingsTab));
    }

    if (settingsSearchEmpty) settingsSearchEmpty.hidden = totalMatches > 0;
}

async function openOfficialUrl(url) {
    const result = await window.flintfix.openExternal(url);
    if (!result?.success) showToast(result?.error || "Could not open the page.", "error");
}

async function refreshMinecraftFlintIcon(versionId = selectedVersion) {
    if (!versionId || !window.flintfix.getMinecraftFlintIcon) return;
    try {
        const result = await window.flintfix.getMinecraftFlintIcon(versionId);
        if (!result?.success || !result.dataUrl) return;
        minecraftFlintDataUrl = result.dataUrl;
        if (brandIcon) brandIcon.src = result.dataUrl;
        if (!minecraftProfile?.skinUrl) {
            setSkinFace(playerSkin, null);
            setSkinFace(accountAvatar, null);
            setSkinFace(dropdownAvatar, null);
            setSkinFace(settingsAccountAvatar, null);
        }
    } catch {
        // Keep the bundled fallback icon when the Minecraft client JAR is not installed yet.
    }
}

function getReadiness() {
    if (!minecraftProfile) return { ready: false, reason: "Sign in to a Minecraft account." };
    if (!selectedVersion) return { ready: false, reason: "Choose a Minecraft version." };
    if (!requiredJava) return { ready: false, reason: "FlintFix is still checking the Java requirement." };
    if (!getUsableJava()) return { ready: false, reason: `Java ${requiredJava} is required for Minecraft ${selectedVersion}.` };
    if (selectedLoader.value !== "vanilla" && selectedLoader.value !== "fabric") {
        return { ready: false, reason: `${selectedLoader.label} support is not available yet.` };
    }
    if (selectedLoader.value === "fabric" && selectedVersion !== FLINTFIX_GAME_VERSION) {
        return { ready: true, reason: `Ready. FlintFix in-game features need ${FLINTFIX_GAME_VERSION}; Fabric ${selectedVersion} launches without them.` };
    }
    return { ready: true, reason: "Everything is ready to launch." };
}

function updateReadiness() {
    const state = getReadiness();
    for (const element of [launcherReady, profileStatus]) {
        if (!element) continue;
        element.classList.toggle("ready", state.ready);
        element.classList.toggle("not-ready", !state.ready);
        element.dataset.tooltip = state.reason;
        element.title = state.reason;
    }
    if (launcherReadyText) launcherReadyText.textContent = state.ready ? "READY" : "NOT READY";
    if (profileStatus) profileStatus.textContent = state.ready ? "READY" : "NOT READY";
    if (profileDescription) profileDescription.textContent = state.reason;
    if (quickLaunchButton) {
        quickLaunchButton.disabled = !state.ready || minecraftActionInProgress;
        quickLaunchButton.title = state.ready ? "Quick launch" : state.reason;
    }
}

function updateProfileUi() {
    const loggedIn = Boolean(microsoftSignedIn || minecraftProfile || cachedMicrosoftUsername);
    const name = getPrimaryAccountName();
    const secondary = getSecondaryAccountText();

    if (playerUsername) playerUsername.textContent = name;
    if (playerMeta) playerMeta.textContent = secondary;
    if (accountNameMini) accountNameMini.textContent = minecraftProfile?.name || "Account";
    if (dropdownAccountName) dropdownAccountName.textContent = name;
    if (dropdownAccountEmail) dropdownAccountEmail.textContent = minecraftProfile?.microsoftUsername || cachedMicrosoftUsername || secondary;

    if (loginButton) loginButton.hidden = loggedIn;
    if (accountArea) accountArea.hidden = !loggedIn;

    const skinUrl = minecraftProfile?.skinUrl || null;
    setSkinFace(playerSkin, skinUrl);
    setSkinFace(accountAvatar, skinUrl);
    setSkinFace(dropdownAvatar, skinUrl);
    syncSettingsAccountUi();

    if (!minecraftActionInProgress) setPlayText(minecraftProfile ? "PLAY" : "SIGN IN & PLAY");
    updateReadiness();
    renderInstances();
}

function openAccountDropdown() {
    if (!accountDropdown) return;
    accountDropdown.hidden = false;
    requestAnimationFrame(() => accountDropdown.classList.add("open"));
}

function closeAccountDropdown() {
    if (!accountDropdown) return;
    accountDropdown.classList.remove("open");
    setTimeout(() => {
        if (!accountDropdown.classList.contains("open")) accountDropdown.hidden = true;
    }, 170);
}

function renderLoaderOptions() {
    if (!loaderList) return;
    loaderList.innerHTML = "";
    for (const option of loaderOptions) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "version-option";
        if (option.value === selectedLoader.value) button.classList.add("selected");
        if (option.unavailable) {
            button.classList.add("unavailable");
            button.disabled = true;
            button.setAttribute("aria-disabled", "true");
        }

        const copy = document.createElement("span");
        copy.className = "loader-option-copy";
        const title = document.createElement("strong");
        title.textContent = option.label;
        const subtitle = document.createElement("small");
        subtitle.textContent = option.subtitle;
        copy.append(title, subtitle);
        button.appendChild(copy);

        button.addEventListener("click", event => {
            event.stopPropagation();
            if (option.unavailable) return;
            selectedLoader = option;
            if (loaderTriggerValue) loaderTriggerValue.textContent = option.label;
            const loaderSubtitle = document.getElementById("loaderTriggerSubtitle");
            if (loaderSubtitle) loaderSubtitle.textContent = option.subtitle;
            renderLoaderOptions();
            closeLoaderDropdown();
            addConsoleLog("Loader", `${option.label} selected`);
            updateReadiness();
            renderInstances();
            if (launchPreferences.discordRichPresence) void window.flintfix.updateDiscordLauncherPresence(getDiscordPresenceContext());
        });
        loaderList.appendChild(button);
    }
}

function closeLoaderDropdown() {
    if (!loaderDropdown || !loaderTrigger) return;
    loaderDropdown.classList.remove("open");
    loaderTrigger.setAttribute("aria-expanded", "false");
}



const flintSelectRegistry = new WeakMap();

function closeAllFlintSelects(except = null) {
    document.querySelectorAll(".flint-select.open").forEach(wrapper => {
        if (wrapper !== except) wrapper.classList.remove("open");
    });
}

function enhanceFlintSelect(select) {
    if (!select) return;
    if (flintSelectRegistry.has(select)) {
        refreshFlintSelect(select);
        return;
    }
    select.classList.add("native-select-hidden");
    const wrapper = document.createElement("div");
    wrapper.className = "flint-select";
    const trigger = document.createElement("button");
    trigger.type = "button";
    trigger.className = "flint-select-trigger selection-trigger";
    const value = document.createElement("span");
    value.className = "flint-select-value";
    const chevron = document.createElement("span");
    chevron.className = "selection-chevron material-symbols-rounded";
    chevron.textContent = "expand_more";
    trigger.append(value, chevron);
    const menu = document.createElement("div");
    menu.className = "flint-select-menu selection-menu";
    const list = document.createElement("div");
    list.className = "selection-list";
    menu.appendChild(list);
    wrapper.append(trigger, menu);
    select.insertAdjacentElement("afterend", wrapper);

    const api = { wrapper, trigger, value, list };
    flintSelectRegistry.set(select, api);

    trigger.addEventListener("click", event => {
        event.stopPropagation();
        if (select.disabled) return;
        const next = !wrapper.classList.contains("open");
        closeAllFlintSelects(wrapper);
        wrapper.classList.toggle("open", next);
        trigger.setAttribute("aria-expanded", String(next));
    });
    wrapper.addEventListener("click", event => event.stopPropagation());
    refreshFlintSelect(select);
}

function refreshFlintSelect(select) {
    const api = flintSelectRegistry.get(select);
    if (!api) return;
    const options = Array.from(select.options || []);
    const selected = options.find(option => option.value === select.value) || options[0] || null;
    api.value.textContent = selected?.textContent || "Choose...";
    api.trigger.disabled = Boolean(select.disabled);
    api.list.innerHTML = "";
    for (const option of options) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `version-option${option.value === select.value ? " selected" : ""}`;
        const title = document.createElement("strong");
        title.className = "version-number";
        title.textContent = option.textContent;
        button.appendChild(title);
        button.disabled = Boolean(option.disabled);
        button.addEventListener("click", () => {
            select.value = option.value;
            select.dispatchEvent(new Event("change", { bubbles: true }));
            refreshFlintSelect(select);
            api.wrapper.classList.remove("open");
        });
        api.list.appendChild(button);
    }
}

document.addEventListener("click", () => closeAllFlintSelects());

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function markdownToSafeHtml(source) {
    const lines = String(source || "").replace(/\r/g, "").split("\n");
    const output = [];
    let inCode = false;
    let codeBuffer = [];
    let inUl = false;
    let inOl = false;
    let paragraph = [];

    const renderInline = raw => {
        let text = escapeHtml(raw);
        const linkTokens = [];
        const imageTokens = [];

        // Images first so they are not consumed as normal links.
        text = text.replace(/!\[([^\]]*)\]\(((?:https?:\/\/)(?:[^()\s]+|\([^()\s]*\))+)\)/g, (_m, alt, url) => {
            const token = `@@FFIMG${imageTokens.length}@@`;
            imageTokens.push(`<img src="${url}" alt="${alt}" loading="lazy">`);
            return token;
        });

        // Markdown links, including URLs containing a simple pair of parentheses.
        text = text.replace(/\[([^\]]+)\]\(((?:https?:\/\/)(?:[^()\s]+|\([^()\s]*\))+)\)/g, (_m, label, url) => {
            const token = `@@FFLINK${linkTokens.length}@@`;
            linkTokens.push(`<a href="${url}" data-external-link="true">${label}</a>`);
            return token;
        });

        // Auto-link raw URLs.
        text = text.replace(/(^|\s)(https?:\/\/[^\s<]+)/g, (_m, prefix, url) => `${prefix}<a href="${url}" data-external-link="true">${url}</a>`);
        text = text.replace(/`([^`]+)`/g, "<code>$1</code>");
        text = text.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
        text = text.replace(/__([^_]+)__/g, "<strong>$1</strong>");
        text = text.replace(/~~([^~]+)~~/g, "<del>$1</del>");
        text = text.replace(/(^|[^*])\*([^*]+)\*/g, "$1<em>$2</em>");
        text = text.replace(/(^|[^_])_([^_]+)_/g, "$1<em>$2</em>");
        linkTokens.forEach((html, index) => { text = text.replace(`@@FFLINK${index}@@`, html); });
        imageTokens.forEach((html, index) => { text = text.replace(`@@FFIMG${index}@@`, html); });
        return text;
    };

    const flushParagraph = () => {
        if (!paragraph.length) return;
        output.push(`<p>${paragraph.join(" ")}</p>`);
        paragraph = [];
    };
    const closeLists = () => {
        if (inUl) output.push("</ul>");
        if (inOl) output.push("</ol>");
        inUl = false;
        inOl = false;
    };
    const flushCode = () => {
        output.push(`<pre><code>${escapeHtml(codeBuffer.join("\n"))}</code></pre>`);
        codeBuffer = [];
    };

    for (const rawLine of lines) {
        const trimmed = rawLine.trimEnd();
        if (/^```/.test(trimmed.trim())) {
            flushParagraph();
            closeLists();
            if (!inCode) {
                inCode = true;
                codeBuffer = [];
            } else {
                flushCode();
                inCode = false;
            }
            continue;
        }
        if (inCode) {
            codeBuffer.push(rawLine);
            continue;
        }
        if (!trimmed.trim()) {
            flushParagraph();
            closeLists();
            continue;
        }
        if (/^---+$/.test(trimmed.trim()) || /^\*\*\*+$/.test(trimmed.trim())) {
            flushParagraph();
            closeLists();
            output.push("<hr>");
            continue;
        }
        const heading = trimmed.match(/^(#{1,4})\s+(.+)$/);
        if (heading) {
            flushParagraph(); closeLists();
            const level = heading[1].length;
            output.push(`<h${level}>${renderInline(heading[2])}</h${level}>`);
            continue;
        }
        const ul = trimmed.match(/^\s*[-*+]\s+(.+)$/);
        if (ul) {
            flushParagraph();
            if (inOl) { output.push("</ol>"); inOl = false; }
            if (!inUl) { output.push("<ul>"); inUl = true; }
            output.push(`<li>${renderInline(ul[1])}</li>`);
            continue;
        }
        const ol = trimmed.match(/^\s*\d+[.)]\s+(.+)$/);
        if (ol) {
            flushParagraph();
            if (inUl) { output.push("</ul>"); inUl = false; }
            if (!inOl) { output.push("<ol>"); inOl = true; }
            output.push(`<li>${renderInline(ol[1])}</li>`);
            continue;
        }
        const quote = trimmed.match(/^>\s?(.*)$/);
        if (quote) {
            flushParagraph(); closeLists();
            output.push(`<blockquote>${renderInline(quote[1])}</blockquote>`);
            continue;
        }
        paragraph.push(renderInline(trimmed.trim()));
    }
    flushParagraph();
    closeLists();
    if (inCode) flushCode();
    return output.join("");
}

function sanitizeRichHtml(source) {
    const raw = String(source || "");
    if (!/<\/?[a-z][^>]*>/i.test(raw)) return markdownToSafeHtml(raw);
    const parser = new DOMParser();
    const doc = parser.parseFromString(raw, "text/html");
    const allowed = new Set(["P","BR","STRONG","B","EM","I","DEL","H1","H2","H3","H4","UL","OL","LI","A","IMG","CODE","PRE","BLOCKQUOTE","HR"]);
    const removeCompletely = new Set(["SCRIPT","STYLE","IFRAME","OBJECT","EMBED","FORM","INPUT","BUTTON","SVG"]);
    const nodes = Array.from(doc.body.querySelectorAll("*"));
    for (const node of nodes) {
        if (removeCompletely.has(node.tagName)) {
            node.remove();
            continue;
        }
        if (!allowed.has(node.tagName)) {
            node.replaceWith(...node.childNodes);
            continue;
        }
        const originalHref = node.tagName === "A" ? node.getAttribute("href") : null;
        const originalSrc = node.tagName === "IMG" ? node.getAttribute("src") : null;
        const originalAlt = node.tagName === "IMG" ? node.getAttribute("alt") : null;
        for (const attr of Array.from(node.attributes)) node.removeAttribute(attr.name);
        if (node.tagName === "A" && originalHref && /^https?:\/\//i.test(originalHref)) {
            node.setAttribute("href", originalHref);
            node.setAttribute("data-external-link", "true");
        }
        if (node.tagName === "IMG" && originalSrc && /^https?:\/\//i.test(originalSrc)) {
            node.setAttribute("src", originalSrc);
            node.setAttribute("alt", originalAlt || "");
            node.setAttribute("loading", "lazy");
        }
    }
    return doc.body.innerHTML;
}

function renderModDescription(source) {
    const raw = String(source || "");
    const looksLikeMarkdown = /(^|\n)#{1,4}\s|\*\*|__|\[[^\]]+\]\([^)]*\)|(^|\n)\s*[-*+]\s+|(^|\n)\s*\d+[.)]\s+/.test(raw);
    return looksLikeMarkdown ? markdownToSafeHtml(raw) : sanitizeRichHtml(raw);
}

function updatePageHeader(name) {
    const display = name === "packs" ? "Resource packs" : name.charAt(0).toUpperCase() + name.slice(1);
    if (pageName) pageName.textContent = display;
    if (pageIcon) {
        pageIcon.classList.add("material-symbols-rounded");
        pageIcon.textContent = pageHeaderIcons[name] || pageHeaderIcons.home;
    }
}

function getMinecraftVersionIds(limit = 80) {
    const ids = minecraftVersions
        .map(version => typeof version === "string" ? version : version?.id)
        .filter(Boolean);
    return Array.from(new Set(ids)).slice(0, limit);
}

function populateModsConfigControls(instance) {
    if (modsVersionSelect) {
        modsVersionSelect.innerHTML = "";
        const ids = getMinecraftVersionIds(80);
        const unique = Array.from(new Set([...ids, instance?.version, selectedVersion, latestRelease].filter(Boolean)));
        for (const versionId of unique) {
            const option = document.createElement("option");
            option.value = versionId;
            option.textContent = versionId;
            modsVersionSelect.appendChild(option);
        }
        if (instance?.version) modsVersionSelect.value = instance.version;
        enhanceFlintSelect(modsVersionSelect);
        refreshFlintSelect(modsVersionSelect);
    }
    if (modsLoaderSelect) {
        modsLoaderSelect.value = instance?.loader || "fabric";
        enhanceFlintSelect(modsLoaderSelect);
        refreshFlintSelect(modsLoaderSelect);
    }
    if (modsConfigSave) modsConfigSave.disabled = !instance;
}

function populateExploreTarget() {
    if (!exploreInstanceSelect) return;
    const previous = selectedModsInstanceId;
    exploreInstanceSelect.innerHTML = "";
    if (!instances.length) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "No instances available";
        exploreInstanceSelect.appendChild(option);
        exploreInstanceSelect.disabled = true;
        selectedModsInstanceId = null;
        enhanceFlintSelect(exploreInstanceSelect);
        refreshFlintSelect(exploreInstanceSelect);
        return;
    }
    exploreInstanceSelect.disabled = false;
    for (const instance of instances) {
        const option = document.createElement("option");
        option.value = instance.id;
        const loader = loaderOptions.find(item => item.value === instance.loader)?.label || instance.loader;
        option.textContent = `${instance.name} — ${instance.version} / ${loader}`;
        exploreInstanceSelect.appendChild(option);
    }
    const chosen = instances.some(item => item.id === previous) ? previous : (instances.find(item => item.loader === "fabric")?.id || instances[0].id);
    selectedModsInstanceId = chosen;
    exploreInstanceSelect.value = chosen;
    enhanceFlintSelect(exploreInstanceSelect);
    refreshFlintSelect(exploreInstanceSelect);
}

function syncExploreTargetCopy() {
    const instance = getSelectedModsInstance();
    if (exploreTargetName) exploreTargetName.textContent = instance ? instance.name : "No instance selected";
    if (exploreTargetMeta) {
        if (!instance) exploreTargetMeta.textContent = "Create an instance first.";
        else {
            const loader = loaderOptions.find(item => item.value === instance.loader)?.label || instance.loader;
            exploreTargetMeta.textContent = `Minecraft ${instance.version} • ${loader} • installs directly into this instance`;
        }
    }
}

function formatDownloadCount(value) {
    const number = Number(value) || 0;
    return new Intl.NumberFormat(undefined, { notation: number >= 1000 ? "compact" : "standard", maximumFractionDigits: 1 }).format(number);
}

function closeModDetails() {
    if (!modDetailsBackdrop) return;
    modDetailsBackdrop.classList.remove("open");
    setTimeout(() => {
        if (!modDetailsBackdrop.classList.contains("open")) modDetailsBackdrop.hidden = true;
    }, 180);
    activeModDetails = null;
}

async function installExploreProject(mod, button = null) {
    const instance = getSelectedModsInstance();
    if (!instance) {
        showToast("Choose an instance first.", "error");
        return false;
    }
    if (instance.loader !== "fabric") {
        showToast("Direct installs currently require a Fabric instance.", "error");
        return false;
    }
    if (button) {
        button.disabled = true;
        button.dataset.originalText = button.textContent;
        button.textContent = "Installing...";
    }
    const result = await window.flintfix.installCatalogMod(instance.id, {
        projectId: mod.projectId,
        version: instance.version,
        loader: instance.loader
    });
    if (!result?.success) {
        showToast(result?.error || "Could not install the mod.", "error");
        if (button) {
            button.disabled = false;
            button.textContent = button.dataset.originalText || "Install";
        }
        return false;
    }
    showToast(`${mod.title} installed to ${instance.name}.`, "success");
    modUpdateMap.clear();
    if (!modsPage?.hidden) await renderModsPage();
    if (button) button.textContent = "Installed";
    return true;
}

async function openModDetails(mod) {
    if (!modDetailsBackdrop || !mod) return;
    activeModDetails = mod;
    modDetailsBackdrop.hidden = false;
    requestAnimationFrame(() => modDetailsBackdrop.classList.add("open"));
    if (modDetailsTitle) modDetailsTitle.textContent = mod.title || "Mod";
    if (modDetailsSubtitle) modDetailsSubtitle.textContent = "Loading compatible versions and project information...";
    if (modDetailsBody) modDetailsBody.textContent = "Loading...";
    if (modDetailsDownloads) modDetailsDownloads.textContent = formatDownloadCount(mod.downloads);
    if (modDetailsFollowers) modDetailsFollowers.textContent = formatDownloadCount(mod.follows);
    if (modDetailsLicense) modDetailsLicense.textContent = "—";
    if (modDetailsCompatible) modDetailsCompatible.innerHTML = "";
    if (modDetailsIcon) {
        modDetailsIcon.innerHTML = "";
        if (mod.iconUrl) {
            const img = document.createElement("img");
            img.src = mod.iconUrl;
            img.alt = "";
            modDetailsIcon.appendChild(img);
        } else {
            modDetailsIcon.textContent = "M";
        }
    }
    const instance = getSelectedModsInstance();
    if (modDetailsInstall) {
        modDetailsInstall.disabled = !instance || instance.loader !== "fabric";
        modDetailsInstall.textContent = instance?.loader === "fabric" ? `Install to ${instance.name}` : "Select a Fabric instance";
    }
    const result = await window.flintfix.getCatalogModDetails(mod.projectId, {
        version: instance?.version || "",
        loader: instance?.loader || "fabric"
    });
    if (!result?.success) {
        if (modDetailsSubtitle) modDetailsSubtitle.textContent = result?.error || "Could not load details.";
        if (modDetailsBody) modDetailsBody.textContent = "Project details are unavailable right now.";
        return;
    }
    const details = result.details;
    activeModDetails = { ...mod, ...details };
    if (modDetailsTitle) modDetailsTitle.textContent = details.title;
    if (modDetailsSubtitle) modDetailsSubtitle.textContent = details.description || "Modrinth project";
    if (modDetailsBody) {
        const source = String(details.body || details.description || "No additional project description was provided.").slice(0, 24000);
        modDetailsBody.innerHTML = renderModDescription(source);
    }
    if (modDetailsDownloads) modDetailsDownloads.textContent = formatDownloadCount(details.downloads);
    if (modDetailsFollowers) modDetailsFollowers.textContent = formatDownloadCount(details.followers);
    if (modDetailsLicense) modDetailsLicense.textContent = details.license || "Unknown";
    if (modDetailsCompatible) {
        modDetailsCompatible.innerHTML = "";
        const heading = document.createElement("strong");
        heading.textContent = "Compatible releases";
        modDetailsCompatible.appendChild(heading);
        const versions = Array.isArray(details.compatibleVersions) ? details.compatibleVersions.slice(0, 6) : [];
        if (!versions.length) {
            const span = document.createElement("span");
            span.textContent = "No matching release found for the current instance filters.";
            modDetailsCompatible.appendChild(span);
        } else {
            for (const version of versions) {
                const badge = document.createElement("span");
                badge.className = "mod-version-badge";
                badge.textContent = version.versionNumber || version.name;
                modDetailsCompatible.appendChild(badge);
            }
        }
    }
}

function renderModsExploreGrid(items) {
    if (!modsExploreGrid) return;
    modsExploreGrid.innerHTML = "";
    if (!items.length) {
        const empty = document.createElement("div");
        empty.className = "mods-explore-empty";
        empty.textContent = modsExploreLoading ? "Loading curated mods..." : "No compatible mods found. Try another category or search.";
        modsExploreGrid.appendChild(empty);
        return;
    }
    const instance = getSelectedModsInstance();
    const canInstall = instance && instance.loader === "fabric";
    for (const mod of items) {
        const card = document.createElement("article");
        card.className = "explore-mod-card premium-card";
        card.tabIndex = 0;

        const top = document.createElement("div");
        top.className = "explore-mod-top";
        const icon = document.createElement("div");
        icon.className = "explore-mod-icon";
        if (mod.iconUrl) {
            const img = document.createElement("img");
            img.src = mod.iconUrl;
            img.alt = "";
            icon.appendChild(img);
        } else {
            icon.textContent = "M";
        }
        const copy = document.createElement("div");
        copy.className = "explore-mod-copy";
        const title = document.createElement("strong");
        title.textContent = mod.title;
        const author = document.createElement("small");
        author.textContent = mod.author ? `by ${mod.author}` : "Modrinth project";
        const desc = document.createElement("span");
        desc.textContent = mod.description || "Discover this mod on Modrinth.";
        copy.append(title, author, desc);
        top.append(icon, copy);

        const meta = document.createElement("div");
        meta.className = "explore-mod-meta";
        const downloads = document.createElement("span");
        downloads.textContent = `${formatDownloadCount(mod.downloads)} downloads`;
        meta.appendChild(downloads);
        for (const category of (mod.categories || []).filter(item => !["fabric", "forge", "neoforge", "quilt"].includes(item)).slice(0, 2)) {
            const tag = document.createElement("span");
            tag.textContent = category;
            meta.appendChild(tag);
        }

        const actions = document.createElement("div");
        actions.className = "explore-mod-actions";
        const details = document.createElement("button");
        details.type = "button";
        details.className = "explore-open-button";
        details.textContent = "Details";
        details.addEventListener("click", event => {
            event.stopPropagation();
            void openModDetails(mod);
        });
        const install = document.createElement("button");
        install.type = "button";
        install.className = "explore-install-button";
        install.textContent = canInstall ? "Install" : "Needs Fabric";
        install.disabled = !canInstall;
        install.addEventListener("click", event => {
            event.stopPropagation();
            void installExploreProject(mod, install);
        });
        actions.append(details, install);
        card.addEventListener("dblclick", () => void openModDetails(mod));
        card.append(top, meta, actions);
        modsExploreGrid.appendChild(card);
    }
}

function renderExplorePagination() {
    if (!explorePagination || !explorePageNumbers) return;
    const totalPages = Math.max(1, Math.ceil(exploreTotalHits / EXPLORE_PAGE_SIZE));
    const shouldShow = exploreTotalHits > EXPLORE_PAGE_SIZE;
    explorePagination.hidden = !shouldShow;
    if (!shouldShow) return;

    if (explorePrevPage) explorePrevPage.disabled = explorePageNumber <= 1;
    if (exploreNextPage) exploreNextPage.disabled = explorePageNumber >= totalPages;
    explorePageNumbers.innerHTML = "";

    const candidates = new Set([1, totalPages, explorePageNumber - 1, explorePageNumber, explorePageNumber + 1]);
    const pages = Array.from(candidates).filter(page => page >= 1 && page <= totalPages).sort((a, b) => a - b);
    let previous = 0;
    for (const page of pages) {
        if (previous && page - previous > 1) {
            const dots = document.createElement("span");
            dots.className = "explore-page-dots";
            dots.textContent = "…";
            explorePageNumbers.appendChild(dots);
        }
        const button = document.createElement("button");
        button.type = "button";
        button.className = `explore-page-number${page === explorePageNumber ? " active" : ""}`;
        button.textContent = String(page);
        button.addEventListener("click", () => {
            if (page === explorePageNumber) return;
            explorePageNumber = page;
            void renderExplorePage(true);
        });
        explorePageNumbers.appendChild(button);
        previous = page;
    }
}

async function renderExplorePage(force = false) {
    if (!modsExploreGrid || explorePage?.hidden) return;
    populateExploreTarget();
    syncExploreTargetCopy();
    const instance = getSelectedModsInstance();
    const query = String(modsExploreSearch?.value || "").trim();
    const index = String(exploreSortSelect?.value || "relevance");
    if (modsExploreStatus) {
        modsExploreStatus.textContent = instance
            ? `Compatible with Minecraft ${instance.version} • ${(loaderOptions.find(option => option.value === instance.loader)?.label || instance.loader)}${exploreCategory ? ` • ${exploreCategory}` : ""}`
            : "Create or select an instance to browse compatible mods.";
    }
    if (!instance) {
        modsExploreResults = [];
        exploreTotalHits = 0;
        explorePageNumber = 1;
        renderModsExploreGrid([]);
        renderExplorePagination();
        return;
    }
    if (!force && modsExploreResults.length && !query) {
        renderModsExploreGrid(modsExploreResults);
        renderExplorePagination();
        return;
    }
    modsExploreLoading = true;
    renderModsExploreGrid([]);
    const result = await window.flintfix.searchCatalogMods({
        query,
        version: instance.version,
        loader: instance.loader,
        category: exploreCategory,
        index,
        limit: EXPLORE_PAGE_SIZE,
        offset: (explorePageNumber - 1) * EXPLORE_PAGE_SIZE
    });
    modsExploreLoading = false;
    if (!result?.success) {
        modsExploreResults = [];
        exploreTotalHits = 0;
        if (modsExploreStatus) modsExploreStatus.textContent = result?.error || "Could not load Explore right now.";
        renderModsExploreGrid([]);
        renderExplorePagination();
        return;
    }
    modsExploreResults = Array.isArray(result.mods) ? result.mods : [];
    exploreTotalHits = Number(result.totalHits) || modsExploreResults.length;
    const totalPages = Math.max(1, Math.ceil(exploreTotalHits / EXPLORE_PAGE_SIZE));
    if (explorePageNumber > totalPages) explorePageNumber = totalPages;
    if (modsExploreStatus) {
        modsExploreStatus.textContent = `Page ${explorePageNumber} of ${totalPages} • ${formatDownloadCount(exploreTotalHits)} compatible mods`;
    }
    renderModsExploreGrid(modsExploreResults);
    renderExplorePagination();
}


function loadInstancesFromStorage() {
    try {
        const parsed = JSON.parse(localStorage.getItem("flintfix.instances.v1") || "[]");
        instances = Array.isArray(parsed) ? parsed.filter(item => item && item.id && item.version && item.loader) : [];
    } catch {
        instances = [];
    }
}

function saveInstances() {
    localStorage.setItem("flintfix.instances.v1", JSON.stringify(instances));
}

function createInstanceId() {
    return `ff-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function formatInstanceDate(value) {
    if (!value) return "Never played";
    try {
        return `Last played ${new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", year: "numeric" }).format(new Date(value))}`;
    } catch {
        return "Played before";
    }
}

function getInstanceStatus(instance) {
    if (runningInstanceId === instance.id) {
        return { key: "in-game", label: "IN GAME", reason: "Minecraft is currently running from this instance." };
    }
    if (!minecraftProfile) {
        return { key: "not-ready", label: "NOT READY", reason: "Sign in to a Minecraft account." };
    }
    if (!instance.version) {
        return { key: "not-ready", label: "NOT READY", reason: "This instance has no Minecraft version selected." };
    }
    if (instance.loader !== "vanilla" && instance.loader !== "fabric") {
        const label = loaderOptions.find(option => option.value === instance.loader)?.label || instance.loader;
        return { key: "not-ready", label: "NOT READY", reason: `${label} support is not available yet.` };
    }
    if (instance.version === selectedVersion && instance.loader === selectedLoader.value) {
        const current = getReadiness();
        if (!current.ready) return { key: "not-ready", label: "NOT READY", reason: current.reason };
    }
    return { key: "ready", label: "READY", reason: "Ready to prepare and launch." };
}

function findMatchingInstance() {
    return instances.find(instance => instance.version === selectedVersion && instance.loader === selectedLoader.value) || null;
}

function ensureDefaultInstance() {
    if (instances.length || !selectedVersion) return;
    const now = new Date().toISOString();
    instances.push({
        id: createInstanceId(),
        name: "FlintFix Default",
        version: selectedVersion,
        loader: selectedLoader.value,
        color: "#7d6cf2",
        createdAt: now,
        lastPlayedAt: null
    });
    saveInstances();
}

function updateInstanceSummary() {
    const statuses = instances.map(getInstanceStatus);
    const inGame = statuses.filter(status => status.key === "in-game").length;
    const ready = statuses.filter(status => status.key === "ready").length;
    const notReady = statuses.filter(status => status.key === "not-ready").length;
    if (instanceTotalCount) instanceTotalCount.textContent = String(instances.length);
    if (instanceInGameCount) instanceInGameCount.textContent = String(inGame);
    if (instanceReadyCount) instanceReadyCount.textContent = String(ready);
    if (instanceNotReadyCount) instanceNotReadyCount.textContent = String(notReady);
}

function renderInstances() {
    if (!instancesGrid) return;
    const query = String(instancesSearch?.value || "").trim().toLowerCase();
    const filtered = instances.filter(instance => {
        if (!query) return true;
        const loaderLabel = loaderOptions.find(option => option.value === instance.loader)?.label || instance.loader;
        return `${instance.name} ${instance.version} ${loaderLabel}`.toLowerCase().includes(query);
    });

    instancesGrid.innerHTML = "";
    if (instancesEmpty) instancesEmpty.hidden = instances.length > 0;
    instancesGrid.hidden = instances.length === 0;

    for (const instance of filtered) {
        const status = getInstanceStatus(instance);
        const loader = loaderOptions.find(option => option.value === instance.loader) || { label: instance.loader };
        const card = document.createElement("article");
        card.className = `instance-card status-${status.key}`;
        card.dataset.instanceId = instance.id;
        card.style.setProperty("--instance-accent", instance.color || "#7d6cf2");

        const header = document.createElement("div");
        header.className = "instance-card-header";
        const icon = document.createElement("div");
        icon.className = "instance-card-icon";
        icon.textContent = instance.loader === "fabric" ? "F" : "M";
        const heading = document.createElement("div");
        heading.className = "instance-card-heading";
        const title = document.createElement("h2");
        title.textContent = instance.name;
        const subtitle = document.createElement("p");
        subtitle.textContent = `Minecraft ${instance.version} • ${loader.label}`;
        heading.append(title, subtitle);
        const badge = document.createElement("span");
        badge.className = `instance-status ${status.key}`;
        badge.textContent = status.label;
        badge.title = status.reason;
        header.append(icon, heading, badge);

        const details = document.createElement("div");
        details.className = "instance-details";
        details.innerHTML = `
            <div><span>Account</span><strong>${minecraftProfile?.name || "Not signed in"}</strong></div>
            <div><span>Loader</span><strong>${loader.label}</strong></div>
            <div><span>Activity</span><strong>${formatInstanceDate(instance.lastPlayedAt)}</strong></div>
        `;

        const footer = document.createElement("div");
        footer.className = "instance-card-footer";
        const reason = document.createElement("span");
        reason.className = "instance-status-reason";
        reason.textContent = status.reason;
        const actions = document.createElement("div");
        actions.className = "instance-actions";

        const folderButton = document.createElement("button");
        folderButton.type = "button";
        folderButton.className = "instance-icon-button";
        folderButton.title = "Open instance folder";
        folderButton.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">folder_open</span>';
        folderButton.addEventListener("click", async () => {
            const result = await window.flintfix.openInstanceFolder(instance.id);
            if (!result?.success) showToast(result?.error || "Could not open the instance folder.", "error");
        });

        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.className = "instance-secondary-action";
        editButton.textContent = "Edit";
        editButton.addEventListener("click", () => openInstanceModal(instance));

        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.className = "instance-delete-button";
        deleteButton.title = status.key === "in-game" ? "Stop Minecraft before deleting this instance" : "Delete instance";
        deleteButton.disabled = status.key === "in-game";
        deleteButton.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">delete</span>';
        deleteButton.addEventListener("click", () => openDeleteInstanceModal(instance));

        const play = document.createElement("button");
        play.type = "button";
        play.className = "instance-play-action";
        play.textContent = status.key === "in-game" ? "Running" : "Play";
        play.disabled = status.key === "in-game" || minecraftActionInProgress;
        play.addEventListener("click", () => launchInstance(instance));

        actions.append(folderButton, editButton, deleteButton, play);
        footer.append(reason, actions);
        card.append(header, details, footer);
        instancesGrid.appendChild(card);
    }

    if (instances.length && filtered.length === 0) {
        const noResults = document.createElement("div");
        noResults.className = "instances-no-results";
        noResults.textContent = "No instances match your search.";
        instancesGrid.appendChild(noResults);
    }
    updateInstanceSummary();
}

function openInstanceModal(instance = null) {
    if (!instanceModalBackdrop) return;
    editingInstanceId = instance?.id || null;
    instanceDraftVersion = instance?.version || selectedVersion;
    instanceDraftLoader = instance?.loader || selectedLoader.value;
    instanceDraftColor = instance?.color || "#7d6cf2";
    instanceColorSwatches.forEach(button => button.classList.toggle("active", button.dataset.instanceColor === instanceDraftColor));
    if (instanceModalTitle) instanceModalTitle.textContent = instance ? "Edit instance" : "Create instance";
    if (instanceModalSubtitle) instanceModalSubtitle.textContent = instance ? "Rename this instance or replace its setup with your current Home configuration." : "Save the current Home configuration as a reusable instance.";
    if (instanceNameInput) instanceNameInput.value = instance?.name || `Minecraft ${selectedVersion || "Instance"}`;
    if (instancePreviewVersion) instancePreviewVersion.textContent = instanceDraftVersion || "Choose a version on Home";
    if (instancePreviewLoader) instancePreviewLoader.textContent = (loaderOptions.find(option => option.value === instanceDraftLoader) || selectedLoader).label;
    if (instanceModalNote) instanceModalNote.textContent = instance ? "You can keep this setup or replace it with the current Home version and loader." : "To change version or loader, configure it on Home first.";
    if (instanceUseCurrentButton) instanceUseCurrentButton.hidden = !instance;
    if (instanceSaveButton) instanceSaveButton.textContent = instance ? "Save changes" : "Create instance";
    instanceModalBackdrop.hidden = false;
    requestAnimationFrame(() => instanceModalBackdrop.classList.add("open"));
    setTimeout(() => instanceNameInput?.focus(), 120);
}

function closeInstanceModal() {
    if (!instanceModalBackdrop) return;
    instanceModalBackdrop.classList.remove("open");
    setTimeout(() => {
        if (!instanceModalBackdrop.classList.contains("open")) instanceModalBackdrop.hidden = true;
    }, 180);
}

function saveInstanceFromModal() {
    const name = String(instanceNameInput?.value || "").trim();
    if (!name) {
        showToast("Give the instance a name first.", "error");
        return;
    }
    if (!instanceDraftVersion && !editingInstanceId) {
        showToast("Choose a Minecraft version on Home first.", "error");
        return;
    }

    const existing = instances.find(instance => instance.id === editingInstanceId);
    if (existing) {
        existing.name = name;
        existing.version = instanceDraftVersion || existing.version;
        existing.loader = instanceDraftLoader || existing.loader;
        existing.color = instanceDraftColor || existing.color || "#7d6cf2";
    } else {
        instances.unshift({
            id: createInstanceId(),
            name,
            version: instanceDraftVersion,
            loader: instanceDraftLoader || selectedLoader.value,
            color: instanceDraftColor || "#7d6cf2",
            createdAt: new Date().toISOString(),
            lastPlayedAt: null
        });
    }
    saveInstances();
    closeInstanceModal();
    renderInstances();
    if (!modsPage?.hidden) void renderModsPage();
    showToast(existing ? "Instance updated." : "Instance created.", "success");
}

function openDeleteInstanceModal(instance) {
    if (!instance || !deleteInstanceBackdrop) return;
    pendingDeleteInstanceId = instance.id;
    if (deleteInstanceTitle) deleteInstanceTitle.textContent = `Delete ${instance.name}?`;
    if (deleteInstanceDescription) {
        deleteInstanceDescription.textContent = "This removes the instance from FlintFix and deletes its FlintFix-managed mod files. This action cannot be undone.";
    }
    deleteInstanceBackdrop.hidden = false;
    requestAnimationFrame(() => deleteInstanceBackdrop.classList.add("open"));
}

function closeDeleteInstanceModal() {
    if (!deleteInstanceBackdrop) return;
    deleteInstanceBackdrop.classList.remove("open");
    setTimeout(() => {
        if (!deleteInstanceBackdrop.classList.contains("open")) deleteInstanceBackdrop.hidden = true;
    }, 180);
}

async function deletePendingInstance() {
    const id = pendingDeleteInstanceId;
    const instance = instances.find(item => item.id === id);
    if (!id || !instance) {
        closeDeleteInstanceModal();
        return;
    }
    if (runningInstanceId === id) {
        showToast("You cannot delete an instance while it is running.", "error");
        return;
    }
    if (deleteInstanceConfirm) {
        deleteInstanceConfirm.disabled = true;
        deleteInstanceConfirm.textContent = "Deleting...";
    }
    try {
        const result = await window.flintfix.deleteInstanceData(id);
        if (!result?.success) throw new Error(result?.error || "Could not delete the instance files.");
        instances = instances.filter(item => item.id !== id);
        if (activeInstanceId === id) activeInstanceId = null;
        if (selectedModsInstanceId === id) selectedModsInstanceId = instances[0]?.id || null;
        saveInstances();
        closeDeleteInstanceModal();
        renderInstances();
        if (!modsPage?.hidden) await renderModsPage();
        showToast(`${instance.name} deleted.`, "success");
    } catch (error) {
        showToast(error.message || "Could not delete the instance.", "error");
    } finally {
        if (deleteInstanceConfirm) {
            deleteInstanceConfirm.disabled = false;
            deleteInstanceConfirm.textContent = "Delete instance";
        }
        pendingDeleteInstanceId = null;
    }
}

function formatModSize(bytes) {
    const value = Number(bytes) || 0;
    if (value < 1024) return `${value} B`;
    if (value < 1024 * 1024) return `${(value / 1024).toFixed(value >= 10240 ? 0 : 1)} KB`;
    return `${(value / (1024 * 1024)).toFixed(value >= 10 * 1024 * 1024 ? 0 : 1)} MB`;
}

function getSelectedModsInstance() {
    let instance = instances.find(item => item.id === selectedModsInstanceId) || null;
    if (!instance && instances.length) {
        const fabric = instances.find(item => item.loader === "fabric");
        instance = fabric || instances[0];
        selectedModsInstanceId = instance.id;
    }
    return instance;
}

function renderModsInstancePicker() {
    if (!modsInstancePicker) return;
    modsInstancePicker.innerHTML = "";
    if (!instances.length) {
        const empty = document.createElement("span");
        empty.className = "instances-hint";
        empty.textContent = "Create an instance first.";
        modsInstancePicker.appendChild(empty);
        return;
    }
    for (const instance of instances) {
        const loader = loaderOptions.find(option => option.value === instance.loader) || { label: instance.loader };
        const button = document.createElement("button");
        button.type = "button";
        button.className = `mods-instance-option${selectedModsInstanceId === instance.id ? " active" : ""}`;
        const icon = document.createElement("span");
        icon.className = "mods-instance-option-icon";
        icon.textContent = instance.loader === "fabric" ? "F" : "M";
        const copy = document.createElement("span");
        copy.className = "mods-instance-option-copy";
        const title = document.createElement("strong");
        title.textContent = instance.name;
        const meta = document.createElement("span");
        meta.textContent = `${instance.version} • ${loader.label}`;
        copy.append(title, meta);
        button.append(icon, copy);
        button.addEventListener("click", async () => {
            selectedModsInstanceId = instance.id;
            modUpdateMap.clear();
            modsExploreResults = [];
            await renderModsPage();
            populateExploreTarget();
            syncExploreTargetCopy();
        });
        modsInstancePicker.appendChild(button);
    }
}

function renderModsList(instance) {
    if (!modsList || !modsEmpty) return;
    const query = String(modsSearch?.value || "").trim().toLowerCase();
    const filtered = currentInstanceMods.filter(mod => {
        if (!query) return true;
        return `${mod.displayName} ${mod.displayFileName}`.toLowerCase().includes(query);
    });
    modsList.innerHTML = "";
    modsEmpty.hidden = currentInstanceMods.length > 0;
    modsList.hidden = currentInstanceMods.length === 0;

    for (const mod of filtered) {
        const updateInfo = modUpdateMap.get(mod.fileName)?.update || null;
        const row = document.createElement("article");
        row.className = `mod-row${mod.enabled ? "" : " disabled"}${updateInfo?.updateAvailable ? " has-update" : ""}`;

        const icon = document.createElement("div");
        icon.className = "mod-icon";
        icon.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">extension</span>';

        const copy = document.createElement("div");
        copy.className = "mod-copy";
        const titleLine = document.createElement("div");
        titleLine.className = "mod-title-line";
        const name = document.createElement("strong");
        name.textContent = mod.displayName;
        titleLine.appendChild(name);
        if (updateInfo?.updateAvailable) {
            const badge = document.createElement("span");
            badge.className = "mod-update-badge";
            badge.textContent = `Update ${updateInfo.versionNumber || "available"}`;
            titleLine.appendChild(badge);
        }
        const file = document.createElement("span");
        file.textContent = mod.displayFileName;
        const meta = document.createElement("small");
        meta.textContent = `${formatModSize(mod.size)} • ${mod.enabled ? "Loaded when this instance starts" : "Disabled"}`;
        copy.append(titleLine, file, meta);

        const state = document.createElement("span");
        state.className = `mod-state ${mod.enabled ? "enabled" : "disabled"}`;
        state.textContent = mod.enabled ? "ENABLED" : "DISABLED";

        const actions = document.createElement("div");
        actions.className = "mod-actions";
        if (updateInfo?.updateAvailable) {
            const update = document.createElement("button");
            update.type = "button";
            update.className = "mod-update-action";
            update.textContent = "Update";
            update.addEventListener("click", async () => {
                update.disabled = true;
                update.textContent = "Updating...";
                const result = await window.flintfix.updateInstanceMod(instance.id, mod.fileName, {
                    version: instance.version,
                    loader: instance.loader
                });
                if (!result?.success) {
                    showToast(result?.error || "Could not update this mod.", "error");
                    update.disabled = false;
                    update.textContent = "Update";
                    return;
                }
                showToast(result.updated ? `${mod.displayName} updated.` : `${mod.displayName} is already current.`, "success");
                modUpdateMap.clear();
                await renderModsPage();
            });
            actions.appendChild(update);
        }

        const toggle = document.createElement("button");
        toggle.type = "button";
        toggle.className = "mod-toggle-button";
        toggle.textContent = mod.enabled ? "Disable" : "Enable";
        toggle.addEventListener("click", async () => {
            toggle.disabled = true;
            const result = await window.flintfix.toggleInstanceMod(instance.id, mod.fileName, !mod.enabled);
            if (!result?.success) showToast(result?.error || "Could not change the mod state.", "error");
            modUpdateMap.clear();
            await renderModsPage();
        });

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "mod-remove-button";
        remove.textContent = "Remove";
        remove.addEventListener("click", async () => {
            remove.disabled = true;
            const result = await window.flintfix.removeInstanceMod(instance.id, mod.fileName);
            if (!result?.success) showToast(result?.error || "Could not remove the mod.", "error");
            else showToast(`${mod.displayName} removed.`, "success");
            modUpdateMap.clear();
            await renderModsPage();
        });

        actions.append(toggle, remove);
        row.append(icon, copy, state, actions);
        modsList.appendChild(row);
    }

    if (currentInstanceMods.length && filtered.length === 0) {
        const noResults = document.createElement("div");
        noResults.className = "instances-no-results";
        noResults.textContent = "No installed mods match your search.";
        modsList.appendChild(noResults);
    }
}

async function checkSelectedInstanceModUpdates() {
    const instance = getSelectedModsInstance();
    if (!instance || !currentInstanceMods.length) {
        showToast("There are no installed mods to check.", "error");
        return;
    }
    if (checkModUpdatesButton) {
        checkModUpdatesButton.disabled = true;
        checkModUpdatesButton.textContent = "Checking...";
    }
    const result = await window.flintfix.checkInstanceModUpdates(instance.id, {
        version: instance.version,
        loader: instance.loader
    });
    if (checkModUpdatesButton) {
        checkModUpdatesButton.disabled = false;
        checkModUpdatesButton.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">update</span>Check updates';
    }
    if (!result?.success) {
        showToast(result?.error || "Could not check for mod updates.", "error");
        return;
    }
    modUpdateMap = new Map((result.updates || []).map(item => [item.fileName, item]));
    const count = Array.from(modUpdateMap.values()).filter(item => item.update?.updateAvailable).length;
    showToast(count ? `${count} mod update${count === 1 ? "" : "s"} available.` : "All recognized mods are up to date.", "success");
    renderModsList(instance);
}

async function renderModsPage() {
    const instance = getSelectedModsInstance();
    renderModsInstancePicker();
    currentInstanceMods = [];
    populateModsConfigControls(instance);

    if (!instance) {
        if (modsTargetName) modsTargetName.textContent = "No instances";
        if (modsTargetMeta) modsTargetMeta.textContent = "Create an instance before adding mods.";
        if (addModButton) addModButton.disabled = true;
        if (modsEmptyAdd) modsEmptyAdd.disabled = true;
        if (openModsFolderButton) openModsFolderButton.disabled = true;
        if (checkModUpdatesButton) checkModUpdatesButton.disabled = true;
        if (modsNotice) {
            modsNotice.hidden = false;
            modsNotice.textContent = "Create an instance first, then choose Fabric to manage mods.";
        }
        renderModsList(null);
        return;
    }

    const loader = loaderOptions.find(option => option.value === instance.loader) || { label: instance.loader };
    const supportsMods = instance.loader === "fabric";
    if (modsTargetName) modsTargetName.textContent = instance.name;
    if (modsTargetMeta) modsTargetMeta.textContent = `Minecraft ${instance.version} • ${loader.label}`;
    if (addModButton) addModButton.disabled = !supportsMods;
    if (modsEmptyAdd) modsEmptyAdd.disabled = !supportsMods;
    if (openModsFolderButton) openModsFolderButton.disabled = false;
    if (checkModUpdatesButton) checkModUpdatesButton.disabled = !supportsMods;
    if (modsNotice) {
        modsNotice.hidden = supportsMods;
        modsNotice.textContent = supportsMods ? "" : "This instance uses Vanilla. Switch it to Fabric before adding mods.";
    }

    modsLoading = true;
    const result = await window.flintfix.listInstanceMods(instance.id);
    modsLoading = false;
    if (!result?.success) {
        showToast(result?.error || "Could not load instance mods.", "error");
        currentInstanceMods = [];
    } else {
        currentInstanceMods = Array.isArray(result.mods) ? result.mods : [];
    }
    renderModsInstancePicker();
    renderModsList(instance);
}

async function addModsToSelectedInstance() {
    const instance = getSelectedModsInstance();
    if (!instance) {
        showToast("Create an instance first.", "error");
        return;
    }
    if (instance.loader !== "fabric") {
        showToast("Mods are currently supported on Fabric instances.", "error");
        return;
    }
    const result = await window.flintfix.addInstanceMods(instance.id);
    if (!result?.success) {
        showToast(result?.error || "Could not add the selected mod.", "error");
        return;
    }
    if (!result.canceled && result.added?.length) {
        showToast(`${result.added.length} mod${result.added.length === 1 ? "" : "s"} added to ${instance.name}.`, "success");
    }
    await renderModsPage();
}

async function launchInstance(instance) {
    if (!instance || minecraftActionInProgress) return;
    activeInstanceId = instance.id;
    const loader = loaderOptions.find(option => option.value === instance.loader) || loaderOptions[0];
    selectedLoader = loader;
    if (loaderTriggerValue) loaderTriggerValue.textContent = loader.label;
    const loaderSubtitle = document.getElementById("loaderTriggerSubtitle");
    if (loaderSubtitle) loaderSubtitle.textContent = loader.subtitle;
    renderLoaderOptions();
    await selectVersion(instance.version);
    updateReadiness();
    renderInstances();

    if (!getUsableJava()) {
        setActiveNavigation("home");
        showToast(`Java ${requiredJava || "runtime"} needs attention before this instance can launch.`, "error");
        return;
    }
    playButton?.click();
}

loadInstancesFromStorage();
updatePageHeader("home");

function setActiveNavigation(name) {
    const allowed = new Set(["home", "instances", "mods", "explore", "packs", "servers", "skins", "chat"]);
    const page = allowed.has(name) ? name : "home";
    activeContentPage = page;
    navItems.forEach(item => item.classList.toggle("active", item.dataset.nav === name));
    appPages.forEach(element => {
        const active = element.dataset.page === page;
        const wasActive = !element.hidden && element.classList.contains("active");
        element.classList.toggle("active", active);
        element.hidden = !active;
        if (active && !wasActive) {
            element.classList.remove("page-enter");
            void element.offsetWidth;
            element.classList.add("page-enter");
        }
    });
    updatePageHeader(name);
    if (page === "instances") renderInstances();
    if (page === "mods") void renderModsPage();
    if (page === "explore") void renderExplorePage(true);
    if (page === "packs") void renderPacksPage();
    if (page === "servers") void renderServersPage();
    if (page === "skins") void renderSkinsPage();
    if (page === "chat") {
        renderChatPage();
        void refreshSocial({ force: true, suppressNotifications: true });
    }
}

function openAccountModal() {
    closeAccountDropdown();
    if (!accountModalBackdrop) return;
    accountModalBackdrop.hidden = false;
    requestAnimationFrame(() => accountModalBackdrop.classList.add("open"));
    loadAccountList();
}

function closeAccountModal() {
    if (!accountModalBackdrop) return;
    accountModalBackdrop.classList.remove("open");
    setTimeout(() => {
        if (!accountModalBackdrop.classList.contains("open")) accountModalBackdrop.hidden = true;
    }, 180);
}

async function loadAccountList() {
    if (!accountList) return;
    accountList.innerHTML = '<div class="account-list-loading">Loading connected accounts...</div>';
    const result = await window.flintfix.listMicrosoftAccounts();
    if (!result.success) {
        accountList.innerHTML = `<div class="account-list-loading">${result.error || "Could not load accounts."}</div>`;
        return;
    }

    accountList.innerHTML = "";
    if (!result.accounts.length) {
        accountList.innerHTML = '<div class="account-list-loading">No connected accounts yet.</div>';
        return;
    }

    for (const account of result.accounts) {
        const card = document.createElement("div");
        card.className = "account-list-item";
        const face = document.createElement("span");
        face.className = "skin-face account-list-face";
        setSkinFace(face, account.minecraft?.skinUrl || null);

        const copy = document.createElement("div");
        copy.className = "account-list-copy";
        const name = document.createElement("strong");
        name.textContent = account.minecraft?.username || account.microsoftUsername || "Microsoft account";
        const email = document.createElement("span");
        email.textContent = account.microsoftUsername || (account.needsReauth ? "Sign in again required" : "Minecraft account");
        copy.append(name, email);

        const action = document.createElement("button");
        action.type = "button";
        const active = Boolean(result.activeHomeAccountId && result.activeHomeAccountId === account.homeAccountId);
        action.className = `account-change-button${active ? " active" : ""}`;
        action.textContent = active ? "Active" : (account.needsReauth ? "Sign in" : "Change");
        action.disabled = active;
        action.addEventListener("click", async () => {
            if (account.needsReauth) {
                await addAnotherAccount();
                return;
            }
            action.disabled = true;
            action.textContent = "Changing...";
            const switched = await window.flintfix.switchMicrosoftAccount(account.homeAccountId);
            if (!switched.success) {
                action.disabled = false;
                action.textContent = "Retry";
                showToast(switched.error || "Could not change account.", "error");
                return;
            }
            minecraftProfile = switched.profile;
            cachedMicrosoftUsername = switched.profile.microsoftUsername || cachedMicrosoftUsername;
            microsoftSignedIn = true;
            stopDiscordLinkPolling();
            updateProfileUi();
            void refreshDiscordLinkStatus({ silent: true });
            closeAccountModal();
            showToast(`Changed to ${switched.profile.name}.`, "success");
            addConsoleLog("Minecraft", `Active account changed to ${switched.profile.name}`);
        });

        card.append(face, copy, action);
        accountList.appendChild(card);
    }
}

async function addAnotherAccount() {
    if (addAccountButton) addAccountButton.disabled = true;
    try {
        const profile = await signIn(true);
        closeAccountModal();
        showToast(`Minecraft account successfully connected — ${profile.name}.`, "success");
    } catch (error) {
        showToast(error.message || "Could not add account.", "error");
    } finally {
        if (addAccountButton) addAccountButton.disabled = false;
    }
}

if (loaderTrigger) {
    loaderTrigger.addEventListener("click", event => {
        event.stopPropagation();
        const open = loaderDropdown.classList.toggle("open");
        loaderTrigger.setAttribute("aria-expanded", String(open));
        dropdown.classList.remove("open");
        trigger.setAttribute("aria-expanded", "false");
        closeAccountDropdown();
        closeAllFlintSelects();
    });
}
if (loaderDropdown) loaderDropdown.addEventListener("click", event => event.stopPropagation());
renderLoaderOptions();

if (sidebarToggle && sidebar) sidebarToggle.addEventListener("click", () => sidebar.classList.toggle("expanded"));
if (brandHomeButton) brandHomeButton.addEventListener("click", () => setActiveNavigation("home"));

navItems.forEach(item => item.addEventListener("click", () => {
    const name = item.dataset.nav || "home";
    if (name === "settings") {
        navItems.forEach(nav => nav.classList.toggle("active", nav.dataset.nav === "settings"));
        updatePageHeader("settings");
        openSettingsModal("game");
        return;
    }
    setActiveNavigation(name);
}));

if (chatSettingsButton) chatSettingsButton.addEventListener("click", openChatSettings);
if (chatSettingsClose) chatSettingsClose.addEventListener("click", closeChatSettings);
if (chatLauncherNotifications) chatLauncherNotifications.addEventListener("change", () => {
    chatPreferences.launcherNotifications = chatLauncherNotifications.checked;
    saveChatPreferences();
});
if (chatInGameNotifications) chatInGameNotifications.addEventListener("change", () => {
    chatPreferences.inGameNotifications = chatInGameNotifications.checked;
    saveChatPreferences();
});
if (chatShowServer) chatShowServer.addEventListener("change", () => {
    chatPreferences.showServer = chatShowServer.checked;
    saveChatPreferences();
    void refreshSocial({ force: true, suppressNotifications: true });
});
if (chatAddFriendButton) chatAddFriendButton.addEventListener("click", () => void addFriendByUsername());
if (chatUserSearch) chatUserSearch.addEventListener("keydown", event => {
    if (event.key !== "Enter") return;
    event.preventDefault();
    void addFriendByUsername();
});
if (chatComposer) chatComposer.addEventListener("submit", async event => {
    event.preventDefault();
    const content = String(chatMessageInput?.value || "").trim();
    if (!content || !selectedChatFriendUuid) return;
    if (chatSendButton) chatSendButton.disabled = true;
    const result = await window.flintfix.sendSocialMessage(selectedChatFriendUuid, content);
    if (chatSendButton) chatSendButton.disabled = false;
    if (!result?.success) {
        showToast(result?.error || "Could not send message.", "error");
        return;
    }
    if (chatMessageInput) chatMessageInput.value = "";
    currentChatMessages.push(result.message);
    renderChatMessages();
    await refreshSocial({ force: true, suppressNotifications: true });
});
if (chatRemoveFriend) chatRemoveFriend.addEventListener("click", async () => {
    const friend = getSocialFriend(selectedChatFriendUuid);
    if (!friend) return;
    const confirmed = confirm(`Remove ${friend.minecraft?.name || "this friend"} from your FlintFix friends?`);
    if (!confirmed) return;
    const result = await window.flintfix.removeSocialFriend(selectedChatFriendUuid);
    if (!result?.success) {
        showToast(result?.error || "Could not remove friend.", "error");
        return;
    }
    selectedChatFriendUuid = null;
    currentChatMessages = [];
    await refreshSocial({ force: true, suppressNotifications: true });
});
syncChatPreferenceControls();
startSocialPolling();

if (createInstanceButton) createInstanceButton.addEventListener("click", () => openInstanceModal());
if (instancesEmptyCreate) instancesEmptyCreate.addEventListener("click", () => openInstanceModal());
if (instancesSearch) instancesSearch.addEventListener("input", renderInstances);
if (instanceModalClose) instanceModalClose.addEventListener("click", closeInstanceModal);
if (instanceModalBackdrop) instanceModalBackdrop.addEventListener("click", event => {
    if (event.target === instanceModalBackdrop) closeInstanceModal();
});
if (instanceSaveButton) instanceSaveButton.addEventListener("click", saveInstanceFromModal);
instanceColorSwatches.forEach(button => button.addEventListener("click", () => {
    instanceDraftColor = button.dataset.instanceColor || "#7d6cf2";
    instanceColorSwatches.forEach(item => item.classList.toggle("active", item === button));
}));
if (deleteInstanceCancel) deleteInstanceCancel.addEventListener("click", closeDeleteInstanceModal);
if (deleteInstanceBackdrop) deleteInstanceBackdrop.addEventListener("click", event => {
    if (event.target === deleteInstanceBackdrop) closeDeleteInstanceModal();
});
if (deleteInstanceConfirm) deleteInstanceConfirm.addEventListener("click", deletePendingInstance);
if (modsSearch) modsSearch.addEventListener("input", () => renderModsList(getSelectedModsInstance()));
if (addModButton) addModButton.addEventListener("click", addModsToSelectedInstance);
if (modsEmptyAdd) modsEmptyAdd.addEventListener("click", addModsToSelectedInstance);
if (openModsFolderButton) openModsFolderButton.addEventListener("click", async () => {
    const instance = getSelectedModsInstance();
    if (!instance) return;
    const result = await window.flintfix.openInstanceModsFolder(instance.id);
    if (!result?.success) showToast(result?.error || "Could not open the mods folder.", "error");
});
if (quickLaunchButton) quickLaunchButton.addEventListener("click", () => playButton?.click());
if (checkModUpdatesButton) checkModUpdatesButton.addEventListener("click", checkSelectedInstanceModUpdates);
if (modsExploreSearch) {
    let exploreSearchTimer = null;
    modsExploreSearch.addEventListener("input", () => {
        clearTimeout(exploreSearchTimer);
        exploreSearchTimer = setTimeout(() => {
            explorePageNumber = 1;
            void renderExplorePage(true);
        }, 280);
    });
}
if (modsExploreRefresh) modsExploreRefresh.addEventListener("click", () => void renderExplorePage(true));
if (explorePrevPage) explorePrevPage.addEventListener("click", () => {
    if (explorePageNumber <= 1) return;
    explorePageNumber -= 1;
    void renderExplorePage(true);
});
if (exploreNextPage) exploreNextPage.addEventListener("click", () => {
    const totalPages = Math.max(1, Math.ceil(exploreTotalHits / EXPLORE_PAGE_SIZE));
    if (explorePageNumber >= totalPages) return;
    explorePageNumber += 1;
    void renderExplorePage(true);
});
if (exploreSortSelect) exploreSortSelect.addEventListener("change", () => {
    explorePageNumber = 1;
    void renderExplorePage(true);
});
if (exploreInstanceSelect) exploreInstanceSelect.addEventListener("change", () => {
    selectedModsInstanceId = exploreInstanceSelect.value || null;
    modsExploreResults = [];
    explorePageNumber = 1;
    syncExploreTargetCopy();
    void renderExplorePage(true);
});
enhanceFlintSelect(exploreSortSelect);
refreshFlintSelect(exploreSortSelect);
enhanceFlintSelect(modsLoaderSelect);
refreshFlintSelect(modsLoaderSelect);
exploreCategoryButtons.forEach(button => button.addEventListener("click", () => {
    exploreCategory = button.dataset.category || "";
    explorePageNumber = 1;
    exploreCategoryButtons.forEach(item => item.classList.toggle("active", item === button));
    void renderExplorePage(true);
}));
if (modDetailsClose) modDetailsClose.addEventListener("click", closeModDetails);
if (modDetailsBackdrop) modDetailsBackdrop.addEventListener("click", event => {
    if (event.target === modDetailsBackdrop) closeModDetails();
});
if (modDetailsBody) modDetailsBody.addEventListener("click", event => {
    const anchor = event.target.closest("a[data-external-link=\"true\"]");
    if (!anchor) return;
    event.preventDefault();
    const href = anchor.getAttribute("href");
    if (href && /^https?:\/\//i.test(href)) window.flintfix.openExternal(href);
});
if (modDetailsWeb) modDetailsWeb.addEventListener("click", () => {
    if (activeModDetails?.pageUrl) window.flintfix.openExternal(activeModDetails.pageUrl);
});
if (modDetailsInstall) modDetailsInstall.addEventListener("click", async () => {
    if (!activeModDetails) return;
    const installed = await installExploreProject(activeModDetails, modDetailsInstall);
    if (installed) closeModDetails();
});
if (modsConfigSave) modsConfigSave.addEventListener("click", async () => {
    const instance = getSelectedModsInstance();
    if (!instance) {
        showToast("Create an instance first.", "error");
        return;
    }
    const nextVersion = String(modsVersionSelect?.value || instance.version || "").trim();
    const nextLoader = String(modsLoaderSelect?.value || instance.loader || "fabric").trim();
    instance.version = nextVersion || instance.version;
    instance.loader = nextLoader || instance.loader;
    saveInstances();
    modUpdateMap.clear();
    modsExploreResults = [];
    showToast(`${instance.name} updated.`, "success");
    renderInstances();
    await renderModsPage();
    populateExploreTarget();
    syncExploreTargetCopy();
});
if (instanceUseCurrentButton) instanceUseCurrentButton.addEventListener("click", () => {
    const existing = instances.find(instance => instance.id === editingInstanceId);
    if (!existing || !selectedVersion) return;
    instanceDraftVersion = selectedVersion;
    instanceDraftLoader = selectedLoader.value;
    if (instancePreviewVersion) instancePreviewVersion.textContent = instanceDraftVersion;
    if (instancePreviewLoader) instancePreviewLoader.textContent = selectedLoader.label;
    if (instanceModalNote) instanceModalNote.textContent = "Using the current Home version and loader. Save changes to apply.";
});

if (accountButton) accountButton.addEventListener("click", event => {
    event.stopPropagation();
    if (accountDropdown.hidden || !accountDropdown.classList.contains("open")) openAccountDropdown();
    else closeAccountDropdown();
});

if (changeAccountButton) changeAccountButton.addEventListener("click", openAccountModal);
if (userSettingsButton) userSettingsButton.addEventListener("click", () => {
    openSettingsModal("game");
});



settingsTabs.forEach(tab => tab.addEventListener("click", () => setSettingsTab(tab.dataset.settingsTab || "game")));
if (settingsModalClose) settingsModalClose.addEventListener("click", closeSettingsModal);
if (settingsModalBackdrop) settingsModalBackdrop.addEventListener("click", event => {
    if (event.target === settingsModalBackdrop) closeSettingsModal();
});
if (settingsSearch) settingsSearch.addEventListener("input", applySettingsSearch);

if (resolutionWidth) resolutionWidth.addEventListener("input", () => {
    const previousWidth = launchPreferences.width;
    launchPreferences.width = clampNumber(resolutionWidth.value, 320, 7680, previousWidth);
    if (launchPreferences.lockAspect && lockedAspectRatio > 0) {
        launchPreferences.height = clampNumber(launchPreferences.width / lockedAspectRatio, 240, 4320, launchPreferences.height);
        if (resolutionHeight) resolutionHeight.value = String(launchPreferences.height);
    }
    persistLaunchPreferences();
});
if (resolutionHeight) resolutionHeight.addEventListener("input", () => {
    const previousHeight = launchPreferences.height;
    launchPreferences.height = clampNumber(resolutionHeight.value, 240, 4320, previousHeight);
    if (launchPreferences.lockAspect && lockedAspectRatio > 0) {
        launchPreferences.width = clampNumber(launchPreferences.height * lockedAspectRatio, 320, 7680, launchPreferences.width);
        if (resolutionWidth) resolutionWidth.value = String(launchPreferences.width);
    }
    persistLaunchPreferences();
});
if (launchFullscreen) launchFullscreen.addEventListener("change", () => {
    launchPreferences.fullscreen = launchFullscreen.checked;
    persistLaunchPreferences();
});
if (lockAspectRatio) lockAspectRatio.addEventListener("change", () => {
    launchPreferences.lockAspect = lockAspectRatio.checked;
    if (launchPreferences.lockAspect && launchPreferences.height > 0) {
        lockedAspectRatio = launchPreferences.width / launchPreferences.height;
    }
    persistLaunchPreferences();
});
launcherVisibilityRadios.forEach(radio => radio.addEventListener("change", () => {
    if (!radio.checked) return;
    launchPreferences.launcherVisibility = "keep";
    persistLaunchPreferences();
}));
if (discordRichPresence) discordRichPresence.addEventListener("change", async () => {
    launchPreferences.discordRichPresence = discordRichPresence.checked;
    persistLaunchPreferences();
    await syncDiscordPresencePreference({ notify: true });
});
if (discordRichPresenceRow && discordRichPresence) {
    discordRichPresenceRow.addEventListener("click", event => {
        if (event.target.closest(".switch-control")) return;
        discordRichPresence.click();
    });
}

if (changeMinecraftUsername) changeMinecraftUsername.addEventListener("click", () => openOfficialUrl("https://www.minecraft.net/en-us/msaprofile/mygames/editprofile"));
if (changeAccountEmail) changeAccountEmail.addEventListener("click", () => openOfficialUrl("https://account.live.com/names/manage"));
if (changeAccountPassword) changeAccountPassword.addEventListener("click", () => openOfficialUrl("https://account.live.com/password/change"));
if (manageTwoStep) manageTwoStep.addEventListener("click", () => openOfficialUrl("https://account.microsoft.com/security"));
if (connectDiscord) connectDiscord.addEventListener("click", beginDiscordLink);
if (copyDiscordLinkCode) copyDiscordLinkCode.addEventListener("click", async () => {
    if (!activeDiscordLinkCommand) return;
    try {
        await navigator.clipboard.writeText(activeDiscordLinkCommand);
        showToast("Discord link command copied.", "success");
    } catch {
        showToast(activeDiscordLinkCommand, "neutral");
    }
});
if (unlinkDiscord) unlinkDiscord.addEventListener("click", unlinkDiscordAccount);
if (settingsLogOut) settingsLogOut.addEventListener("click", async () => {
    const result = await window.flintfix.signOutMicrosoft();
    if (!result.success) {
        showToast(result.error || "Log out failed.", "error");
        return;
    }
    microsoftSignedIn = false;
    minecraftProfile = null;
    cachedMicrosoftUsername = null;
    stopDiscordLinkPolling();
    activeDiscordLinkChallengeId = null;
    activeDiscordLinkCommand = null;
    renderDiscordLinkUi({ linked: false });
    updateProfileUi();
    renderInstances();
    if (launchPreferences.discordRichPresence) void window.flintfix.updateDiscordLauncherPresence(getDiscordPresenceContext());
    closeSettingsModal();
    showToast("Logged out of Minecraft account.", "neutral");
});

const topbarElement = document.querySelector(".topbar");
let windowControlBusy = false;

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function setMaximizeVisualState(maximized) {
    if (!windowMaximize) return;
    windowMaximize.classList.toggle("is-maximized", Boolean(maximized));
    windowMaximize.setAttribute("aria-pressed", String(Boolean(maximized)));
    windowMaximize.setAttribute("aria-label", maximized ? "Restore window" : "Maximize window");
    windowMaximize.title = maximized ? "Restore" : "Maximize";
}

async function syncWindowVisualState() {
    try {
        const state = await window.flintfix.getWindowState();
        if (state?.success) setMaximizeVisualState(state.maximized);
    } catch {
        // Visual state will be corrected on the next maximize/unmaximize event.
    }
}

async function animateWindowControl(button, duration, action) {
    if (!button || windowControlBusy) return null;
    windowControlBusy = true;
    button.classList.remove("is-activating");
    // Force a reflow so repeated clicks replay the animation from frame zero.
    void button.offsetWidth;
    button.classList.add("is-activating");

    try {
        await sleep(duration);
        return await action();
    } finally {
        // Keep the class just long enough for the tail of the animation to render.
        setTimeout(() => button.classList.remove("is-activating"), 70);
        setTimeout(() => { windowControlBusy = false; }, 90);
    }
}

async function toggleWindowMaximizeAnimated() {
    const result = await animateWindowControl(windowMaximize, 115, () => window.flintfix.toggleMaximizeWindow());
    if (result?.success) setMaximizeVisualState(result.maximized);
    return result;
}

if (topbarElement) {
    topbarElement.addEventListener("dblclick", async event => {
        if (event.target.closest("button") || event.target.closest(".account-dropdown")) return;
        await toggleWindowMaximizeAnimated();
    });
}

if (windowMinimize) {
    windowMinimize.title = "Minimize";
    windowMinimize.addEventListener("click", async () => {
        await animateWindowControl(windowMinimize, 145, () => window.flintfix.minimizeWindow());
    });
}

if (windowMaximize) {
    windowMaximize.addEventListener("click", toggleWindowMaximizeAnimated);
}

if (windowClose) {
    windowClose.title = "Close";
    windowClose.addEventListener("click", async () => {
        // Delay the actual close so the red activation animation is visible.
        await animateWindowControl(windowClose, 175, () => window.flintfix.closeWindow());
    });
}

if (window.flintfix.onWindowMaximizedChanged) {
    window.flintfix.onWindowMaximizedChanged(setMaximizeVisualState);
}
void syncWindowVisualState();

if (accountModalClose) accountModalClose.addEventListener("click", closeAccountModal);
if (accountModalBackdrop) accountModalBackdrop.addEventListener("click", event => {
    if (event.target === accountModalBackdrop) closeAccountModal();
});
if (addAccountButton) addAccountButton.addEventListener("click", addAnotherAccount);

if (loginButton) loginButton.addEventListener("click", async () => {
    if (minecraftActionInProgress) return;
    loginButton.disabled = true;
    try {
        const profile = await signIn(true);
        showToast(`Minecraft account successfully connected — ${profile.name}.`, "success");
    } catch (error) {
        addConsoleLog("Error", error.message);
        showToast(error.message || "Sign in failed.", "error");
    } finally {
        loginButton.disabled = false;
    }
});

if (signOutButton) signOutButton.addEventListener("click", async () => {
    closeAccountDropdown();
    const result = await window.flintfix.signOutMicrosoft();
    if (!result.success) {
        showToast(result.error || "Log out failed.", "error");
        return;
    }
    microsoftSignedIn = false;
    minecraftProfile = null;
    cachedMicrosoftUsername = null;
    updateProfileUi();
    if (launchPreferences.discordRichPresence) void window.flintfix.updateDiscordLauncherPresence(getDiscordPresenceContext());
    showToast("Logged out of Minecraft account.", "neutral");
    addConsoleLog("Microsoft", "Logged out successfully.");
});

trigger.addEventListener("click", event => {
    event.stopPropagation();

    const isOpen =
        dropdown.classList.toggle("open");

    trigger.setAttribute(
        "aria-expanded",
        String(isOpen)
    );

    if (isOpen) {
        closeLoaderDropdown();
        closeAccountDropdown();
        closeAllFlintSelects();
        setTimeout(
            () => searchInput.focus(),
            100
        );
    }
});

const versionMenu =
    document.getElementById(
        "versionMenu"
    );

if (versionMenu) {
    versionMenu.addEventListener(
        "click",
        event => event.stopPropagation()
    );
}

document.addEventListener(
    "click",
    () => {
        dropdown.classList.remove("open");
        trigger.setAttribute(
            "aria-expanded",
            "false"
        );
        closeLoaderDropdown();
        closeAccountDropdown();
    }
);

document.addEventListener(
    "keydown",
    event => {
        if (event.key === "Escape") {
            dropdown.classList.remove("open");
            trigger.setAttribute(
                "aria-expanded",
                "false"
            );
            closeLoaderDropdown();
            closeAccountDropdown();
            closeAccountModal();
            closeSettingsModal();
        }
    }
);

searchInput.addEventListener(
    "input",
    () => renderVersions(
        searchInput.value
    )
);

function renderVersions(search = "") {
    versionList.innerHTML = "";

    const query =
        search.trim().toLowerCase();

    const filtered =
        minecraftVersions.filter(
            version =>
                version.id
                    .toLowerCase()
                    .includes(query)
        );

    if (!filtered.length) {
        versionList.innerHTML =
            '<div class="version-empty">No Minecraft versions found</div>';

        return;
    }

    for (const version of filtered) {
        const button =
            document.createElement(
                "button"
            );

        button.type = "button";
        button.className =
            "version-option";

        if (
            version.id ===
            selectedVersion
        ) {
            button.classList.add(
                "selected"
            );
        }

        const number =
            document.createElement(
                "span"
            );

        number.className =
            "version-number";

        number.textContent =
            version.id;

        button.appendChild(
            number
        );

        if (
            version.id ===
            latestRelease
        ) {
            const badge =
                document.createElement(
                    "span"
                );

            badge.className =
                "version-latest";

            badge.textContent =
                "LATEST";

            button.appendChild(
                badge
            );
        }

        if (version.id === FLINTFIX_GAME_VERSION) {
            const badge = document.createElement("span");
            badge.className = "version-latest version-flintfix";
            badge.textContent = "FLINTFIX";
            badge.title = "FlintFix in-game features run on this version";
            button.appendChild(badge);
        }

        button.addEventListener(
            "click",
            async () => {
                dropdown.classList.remove(
                    "open"
                );

                trigger.setAttribute(
                    "aria-expanded",
                    "false"
                );

                searchInput.value = "";

                await selectVersion(
                    version.id
                );

                renderVersions();
            }
        );

        versionList.appendChild(
            button
        );
    }
}

async function selectVersion(versionId) {
    selectedVersion =
        versionId;

    triggerValue.textContent =
        versionId === latestRelease
            ? `${versionId} • Latest`
            : versionId;

    localStorage.setItem(
        "flintfix.minecraftVersion",
        versionId
    );

    addConsoleLog(
        "Minecraft",
        `Selected ${versionId}`
    );

    updateProfileUi();
    if (launchPreferences.discordRichPresence) void window.flintfix.updateDiscordLauncherPresence(getDiscordPresenceContext());
    refreshMinecraftFlintIcon(versionId);

    await checkJavaForVersion(
        versionId
    );
}

function getJavaMajor(versionString) {
    if (!versionString) {
        return null;
    }

    if (
        versionString.startsWith(
            "1."
        )
    ) {
        return Number(
            versionString.split(".")[1]
        );
    }

    return Number(
        versionString.split(".")[0]
    );
}

function getUsableJava() {
    if (
        managedJava &&
        getJavaMajor(
            managedJava.version
        ) === requiredJava
    ) {
        return managedJava;
    }

    if (
        installedJava &&
        getJavaMajor(
            installedJava.version
        ) === requiredJava
    ) {
        return installedJava;
    }

    return null;
}

function setJavaReady(
    version,
    executable,
    managed = false
) {
    javaStatus.textContent =
        `Java ${version} ready`;

    javaPath.textContent =
        executable;

    javaPath.title =
        executable;

    javaBadge.textContent =
        "READY";

    javaBadge.className =
        "java-badge found";

    javaInstallButton.hidden =
        true;

    addConsoleLog(
        "Java",
        `${managed ? "Managed" : "System"} Java ${version} ready`
    );
    updateReadiness();
}

function setJavaNeeded(
    minecraftVersion,
    javaVersion
) {
    javaStatus.textContent =
        `Minecraft ${minecraftVersion} requires Java ${javaVersion}`;

    javaPath.textContent =
        `Java ${javaVersion} is not installed.`;

    javaBadge.textContent =
        "NEEDS JAVA";

    javaBadge.className =
        "java-badge missing";

    javaInstallButton.textContent =
        `INSTALL JAVA ${javaVersion}`;

    javaInstallButton.hidden =
        false;

    javaInstallButton.disabled =
        false;
    updateReadiness();
}

async function checkJavaForVersion(
    versionId
) {
    const check =
        ++javaCheckId;

    const version =
        minecraftVersions.find(
            item =>
                item.id === versionId
        );

    if (!version) return;

    managedJava = null;
    requiredJava = null;

    javaInstallButton.hidden =
        true;

    javaStatus.textContent =
        `Checking Java requirement for Minecraft ${versionId}...`;

    javaPath.textContent =
        "";

    javaBadge.textContent =
        "CHECKING";

    javaBadge.className =
        "java-badge checking";

    try {
        const info =
            await window.flintfix
                .getMinecraftVersionInfo(
                    version.url
                );

        if (
            check !==
            javaCheckId
        ) {
            return;
        }

        if (!info.success) {
            throw new Error(
                info.error ||
                "Metadata request failed."
            );
        }

        requiredJava =
            info.javaVersion
                ?.majorVersion ??
            null;

        if (!requiredJava) {
            javaStatus.textContent =
                "No Java requirement was provided for this version.";

            javaBadge.textContent =
                "UNKNOWN";
            updateReadiness();

            return;
        }

        addConsoleLog(
            "Java",
            `Minecraft ${versionId} requires Java ${requiredJava}`
        );

        const managed =
            await window.flintfix
                .getManagedJava(
                    requiredJava
                );

        if (
            check !==
            javaCheckId
        ) {
            return;
        }

        if (
            managed.success &&
            managed.found &&
            getJavaMajor(
                managed.version
            ) === requiredJava
        ) {
            managedJava =
                managed;

            setJavaReady(
                managed.version,
                managed.executable,
                true
            );

            return;
        }

        if (
            installedJava &&
            getJavaMajor(
                installedJava.version
            ) === requiredJava
        ) {
            setJavaReady(
                installedJava.version,
                installedJava.executable,
                false
            );

            return;
        }

        setJavaNeeded(
            versionId,
            requiredJava
        );

    } catch (error) {
        javaStatus.textContent =
            "Couldn't determine Java requirement.";

        javaPath.textContent =
            error.message;

        javaBadge.textContent =
            "ERROR";

        javaBadge.className =
            "java-badge missing";

        addConsoleLog(
            "Error",
            `Java check failed: ${error.message}`
        );
        updateReadiness();
    }
}

async function detectJava() {
    try {
        const result =
            await window.flintfix
                .detectJava();

        installedJava =
            result.success &&
            result.found
                ? result
                : null;

        addConsoleLog(
            "Java",
            installedJava
                ? `System Java ${installedJava.version} detected`
                : "No system Java detected"
        );

        if (selectedVersion) {
            await checkJavaForVersion(
                selectedVersion
            );
        }

    } catch (error) {
        installedJava = null;

        addConsoleLog(
            "Error",
            `Java detection failed: ${error.message}`
        );
    }
}

javaInstallButton.addEventListener(
    "click",
    async () => {
        if (
            !requiredJava ||
            javaInstallInProgress
        ) {
            return;
        }

        const major =
            requiredJava;

        javaInstallInProgress =
            true;

        javaInstallButton.disabled =
            true;

        javaInstallButton.hidden =
            false;

        javaInstallButton.textContent =
            "PREPARING...";

        javaBadge.textContent =
            "INSTALLING";

        javaBadge.className =
            "java-badge checking";

        try {
            const result =
                await window.flintfix
                    .installJava(
                        major
                    );

            if (!result.success) {
                throw new Error(
                    result.error ||
                    "Java installation failed."
                );
            }

            managedJava =
                result;

            setJavaReady(
                result.version,
                result.executable,
                true
            );

        } catch (error) {
            javaStatus.textContent =
                `Java ${major} installation failed`;

            javaPath.textContent =
                error.message;

            javaBadge.textContent =
                "ERROR";

            javaBadge.className =
                "java-badge missing";

            javaInstallButton.hidden =
                false;

            javaInstallButton.textContent =
                `RETRY JAVA ${major}`;

            addConsoleLog(
                "Error",
                `Java installation failed: ${error.message}`
            );

        } finally {
            javaInstallInProgress =
                false;

            javaInstallButton.disabled =
                false;
        }
    }
);

window.flintfix.onJavaProgress(
    data => {
        if (
            Number(
                data.majorVersion
            ) !==
            Number(
                requiredJava
            )
        ) {
            return;
        }

        if (
            data.stage ===
            "download"
        ) {
            javaStatus.textContent =
                `Downloading Java ${requiredJava}...`;

            javaPath.textContent =
                `${data.percent || 0}% downloaded`;

            javaInstallButton.textContent =
                `${data.percent || 0}%`;

        } else if (
            data.stage ===
            "extract"
        ) {
            javaStatus.textContent =
                `Installing Java ${requiredJava}...`;

            javaPath.textContent =
                "Extracting runtime...";

            javaInstallButton.textContent =
                "INSTALLING...";

        } else if (
            data.stage ===
            "complete"
        ) {
            javaStatus.textContent =
                `Finishing Java ${requiredJava}...`;

            javaPath.textContent =
                "Verifying runtime...";
        }
    }
);

window.flintfix.onMinecraftProgress(
    data => {
        updateLaunchTelemetry(data);

        const etaSuffix = launchEta && !launchEta.hidden && launchEta.textContent.startsWith("ETA")
            ? ` • ${launchEta.textContent}`
            : "";

        if (data.stage === "client" && data.bytesTotal) {
            const pct = Math.round((data.received / data.bytesTotal) * 100);
            setPlayText(`CLIENT ${pct}%${etaSuffix}`);
        } else if (data.stage === "libraries" && data.total) {
            setPlayText(`LIBRARIES ${data.current || 0}/${data.total}${etaSuffix}`);
        } else if (data.stage === "assets" && data.total) {
            setPlayText(`ASSETS ${data.current || 0}/${data.total}${etaSuffix}`);
        } else if (data.stage === "natives") {
            setPlayText(`NATIVES${etaSuffix}`);
        } else if (data.stage === "fabric") {
            setPlayText(`FABRIC${etaSuffix}`);
        } else if (data.stage === "launch") {
            setPlayText(`STARTING${etaSuffix}`);
        } else if (data.stage === "started") {
            setPlayText("STARTING GAME...");
        }
    }
);

window.flintfix.onMinecraftLog(
    data => {
        addConsoleLog(
            data.source || "Game",
            data.message || ""
        );
        if (String(data.message || "").startsWith("Minecraft exited with code")) {
            consoleLoggingEnabled = false;
            runningInstanceId = null;
            activeInstanceId = null;
            renderInstances();
        }
    }
);

async function loadMinecraftVersions() {
    triggerValue.textContent =
        "Loading...";

    try {
        const result =
            await window.flintfix
                .getMinecraftVersions();

        if (!result.success) {
            throw new Error(
                result.error ||
                "Version manifest failed."
            );
        }

        latestRelease =
            result.latest.release;

        minecraftVersions =
            result.versions.filter(
                version =>
                    version.type ===
                    "release"
            );

        const saved =
            localStorage.getItem(
                "flintfix.minecraftVersion"
            );

        const chosen =
            minecraftVersions.some(
                version =>
                    version.id === saved
            )
                ? saved
                : (minecraftVersions.some(version => version.id === FLINTFIX_GAME_VERSION) ? FLINTFIX_GAME_VERSION : latestRelease);

        await selectVersion(
            chosen
        );

        ensureDefaultInstance();
        renderInstances();
        renderVersions();

        addConsoleLog(
            "Minecraft",
            `Loaded ${minecraftVersions.length} releases`
        );

    } catch (error) {
        triggerValue.textContent =
            "Unavailable";

        versionList.innerHTML =
            '<div class="version-empty">Couldn\'t load Minecraft versions.</div>';

        addConsoleLog(
            "Error",
            `Version loading failed: ${error.message}`
        );
    }
}

async function loadAuthStatus() {
    try {
        const result =
            await window.flintfix
                .getAuthStatus();

        microsoftSignedIn =
            Boolean(
                result.success &&
                result.signedIn
            );

        cachedMicrosoftUsername =
            result.success && result.microsoftUsername
                ? result.microsoftUsername
                : null;

        if (result.success && result.activeProfile) {
            minecraftProfile = result.activeProfile;
            cachedMicrosoftUsername = result.activeProfile.microsoftUsername || cachedMicrosoftUsername;
        }

        if (
            microsoftSignedIn &&
            result.microsoftUsername
        ) {
            addConsoleLog(
                "Microsoft",
                `Cached account: ${result.microsoftUsername}`
            );
        }

    } catch {
        microsoftSignedIn =
            false;
        cachedMicrosoftUsername = null;
    }

    updateProfileUi();
}

async function signIn() {
    setPlayText("SIGNING IN...");

    addConsoleLog(
        "Microsoft",
        "Opening Microsoft sign-in window..."
    );

    const result =
        await window.flintfix
            .signInMicrosoft();

    if (!result.success) {
        if (
            result.code ===
            "MINECRAFT_APP_NOT_AUTHORIZED"
        ) {
            addConsoleLog(
                "Minecraft",
                "Microsoft/Xbox sign-in succeeded, but Minecraft Services has not authorized the FlintFix client ID yet."
            );
        }

        throw new Error(
            result.error ||
            "Microsoft sign-in failed."
        );
    }

    microsoftSignedIn =
        true;

    minecraftProfile =
        result.profile;
    cachedMicrosoftUsername = result.profile.microsoftUsername || cachedMicrosoftUsername;

    closeAccountDropdown();

    updateProfileUi();
    void refreshDiscordLinkStatus({ silent: true });
    if (launchPreferences.discordRichPresence) void window.flintfix.updateDiscordLauncherPresence(getDiscordPresenceContext());

    addConsoleLog(
        "Minecraft",
        `Signed in as ${result.profile.name}`
    );

    return result.profile;
}

playButton.addEventListener(
    "click",
    async () => {
        if (
            !selectedVersion ||
            minecraftActionInProgress
        ) {
            return;
        }

        const usableJava =
            getUsableJava();

        if (!usableJava) {
            addConsoleLog(
                "Java",
                `Java ${requiredJava} must be installed first`
            );

            javaInstallButton.hidden =
                false;

            return;
        }

        minecraftActionInProgress =
            true;

        playButton.disabled =
            true;

        consoleLoggingEnabled = true;
        clearConsoleForLaunch();
        startLaunchTelemetry();
        addConsoleLog("Launcher", `Launch started: Minecraft ${selectedVersion} • ${selectedLoader.label}`);
        addConsoleLog("Launcher", "Preparing files. ETA is approximate and adapts as downloads progress.");

        try {
            // --------------------------------------------
            // 1. Install / verify Minecraft.
            // --------------------------------------------

            setPlayText("PREPARING...");

            if (profileStatus) {
                profileStatus.textContent =
                    "PREPARING";
            }

            addConsoleLog(
                "Launcher",
                `Preparing Minecraft ${selectedVersion}...`
            );

            const installResult =
                await window.flintfix
                    .installMinecraft(
                        selectedVersion
                    );

            if (
                !installResult.success
            ) {
                throw new Error(
                    installResult.error ||
                    "Minecraft installation failed."
                );
            }

            addConsoleLog(
                "Launcher",
                `Minecraft ${selectedVersion} is ready`
            );
            refreshMinecraftFlintIcon(selectedVersion);

            // --------------------------------------------
            // 2. Sign in if needed.
            // --------------------------------------------

            if (!minecraftProfile) {
                const signedProfile = await signIn();
                showToast(`Minecraft account successfully connected — ${signedProfile.name}.`, "success");
            }

            // --------------------------------------------
            // 3. Launch.
            // --------------------------------------------

            setPlayText("LAUNCHING...");

            if (profileStatus) {
                profileStatus.textContent =
                    "LAUNCHING";
            }

            addConsoleLog(
                "Launcher",
                `Opening Minecraft ${selectedVersion} with ${selectedLoader.label}...`
            );

            const launchResult = await window.flintfix.launchMinecraft(
                    selectedVersion,
                    usableJava.executable,
                    selectedLoader ? selectedLoader.value : "vanilla",
                    {
                        width: launchPreferences.width,
                        height: launchPreferences.height,
                        fullscreen: launchPreferences.fullscreen,
                        launcherVisibility: launchPreferences.launcherVisibility,
                        discordRichPresence: launchPreferences.discordRichPresence,
                        instanceId: activeInstanceId || null,
                        // Only a join started in the last few seconds; a cancelled one must not stick.
                        joinServer: pendingJoinServer && Date.now() - pendingJoinServer.at < 15000
                            ? pendingJoinServer.address : null
                    }
                );
            pendingJoinServer = null;

            if (!launchResult.success) {
                if (
                    launchResult.code ===
                    "MINECRAFT_APP_NOT_AUTHORIZED"
                ) {
                    addConsoleLog(
                        "Minecraft",
                        "FlintFix's Microsoft client ID is not currently authorized by Minecraft Services."
                    );
                }

                throw new Error(
                    launchResult.error ||
                    "Minecraft launch failed."
                );
            }

            finishLaunchTelemetry(true);
            addConsoleLog("Launcher", "Minecraft process is running. The game window may take a few more seconds to appear.");

            minecraftProfile =
                launchResult.profile ||
                minecraftProfile;
            cachedMicrosoftUsername = minecraftProfile?.microsoftUsername || cachedMicrosoftUsername;

            microsoftSignedIn = true;

            const activeCandidate = instances.find(instance => instance.id === activeInstanceId);
            const matchedInstance = activeCandidate && activeCandidate.version === selectedVersion && activeCandidate.loader === selectedLoader.value
                ? activeCandidate
                : findMatchingInstance();
            if (matchedInstance) {
                runningInstanceId = matchedInstance.id;
                matchedInstance.lastPlayedAt = new Date().toISOString();
                saveInstances();
            }

            updateProfileUi();
            renderInstances();

            setPlayText("PLAY");

            if (profileStatus) {
                profileStatus.textContent =
                    "RUNNING";
            }

            addConsoleLog(
                "Launcher",
                `Minecraft started (PID ${launchResult.pid})`
            );

        } catch (error) {
            finishLaunchTelemetry(false);
            setPlayText(minecraftProfile ? "RETRY LAUNCH" : "SIGN IN & PLAY");

            if (profileStatus) {
                profileStatus.textContent =
                    "ERROR";
            }

            addConsoleLog(
                "Error",
                error.message
            );
            showToast(error.message || "Minecraft could not be opened.", "error");

        } finally {
            minecraftActionInProgress =
                false;

            playButton.disabled =
                false;
            updateProfileUi();
        }
    }
);

// Launch splash: shown until the first data has loaded, at least long enough
// for its intro animation, and never longer than SPLASH_MAX_MS.
const SPLASH_MIN_MS = 1400;
const SPLASH_MAX_MS = 9000;
const splashStartedAt = performance.now();
const splash = {
    root: document.getElementById("ffSplash"),
    bar: document.getElementById("ffSplashBar"),
    status: document.getElementById("ffSplashStatus"),
    progress: 0,
    done: false,
    step(text, progress) {
        if (this.done) return;
        if (text && this.status) this.status.textContent = text;
        this.progress = Math.max(this.progress, progress);
        if (this.bar) this.bar.style.width = `${Math.round(this.progress * 100)}%`;
    },
    async finish() {
        if (this.done) return;
        this.step("Ready", 1);
        const wait = Math.max(0, SPLASH_MIN_MS - (performance.now() - splashStartedAt));
        await new Promise(resolve => setTimeout(resolve, wait + 220));
        this.done = true;
        document.body.classList.remove("is-booting");
        this.root?.classList.add("is-done");
        setTimeout(() => this.root?.remove(), 700);
    }
};
setTimeout(() => void splash.finish(), SPLASH_MAX_MS);
window.flintfix.getAppVersion?.().then(version => {
    const label = document.getElementById("ffSplashVersion");
    if (label && version) label.textContent = `V${version}`;
}).catch(() => {});

async function initializeFlintFix() {
    addConsoleLog(
        "FlintFix",
        "Starting launcher..."
    );
    splash.step("Starting up", 0.12);

    if (loaderTriggerValue) {
        loaderTriggerValue.textContent = selectedLoader.label;
    }
    syncSettingsControls();

    try {
        let finished = 0;
        const track = (promise, label) => Promise.resolve(promise).finally(() => {
            finished++;
            splash.step(label, 0.2 + finished * 0.22);
        });
        splash.step("Checking Java, versions and account", 0.2);
        await Promise.all([
            track(detectJava(), "Java checked"),
            track(loadMinecraftVersions(), "Minecraft versions loaded"),
            track(loadAuthStatus(), "Account loaded")
        ]);

        splash.step("Connecting services", 0.9);
        if (minecraftProfile) await refreshDiscordLinkStatus({ silent: true });
        else renderDiscordLinkUi({ linked: false });

        await syncDiscordPresencePreference();

        setPlayText(minecraftProfile ? "PLAY" : "SIGN IN & PLAY");

        updateProfileUi();
        updateReadiness();

        addConsoleLog(
            "FlintFix",
            "Launcher ready"
        );
    } finally {
        void splash.finish();
    }
}

// ---------------------------------------------------------------------------
// Resource packs: browse Modrinth, preview galleries, install into the game.
// ---------------------------------------------------------------------------

const PACKS_PAGE_SIZE = 12;
const packsPage = document.getElementById("packsPage");
const packsSearch = document.getElementById("packsSearch");
const packsVersionSelect = document.getElementById("packsVersionSelect");
const packsSortSelect = document.getElementById("packsSortSelect");
const packsCategoryButtons = Array.from(document.querySelectorAll("#packsCategoryRow .explore-category"));
const packsStatus = document.getElementById("packsStatus");
const packsGrid = document.getElementById("packsGrid");
const packsPagination = document.getElementById("packsPagination");
const packsPrevPage = document.getElementById("packsPrevPage");
const packsNextPage = document.getElementById("packsNextPage");
const packsPageNumbers = document.getElementById("packsPageNumbers");
const packsViewTabs = Array.from(document.querySelectorAll(".packs-view-tab[data-packs-view]"));
const packsBrowse = document.getElementById("packsBrowse");
const packsInstalled = document.getElementById("packsInstalled");
const packsInstalledList = document.getElementById("packsInstalledList");
const packsInstalledCount = document.getElementById("packsInstalledCount");
const packsOpenFolder = document.getElementById("packsOpenFolder");
const packPreviewBackdrop = document.getElementById("packPreviewBackdrop");
const packPreviewImage = document.getElementById("packPreviewImage");
const packPreviewFallback = document.getElementById("packPreviewFallback");
const packPreviewPrev = document.getElementById("packPreviewPrev");
const packPreviewNext = document.getElementById("packPreviewNext");
const packPreviewCaption = document.getElementById("packPreviewCaption");
const packPreviewThumbs = document.getElementById("packPreviewThumbs");
const packPreviewClose = document.getElementById("packPreviewClose");
const packPreviewIcon = document.getElementById("packPreviewIcon");
const packPreviewTitle = document.getElementById("packPreviewTitle");
const packPreviewSubtitle = document.getElementById("packPreviewSubtitle");
const packPreviewTags = document.getElementById("packPreviewTags");
const packPreviewBody = document.getElementById("packPreviewBody");
const packPreviewDownloads = document.getElementById("packPreviewDownloads");
const packPreviewFollowers = document.getElementById("packPreviewFollowers");
const packPreviewVersions = document.getElementById("packPreviewVersions");
const packPreviewProgress = document.getElementById("packPreviewProgress");
const packPreviewInstall = document.getElementById("packPreviewInstall");
const packPreviewWeb = document.getElementById("packPreviewWeb");

const packsState = {
    view: "browse",
    category: "",
    page: 1,
    totalHits: 0,
    results: [],
    installed: [],
    installing: new Map(),
    requestId: 0,
    preview: null,
    previewIndex: 0
};

function packsVersion() {
    return packsVersionSelect?.value || "";
}

function populatePacksVersions() {
    if (!packsVersionSelect) return;
    const previous = packsVersionSelect.value || localStorage.getItem("flintfix.packs.version");
    const ids = getMinecraftVersionIds(40);
    const preferred = getSelectedModsInstance()?.version || instances[0]?.version || ids[0] || "";
    packsVersionSelect.innerHTML = "";
    const any = document.createElement("option");
    any.value = "";
    any.textContent = "Any version";
    packsVersionSelect.appendChild(any);
    for (const id of new Set([preferred, ...ids].filter(Boolean))) {
        const option = document.createElement("option");
        option.value = id;
        option.textContent = `Minecraft ${id}`;
        packsVersionSelect.appendChild(option);
    }
    const options = Array.from(packsVersionSelect.options).map(option => option.value);
    packsVersionSelect.value = previous !== null && options.includes(previous) ? previous : preferred;
    enhanceFlintSelect(packsVersionSelect);
    refreshFlintSelect(packsVersionSelect);
}

/** Card banner: the pack's featured screenshot, or a tint from its brand color. */
function packBanner(pack) {
    const banner = document.createElement("div");
    banner.className = "pack-card-banner";
    if (Number.isFinite(pack.color)) {
        const hex = `#${pack.color.toString(16).padStart(6, "0")}`;
        banner.style.setProperty("--pack-tint", hex);
    }
    if (pack.previewUrl) {
        const img = document.createElement("img");
        img.loading = "lazy";
        img.decoding = "async";
        img.alt = "";
        img.src = pack.previewUrl;
        img.addEventListener("load", () => banner.classList.add("loaded"));
        img.addEventListener("error", () => img.remove());
        banner.appendChild(img);
    } else if (pack.iconUrl) {
        banner.classList.add("icon-only");
        const img = document.createElement("img");
        img.alt = "";
        img.src = pack.iconUrl;
        img.className = "pack-card-banner-icon";
        banner.appendChild(img);
    }
    const zoom = document.createElement("span");
    zoom.className = "pack-card-zoom material-symbols-rounded";
    zoom.setAttribute("aria-hidden", "true");
    zoom.textContent = "zoom_in";
    banner.appendChild(zoom);
    return banner;
}

function installedPackFor(projectId) {
    return packsState.installed.find(pack => pack.projectId === projectId) || null;
}

function packInstallLabel(pack) {
    const progress = packsState.installing.get(pack.projectId);
    if (progress) return progress.total ? `Downloading ${Math.round(progress.received / progress.total * 100)}%` : "Downloading...";
    const installed = installedPackFor(pack.projectId);
    if (!installed) return "Download";
    return installed.enabled ? "Active" : "Installed";
}

function syncPackButtons(projectId) {
    const pack = packsState.results.find(item => item.projectId === projectId)
        || (packsState.preview?.projectId === projectId ? packsState.preview : null);
    if (!pack) return;
    const busy = packsState.installing.has(projectId);
    const installed = installedPackFor(projectId);
    for (const button of document.querySelectorAll(`[data-pack-install="${CSS.escape(projectId)}"]`)) {
        button.textContent = packInstallLabel(pack);
        button.disabled = busy || Boolean(installed);
        button.classList.toggle("is-installed", Boolean(installed));
    }
    if (packsState.preview?.projectId === projectId && packPreviewProgress) {
        const progress = packsState.installing.get(projectId);
        packPreviewProgress.hidden = !progress;
        const bar = packPreviewProgress.querySelector("span");
        if (bar && progress) bar.style.width = `${progress.total ? Math.round(progress.received / progress.total * 100) : 30}%`;
    }
}

function renderPacksGrid() {
    if (!packsGrid) return;
    packsGrid.innerHTML = "";
    if (!packsState.results.length) {
        const empty = document.createElement("div");
        empty.className = "mods-explore-empty";
        empty.textContent = packsState.loading ? "" : "No resource packs found. Try another search, category or version.";
        if (packsState.loading) {
            packsGrid.classList.add("loading");
            for (let i = 0; i < 6; i += 1) {
                const skeleton = document.createElement("div");
                skeleton.className = "pack-card skeleton";
                packsGrid.appendChild(skeleton);
            }
            return;
        }
        packsGrid.classList.remove("loading");
        packsGrid.appendChild(empty);
        return;
    }
    packsGrid.classList.remove("loading");
    packsState.results.forEach((pack, index) => {
        const card = document.createElement("article");
        card.className = "pack-card";
        card.tabIndex = 0;
        card.style.setProperty("--stagger", `${Math.min(index, 11) * 28}ms`);
        card.setAttribute("aria-label", `${pack.title} resource pack`);

        const body = document.createElement("div");
        body.className = "pack-card-body";
        const head = document.createElement("div");
        head.className = "pack-card-head";
        const icon = document.createElement("div");
        icon.className = "pack-card-icon";
        if (pack.iconUrl) {
            const img = document.createElement("img");
            img.src = pack.iconUrl;
            img.alt = "";
            img.loading = "lazy";
            icon.appendChild(img);
        } else {
            icon.textContent = (pack.title || "?").charAt(0).toUpperCase();
        }
        const titles = document.createElement("div");
        titles.className = "pack-card-titles";
        const title = document.createElement("strong");
        title.textContent = pack.title;
        const author = document.createElement("small");
        author.textContent = pack.author ? `by ${pack.author}` : "Modrinth";
        titles.append(title, author);
        head.append(icon, titles);

        const desc = document.createElement("p");
        desc.textContent = pack.description || "A resource pack on Modrinth.";

        const foot = document.createElement("div");
        foot.className = "pack-card-foot";
        const stats = document.createElement("span");
        stats.className = "pack-card-stats";
        stats.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">download</span>';
        stats.append(formatDownloadCount(pack.downloads));
        const install = document.createElement("button");
        install.type = "button";
        install.className = "pack-card-install";
        install.dataset.packInstall = pack.projectId;
        install.addEventListener("click", event => {
            event.stopPropagation();
            void installPack(pack);
        });
        foot.append(stats, install);
        body.append(head, desc, foot);

        card.append(packBanner(pack), body);
        card.addEventListener("click", () => void openPackPreview(pack));
        card.addEventListener("keydown", event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                void openPackPreview(pack);
            }
        });
        packsGrid.appendChild(card);
        syncPackButtons(pack.projectId);
    });
}

function renderPacksPagination() {
    if (!packsPagination || !packsPageNumbers) return;
    const totalPages = Math.max(1, Math.ceil(packsState.totalHits / PACKS_PAGE_SIZE));
    packsPagination.hidden = totalPages <= 1;
    if (totalPages <= 1) return;
    if (packsPrevPage) packsPrevPage.disabled = packsState.page <= 1;
    if (packsNextPage) packsNextPage.disabled = packsState.page >= totalPages;
    packsPageNumbers.innerHTML = "";
    const pages = Array.from(new Set([1, totalPages, packsState.page - 1, packsState.page, packsState.page + 1]))
        .filter(page => page >= 1 && page <= totalPages)
        .sort((a, b) => a - b);
    let previous = 0;
    for (const page of pages) {
        if (previous && page - previous > 1) {
            const dots = document.createElement("span");
            dots.className = "explore-page-dots";
            dots.textContent = "…";
            packsPageNumbers.appendChild(dots);
        }
        const button = document.createElement("button");
        button.type = "button";
        button.className = `explore-page-number${page === packsState.page ? " active" : ""}`;
        button.textContent = String(page);
        button.addEventListener("click", () => {
            if (page === packsState.page) return;
            packsState.page = page;
            void loadPacks();
        });
        packsPageNumbers.appendChild(button);
        previous = page;
    }
}

async function loadPacks() {
    if (!packsGrid) return;
    const requestId = ++packsState.requestId;
    packsState.loading = true;
    packsState.results = [];
    renderPacksGrid();
    const version = packsVersion();
    if (packsStatus) packsStatus.textContent = "Loading resource packs...";
    const result = await window.flintfix.searchResourcePacks({
        query: String(packsSearch?.value || "").trim(),
        version,
        category: packsState.category,
        index: packsSortSelect?.value || "downloads",
        limit: PACKS_PAGE_SIZE,
        offset: (packsState.page - 1) * PACKS_PAGE_SIZE
    });
    if (requestId !== packsState.requestId) return;
    packsState.loading = false;
    if (!result?.success) {
        packsState.results = [];
        packsState.totalHits = 0;
        if (packsStatus) packsStatus.textContent = result?.error || "Could not reach Modrinth right now.";
        renderPacksGrid();
        renderPacksPagination();
        return;
    }
    packsState.results = Array.isArray(result.packs) ? result.packs : [];
    packsState.totalHits = Number(result.totalHits) || packsState.results.length;
    const totalPages = Math.max(1, Math.ceil(packsState.totalHits / PACKS_PAGE_SIZE));
    if (packsStatus) {
        packsStatus.textContent = `${formatDownloadCount(packsState.totalHits)} packs${version ? ` for Minecraft ${version}` : ""} • page ${packsState.page} of ${totalPages}`;
    }
    renderPacksGrid();
    renderPacksPagination();
}

async function refreshInstalledPacks() {
    const result = await window.flintfix.listResourcePacks();
    packsState.installed = result?.success && Array.isArray(result.packs) ? result.packs : [];
    if (packsInstalledCount) packsInstalledCount.textContent = String(packsState.installed.length);
    renderInstalledPacks();
    for (const pack of packsState.results) syncPackButtons(pack.projectId);
    if (packsState.preview) syncPackButtons(packsState.preview.projectId);
}

function renderInstalledPacks() {
    if (!packsInstalledList) return;
    packsInstalledList.innerHTML = "";
    if (!packsState.installed.length) {
        const empty = document.createElement("div");
        empty.className = "mods-explore-empty";
        empty.textContent = "No resource packs yet. Download one from Browse, or drop .zip packs into the folder.";
        packsInstalledList.appendChild(empty);
        return;
    }
    for (const pack of packsState.installed) {
        const row = document.createElement("div");
        row.className = `pack-row${pack.enabled ? " active" : ""}`;
        const icon = document.createElement("span");
        icon.className = "pack-row-icon material-symbols-rounded";
        icon.setAttribute("aria-hidden", "true");
        icon.textContent = pack.folder ? "folder" : "texture";
        const copy = document.createElement("div");
        copy.className = "pack-row-copy";
        const name = document.createElement("strong");
        name.textContent = pack.title;
        const meta = document.createElement("small");
        const size = pack.folder ? "Folder" : `${(pack.size / 1048576).toFixed(pack.size > 10485760 ? 0 : 1)} MB`;
        meta.textContent = [pack.versionNumber, size, pack.projectId ? "Modrinth" : "Added manually"].filter(Boolean).join(" • ");
        copy.append(name, meta);

        const toggle = document.createElement("label");
        toggle.className = "pack-switch";
        toggle.title = pack.enabled ? "Active in game" : "Not active";
        const input = document.createElement("input");
        input.type = "checkbox";
        input.checked = pack.enabled;
        input.setAttribute("aria-label", `Use ${pack.title} in game`);
        input.addEventListener("change", async () => {
            input.disabled = true;
            const result = await window.flintfix.setResourcePackEnabled(pack.fileName, input.checked);
            if (!result?.success) {
                input.checked = !input.checked;
                showToast(result?.error || "Could not change the pack.", "error");
            } else {
                showToast(input.checked ? `${pack.title} will load next launch.` : `${pack.title} turned off.`, "success");
            }
            await refreshInstalledPacks();
        });
        const track = document.createElement("span");
        track.setAttribute("aria-hidden", "true");
        toggle.append(input, track);

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "pack-row-remove";
        remove.setAttribute("aria-label", `Remove ${pack.title}`);
        remove.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">delete</span>';
        remove.addEventListener("click", async () => {
            if (remove.dataset.confirm !== "1") {
                remove.dataset.confirm = "1";
                remove.classList.add("confirm");
                remove.title = "Click again to remove";
                setTimeout(() => {
                    remove.dataset.confirm = "";
                    remove.classList.remove("confirm");
                }, 2500);
                return;
            }
            const result = await window.flintfix.removeResourcePack(pack.fileName);
            if (!result?.success) showToast(result?.error || "Could not remove the pack.", "error");
            else showToast(`${pack.title} removed.`, "success");
            await refreshInstalledPacks();
        });

        row.append(icon, copy, toggle, remove);
        packsInstalledList.appendChild(row);
    }
}

async function installPack(pack) {
    if (!pack?.projectId || packsState.installing.has(pack.projectId) || installedPackFor(pack.projectId)) return;
    packsState.installing.set(pack.projectId, { received: 0, total: 0 });
    syncPackButtons(pack.projectId);
    const result = await window.flintfix.installResourcePack({
        projectId: pack.projectId,
        version: packsVersion(),
        title: pack.title
    });
    packsState.installing.delete(pack.projectId);
    if (!result?.success) {
        showToast(result?.error || `Could not download ${pack.title}.`, "error");
    } else if (result.alreadyInstalled) {
        showToast(`${pack.title} is already in your game.`, "success");
    } else {
        const note = result.exactMatch === false ? " (no exact match for this version)" : "";
        showToast(`${pack.title} added and turned on for your next launch${note}.`, "success");
    }
    await refreshInstalledPacks();
    syncPackButtons(pack.projectId);
}

function showPackPreviewImage(index) {
    const gallery = packsState.preview?.gallery || [];
    if (!packPreviewImage) return;
    if (!gallery.length) {
        packPreviewImage.hidden = true;
        if (packPreviewFallback) packPreviewFallback.hidden = false;
        if (packPreviewPrev) packPreviewPrev.hidden = true;
        if (packPreviewNext) packPreviewNext.hidden = true;
        if (packPreviewCaption) packPreviewCaption.textContent = "";
        return;
    }
    packsState.previewIndex = (index + gallery.length) % gallery.length;
    const item = gallery[packsState.previewIndex];
    if (packPreviewFallback) packPreviewFallback.hidden = true;
    packPreviewImage.hidden = false;
    packPreviewImage.classList.remove("ready");
    packPreviewImage.onload = () => packPreviewImage.classList.add("ready");
    packPreviewImage.src = item.rawUrl || item.url;
    packPreviewImage.alt = item.title || packsState.preview.title;
    const many = gallery.length > 1;
    if (packPreviewPrev) packPreviewPrev.hidden = !many;
    if (packPreviewNext) packPreviewNext.hidden = !many;
    if (packPreviewCaption) {
        packPreviewCaption.textContent = [item.title, many ? `${packsState.previewIndex + 1} / ${gallery.length}` : ""].filter(Boolean).join("  •  ");
    }
    if (packPreviewThumbs) {
        Array.from(packPreviewThumbs.children).forEach((thumb, i) => thumb.classList.toggle("active", i === packsState.previewIndex));
        packPreviewThumbs.children[packsState.previewIndex]?.scrollIntoView({ block: "nearest", inline: "nearest", behavior: "smooth" });
    }
}

async function openPackPreview(pack) {
    if (!packPreviewBackdrop || !pack) return;
    packsState.preview = { ...pack, gallery: pack.previewUrl ? [{ url: pack.previewUrl, rawUrl: pack.previewUrl, title: "" }] : [] };
    packPreviewBackdrop.hidden = false;
    requestAnimationFrame(() => packPreviewBackdrop.classList.add("open"));
    if (packPreviewTitle) packPreviewTitle.textContent = pack.title;
    if (packPreviewSubtitle) packPreviewSubtitle.textContent = pack.author ? `by ${pack.author}` : "";
    if (packPreviewBody) packPreviewBody.textContent = pack.description || "";
    if (packPreviewDownloads) packPreviewDownloads.textContent = formatDownloadCount(pack.downloads);
    if (packPreviewFollowers) packPreviewFollowers.textContent = formatDownloadCount(pack.follows);
    if (packPreviewVersions) packPreviewVersions.textContent = "Checking...";
    if (packPreviewTags) packPreviewTags.innerHTML = "";
    if (packPreviewThumbs) packPreviewThumbs.innerHTML = "";
    if (packPreviewIcon) {
        packPreviewIcon.innerHTML = "";
        if (pack.iconUrl) {
            const img = document.createElement("img");
            img.src = pack.iconUrl;
            img.alt = "";
            packPreviewIcon.appendChild(img);
        } else {
            packPreviewIcon.textContent = (pack.title || "?").charAt(0).toUpperCase();
        }
    }
    if (packPreviewInstall) packPreviewInstall.dataset.packInstall = pack.projectId;
    syncPackButtons(pack.projectId);
    showPackPreviewImage(0);

    const result = await window.flintfix.getResourcePackDetails(pack.projectId, { version: packsVersion() });
    if (packsState.preview?.projectId !== pack.projectId) return;
    if (!result?.success) {
        if (packPreviewVersions) packPreviewVersions.textContent = "Unknown";
        return;
    }
    const details = result.details;
    packsState.preview = { ...packsState.preview, ...details, gallery: details.gallery.length ? details.gallery : packsState.preview.gallery };
    if (packPreviewSubtitle) packPreviewSubtitle.textContent = details.description || packPreviewSubtitle.textContent;
    if (packPreviewBody) packPreviewBody.innerHTML = renderModDescription(String(details.body || details.description || "").slice(0, 24000));
    if (packPreviewFollowers) packPreviewFollowers.textContent = formatDownloadCount(details.followers);
    if (packPreviewVersions) {
        const versions = details.gameVersions || [];
        packPreviewVersions.textContent = versions.length > 1 ? `${versions[0]} – ${versions[versions.length - 1]}` : (versions[0] || "Unknown");
    }
    if (packPreviewTags) {
        for (const tag of Array.from(new Set(details.categories || [])).slice(0, 6)) {
            const span = document.createElement("span");
            span.textContent = tag;
            packPreviewTags.appendChild(span);
        }
    }
    if (packPreviewThumbs) {
        packPreviewThumbs.innerHTML = "";
        packsState.preview.gallery.forEach((item, i) => {
            const thumb = document.createElement("button");
            thumb.type = "button";
            thumb.className = "pack-preview-thumb";
            thumb.setAttribute("aria-label", item.title || `Preview ${i + 1}`);
            const img = document.createElement("img");
            img.src = item.url;
            img.alt = "";
            img.loading = "lazy";
            thumb.appendChild(img);
            thumb.addEventListener("click", () => showPackPreviewImage(i));
            packPreviewThumbs.appendChild(thumb);
        });
        packPreviewThumbs.hidden = packsState.preview.gallery.length < 2;
    }
    showPackPreviewImage(0);
}

function closePackPreview() {
    if (!packPreviewBackdrop || packPreviewBackdrop.hidden) return;
    packPreviewBackdrop.classList.remove("open");
    packsState.preview = null;
    setTimeout(() => {
        if (!packPreviewBackdrop.classList.contains("open")) packPreviewBackdrop.hidden = true;
    }, 180);
}

function setPacksView(view) {
    packsState.view = view === "installed" ? "installed" : "browse";
    packsViewTabs.forEach(tab => {
        const active = tab.dataset.packsView === packsState.view;
        tab.classList.toggle("active", active);
        tab.setAttribute("aria-selected", String(active));
    });
    if (packsBrowse) packsBrowse.hidden = packsState.view !== "browse";
    if (packsInstalled) packsInstalled.hidden = packsState.view !== "installed";
}

async function renderPacksPage() {
    if (!packsPage) return;
    populatePacksVersions();
    await refreshInstalledPacks();
    if (!packsState.results.length && !packsState.loading) await loadPacks();
}

if (packsSearch) {
    let timer = null;
    packsSearch.addEventListener("input", () => {
        clearTimeout(timer);
        timer = setTimeout(() => {
            packsState.page = 1;
            void loadPacks();
        }, 300);
    });
}
for (const select of [packsVersionSelect, packsSortSelect]) {
    if (!select) continue;
    select.addEventListener("change", () => {
        if (select === packsVersionSelect) localStorage.setItem("flintfix.packs.version", select.value);
        packsState.page = 1;
        void loadPacks();
    });
}
enhanceFlintSelect(packsSortSelect);
refreshFlintSelect(packsSortSelect);
packsCategoryButtons.forEach(button => button.addEventListener("click", () => {
    packsState.category = button.dataset.category || "";
    packsState.page = 1;
    packsCategoryButtons.forEach(item => item.classList.toggle("active", item === button));
    void loadPacks();
}));
packsViewTabs.forEach(tab => tab.addEventListener("click", () => setPacksView(tab.dataset.packsView)));
if (packsPrevPage) packsPrevPage.addEventListener("click", () => {
    if (packsState.page <= 1) return;
    packsState.page -= 1;
    void loadPacks();
});
if (packsNextPage) packsNextPage.addEventListener("click", () => {
    if (packsState.page >= Math.ceil(packsState.totalHits / PACKS_PAGE_SIZE)) return;
    packsState.page += 1;
    void loadPacks();
});
if (packsOpenFolder) packsOpenFolder.addEventListener("click", async () => {
    const result = await window.flintfix.openResourcePacksFolder();
    if (!result?.success) showToast(result?.error || "Could not open the folder.", "error");
});
if (packPreviewClose) packPreviewClose.addEventListener("click", closePackPreview);
if (packPreviewBackdrop) packPreviewBackdrop.addEventListener("click", event => {
    if (event.target === packPreviewBackdrop) closePackPreview();
});
if (packPreviewPrev) packPreviewPrev.addEventListener("click", () => showPackPreviewImage(packsState.previewIndex - 1));
if (packPreviewNext) packPreviewNext.addEventListener("click", () => showPackPreviewImage(packsState.previewIndex + 1));
if (packPreviewInstall) packPreviewInstall.addEventListener("click", () => {
    if (packsState.preview) void installPack(packsState.preview);
});
if (packPreviewWeb) packPreviewWeb.addEventListener("click", () => {
    if (packsState.preview?.pageUrl) window.flintfix.openExternal(packsState.preview.pageUrl);
});
if (packPreviewBody) packPreviewBody.addEventListener("click", event => {
    const anchor = event.target.closest("a[data-external-link=\"true\"]");
    if (!anchor) return;
    event.preventDefault();
    const href = anchor.getAttribute("href");
    if (href && /^https?:\/\//i.test(href)) window.flintfix.openExternal(href);
});
document.addEventListener("keydown", event => {
    if (!packPreviewBackdrop || packPreviewBackdrop.hidden) return;
    if (event.key === "Escape") closePackPreview();
    else if (event.key === "ArrowLeft") showPackPreviewImage(packsState.previewIndex - 1);
    else if (event.key === "ArrowRight") showPackPreviewImage(packsState.previewIndex + 1);
});
window.flintfix.onResourcePackProgress?.(progress => {
    if (!progress?.projectId || !packsState.installing.has(progress.projectId)) return;
    packsState.installing.set(progress.projectId, progress);
    syncPackButtons(progress.projectId);
});


// ---------------------------------------------------------------------------
// Servers: favorites with live status, popular servers, one-click join.
// ---------------------------------------------------------------------------

let pendingJoinServer = null;
const SERVERS_KEY = "flintfix.servers.v1";
const POPULAR_SERVERS = [
    { name: "Hypixel", address: "mc.hypixel.net" },
    { name: "CubeCraft", address: "play.cubecraft.net" },
    { name: "Wynncraft", address: "play.wynncraft.com" },
    { name: "MCC Island", address: "play.mccisland.net" }
];
const serversList = document.getElementById("serversList");
const serversPopular = document.getElementById("serversPopular");
const serverAddForm = document.getElementById("serverAddForm");
const serverAddressInput = document.getElementById("serverAddressInput");
const serverNameInput = document.getElementById("serverNameInput");
const serversInstanceSelect = document.getElementById("serversInstanceSelect");
const serversRefresh = document.getElementById("serversRefresh");
const serverStatus = new Map();
let serversRefreshTimer = null;

function loadServers() {
    try {
        const parsed = JSON.parse(localStorage.getItem(SERVERS_KEY) || "[]");
        return Array.isArray(parsed) ? parsed.filter(item => item && typeof item.address === "string") : [];
    } catch {
        return [];
    }
}

function saveServers(list) {
    localStorage.setItem(SERVERS_KEY, JSON.stringify(list));
}

function populateServersInstances() {
    if (!serversInstanceSelect) return;
    const previous = serversInstanceSelect.value || localStorage.getItem("flintfix.servers.instance") || "";
    serversInstanceSelect.innerHTML = "";
    if (!instances.length) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "Create an instance first";
        serversInstanceSelect.appendChild(option);
    }
    for (const instance of instances) {
        const option = document.createElement("option");
        option.value = instance.id;
        option.textContent = `${instance.name} — ${instance.version}`;
        serversInstanceSelect.appendChild(option);
    }
    if (instances.some(item => item.id === previous)) serversInstanceSelect.value = previous;
    enhanceFlintSelect(serversInstanceSelect);
    refreshFlintSelect(serversInstanceSelect);
}

function serverCard(server, { favorite }) {
    const status = serverStatus.get(server.address.toLowerCase());
    const card = document.createElement("article");
    card.className = `server-card${status?.online ? " online" : status ? " offline" : ""}`;

    const icon = document.createElement("div");
    icon.className = "server-icon";
    if (status?.icon) {
        const img = document.createElement("img");
        img.src = status.icon;
        img.alt = "";
        icon.appendChild(img);
    } else {
        icon.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">dns</span>';
    }

    const copy = document.createElement("div");
    copy.className = "server-copy";
    const title = document.createElement("strong");
    title.textContent = server.name || server.address;
    const address = document.createElement("small");
    address.textContent = server.address;
    const motd = document.createElement("p");
    motd.textContent = !status ? "Checking..." : status.online ? (status.motd || "No message") : (status.error || "Offline");
    copy.append(title, address, motd);

    const meta = document.createElement("div");
    meta.className = "server-meta";
    if (status?.online) {
        const players = document.createElement("span");
        players.className = "server-players";
        players.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">group</span>';
        players.append(`${formatDownloadCount(status.players)} / ${formatDownloadCount(status.maxPlayers)}`);
        const ping = document.createElement("span");
        const quality = status.latency <= 80 ? "good" : status.latency <= 160 ? "ok" : "bad";
        ping.className = `server-ping ${quality}`;
        ping.textContent = `${status.latency} ms`;
        meta.append(players, ping);
        if (status.version) {
            const version = document.createElement("span");
            version.className = "server-version";
            version.textContent = status.version;
            meta.appendChild(version);
        }
    }

    const actions = document.createElement("div");
    actions.className = "server-actions";
    const join = document.createElement("button");
    join.type = "button";
    join.className = "create-instance-button";
    join.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">play_arrow</span><span>Join</span>';
    join.addEventListener("click", () => joinServer(server));
    actions.appendChild(join);
    const second = document.createElement("button");
    second.type = "button";
    second.className = "mods-folder-button server-secondary";
    if (favorite) {
        second.setAttribute("aria-label", `Remove ${server.name || server.address}`);
        second.innerHTML = '<span class="material-symbols-rounded" aria-hidden="true">delete</span>';
        second.addEventListener("click", () => {
            saveServers(loadServers().filter(item => item.address.toLowerCase() !== server.address.toLowerCase()));
            renderServerLists();
        });
    } else {
        const saved = loadServers().some(item => item.address.toLowerCase() === server.address.toLowerCase());
        second.innerHTML = `<span class="material-symbols-rounded" aria-hidden="true">${saved ? "check" : "star"}</span>`;
        second.disabled = saved;
        second.setAttribute("aria-label", saved ? "Saved" : `Save ${server.name}`);
        second.addEventListener("click", () => {
            saveServers([...loadServers(), { name: server.name, address: server.address }]);
            renderServerLists();
        });
    }
    actions.appendChild(second);
    card.append(icon, copy, meta, actions);
    return card;
}

function renderServerLists() {
    if (!serversList || !serversPopular) return;
    const favorites = loadServers();
    serversList.innerHTML = "";
    if (!favorites.length) {
        const empty = document.createElement("div");
        empty.className = "mods-explore-empty";
        empty.textContent = "No saved servers yet. Add one above, or star a popular server.";
        serversList.appendChild(empty);
    }
    for (const server of favorites) serversList.appendChild(serverCard(server, { favorite: true }));
    serversPopular.innerHTML = "";
    for (const server of POPULAR_SERVERS) serversPopular.appendChild(serverCard(server, { favorite: false }));
}

async function refreshServerStatus() {
    const all = [...loadServers(), ...POPULAR_SERVERS];
    const unique = Array.from(new Map(all.map(server => [server.address.toLowerCase(), server])).values());
    await Promise.all(unique.map(async server => {
        const result = await window.flintfix.pingServer(server.address);
        serverStatus.set(server.address.toLowerCase(), result?.success ? result : { online: false, error: result?.error });
    }));
    renderServerLists();
}

async function renderServersPage() {
    populateServersInstances();
    renderServerLists();
    await refreshServerStatus();
    clearInterval(serversRefreshTimer);
    serversRefreshTimer = setInterval(() => {
        if (activeContentPage !== "servers") {
            clearInterval(serversRefreshTimer);
            return;
        }
        void refreshServerStatus();
    }, 30000);
}

function joinServer(server) {
    const instance = instances.find(item => item.id === serversInstanceSelect?.value) || instances[0];
    if (!instance) {
        showToast("Create an instance first, then join servers with it.", "error");
        setActiveNavigation("instances");
        return;
    }
    pendingJoinServer = { address: server.address, at: Date.now() };
    showToast(`Joining ${server.name || server.address} with ${instance.name}...`, "success");
    void launchInstance(instance);
}

if (serverAddForm) serverAddForm.addEventListener("submit", event => {
    event.preventDefault();
    const address = String(serverAddressInput?.value || "").trim();
    if (!/^[A-Za-z0-9.\-[\]:]{3,255}$/.test(address)) {
        showToast("Enter a valid server address.", "error");
        return;
    }
    const list = loadServers();
    if (list.some(item => item.address.toLowerCase() === address.toLowerCase())) {
        showToast("That server is already saved.", "error");
        return;
    }
    list.push({ name: String(serverNameInput?.value || "").trim().slice(0, 40) || address, address });
    saveServers(list);
    if (serverAddressInput) serverAddressInput.value = "";
    if (serverNameInput) serverNameInput.value = "";
    renderServerLists();
    void refreshServerStatus();
});
if (serversInstanceSelect) serversInstanceSelect.addEventListener("change", () => {
    localStorage.setItem("flintfix.servers.instance", serversInstanceSelect.value);
});
if (serversRefresh) serversRefresh.addEventListener("click", () => void refreshServerStatus());

// ---------------------------------------------------------------------------
// Skins: 3D preview, library, apply to the Minecraft account.
// ---------------------------------------------------------------------------

const skinCanvas = document.getElementById("skinCanvas");
const skinStageEmpty = document.getElementById("skinStageEmpty");
const skinStageName = document.getElementById("skinStageName");
const skinStageMeta = document.getElementById("skinStageMeta");
const skinGrid = document.getElementById("skinGrid");
const skinApply = document.getElementById("skinApply");
const skinDelete = document.getElementById("skinDelete");
const skinReset = document.getElementById("skinReset");
const skinImport = document.getElementById("skinImport");
const skinSaveCurrent = document.getElementById("skinSaveCurrent");
const skinModelButtons = Array.from(document.querySelectorAll("[data-skin-model]"));
const skinAnimButtons = Array.from(document.querySelectorAll("[data-skin-anim]"));
const skinState = { viewer: null, skins: [], selectedId: null, current: null, model: "classic", anim: "idle" };

function ensureSkinViewer() {
    if (skinState.viewer || !skinCanvas || !window.skinview3d) return skinState.viewer;
    const box = skinCanvas.parentElement.getBoundingClientRect();
    skinState.viewer = new window.skinview3d.SkinViewer({
        canvas: skinCanvas,
        width: Math.max(200, Math.round(box.width)),
        height: Math.max(260, Math.round(box.height)),
        zoom: 0.62
    });
    skinState.viewer.autoRotate = true;
    skinState.viewer.autoRotateSpeed = 0.6;
    setSkinAnimation(skinState.anim);
    new ResizeObserver(() => {
        const rect = skinCanvas.parentElement.getBoundingClientRect();
        skinState.viewer?.setSize(Math.max(200, Math.round(rect.width)), Math.max(260, Math.round(rect.height)));
    }).observe(skinCanvas.parentElement);
    return skinState.viewer;
}

function setSkinAnimation(name) {
    skinState.anim = name;
    skinAnimButtons.forEach(button => button.classList.toggle("active", button.dataset.skinAnim === name));
    const lib = window.skinview3d;
    if (!skinState.viewer || !lib) return;
    skinState.viewer.animation = name === "walk" ? new lib.WalkingAnimation()
        : name === "run" ? new lib.RunningAnimation() : new lib.IdleAnimation();
}

function showSkinInViewer(dataUrl, model, name, meta) {
    const viewer = ensureSkinViewer();
    skinState.model = model === "slim" ? "slim" : "classic";
    skinModelButtons.forEach(button => button.classList.toggle("active", button.dataset.skinModel === skinState.model));
    if (skinStageName) skinStageName.textContent = name || "Skin";
    if (skinStageMeta) skinStageMeta.textContent = meta || "Drag to rotate";
    if (skinStageEmpty) skinStageEmpty.hidden = Boolean(dataUrl);
    if (viewer && dataUrl) viewer.loadSkin(dataUrl, { model: skinState.model === "slim" ? "slim" : "default" });
}

/** Front view of a skin (head, body, arms, legs) for library tiles. */
function drawSkinFront(canvas, dataUrl) {
    const image = new Image();
    image.onload = () => {
        const ctx = canvas.getContext("2d");
        const s = canvas.width / 16;
        ctx.imageSmoothingEnabled = false;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        const part = (sx, sy, sw, sh, dx, dy) => ctx.drawImage(image, sx, sy, sw, sh, dx * s, dy * s, sw * s, sh * s);
        const legacy = image.height === 32;
        part(8, 8, 8, 8, 4, 0);
        part(20, 20, 8, 12, 4, 8);
        part(44, 20, 4, 12, 0, 8);
        if (legacy) {
            ctx.save();
            ctx.translate(16 * s, 0);
            ctx.scale(-1, 1);
            part(44, 20, 4, 12, 0, 8);
            part(4, 20, 4, 12, 4, 20);
            ctx.restore();
        } else {
            part(36, 52, 4, 12, 12, 8);
            part(20, 52, 4, 12, 8, 20);
        }
        part(4, 20, 4, 12, 4, 20);
        part(40, 8, 8, 8, 4, 0);
    };
    image.src = dataUrl;
}

function renderSkinGrid() {
    if (!skinGrid) return;
    skinGrid.innerHTML = "";
    if (!skinState.skins.length) {
        const empty = document.createElement("div");
        empty.className = "mods-explore-empty";
        empty.textContent = "Import a .png skin or save the one you're wearing.";
        skinGrid.appendChild(empty);
    }
    for (const skin of skinState.skins) {
        const tile = document.createElement("button");
        tile.type = "button";
        tile.className = `skin-tile${skin.id === skinState.selectedId ? " selected" : ""}`;
        tile.setAttribute("aria-label", skin.name);
        const canvas = document.createElement("canvas");
        canvas.width = 64;
        canvas.height = 128;
        drawSkinFront(canvas, skin.dataUrl);
        const name = document.createElement("span");
        name.textContent = skin.name;
        const variant = document.createElement("small");
        variant.textContent = skin.variant === "slim" ? "Slim" : "Classic";
        tile.append(canvas, name, variant);
        tile.addEventListener("click", () => {
            skinState.selectedId = skin.id;
            showSkinInViewer(skin.dataUrl, skin.variant, skin.name, "From your library");
            renderSkinGrid();
        });
        skinGrid.appendChild(tile);
    }
    if (skinApply) skinApply.disabled = !skinState.selectedId;
    if (skinDelete) skinDelete.disabled = !skinState.selectedId;
}

async function loadSkinLibrary() {
    const result = await window.flintfix.listSkins();
    skinState.skins = result?.success ? result.skins : [];
    if (!skinState.skins.some(skin => skin.id === skinState.selectedId)) skinState.selectedId = null;
    renderSkinGrid();
}

async function renderSkinsPage() {
    ensureSkinViewer();
    await loadSkinLibrary();
    if (skinState.selectedId) return;
    if (!minecraftProfile) {
        showSkinInViewer("", "classic", "Not signed in", "Sign in to see your skin");
        return;
    }
    const current = await window.flintfix.getCurrentSkin();
    if (current?.success && current.dataUrl) {
        skinState.current = current;
        showSkinInViewer(current.dataUrl, current.variant, current.name || "Your skin", "Currently wearing");
    } else {
        showSkinInViewer("", "classic", "Default skin", current?.error || "Import a skin to change it");
    }
}

skinModelButtons.forEach(button => button.addEventListener("click", async () => {
    const model = button.dataset.skinModel;
    const selected = skinState.skins.find(skin => skin.id === skinState.selectedId);
    if (selected) {
        await window.flintfix.updateSkin(selected.id, { variant: model });
        selected.variant = model;
        showSkinInViewer(selected.dataUrl, model, selected.name, "From your library");
        renderSkinGrid();
    } else if (skinState.current?.dataUrl) {
        showSkinInViewer(skinState.current.dataUrl, model, skinStageName?.textContent, "Preview only");
    }
}));
skinAnimButtons.forEach(button => button.addEventListener("click", () => setSkinAnimation(button.dataset.skinAnim)));
if (skinImport) skinImport.addEventListener("click", async () => {
    const result = await window.flintfix.importSkins(skinState.model);
    if (!result?.success) showToast(result?.error || "Could not import that skin.", "error");
    else if (result.added?.length) {
        skinState.selectedId = result.added[0].id;
        await loadSkinLibrary();
        const skin = skinState.skins.find(item => item.id === skinState.selectedId);
        if (skin) showSkinInViewer(skin.dataUrl, skin.variant, skin.name, "From your library");
        showToast(`${result.added.length} skin${result.added.length > 1 ? "s" : ""} added to your library.`, "success");
    }
});
if (skinSaveCurrent) skinSaveCurrent.addEventListener("click", async () => {
    const result = await window.flintfix.saveCurrentSkin();
    if (!result?.success) showToast(result?.error || "Could not save your current skin.", "error");
    else {
        await loadSkinLibrary();
        showToast("Saved your current skin to the library.", "success");
    }
});
if (skinApply) skinApply.addEventListener("click", async () => {
    const skin = skinState.skins.find(item => item.id === skinState.selectedId);
    if (!skin) return;
    skinApply.disabled = true;
    const result = await window.flintfix.applySkin(skin.id);
    skinApply.disabled = false;
    if (!result?.success) showToast(result?.error || "Could not change your skin.", "error");
    else showToast(`You're now wearing ${skin.name}. It shows in game after rejoining.`, "success");
});
if (skinDelete) skinDelete.addEventListener("click", async () => {
    if (!skinState.selectedId) return;
    if (skinDelete.dataset.confirm !== "1") {
        skinDelete.dataset.confirm = "1";
        skinDelete.textContent = "Click to confirm";
        setTimeout(() => {
            skinDelete.dataset.confirm = "";
            skinDelete.textContent = "Delete";
        }, 2500);
        return;
    }
    await window.flintfix.deleteSkin(skinState.selectedId);
    skinDelete.dataset.confirm = "";
    skinDelete.textContent = "Delete";
    skinState.selectedId = null;
    await loadSkinLibrary();
});
if (skinReset) skinReset.addEventListener("click", async () => {
    const result = await window.flintfix.resetSkin();
    if (!result?.success) showToast(result?.error || "Could not reset your skin.", "error");
    else {
        showToast("Your account is back on a default skin.", "success");
        skinState.selectedId = null;
        await renderSkinsPage();
    }
});

// ---------------------------------------------------------------------------
// What's new and FlintFix updates.
// ---------------------------------------------------------------------------

const newsTitle = document.getElementById("newsTitle");
const newsMeta = document.getElementById("newsMeta");
const newsItems = document.getElementById("newsItems");
const newsOlder = document.getElementById("newsOlder");
const newsToggle = document.getElementById("newsToggle");

function formatNewsDate(value) {
    try {
        return new Intl.DateTimeFormat(undefined, { month: "long", day: "numeric", year: "numeric" }).format(new Date(value));
    } catch {
        return value || "";
    }
}

async function loadNews() {
    const result = await window.flintfix.getNews();
    const posts = Array.isArray(result?.posts) ? result.posts : [];
    if (!posts.length || !newsItems) {
        document.getElementById("newsCard")?.setAttribute("hidden", "");
        return;
    }
    const [latest, ...older] = posts;
    const seen = localStorage.getItem("flintfix.news.seen");
    if (newsTitle) {
        newsTitle.textContent = latest.title || `Version ${latest.version}`;
        if (seen !== latest.version) {
            const badge = document.createElement("span");
            badge.className = "news-new";
            badge.textContent = "NEW";
            newsTitle.appendChild(badge);
        }
    }
    if (newsMeta) newsMeta.textContent = [latest.version ? `Version ${latest.version}` : "", formatNewsDate(latest.date)].filter(Boolean).join(" • ");
    newsItems.innerHTML = "";
    for (const item of latest.items || []) {
        const li = document.createElement("li");
        li.textContent = item;
        newsItems.appendChild(li);
    }
    if (newsOlder) {
        newsOlder.innerHTML = "";
        for (const post of older) {
            const block = document.createElement("div");
            block.className = "news-older-post";
            const heading = document.createElement("strong");
            heading.textContent = `${post.title || ""}${post.version ? ` — ${post.version}` : ""}`;
            const list = document.createElement("ul");
            for (const item of post.items || []) {
                const li = document.createElement("li");
                li.textContent = item;
                list.appendChild(li);
            }
            block.append(heading, list);
            newsOlder.appendChild(block);
        }
    }
    if (newsToggle) newsToggle.hidden = !older.length;
    localStorage.setItem("flintfix.news.seen", latest.version || "");
}

if (newsToggle) newsToggle.addEventListener("click", () => {
    if (!newsOlder) return;
    newsOlder.hidden = !newsOlder.hidden;
    newsToggle.textContent = newsOlder.hidden ? "Earlier updates" : "Hide earlier updates";
});

const updateBanner = document.getElementById("updateBanner");
const updateBannerTitle = document.getElementById("updateBannerTitle");
const updateBannerText = document.getElementById("updateBannerText");
const updateBannerAction = document.getElementById("updateBannerAction");
const updateBannerDismiss = document.getElementById("updateBannerDismiss");
const updateBannerProgress = document.getElementById("updateBannerProgress");
let updateInfo = null;

function renderUpdateBanner(state) {
    if (!updateBanner || !state) return;
    updateInfo = state;
    const dismissed = localStorage.getItem("flintfix.update.dismissed");
    const show = ["available", "downloading", "ready"].includes(state.status) && dismissed !== `${state.version}:${state.status}`;
    updateBanner.hidden = !show;
    if (!show) return;
    if (updateBannerProgress) {
        updateBannerProgress.hidden = state.status !== "downloading";
        const bar = updateBannerProgress.querySelector("span");
        if (bar) bar.style.width = `${state.percent || 0}%`;
    }
    if (state.status === "ready") {
        updateBannerTitle.textContent = `FlintFix ${state.version} is ready`;
        updateBannerText.textContent = "Restart the launcher to finish updating.";
        updateBannerAction.hidden = false;
        updateBannerAction.textContent = "Restart now";
    } else if (state.status === "downloading") {
        updateBannerTitle.textContent = `Downloading FlintFix ${state.version || "update"}`;
        updateBannerText.textContent = `${state.percent || 0}% — you can keep using the launcher.`;
        updateBannerAction.hidden = true;
    } else {
        updateBannerTitle.textContent = `FlintFix ${state.version} is available`;
        updateBannerText.textContent = `You have ${state.currentVersion}. Get the new version for the latest features and fixes.`;
        updateBannerAction.hidden = false;
        updateBannerAction.textContent = "Download";
    }
}

if (updateBannerAction) updateBannerAction.addEventListener("click", () => {
    if (updateInfo?.status === "ready") void window.flintfix.installUpdate();
    else if (updateInfo?.url) window.flintfix.openExternal(updateInfo.url);
});
if (updateBannerDismiss) updateBannerDismiss.addEventListener("click", () => {
    if (updateInfo) localStorage.setItem("flintfix.update.dismissed", `${updateInfo.version}:${updateInfo.status}`);
    if (updateBanner) updateBanner.hidden = true;
});
window.flintfix.onUpdateStatus?.(renderUpdateBanner);

async function checkForFlintFixUpdates() {
    const result = await window.flintfix.checkForUpdates();
    if (result?.success) renderUpdateBanner(result);
}

// ---------------------------------------------------------------------------
// Performance mode: install proven performance mods into the instance.
// ---------------------------------------------------------------------------

const performanceModeButton = document.getElementById("performanceModeButton");
if (performanceModeButton) performanceModeButton.addEventListener("click", async () => {
    const instance = getSelectedModsInstance();
    if (!instance) {
        showToast("Choose an instance first.", "error");
        return;
    }
    if (instance.loader !== "fabric") {
        showToast("Performance mode needs a Fabric instance.", "error");
        return;
    }
    if (performanceModeButton.dataset.confirm !== "1") {
        performanceModeButton.dataset.confirm = "1";
        const label = performanceModeButton.lastChild;
        const original = label.textContent;
        label.textContent = " Click again: install Sodium, Lithium + 4 more";
        setTimeout(() => {
            performanceModeButton.dataset.confirm = "";
            label.textContent = original;
        }, 3500);
        return;
    }
    performanceModeButton.dataset.confirm = "";
    performanceModeButton.disabled = true;
    const label = performanceModeButton.lastChild;
    label.textContent = " Installing...";
    const stop = window.flintfix.onPerformanceProgress?.(progress => {
        label.textContent = ` Installing ${progress.title}...`;
    });
    const result = await window.flintfix.installPerformanceMods(instance.id, { version: instance.version });
    stop?.();
    performanceModeButton.disabled = false;
    label.textContent = " Performance mode";
    if (!result?.success) {
        showToast(result?.error || "Performance mode failed.", "error");
        return;
    }
    const installed = result.results.filter(item => item.status === "installed").map(item => item.title);
    const failed = result.results.filter(item => item.status === "failed");
    if (installed.length) showToast(`Installed ${installed.join(", ")}.`, "success");
    else if (!failed.length) showToast("All performance mods are already installed.", "success");
    if (failed.length) showToast(`Not available for ${instance.version}: ${failed.map(item => item.title).join(", ")}.`, "error");
    modUpdateMap.clear();
    await renderModsPage();
});

setTimeout(() => {
    void loadNews();
    void checkForFlintFixUpdates();
}, 1500);
setInterval(() => void checkForFlintFixUpdates(), 6 * 60 * 60 * 1000);


initializeFlintFix();
