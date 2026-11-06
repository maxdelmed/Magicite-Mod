$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $project

function Stage($msg) { Write-Host "`n===== $msg =====" -ForegroundColor Cyan }
function Fail($msg) { Write-Host "`nBUILD STOPPED: $msg" -ForegroundColor Red; exit 1 }

function Find-JDK17 {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME.Trim('"')) }

    $roots = @()
    if ($env:ProgramFiles) {
        $roots += (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
        $roots += (Join-Path $env:ProgramFiles 'Java')
        $roots += (Join-Path $env:ProgramFiles 'Microsoft')
        $roots += (Join-Path $env:ProgramFiles 'Zulu')
        $roots += (Join-Path $env:ProgramFiles 'Amazon Corretto')
        $roots += (Join-Path $env:ProgramFiles 'BellSoft')
    }
    if ($env:LOCALAPPDATA) {
        $roots += (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium')
        $roots += (Join-Path $env:LOCALAPPDATA 'Programs\Java')
    }
    if ($env:USERPROFILE) { $roots += (Join-Path $env:USERPROFILE '.jdks') }

    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root -PathType Container)) { continue }
        foreach ($dir in (Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue)) {
            $candidates.Add($dir.FullName)
        }
    }

    $pathJavac = Get-Command 'javac.exe' -ErrorAction SilentlyContinue
    if ($pathJavac) { $candidates.Add((Split-Path -Parent (Split-Path -Parent $pathJavac.Source))) }

    $checked = @{}
    foreach ($candidatePath in $candidates) {
        if (-not $candidatePath) { continue }
        try { $candidatePath = [System.IO.Path]::GetFullPath($candidatePath) } catch { continue }
        if ($checked.ContainsKey($candidatePath)) { continue }
        $checked[$candidatePath] = $true
        $compiler = Join-Path $candidatePath 'bin\javac.exe'
        $runtime = Join-Path $candidatePath 'bin\java.exe'
        if ((-not (Test-Path -LiteralPath $compiler)) -or (-not (Test-Path -LiteralPath $runtime))) { continue }
        try {
            $result = (& $compiler -version 2>&1 | Out-String).Trim()
            if ($LASTEXITCODE -eq 0 -and $result -match '^javac\s+17(\.|\s|$)') {
                return $candidatePath
            }
        } catch { continue }
    }
    return $null
}

Stage 'Finding Java 17 JDK'
$jdk = Find-JDK17
if (-not $jdk) {
    Write-Host 'Could not find a Java 17 JDK (javac.exe) in JAVA_HOME, PATH, or common installation folders.' -ForegroundColor Yellow
    Write-Host 'To diagnose: open Command Prompt (cmd) and run these 3 commands:'
    Write-Host '  java -version'
    Write-Host '  javac -version'
    Write-Host '  where javac'
    Fail 'If Java 17 is installed in a custom folder, set JAVA_HOME to that JDK and try again.'
}
$env:JAVA_HOME = $jdk
$env:PATH = (Join-Path $jdk 'bin') + ';' + $env:PATH
Write-Host "Using JAVA_HOME: $jdk" -ForegroundColor Green
& (Join-Path $jdk 'bin\javac.exe') -version
& (Join-Path $jdk 'bin\java.exe') -version

if (-not (Test-Path (Join-Path $project 'gradlew.bat'))) {
    Stage 'Getting official Forge 1.20.1-47.4.20 development kit'
    $url = 'https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.20/forge-1.20.1-47.4.20-mdk.zip'
    $expectedSha1 = 'ae89b7adec05802fb805c9345b509029c6952ebb'
    $tempDir = Join-Path $env:TEMP 'MemoriaMagicite_ForgeMDK_47_4_20'
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    $zip = Join-Path $tempDir 'forge-mdk.zip'
    try {
        Invoke-WebRequest -Uri $url -UseBasicParsing -OutFile $zip
    } catch { Fail 'Could not download the official Forge MDK. Check your internet connection and try again.' }
    $gotSha1 = (Get-FileHash $zip -Algorithm SHA1).Hash.ToLowerInvariant()
    if ($gotSha1 -ne $expectedSha1) { Fail 'Downloaded Forge MDK did not match the official SHA-1 checksum. No files were installed.' }
    $unpacked = Join-Path $tempDir 'unpacked'
    if (Test-Path $unpacked) { Remove-Item $unpacked -Force -Recurse }
    Expand-Archive -Path $zip -DestinationPath $unpacked
    $wrapper = Get-ChildItem -Path $unpacked -Filter 'gradlew.bat' -File -Recurse | Select-Object -First 1
    if (-not $wrapper) { Fail 'The official MDK archive did not contain gradlew.bat.' }
    $mdkRoot = $wrapper.Directory.FullName
    if (-not (Test-Path (Join-Path $mdkRoot 'gradle\wrapper\gradle-wrapper.jar'))) { Fail 'The MDK did not include a usable Gradle wrapper.' }
    Copy-Item (Join-Path $mdkRoot 'gradlew.bat') $project -Force
    Copy-Item (Join-Path $mdkRoot 'gradlew') $project -Force
    Copy-Item (Join-Path $mdkRoot 'gradle') (Join-Path $project 'gradle') -Recurse -Force
    Write-Host 'Official Gradle wrapper added.' -ForegroundColor Green
}

Stage 'Compiling Memoria Magicite'
Write-Host 'First build requires internet for dependencies.'
Write-Host 'Compilation requires Forge dependencies.'
$logFile = Join-Path $project 'BUILD_LOG.txt'
$ErrorActionPreference = 'Continue'
& (Join-Path $project 'gradlew.bat') 'build' '--console=plain' *>&1 | Tee-Object -FilePath $logFile
$code = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($code -ne 0) { Fail "Gradle returned exit code $code. Check BUILD_LOG.txt for details." }

Stage 'Locating compiled JAR'
$libdir = Join-Path $project 'build\libs'
$jars = @(Get-ChildItem -Path $libdir -Filter 'memoria_magicite*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch '(sources|javadoc|slim)' })
if ($jars.Count -eq 0) { Fail 'Gradle completed but no compiled Magicite JAR was found in build/libs.' }
Stage 'Verifying the JAR has Minecraft resource metadata, item models, and sounds'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$required = @('pack.mcmeta', 'META-INF/mods.toml', 'assets/memoria_magicite/sounds.json', 'com/memoria/magicite/client/GauntletArmLayer.class', 'com/memoria/magicite/spell/FusionDescriptions.class', 'com/memoria/magicite/spell/FusionRules.class', 'com/memoria/magicite/item/MagiciteCharge.class', 'com/memoria/magicite/spell/QuarterChargeMath.class', 'com/memoria/magicite/spell/VillagerMagicEvents.class', 'com/memoria/magicite/spell/HalfChargeMath.class', 'com/memoria/magicite/spell/CooperativeBalance.class', 'com/memoria/magicite/spell/SpellWorldInteractions.class', 'com/memoria/magicite/spell/FusionNames.class', 'com/memoria/magicite/net/MagiciteNetwork$FootprintPacket.class', 'com/memoria/magicite/spell/HolyBalance.class')
$names = @('fire_magicite', 'blizzard_magicite', 'thunder_magicite', 'pure_memory_magicite', 'cure_magicite', 'clear_magicite', 'life_magicite', 'fusion_gauntlet', 'aether_shard', 'magicite_shard')
foreach ($name in $names) {
    $required += "assets/memoria_magicite/models/item/$name.json"
    $required += "assets/memoria_magicite/textures/item/$name.png"
}
foreach ($name in @('charge', 'release', 'fusion', 'life_trigger', 'shatter', 'cast_fire', 'cast_blizzard', 'cast_thunder', 'cast_pure_memory', 'cast_cure', 'cast_clear', 'cast_life', 'resonance_ready', 'power_two', 'power_three', 'unstable_release')) {
    $required += "assets/memoria_magicite/sounds/$name.ogg"
}
foreach ($name in @('gauntlet_metal', 'gauntlet_dark', 'gauntlet_gold', 'gauntlet_socket', 'gauntlet_jewel', 'gauntlet_rune')) {
    $required += "assets/memoria_magicite/textures/item/$name.png"
}
foreach ($jar in $jars) {
    $archive = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $notFound = @($required | Where-Object { $null -eq $archive.GetEntry($_) })
        if ($notFound.Count -gt 0) { Fail "The compiled JAR is missing required files: $($notFound -join ', ')" }
        $entry = $archive.GetEntry('pack.mcmeta')
        $stream = $entry.Open()
        $reader = New-Object System.IO.StreamReader($stream)
        try {
            $packJson = $reader.ReadToEnd() | ConvertFrom-Json
            if ($packJson.pack.pack_format -ne 15) { Fail 'Wrong pack_format in compiled JAR; expected 15 for Minecraft 1.20.1.' }
        } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
    Write-Host "Verified: $($jar.Name) has pack.mcmeta (format 15), item models, textures, and sounds." -ForegroundColor Green
}
$output = Join-Path $project 'READY_FOR_TEST'
New-Item -ItemType Directory -Force -Path $output | Out-Null
Get-ChildItem -Path $output -Filter 'memoria_magicite*.jar' -File -ErrorAction SilentlyContinue | Remove-Item -Force
foreach ($jar in $jars) { Copy-Item $jar.FullName $output -Force; Write-Host "Copied: $($jar.Name)" -ForegroundColor Green }
Write-Host "`nBuild completed. Test JAR folder: $output" -ForegroundColor Green
Write-Host 'Test separately before adding to your full Memoria modpack.'
exit 0
