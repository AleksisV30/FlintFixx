const fs = require("fs");
const path = require("path");
const https = require("https");
const crypto = require("crypto");
const { PublicClientApplication } = require("@azure/msal-node");
const { safeStorage, BrowserWindow } = require("electron");

const CLIENT_ID = "ef8ee883-da59-4624-894f-d78ad0f46c94";
const AUTHORITY = "https://login.microsoftonline.com/consumers";
const SCOPES = ["XboxLive.signin", "offline_access"];

let pca = null;
let cacheFile = null;

function requestJson(url, options = {}, body = null) {
    return new Promise((resolve, reject) => {
        const parsed = new URL(url);
        const payload = body == null ? null : JSON.stringify(body);

        const req = https.request({
            protocol: parsed.protocol,
            hostname: parsed.hostname,
            port: parsed.port || 443,
            path: `${parsed.pathname}${parsed.search}`,
            method: options.method || (payload ? "POST" : "GET"),
            headers: {
                "User-Agent": "FlintFix-Client/0.1",
                "Accept": "application/json",
                ...(payload ? {
                    "Content-Type": "application/json",
                    "Content-Length": Buffer.byteLength(payload)
                } : {}),
                ...(options.headers || {})
            }
        }, response => {
            let data = "";
            response.setEncoding("utf8");
            response.on("data", chunk => data += chunk);
            response.on("end", () => {
                let parsedBody = null;
                try {
                    parsedBody = data ? JSON.parse(data) : null;
                } catch {
                    parsedBody = data;
                }

                if (response.statusCode < 200 || response.statusCode >= 300) {
                    const message =
                        parsedBody?.errorMessage ||
                        parsedBody?.error?.message ||
                        parsedBody?.error_description ||
                        (typeof parsedBody === "string" ? parsedBody : "") ||
                        `HTTP ${response.statusCode}`;

                    const error = new Error(`HTTP ${response.statusCode}: ${message}`);
                    error.statusCode = response.statusCode;
                    error.body = parsedBody;
                    reject(error);
                    return;
                }

                resolve(parsedBody);
            });
        });

        req.on("error", reject);
        if (payload) req.write(payload);
        req.end();
    });
}

async function loadCache() {
    if (!pca || !cacheFile || !fs.existsSync(cacheFile)) return;

    try {
        if (!safeStorage.isEncryptionAvailable()) return;
        const encrypted = Buffer.from(await fs.promises.readFile(cacheFile, "utf8"), "base64");
        const serialized = safeStorage.decryptString(encrypted);
        pca.getTokenCache().deserialize(serialized);
    } catch (error) {
        console.warn("Couldn't load Microsoft token cache:", error.message);
    }
}

async function saveCache() {
    if (!pca || !cacheFile || !safeStorage.isEncryptionAvailable()) return;

    const serialized = pca.getTokenCache().serialize();
    const encrypted = safeStorage.encryptString(serialized);

    await fs.promises.mkdir(path.dirname(cacheFile), { recursive: true });
    await fs.promises.writeFile(cacheFile, encrypted.toString("base64"), "utf8");
}

async function initialize(userDataDirectory) {
    cacheFile = path.join(userDataDirectory, "auth", "msal-cache.dat");

    pca = new PublicClientApplication({
        auth: {
            clientId: CLIENT_ID,
            authority: AUTHORITY
        },
        system: {
            loggerOptions: {
                piiLoggingEnabled: false,
                logLevel: 2,
                loggerCallback: () => {}
            }
        }
    });

    await loadCache();
}

async function getAccounts() {
    if (!pca) return [];
    return pca.getTokenCache().getAllAccounts();
}

async function getAccount() {
    const accounts = await getAccounts();
    return accounts[0] || null;
}

async function getAccountById(homeAccountId) {
    const accounts = await getAccounts();
    return accounts.find(account => account.homeAccountId === homeAccountId) || null;
}

function base64Url(buffer) {
    return buffer
        .toString("base64")
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/g, "");
}

function createPkceCodes() {
    const verifier = base64Url(crypto.randomBytes(64));
    const challenge = base64Url(
        crypto.createHash("sha256").update(verifier).digest()
    );

    return {
        verifier,
        challenge
    };
}

function createState() {
    return base64Url(crypto.randomBytes(32));
}

function openMicrosoftAuthWindow(authUrl, expectedState) {
    return new Promise((resolve, reject) => {
        let finished = false;

        const authWindow = new BrowserWindow({
            width: 520,
            height: 720,
            minWidth: 420,
            minHeight: 600,
            title: "Sign in to Microsoft",
            backgroundColor: "#0d0d10",
            autoHideMenuBar: true,
            show: false,
            webPreferences: {
                contextIsolation: true,
                nodeIntegration: false,
                sandbox: true
            }
        });

        const finish = (error, result = null) => {
            if (finished) return;
            finished = true;

            if (!authWindow.isDestroyed()) {
                authWindow.close();
            }

            if (error) reject(error);
            else resolve(result);
        };

        const handleRedirect = (event, navigationUrl) => {
            let parsed;

            try {
                parsed = new URL(navigationUrl);
            } catch {
                return;
            }

            if (
                parsed.protocol !== "msalef8ee883-da59-4624-894f-d78ad0f46c94:" ||
                parsed.hostname !== "auth"
            ) {
                return;
            }

            event.preventDefault();

            const returnedState = parsed.searchParams.get("state");
            const code = parsed.searchParams.get("code");
            const oauthError = parsed.searchParams.get("error");
            const oauthDescription =
                parsed.searchParams.get("error_description");

            if (returnedState !== expectedState) {
                finish(
                    new Error(
                        "Microsoft sign-in returned an invalid state value."
                    )
                );
                return;
            }

            if (oauthError) {
                finish(
                    new Error(
                        oauthDescription ||
                        `Microsoft sign-in failed: ${oauthError}`
                    )
                );
                return;
            }

            if (!code) {
                finish(
                    new Error(
                        "Microsoft sign-in did not return an authorization code."
                    )
                );
                return;
            }

            finish(null, {
                code,
                state: returnedState,
                clientInfo: parsed.searchParams.get("client_info") || undefined
            });
        };

        authWindow.webContents.on("will-redirect", handleRedirect);
        authWindow.webContents.on("will-navigate", handleRedirect);

        authWindow.webContents.setWindowOpenHandler(({ url }) => {
            // Microsoft may try to open a continuation in a new window.
            // Keep HTTPS navigation inside the isolated auth window.
            if (/^https:\/\//i.test(url)) {
                authWindow.loadURL(url).catch(error => finish(error));
            }

            return { action: "deny" };
        });

        authWindow.once("ready-to-show", () => {
            authWindow.show();
            authWindow.focus();
        });

        authWindow.on("closed", () => {
            if (!finished) {
                finished = true;
                reject(
                    new Error(
                        "Microsoft sign-in window was closed before authentication completed."
                    )
                );
            }
        });

        authWindow.loadURL(authUrl).catch(error => finish(error));
    });
}

async function acquireMicrosoftToken(_unusedCallback = null, forceInteractive = false) {
    if (!pca) {
        throw new Error(
            "Microsoft authentication has not been initialized."
        );
    }

    const account = await getAccount();

    if (account && !forceInteractive) {
        try {
            const result = await pca.acquireTokenSilent({
                account,
                scopes: SCOPES
            });

            await saveCache();
            return result;
        } catch {
            // Fall through to authorization-code + PKCE.
        }
    }

    const redirectUri =
        "msalef8ee883-da59-4624-894f-d78ad0f46c94://auth";

    const { verifier, challenge } = createPkceCodes();
    const state = createState();

    const authUrl = await pca.getAuthCodeUrl({
        scopes: SCOPES,
        redirectUri,
        responseMode: "query",
        codeChallenge: challenge,
        codeChallengeMethod: "S256",
        state,
        prompt: forceInteractive ? "select_account" : undefined
    });

    const authResponse =
        await openMicrosoftAuthWindow(authUrl, state);

    const tokenRequest = {
        code: authResponse.code,
        scopes: SCOPES,
        redirectUri,
        codeVerifier: verifier
    };

    // FlintFix already validates the OAuth state returned by Microsoft
    // before reaching this point. For PublicClientApplication, pass only
    // AuthorizationCodeRequest here. Supplying a second hand-built
    // AuthorizationCodePayload makes MSAL try to parse our opaque state as
    // MSAL's own encoded library state, which causes invalid_state.
    const result =
        await pca.acquireTokenByCode(tokenRequest);

    if (!result) {
        throw new Error(
            "Microsoft sign-in did not return a token."
        );
    }

    await saveCache();
    return result;
}

async function acquireMicrosoftTokenForAccount(homeAccountId) {
    if (!pca) {
        throw new Error("Microsoft authentication has not been initialized.");
    }

    const account = await getAccountById(homeAccountId);
    if (!account) {
        throw new Error("The selected Microsoft account is no longer connected.");
    }

    try {
        const result = await pca.acquireTokenSilent({
            account,
            scopes: SCOPES
        });
        await saveCache();
        return result;
    } catch (error) {
        const wrapped = new Error(
            "This account needs to sign in again before FlintFix can use it."
        );
        wrapped.code = "ACCOUNT_REAUTH_REQUIRED";
        wrapped.cause = error;
        throw wrapped;
    }
}

async function xboxAuthenticate(microsoftAccessToken) {
    return requestJson(
        "https://user.auth.xboxlive.com/user/authenticate",
        { method: "POST" },
        {
            Properties: {
                AuthMethod: "RPS",
                SiteName: "user.auth.xboxlive.com",
                RpsTicket: `d=${microsoftAccessToken}`
            },
            RelyingParty: "http://auth.xboxlive.com",
            TokenType: "JWT"
        }
    );
}

async function xstsAuthenticate(xboxToken) {
    return requestJson(
        "https://xsts.auth.xboxlive.com/xsts/authorize",
        { method: "POST" },
        {
            Properties: {
                SandboxId: "RETAIL",
                UserTokens: [xboxToken]
            },
            RelyingParty: "rp://api.minecraftservices.com/",
            TokenType: "JWT"
        }
    );
}

async function minecraftAuthenticate(userHash, xstsToken) {
    try {
        return await requestJson(
            "https://api.minecraftservices.com/authentication/login_with_xbox",
            { method: "POST" },
            {
                identityToken: `XBL3.0 x=${userHash};${xstsToken}`
            }
        );
    } catch (error) {
        if (
            error.statusCode === 403 &&
            /invalid app registration/i.test(error.message || "")
        ) {
            const wrapped = new Error(
                "Minecraft Services rejected the FlintFix client ID: Invalid app registration. " +
                "Microsoft/Xbox sign-in succeeded, but this app registration is not currently authorized by Minecraft Services."
            );
            wrapped.code = "MINECRAFT_APP_NOT_AUTHORIZED";
            throw wrapped;
        }
        throw error;
    }
}

async function getMinecraftProfile(accessToken) {
    return requestJson(
        "https://api.minecraftservices.com/minecraft/profile",
        {
            method: "GET",
            headers: {
                Authorization: `Bearer ${accessToken}`
            }
        }
    );
}

async function getMinecraftEntitlements(accessToken) {
    return requestJson(
        "https://api.minecraftservices.com/entitlements/mcstore",
        {
            method: "GET",
            headers: {
                Authorization: `Bearer ${accessToken}`
            }
        }
    );
}

async function createMinecraftSessionFromMicrosoft(microsoft) {
    const xbox = await xboxAuthenticate(microsoft.accessToken);

    const userHash =
        xbox?.DisplayClaims?.xui?.[0]?.uhs;

    if (!userHash || !xbox?.Token) {
        throw new Error("Xbox authentication did not return the expected user token.");
    }

    const xsts = await xstsAuthenticate(xbox.Token);

    const xstsUserHash =
        xsts?.DisplayClaims?.xui?.[0]?.uhs || userHash;

    if (!xsts?.Token) {
        throw new Error("XSTS authentication did not return a token.");
    }

    const minecraft = await minecraftAuthenticate(xstsUserHash, xsts.Token);

    if (!minecraft?.access_token) {
        throw new Error("Minecraft Services did not return an access token.");
    }

    const [profile, entitlements] = await Promise.all([
        getMinecraftProfile(minecraft.access_token),
        getMinecraftEntitlements(minecraft.access_token)
    ]);

    if (!profile?.id || !profile?.name) {
        throw new Error(
            "No Minecraft: Java Edition profile was found for this Microsoft account."
        );
    }

    const skinUrl = Array.isArray(profile.skins)
        ? profile.skins.find(skin => skin?.state === "ACTIVE")?.url || profile.skins[0]?.url || null
        : null;

    return {
        account: {
            homeAccountId: microsoft.account?.homeAccountId || null,
            username: microsoft.account?.username || null
        },
        minecraft: {
            accessToken: minecraft.access_token,
            expiresIn: minecraft.expires_in || null,
            username: profile.name,
            uuid: profile.id,
            profile,
            skinUrl,
            entitlements
        }
    };
}

async function createMinecraftSession(deviceCodeCallback = null, forceInteractive = false) {
    const microsoft = await acquireMicrosoftToken(deviceCodeCallback, forceInteractive);
    return createMinecraftSessionFromMicrosoft(microsoft);
}

async function createMinecraftSessionForAccount(homeAccountId) {
    const microsoft = await acquireMicrosoftTokenForAccount(homeAccountId);
    return createMinecraftSessionFromMicrosoft(microsoft);
}

async function listMinecraftAccounts() {
    const accounts = await getAccounts();
    const results = [];

    for (const account of accounts) {
        const base = {
            homeAccountId: account.homeAccountId,
            microsoftUsername: account.username || null,
            minecraft: null,
            needsReauth: false
        };

        try {
            const session = await createMinecraftSessionForAccount(account.homeAccountId);
            results.push({
                ...base,
                minecraft: {
                    username: session.minecraft.username,
                    uuid: session.minecraft.uuid,
                    skinUrl: session.minecraft.skinUrl || null
                }
            });
        } catch (error) {
            results.push({
                ...base,
                needsReauth: true,
                error: error.message
            });
        }
    }

    return results;
}

async function getStatus() {
    if (!pca) return { signedIn: false };

    const account = await getAccount();
    if (!account) return { signedIn: false };

    const accounts = await getAccounts();
    return {
        signedIn: true,
        microsoftUsername: account.username || null,
        accountCount: accounts.length
    };
}

async function signOut() {
    if (!pca) return;

    const cache = pca.getTokenCache();
    const accounts = await cache.getAllAccounts();

    for (const account of accounts) {
        await cache.removeAccount(account);
    }

    if (cacheFile) {
        try {
            await fs.promises.rm(cacheFile, { force: true });
        } catch {
            // Ignore cache cleanup failure.
        }
    }
}

module.exports = {
    initialize,
    getStatus,
    getAccounts,
    listMinecraftAccounts,
    createMinecraftSession,
    createMinecraftSessionForAccount,
    signOut
};
