$ErrorActionPreference = "Stop"
$GradleVersion = "8.14.3"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Local = Join-Path $Root ".gradle-local"
$Zip = Join-Path $Local "gradle-$GradleVersion-bin.zip"
$GradleHome = Join-Path $Local "gradle-$GradleVersion"
$GradleBat = Join-Path $GradleHome "bin\gradle.bat"

New-Item -ItemType Directory -Force -Path $Local | Out-Null
if (-not (Test-Path $GradleBat)) {
    Write-Host "Downloading Gradle $GradleVersion..."
    Invoke-WebRequest -UseBasicParsing "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $Zip
    Expand-Archive -Force $Zip $Local
}

& $GradleBat --no-daemon --parallel build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
