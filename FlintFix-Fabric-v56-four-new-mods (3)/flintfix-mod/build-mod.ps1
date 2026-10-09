# Builds the FlintFix mod with the bundled Gradle wrapper.
#   build-mod.ps1 -MinecraftVersion 1.21.1   -> versions/1.21.1/build/libs/flintfix-client-mod-<ver>+1.21.1.jar
#   build-mod.ps1                            -> every supported version, also copied to build/libs/all
param([string]$MinecraftVersion = "")

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

if ($MinecraftVersion) {
    if (-not (Test-Path (Join-Path $Root "versions\$MinecraftVersion\gradle.properties"))) {
        throw "FlintFix does not support Minecraft $MinecraftVersion."
    }
    & (Join-Path $Root "gradlew.bat") --no-daemon ":${MinecraftVersion}:build"
} else {
    & (Join-Path $Root "gradlew.bat") --no-daemon buildAll
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
