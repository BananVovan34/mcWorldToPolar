param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version,

    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ($env:OS -ne "Windows_NT") {
    throw "Windows packaging must run on Windows."
}

$root = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $root "target"
$inputDir = Join-Path $targetDir "jpackage-input"
$appImageRoot = Join-Path $targetDir "jpackage-app"
$installerDir = Join-Path $targetDir "jpackage-installer"
$tempDir = Join-Path $targetDir "jpackage-temp"
$releaseDir = Join-Path $targetDir "release"
$iconPath = Join-Path $root "packaging/windows/mc-world-to-polar.ico"
$mavenWrapper = Join-Path $root "mvnw.cmd"
$mainJarName = "mcWorldToPolar.jar"
$mainJar = Join-Path $targetDir $mainJarName
$appName = "MC World to Polar"
$mainClass = "com.bananvovan.mcworldtopolar.MainKt"
$upgradeUuid = "C539619D-6B57-4CB8-A459-F05E36A6B3C8"

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME must point to JDK 24."
}

$java = Join-Path $env:JAVA_HOME "bin/java.exe"
$jpackage = Join-Path $env:JAVA_HOME "bin/jpackage.exe"

if (-not (Test-Path $java) -or -not (Test-Path $jpackage)) {
    throw "JAVA_HOME must point to a full JDK containing java.exe and jpackage.exe."
}

$releaseFile = Join-Path $env:JAVA_HOME "release"
if (-not (Test-Path $releaseFile)) {
    throw "JAVA_HOME does not contain a JDK release metadata file."
}

$javaVersionLine = Get-Content $releaseFile | Select-String '^JAVA_VERSION="([^"]+)"$'
if (-not $javaVersionLine) {
    throw "Unable to determine the JDK version from $releaseFile."
}

$javaVersion = $javaVersionLine.Matches[0].Groups[1].Value
$javaMajorVersion = ($javaVersion -split '[.\-+]')[0]
if ($javaMajorVersion -ne "24") {
    throw "JDK 24 is required, but JAVA_HOME provides Java $javaVersion."
}

if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue) -and
    -not (Get-Command wix.exe -ErrorAction SilentlyContinue)) {
    throw "WiX Toolset is required to create the Windows EXE installer."
}

if (-not (Test-Path $iconPath)) {
    throw "Windows icon not found: $iconPath"
}

if (-not $SkipBuild) {
    & $mavenWrapper -B "-Drevision=$Version" clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }
}

if (-not (Test-Path $mainJar)) {
    throw "Executable JAR not found: $mainJar"
}

@($inputDir, $appImageRoot, $installerDir, $tempDir, $releaseDir) | ForEach-Object {
    Remove-Item $_ -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force $_ | Out-Null
}

Copy-Item $mainJar (Join-Path $inputDir $mainJarName)

$appImageArguments = @(
    "--type", "app-image",
    "--dest", $appImageRoot,
    "--temp", $tempDir,
    "--input", $inputDir,
    "--name", $appName,
    "--main-jar", $mainJarName,
    "--main-class", $mainClass,
    "--app-version", $Version,
    "--vendor", "BananVovan34",
    "--description", "Desktop converter for Minecraft Anvil worlds and Polar world data",
    "--copyright", "Copyright (c) BananVovan34",
    "--icon", $iconPath,
    "--java-options", "-Dfile.encoding=UTF-8"
)
& $jpackage @appImageArguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage app-image failed with exit code $LASTEXITCODE."
}

$appImagePath = Join-Path $appImageRoot $appName
$launcher = Join-Path $appImagePath "$appName.exe"
if (-not (Test-Path $launcher)) {
    throw "Bundled launcher not found: $launcher"
}

$smokeProcess = Start-Process -FilePath $launcher -ArgumentList "--version" -Wait -PassThru
if ($smokeProcess.ExitCode -ne 0) {
    throw "Bundled launcher smoke test failed with exit code $($smokeProcess.ExitCode)."
}

$portableName = "MC-World-to-Polar-$Version-windows-x64-portable.zip"
$portablePath = Join-Path $releaseDir $portableName
Compress-Archive -Path $appImagePath -DestinationPath $portablePath -CompressionLevel Optimal

Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $tempDir | Out-Null

$installerArguments = @(
    "--type", "exe",
    "--dest", $installerDir,
    "--temp", $tempDir,
    "--name", $appName,
    "--app-version", $Version,
    "--vendor", "BananVovan34",
    "--description", "Desktop converter for Minecraft Anvil worlds and Polar world data",
    "--copyright", "Copyright (c) BananVovan34",
    "--icon", $iconPath,
    "--app-image", $appImagePath,
    "--win-per-user-install",
    "--win-dir-chooser",
    "--win-menu",
    "--win-menu-group", $appName,
    "--win-shortcut",
    "--win-upgrade-uuid", $upgradeUuid
)
& $jpackage @installerArguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage EXE installer failed with exit code $LASTEXITCODE."
}

$installer = Get-ChildItem $installerDir -Filter *.exe | Select-Object -First 1
if (-not $installer) {
    throw "jpackage did not produce an EXE installer."
}

$jarReleasePath = Join-Path $releaseDir "MC-World-to-Polar-$Version.jar"
$installerReleasePath = Join-Path $releaseDir "MC-World-to-Polar-$Version-windows-x64.exe"
Copy-Item $mainJar $jarReleasePath
Copy-Item $installer.FullName $installerReleasePath

$checksumPath = Join-Path $releaseDir "SHA256SUMS.txt"
Get-ChildItem $releaseDir -File |
    Where-Object { $_.Name -ne "SHA256SUMS.txt" } |
    Sort-Object Name |
    ForEach-Object {
        $hash = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $($_.Name)"
    } |
    Set-Content $checksumPath -Encoding Ascii

Write-Host "Release artifacts:"
Get-ChildItem $releaseDir -File | ForEach-Object { Write-Host "  $($_.FullName)" }
