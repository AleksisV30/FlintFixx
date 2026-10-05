const fs = require("fs");
const path = require("path");
const https = require("https");
const crypto = require("crypto");
const os = require("os");

const extract = require("extract-zip");


// ============================================================
// FLINTFIX MINECRAFT INSTALLER
// ============================================================
//
// Vanilla Minecraft installation layer.
//
// Features:
//
// - Minecraft version metadata
// - Client JAR
// - Libraries
// - Windows natives
// - Asset index
// - Minecraft assets
// - SHA-1 verification
// - Concurrent downloads/checks
// - Fast verified-install cache
//
// ============================================================


// ============================================================
// CONFIGURATION
// ============================================================

const LIBRARY_CONCURRENCY = 12;

const ASSET_CONCURRENCY = 32;

const PROGRESS_INTERVAL_MS = 200;

// Reuse HTTPS sockets during first-time installs. Minecraft has thousands of
// small asset/library requests, so avoiding a new TLS connection for every
// file noticeably reduces startup overhead on fast connections.
const DOWNLOAD_AGENT = new https.Agent({
    keepAlive: true,
    maxSockets: 48,
    maxFreeSockets: 16,
    timeout: 30_000
});


// Increment this if the installation format changes
// significantly in the future.
//
// Doing that will automatically force one new full
// verification pass.

const INSTALL_RECORD_VERSION = 2;


// ============================================================
// ALLOWED MOJANG DOWNLOAD HOSTS
// ============================================================

const MOJANG_HOSTS = new Set([
    "piston-meta.mojang.com",
    "launchermeta.mojang.com",
    "piston-data.mojang.com",
    "launcher.mojang.com",
    "libraries.minecraft.net",
    "resources.download.minecraft.net"
]);


// ============================================================
// DIRECTORY HELPER
// ============================================================

function mkdir(directory) {

    fs.mkdirSync(
        directory,
        {
            recursive: true
        }
    );
}


// ============================================================
// SHA-1
// ============================================================

function sha1File(file) {

    return new Promise(
        (resolve, reject) => {

            const hash =
                crypto.createHash(
                    "sha1"
                );


            const stream =
                fs.createReadStream(
                    file
                );


            stream.on(
                "data",
                chunk => {

                    hash.update(
                        chunk
                    );
                }
            );


            stream.on(
                "error",
                reject
            );


            stream.on(
                "end",
                () => {

                    resolve(
                        hash.digest(
                            "hex"
                        )
                    );
                }
            );
        }
    );
}


// ============================================================
// FILE VALIDATION
// ============================================================

async function isValid(
    file,
    sha1,
    size
) {

    if (
        !fs.existsSync(file)
    ) {

        return false;
    }


    let stat;


    try {

        stat =
            await fs.promises.stat(
                file
            );

    } catch {

        return false;
    }


    // --------------------------------------------------------
    // SIZE FIRST
    // --------------------------------------------------------
    //
    // This is much cheaper than SHA-1.
    //
    // If the size is already wrong, don't waste time hashing.

    if (
        size != null &&
        stat.size !== Number(size)
    ) {

        return false;
    }


    // --------------------------------------------------------
    // SHA-1
    // --------------------------------------------------------

    if (sha1) {

        const actualHash =
            await sha1File(
                file
            );


        if (
            actualHash.toLowerCase() !==
            String(sha1).toLowerCase()
        ) {

            return false;
        }
    }


    return true;
}


// ============================================================
// URL SECURITY
// ============================================================

function validateUrl(
    url,
    allowAssetCdn = true
) {

    const parsed =
        new URL(url);


    if (
        parsed.protocol !==
        "https:"
    ) {

        throw new Error(
            "Only HTTPS downloads are allowed."
        );
    }


    if (
        !MOJANG_HOSTS.has(
            parsed.hostname
        )
    ) {

        throw new Error(
            `Blocked download host: ${parsed.hostname}`
        );
    }


    if (
        !allowAssetCdn &&
        parsed.hostname ===
            "resources.download.minecraft.net"
    ) {

        throw new Error(
            "Asset CDN is not valid for this download."
        );
    }


    return parsed.toString();
}


// ============================================================
// DOWNLOAD
// ============================================================

function download(
    url,
    destination,
    onProgress,
    redirects = 0
) {

    return new Promise(
        (resolve, reject) => {

            if (
                redirects > 8
            ) {

                reject(
                    new Error(
                        "Too many redirects."
                    )
                );

                return;
            }


            let safeUrl;


            try {

                safeUrl =
                    validateUrl(
                        url
                    );

            } catch (error) {

                reject(error);

                return;
            }


            mkdir(
                path.dirname(
                    destination
                )
            );


            const request =
                https.get(

                    safeUrl,

                    {
                        agent: DOWNLOAD_AGENT,
                        headers: {

                            "User-Agent":
                                "FlintFix-Client/0.56"
                        }
                    },

                    response => {

                        // ====================================
                        // REDIRECT
                        // ====================================

                        if (
                            response.statusCode >= 300 &&
                            response.statusCode < 400 &&
                            response.headers.location
                        ) {

                            response.resume();


                            try {

                                const next =
                                    new URL(
                                        response.headers.location,
                                        safeUrl
                                    ).toString();


                                validateUrl(
                                    next
                                );


                                download(
                                    next,
                                    destination,
                                    onProgress,
                                    redirects + 1
                                )
                                    .then(resolve)
                                    .catch(reject);

                            } catch (error) {

                                reject(
                                    error
                                );
                            }


                            return;
                        }


                        // ====================================
                        // HTTP ERROR
                        // ====================================

                        if (
                            response.statusCode !==
                            200
                        ) {

                            response.resume();


                            reject(
                                new Error(
                                    `Download failed with HTTP ${response.statusCode}`
                                )
                            );


                            return;
                        }


                        // ====================================
                        // DOWNLOAD BODY
                        // ====================================

                        const total =
                            Number(
                                response.headers[
                                    "content-length"
                                ]
                            ) || 0;


                        let received = 0;


                        const temp =
                            `${destination}.part`;


                        try {

                            fs.rmSync(
                                temp,
                                {
                                    force: true
                                }
                            );

                        } catch {
                            // Ignore stale temp cleanup.
                        }


                        const file =
                            fs.createWriteStream(
                                temp
                            );


                        response.on(
                            "data",
                            chunk => {

                                received +=
                                    chunk.length;


                                if (
                                    onProgress
                                ) {

                                    onProgress(
                                        received,
                                        total
                                    );
                                }
                            }
                        );


                        response.pipe(
                            file
                        );


                        file.on(
                            "finish",
                            () => {

                                file.close(
                                    () => {

                                        try {

                                            fs.rmSync(
                                                destination,
                                                {
                                                    force: true
                                                }
                                            );


                                            fs.renameSync(
                                                temp,
                                                destination
                                            );


                                            resolve();

                                        } catch (error) {

                                            reject(
                                                error
                                            );
                                        }
                                    }
                                );
                            }
                        );


                        file.on(
                            "error",
                            error => {

                                try {

                                    fs.rmSync(
                                        temp,
                                        {
                                            force: true
                                        }
                                    );

                                } catch {
                                    // Ignore cleanup error.
                                }


                                reject(
                                    error
                                );
                            }
                        );
                    }
                );


            request.on(
                "error",
                error => {

                    try {

                        fs.rmSync(
                            `${destination}.part`,
                            {
                                force: true
                            }
                        );

                    } catch {
                        // Ignore cleanup error.
                    }


                    reject(
                        error
                    );
                }
            );
        }
    );
}


// ============================================================
// ENSURE DOWNLOAD
// ============================================================

async function ensureDownload(
    item,
    emit
) {

    // --------------------------------------------------------
    // ALREADY VALID?
    // --------------------------------------------------------

    if (
        await isValid(
            item.path,
            item.sha1,
            item.size
        )
    ) {

        return false;
    }


    // --------------------------------------------------------
    // DOWNLOAD
    // --------------------------------------------------------

    await download(

        item.url,

        item.path,

        (received, total) => {

            if (!emit) {
                return;
            }


            emit({

                stage:
                    item.stage,

                current:
                    item.current,

                total:
                    item.total,

                received,

                bytesTotal:
                    total,

                label:
                    item.label
            });
        }
    );


    // --------------------------------------------------------
    // VERIFY DOWNLOAD
    // --------------------------------------------------------

    const valid =
        await isValid(
            item.path,
            item.sha1,
            item.size
        );


    if (!valid) {

        try {

            fs.rmSync(
                item.path,
                {
                    force: true
                }
            );

        } catch {
            // Ignore cleanup error.
        }


        throw new Error(
            `Hash/size verification failed: ${item.label}`
        );
    }


    return true;
}


// ============================================================
// CONCURRENT WORKER POOL
// ============================================================

async function runWorkerPool(
    items,
    concurrency,
    worker
) {

    if (
        !Array.isArray(items) ||
        items.length === 0
    ) {

        return;
    }


    let nextIndex = 0;


    async function runWorker() {

        while (true) {

            const index =
                nextIndex++;


            if (
                index >=
                items.length
            ) {

                return;
            }


            await worker(
                items[index],
                index
            );
        }
    }


    const workerCount =
        Math.min(
            Math.max(
                1,
                concurrency
            ),
            items.length
        );


    const workers =
        Array.from(
            {
                length:
                    workerCount
            },

            () =>
                runWorker()
        );


    await Promise.all(
        workers
    );
}


// ============================================================
// RULE MATCHING
// ============================================================

function ruleMatches(
    rule
) {

    if (
        !rule.os
    ) {

        return true;
    }


    if (
        rule.os.name &&
        rule.os.name !==
            "windows"
    ) {

        return false;
    }


    if (
        rule.os.arch
    ) {

        const arch =
            process.arch === "x64"
                ? "x86_64"
                : process.arch === "ia32"
                    ? "x86"
                    : process.arch;


        if (
            rule.os.arch !==
            arch
        ) {

            return false;
        }
    }


    if (
        rule.os.version
    ) {

        try {

            const expression =
                new RegExp(
                    rule.os.version
                );


            if (
                !expression.test(
                    os.release()
                )
            ) {

                return false;
            }

        } catch {

            return false;
        }
    }


    return true;
}


// ============================================================
// LIBRARY RULES
// ============================================================

function libraryAllowed(
    library
) {

    if (
        !library.rules ||
        library.rules.length === 0
    ) {

        return true;
    }


    let allowed = false;


    for (
        const rule of
        library.rules
    ) {

        if (
            ruleMatches(
                rule
            )
        ) {

            allowed =
                rule.action ===
                "allow";
        }
    }


    return allowed;
}


// ============================================================
// WINDOWS NATIVE CLASSIFIER
// ============================================================

function nativeClassifier(
    library
) {

    if (
        !library.natives ||
        !library.natives.windows
    ) {

        return null;
    }


    const arch =
        process.arch === "x64"
            ? "64"
            : process.arch === "ia32"
                ? "32"
                : process.arch;


    return library.natives.windows.replace(
        "${arch}",
        arch
    );
}


// ============================================================
// JSON DOWNLOAD
// ============================================================

async function fetchJson(
    url,
    destination,
    sha1,
    emit,
    label
) {

    await ensureDownload(

        {
            url:
                validateUrl(
                    url,
                    false
                ),

            path:
                destination,

            sha1,

            stage:
                "metadata",

            label
        },

        emit
    );


    const text =
        await fs.promises.readFile(
            destination,
            "utf8"
        );


    return JSON.parse(
        text
    );
}


// ============================================================
// FAST VERIFIED INSTALL CACHE
// ============================================================
//
// This is intentionally different from the full verifier.
//
// A completed installation has already had:
//
// - client SHA-1 checked
// - libraries SHA-1 checked
// - assets SHA-1 checked
// - native libraries extracted
//
// On later PLAY presses we can use the installation record
// as a fast path instead of re-reading thousands of assets.
//
// A future "Repair Installation" command should deliberately
// bypass this cache and perform full verification.
// ============================================================

async function getCachedInstallation(
    rootDir,
    versionId,
    metadataSha1
) {

    const versionsDir =
        path.join(
            rootDir,
            "versions",
            versionId
        );


    const recordPath =
        path.join(
            versionsDir,
            "flintfix-install.json"
        );


    const clientPath =
        path.join(
            versionsDir,
            `${versionId}.jar`
        );


    const versionJsonPath =
        path.join(
            versionsDir,
            `${versionId}.json`
        );


    // --------------------------------------------------------
    // BASIC FILES
    // --------------------------------------------------------

    if (
        !fs.existsSync(
            recordPath
        ) ||
        !fs.existsSync(
            clientPath
        ) ||
        !fs.existsSync(
            versionJsonPath
        )
    ) {

        return null;
    }


    try {

        const recordText =
            await fs.promises.readFile(
                recordPath,
                "utf8"
            );


        const record =
            JSON.parse(
                recordText
            );


        // ----------------------------------------------------
        // INSTALL RECORD FORMAT
        // ----------------------------------------------------

        if (
            record.installRecordVersion !==
            INSTALL_RECORD_VERSION
        ) {

            return null;
        }


        // ----------------------------------------------------
        // VERSION
        // ----------------------------------------------------

        if (
            record.versionId !==
            versionId
        ) {

            return null;
        }


        // ----------------------------------------------------
        // VERIFIED MARKER
        // ----------------------------------------------------

        if (
            record.verified !==
            true
        ) {

            return null;
        }


        // ----------------------------------------------------
        // METADATA HASH
        // ----------------------------------------------------
        //
        // If Mojang's manifest gives us a SHA-1, make sure
        // we're using the exact metadata that was originally
        // verified.

        if (
            metadataSha1 &&
            record.metadataSha1 !==
                metadataSha1
        ) {

            return null;
        }


        // ----------------------------------------------------
        // CLIENT
        // ----------------------------------------------------

        if (
            !record.clientJar ||
            !fs.existsSync(
                record.clientJar
            )
        ) {

            return null;
        }


        // ----------------------------------------------------
        // NATIVES
        // ----------------------------------------------------

        if (
            !record.nativesDir ||
            !fs.existsSync(
                record.nativesDir
            )
        ) {

            return null;
        }


        // ----------------------------------------------------
        // ASSETS + LIBRARIES
        // ----------------------------------------------------

        const assetsDir =
            path.join(
                rootDir,
                "assets"
            );


        const librariesDir =
            path.join(
                rootDir,
                "libraries"
            );


        if (
            !fs.existsSync(
                assetsDir
            ) ||
            !fs.existsSync(
                librariesDir
            )
        ) {

            return null;
        }


        // ----------------------------------------------------
        // ASSET INDEX
        // ----------------------------------------------------

        if (
            !record.assetIndex
        ) {

            return null;
        }


        const assetIndexPath =
            path.join(
                assetsDir,
                "indexes",
                `${record.assetIndex}.json`
            );


        if (
            !fs.existsSync(
                assetIndexPath
            )
        ) {

            return null;
        }


        return record;


    } catch (error) {

        console.warn(
            "[FlintFix] Install cache invalid:",
            error
        );


        return null;
    }
}


// ============================================================
// INSTALL MINECRAFT
// ============================================================

async function installMinecraft({
    rootDir,
    versionId,
    metadataUrl,
    metadataSha1,
    emit,
    forceVerify = false
}) {

    // ========================================================
    // VALIDATE VERSION ID
    // ========================================================

    if (
        !/^[A-Za-z0-9._+-]+$/.test(
            versionId
        )
    ) {

        throw new Error(
            "Invalid Minecraft version ID."
        );
    }


    validateUrl(
        metadataUrl,
        false
    );


    // ========================================================
    // FAST PATH
    // ========================================================
    //
    // Normal PLAY:
    //
    //     use verified cache
    //
    // Repair Installation:
    //
    //     forceVerify = true
    //
    //     bypass cache and perform complete verification.

    if (
        !forceVerify
    ) {

        const cachedInstallation =
            await getCachedInstallation(
                rootDir,
                versionId,
                metadataSha1
            );


        if (
            cachedInstallation
        ) {

            emit({
                stage:
                    "cache",

                message:
                    `Minecraft ${versionId} is already installed.`
            });


            emit({
                stage:
                    "complete",

                message:
                    `Minecraft ${versionId} is installed.`,

                cached:
                    true
            });


            return {
                ...cachedInstallation,

                cached:
                    true
            };
        }
    }


    // ========================================================
    // DIRECTORIES
    // ========================================================

    const versionsDir =
        path.join(
            rootDir,
            "versions",
            versionId
        );


    const librariesDir =
        path.join(
            rootDir,
            "libraries"
        );


    const assetsDir =
        path.join(
            rootDir,
            "assets"
        );


    const nativesDir =
        path.join(
            versionsDir,
            "natives"
        );


    mkdir(
        versionsDir
    );

    mkdir(
        librariesDir
    );

    mkdir(
        assetsDir
    );

    mkdir(
        nativesDir
    );


    // ========================================================
    // VERSION METADATA
    // ========================================================

    emit({
        stage:
            "metadata",

        message:
            `Reading Minecraft ${versionId} metadata...`
    });


    const versionJsonPath =
        path.join(
            versionsDir,
            `${versionId}.json`
        );


    const metadata =
        await fetchJson(
            metadataUrl,
            versionJsonPath,
            metadataSha1,
            emit,
            `${versionId}.json`
        );


    if (
        !metadata.downloads ||
        !metadata.downloads.client
    ) {

        throw new Error(
            "Version metadata has no client download."
        );
    }


    // ========================================================
    // CLIENT JAR
    // ========================================================

    const client =
        metadata.downloads.client;


    emit({
        stage:
            "client",

        message:
            "Checking Minecraft client..."
    });


    await ensureDownload(

        {
            url:
                validateUrl(
                    client.url
                ),

            path:
                path.join(
                    versionsDir,
                    `${versionId}.jar`
                ),

            sha1:
                client.sha1,

            size:
                client.size,

            stage:
                "client",

            label:
                `${versionId}.jar`
        },

        emit
    );


    // ========================================================
    // BUILD LIBRARY LIST
    // ========================================================

    const libraryItems = [];

    const nativeJars = [];


    for (
        const lib of
        metadata.libraries || []
    ) {

        if (
            !libraryAllowed(
                lib
            )
        ) {

            continue;
        }


        // ----------------------------------------------------
        // NORMAL LIBRARY
        // ----------------------------------------------------

        const artifact =
            lib.downloads &&
            lib.downloads.artifact;


        if (
            artifact &&
            artifact.url &&
            artifact.path
        ) {

            libraryItems.push({

                url:
                    validateUrl(
                        artifact.url
                    ),

                path:
                    path.join(
                        librariesDir,
                        ...artifact.path.split(
                            "/"
                        )
                    ),

                sha1:
                    artifact.sha1,

                size:
                    artifact.size,

                label:
                    lib.name
            });
        }


        // ----------------------------------------------------
        // WINDOWS NATIVES
        // ----------------------------------------------------

        const classifier =
            nativeClassifier(
                lib
            );


        const native =
            classifier &&
            lib.downloads &&
            lib.downloads.classifiers &&
            lib.downloads.classifiers[
                classifier
            ];


        if (
            native &&
            native.url &&
            native.path
        ) {

            const item = {

                url:
                    validateUrl(
                        native.url
                    ),

                path:
                    path.join(
                        librariesDir,
                        ...native.path.split(
                            "/"
                        )
                    ),

                sha1:
                    native.sha1,

                size:
                    native.size,

                label:
                    `${lib.name} (${classifier})`
            };


            libraryItems.push(
                item
            );


            nativeJars.push(
                item.path
            );
        }
    }


    // ========================================================
    // LIBRARIES
    // ========================================================

    emit({
        stage:
            "libraries",

        message:
            `Checking ${libraryItems.length} libraries...`,

        current:
            0,

        total:
            libraryItems.length
    });


    let completedLibraries = 0;

    let downloadedLibraries = 0;

    let lastLibraryEmit = 0;


    await runWorkerPool(

        libraryItems,

        LIBRARY_CONCURRENCY,

        async item => {

            const downloaded =
                await ensureDownload(

                    {
                        ...item,

                        stage:
                            "libraries"
                    },

                    null
                );


            if (
                downloaded
            ) {

                downloadedLibraries++;
            }


            completedLibraries++;


            const now =
                Date.now();


            if (
                completedLibraries ===
                    libraryItems.length ||
                now -
                    lastLibraryEmit >=
                    PROGRESS_INTERVAL_MS
            ) {

                lastLibraryEmit =
                    now;


                emit({

                    stage:
                        "libraries",

                    current:
                        completedLibraries,

                    total:
                        libraryItems.length,

                    label:
                        item.label,

                    downloaded:
                        downloadedLibraries
                });
            }
        }
    );


    emit({

        stage:
            "libraries",

        message:
            downloadedLibraries > 0
                ? `Libraries ready. Downloaded ${downloadedLibraries} files.`
                : "Libraries verified.",

        current:
            libraryItems.length,

        total:
            libraryItems.length,

        downloaded:
            downloadedLibraries
    });


    // ========================================================
    // ASSET INDEX
    // ========================================================

    if (
        !metadata.assetIndex ||
        !metadata.assetIndex.url
    ) {

        throw new Error(
            "Version metadata has no asset index."
        );
    }


    const assetIndexPath =
        path.join(
            assetsDir,
            "indexes",
            `${metadata.assetIndex.id}.json`
        );


    emit({

        stage:
            "assets-index",

        message:
            "Checking asset index..."
    });


    const assetIndex =
        await fetchJson(
            metadata.assetIndex.url,
            assetIndexPath,
            metadata.assetIndex.sha1,
            emit,
            `assets ${metadata.assetIndex.id}`
        );


    // ========================================================
    // BUILD ASSET LIST
    // ========================================================

    const objects =
        Object.entries(
            assetIndex.objects || {}
        );


    emit({

        stage:
            "assets",

        message:
            `Checking ${objects.length} assets...`,

        current:
            0,

        total:
            objects.length
    });


    // ========================================================
    // FAST CONCURRENT ASSET CHECK
    // ========================================================

    let completedAssets = 0;

    let downloadedAssets = 0;

    let lastAssetEmit = 0;


    await runWorkerPool(

        objects,

        ASSET_CONCURRENCY,

        async entry => {

            const [
                name,
                object
            ] = entry;


            const hash =
                object.hash;


            if (
                !hash ||
                typeof hash !==
                    "string" ||
                hash.length < 2
            ) {

                throw new Error(
                    `Invalid asset hash: ${name}`
                );
            }


            const objectPath =
                path.join(
                    assetsDir,
                    "objects",
                    hash.slice(0, 2),
                    hash
                );


            const assetUrl =
                `https://resources.download.minecraft.net/${hash.slice(0, 2)}/${hash}`;


            const downloaded =
                await ensureDownload(

                    {
                        url:
                            assetUrl,

                        path:
                            objectPath,

                        sha1:
                            hash,

                        size:
                            object.size,

                        stage:
                            "assets",

                        label:
                            name
                    },

                    null
                );


            if (
                downloaded
            ) {

                downloadedAssets++;
            }


            completedAssets++;


            const now =
                Date.now();


            if (
                completedAssets ===
                    objects.length ||
                completedAssets % 25 ===
                    0 ||
                now -
                    lastAssetEmit >=
                    PROGRESS_INTERVAL_MS
            ) {

                lastAssetEmit =
                    now;


                emit({

                    stage:
                        "assets",

                    current:
                        completedAssets,

                    total:
                        objects.length,

                    label:
                        name,

                    downloaded:
                        downloadedAssets
                });
            }
        }
    );


    emit({

        stage:
            "assets",

        message:
            downloadedAssets > 0
                ? `Assets ready. Downloaded ${downloadedAssets} missing or invalid assets.`
                : "Assets verified.",

        current:
            objects.length,

        total:
            objects.length,

        downloaded:
            downloadedAssets
    });


    // ========================================================
    // WINDOWS NATIVES
    // ========================================================

    emit({

        stage:
            "natives",

        message:
            "Extracting Windows natives...",

        current:
            0,

        total:
            nativeJars.length
    });


    // Always rebuild this directory during a full verification
    // so stale native DLLs aren't carried over.

    fs.rmSync(
        nativesDir,
        {
            recursive: true,
            force: true
        }
    );


    mkdir(
        nativesDir
    );


    for (
        let i = 0;
        i < nativeJars.length;
        i++
    ) {

        await extract(
            nativeJars[i],
            {
                dir:
                    nativesDir
            }
        );


        emit({

            stage:
                "natives",

            current:
                i + 1,

            total:
                nativeJars.length,

            label:
                path.basename(
                    nativeJars[i]
                )
        });
    }


    // ========================================================
    // VERIFIED INSTALL RECORD
    // ========================================================

    const now =
        new Date()
            .toISOString();


    const installRecord = {

        installRecordVersion:
            INSTALL_RECORD_VERSION,

        versionId,

        installedAt:
            now,

        verified:
            true,

        verifiedAt:
            now,

        metadataSha1:
            metadataSha1 || null,

        mainClass:
            metadata.mainClass,

        assetIndex:
            metadata.assetIndex.id,

        javaVersion:
            metadata.javaVersion ||
            null,

        clientJar:
            path.join(
                versionsDir,
                `${versionId}.jar`
            ),

        nativesDir,

        rootDir
    };


    const installRecordPath =
        path.join(
            versionsDir,
            "flintfix-install.json"
        );


    await fs.promises.writeFile(

        installRecordPath,

        JSON.stringify(
            installRecord,
            null,
            2
        ),

        "utf8"
    );


    // ========================================================
    // COMPLETE
    // ========================================================

    emit({

        stage:
            "complete",

        message:
            `Minecraft ${versionId} is installed.`,

        cached:
            false
    });


    return {
        ...installRecord,

        cached:
            false
    };
}


// ============================================================
// EXPORTS
// ============================================================

module.exports = {
    installMinecraft
};