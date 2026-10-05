[CmdletBinding()]
param(
    [string]$ModsDirectory = ""
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Join-Path $PSScriptRoot "flintfix-mod"
$BuildScript = Join-Path $ProjectRoot "build-mod.ps1"
$LocalRuntimeRoot = Join-Path $ProjectRoot ".gradle-local"
$ManagedJdkRoot = Join-Path $LocalRuntimeRoot "jdk-21"

function Use-Java21([string]$JavaHome) {
    if ([string]::IsNullOrWhiteSpace($JavaHome)) { return $false }
    $JavaExe = Join-Path $JavaHome "bin\java.exe"
    $JavacExe = Join-Path $JavaHome "bin\javac.exe"
    $ReleaseFile = Join-Path $JavaHome "release"
    if (-not (Test-Path $JavaExe) -or -not (Test-Path $JavacExe) -or -not (Test-Path $ReleaseFile)) { return $false }

    $ReleaseText = Get-Content -LiteralPath $ReleaseFile -Raw
    if ($ReleaseText -notmatch 'JAVA_VERSION\s*=\s*"21(?:[._+-]|\")') { return $false }

    $env:JAVA_HOME = $JavaHome
    $env:PATH = (Join-Path $JavaHome "bin") + [IO.Path]::PathSeparator + $env:PATH
    Write-Host "Using Java 21 JDK at $JavaHome" -ForegroundColor Green
    return $true
}

function Download-PortableJdk([string]$Url, [string]$Destination) {
    $Curl = Get-Command "curl.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $Curl -or -not $Curl.Source) {
        Write-Host "Using PowerShell's downloader (curl.exe was not found)." -ForegroundColor Yellow
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Destination
        return
    }

    $PartialFile = "$Destination.partial"
    $ErrorFile = "$Destination.curl-error"
    $CommonArgs = @(
        "--fail", "--location", "--silent", "--show-error",
        "--retry", "4", "--retry-delay", "1",
        "--output", $PartialFile
    )

    Write-Host "Downloading with curl.exe; an interrupted transfer can resume next time..." -ForegroundColor Cyan
    & $Curl.Source @CommonArgs "--continue-at" "-" $Url 2> $ErrorFile
    $CurlExitCode = $LASTEXITCODE
    $CurlError = if (Test-Path $ErrorFile) { Get-Content -LiteralPath $ErrorFile -Raw } else { "" }

    if ($CurlExitCode -ne 0 -and $CurlError -match "(?i)(416|range.*(not|support|satisf|resume)|resume.*range)") {
        # Some mirrors reject range requests. Restart once from byte zero.
        Remove-Item -LiteralPath $PartialFile -Force -ErrorAction SilentlyContinue
        & $Curl.Source @CommonArgs $Url 2> $ErrorFile
        $CurlExitCode = $LASTEXITCODE
        $CurlError = if (Test-Path $ErrorFile) { Get-Content -LiteralPath $ErrorFile -Raw } else { "" }
    }

    if ($CurlExitCode -ne 0) {
        throw "Java 21 download failed (curl exit $CurlExitCode). $CurlError"
    }

    Move-Item -LiteralPath $PartialFile -Destination $Destination -Force
    Remove-Item -LiteralPath $ErrorFile -Force -ErrorAction SilentlyContinue
}

function Set-UpJava21 {
    $Candidates = @()
    if ($env:JAVA_HOME) { $Candidates += $env:JAVA_HOME }

    foreach ($CommandName in @("javac.exe", "java.exe")) {
        $Command = Get-Command $CommandName -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($Command -and $Command.Source) {
            $Candidates += Split-Path -Parent (Split-Path -Parent $Command.Source)
        }
    }

    # The FlintFix desktop app keeps its build JDK in Electron's user-data
    # directory. Reuse it when available instead of downloading another copy.
    foreach ($AppDataName in @("flintfix-client", "FlintFix Client")) {
        $AppManagedJdk = Join-Path $env:APPDATA "$AppDataName\runtime\jdk-21-build"
        if (Test-Path $AppManagedJdk) { $Candidates += $AppManagedJdk }
    }

    if (Test-Path $ManagedJdkRoot) {
        $JavacFiles = Get-ChildItem -Path $ManagedJdkRoot -Filter "javac.exe" -File -Recurse -ErrorAction SilentlyContinue
        foreach ($JavacFile in $JavacFiles) {
            $Candidates += Split-Path -Parent (Split-Path -Parent $JavacFile.FullName)
        }
    }

    foreach ($Candidate in ($Candidates | Select-Object -Unique)) {
        if (Use-Java21 $Candidate) { return }
    }

    Write-Host "Java 21 JDK not found. Downloading a portable JDK for this project..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $LocalRuntimeRoot | Out-Null
    if (Test-Path $ManagedJdkRoot) {
        Remove-Item -LiteralPath $ManagedJdkRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $ManagedJdkRoot | Out-Null
    $JdkZip = Join-Path $LocalRuntimeRoot "temurin-21-windows-x64.zip"
    $JdkUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
    Download-PortableJdk $JdkUrl $JdkZip
    Expand-Archive -Path $JdkZip -DestinationPath $ManagedJdkRoot -Force
    Remove-Item -LiteralPath $JdkZip -Force

    $JavacFile = Get-ChildItem -Path $ManagedJdkRoot -Filter "javac.exe" -File -Recurse |
        Select-Object -First 1
    if (-not $JavacFile) {
        throw "The Java 21 download finished, but javac.exe was not found in the archive."
    }
    $DownloadedJavaHome = Split-Path -Parent (Split-Path -Parent $JavacFile.FullName)
    if (-not (Use-Java21 $DownloadedJavaHome)) {
        throw "The downloaded JDK is not Java 21 or is missing java.exe."
    }
}

if (-not (Test-Path $BuildScript)) {
    throw "FlintFix build script not found: $BuildScript"
}

Set-UpJava21
Write-Host "Building the updated FlintFix Fabric mod..." -ForegroundColor Cyan
Push-Location $ProjectRoot
try {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $BuildScript
    if ($LASTEXITCODE -ne 0) {
        throw "FlintFix build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$BuiltJar = Get-ChildItem (Join-Path $ProjectRoot "build\libs") -Filter "flintfix-client-mod-*.jar" -File |
    Where-Object { $_.Name -notmatch "(sources|javadoc)" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $BuiltJar) {
    throw "Build finished, but no FlintFix mod JAR was found in build\libs."
}

if ([string]::IsNullOrWhiteSpace($ModsDirectory)) {
    # FlintFix Client launches from its own Electron userData game root, while
    # the standalone Minecraft Launcher uses %APPDATA%\.minecraft.
    $InstallTargets = @(
        (Join-Path $env:APPDATA ".minecraft\mods"),
        (Join-Path $env:APPDATA "flintfix-client\minecraft\game\mods"),
        (Join-Path $env:APPDATA "FlintFix Client\minecraft\game\mods")
    ) | Select-Object -Unique
} else {
    $InstallTargets = @($ModsDirectory)
}

$InstallWarnings = @()
foreach ($TargetModsDirectory in $InstallTargets) {
    New-Item -ItemType Directory -Force -Path $TargetModsDirectory | Out-Null
    $OldMods = Get-ChildItem $TargetModsDirectory -Filter "*flintfix*.jar" -File |
        Where-Object { $_.Name -notmatch "(sources|javadoc)" }
    $TargetBlocked = $false

    if ($OldMods) {
        $BackupRoot = Join-Path (Split-Path -Parent $TargetModsDirectory) "flintfix-backups"
        $BackupDirectory = Join-Path $BackupRoot (Get-Date -Format "yyyyMMdd-HHmmss")
        New-Item -ItemType Directory -Force -Path $BackupDirectory | Out-Null
        foreach ($OldMod in $OldMods) {
            try {
                Move-Item -LiteralPath $OldMod.FullName -Destination $BackupDirectory -ErrorAction Stop
            } catch {
                Write-Warning "Could not replace $($OldMod.FullName) because another process is using it."
                $InstallWarnings += $TargetModsDirectory
                $TargetBlocked = $true
                break
            }
        }
        if (-not $TargetBlocked) {
            Write-Host "Moved the previous FlintFix JAR to $BackupDirectory" -ForegroundColor Yellow
        }
    }

    if (-not $TargetBlocked) {
        $InstalledJar = Join-Path $TargetModsDirectory $BuiltJar.Name
        try {
            Copy-Item -LiteralPath $BuiltJar.FullName -Destination $InstalledJar -Force -ErrorAction Stop
            Write-Host "Installed updated FlintFix: $InstalledJar" -ForegroundColor Green
        } catch {
            Write-Warning "Could not install to $TargetModsDirectory : $($_.Exception.Message)"
            $InstallWarnings += $TargetModsDirectory
        }
    }
}

if ($InstallWarnings.Count -gt 0) {
    $UniqueWarnings = $InstallWarnings | Select-Object -Unique
    Write-Host "The mod built successfully, but these folder(s) still need updating:" -ForegroundColor Yellow
    foreach ($BlockedDirectory in $UniqueWarnings) {
        Write-Host "  $BlockedDirectory" -ForegroundColor Yellow
    }
    Write-Host "Close Minecraft and FlintFix Client completely, then run install-flintfix.bat again." -ForegroundColor Yellow
    exit 1
}
Write-Host "Start Minecraft with Fabric to load the updated GUI." -ForegroundColor Green
