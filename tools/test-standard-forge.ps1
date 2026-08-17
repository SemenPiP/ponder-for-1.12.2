param(
    [string]$ForgeDirectory = "",
    [string]$MixinBooterJar = "",
    [string]$MixinBooterVersion = "11.2",
    [string]$ExpectedMixinBooterHash = "",
    [string]$CraftTweakerJar = "",
    [string]$ServerHarnessJar = "",
    [string]$JavaExecutable = "",
    [int]$TimeoutSeconds = 600,
    [int]$ServerPort = 25566
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2.0

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$buildRoot = Join-Path $projectRoot "build"
$smokeRoot = Join-Path $buildRoot "standard-forge-smoke"
$reportRoot = Join-Path $buildRoot "reports"
$runId = [DateTime]::UtcNow.ToString("yyyyMMdd-HHmmssfff") + "-" +
    (([Guid]::NewGuid().ToString("N")).Substring(0, 8))
$testRoot = Join-Path $smokeRoot $runId
$report = Join-Path $reportRoot "standard-forge-verification-$runId.md"
$baselineRoot = Join-Path $testRoot "baseline"
$moddedRoot = Join-Path $testRoot "modded"

$forgeVersion = "14.23.5.2847"
$forgeFileName = "forge-1.12.2-$forgeVersion-universal.jar"
$expectedForgeHash = "29A7372B5801C2EA01ACFFA8B238256D131D770BCD18148D6F2D5C2A40BC6A6A"
$forgeInstallerFileName = "forge-1.12.2-$forgeVersion-installer.jar"
$forgeInstallerUri = "https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-$forgeVersion/$forgeInstallerFileName"
$expectedForgeInstallerHash = "3A74473FC62DCF13BAA4130E6EA31A80C6A872B6B25F1A4C9195C8E878415BD0"
$minecraftServerFileName = "minecraft_server.1.12.2.jar"
$expectedMinecraftServerHash = "FE1F9274E6DAD9191BF6E6E8E36EE6EBC737F373603DF0946AAFCDED0D53167E"
$ponderVersion = "1.3.0-alpha.1-mc1.12.2"
$ponderFileName = "Ponder-1.12.2-1.3.0-alpha.1.jar"
$ponderJar = Join-Path $buildRoot "libs\$ponderFileName"
$serverHarnessFileName = "Ponder-Server-Harness-1.12.2-1.3.0-alpha.1.jar"
if ([string]::IsNullOrWhiteSpace($ServerHarnessJar)) {
    $ServerHarnessJar = Join-Path $buildRoot "verification\server-harness\$serverHarnessFileName"
}
$fixtureScripts = Join-Path $projectRoot "verification\crafttweaker-fixtures"
if ([string]::IsNullOrWhiteSpace($ExpectedMixinBooterHash) -and $MixinBooterVersion -eq "11.2") {
    $ExpectedMixinBooterHash = "48667BC07D4F9D54A5C0F808DAA02DEB956128664DB24269EB34460F4CA2462E"
}
$mixinBooterUri = "https://maven.cleanroommc.com/zone/rong/mixinbooter/$mixinBooterVersion/mixinbooter-$mixinBooterVersion.jar"
$craftTweakerVersion = "4.1.20.698"
$craftTweakerUri = "https://api.modrinth.com/maven/maven/modrinth/crafttweaker/$craftTweakerVersion/crafttweaker-$craftTweakerVersion.jar"

$null = New-Item -ItemType Directory -Path $testRoot -Force
$null = New-Item -ItemType Directory -Path $reportRoot -Force

$phaseResults = [Collections.Generic.List[object]]::new()
$blockingFindings = [Collections.Generic.List[string]]::new()
$scriptFailure = ""
$forgeJar = "not resolved"
$minecraftServerJar = "not resolved"
$librariesDirectory = "not resolved"
$mixinBooterSource = "not resolved"
$javaVersion = "not checked"
$forgeHash = "not checked"
$minecraftServerHash = "not checked"
$ponderHash = "not checked"
$mixinBooterHash = "not checked"
$libraryManifestHash = "not checked"
$libraryCount = 0
$initialLevelHash = "not checked"
$restartLevelHash = "not checked"
$baselineClassCount = 0
$moddedClassCount = 0
$introducedClassCount = 0
$introducedProjectClassCount = 0
$baselineVanillaClientCount = 0
$moddedVanillaClientCount = 0
$introducedVanillaClientClasses = @()
$forbiddenProjectClasses = @()

function ConvertTo-MarkdownCell {
    param([string]$Value)
    if ($null -eq $Value) { return "" }
    return (($Value -replace "\r?\n", " ") -replace "\|", "&#124;")
}

function Get-VerifiedMixinBooter {
    param([string]$PreferredPath)

    $candidates = [Collections.Generic.List[string]]::new()
    if (![string]::IsNullOrWhiteSpace($PreferredPath)) {
        $candidates.Add((Resolve-Path -LiteralPath $PreferredPath).Path)
    } else {
        $gradleHome = if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
            Join-Path $env:USERPROFILE ".gradle"
        } else {
            $env:GRADLE_USER_HOME
        }
        $cacheRoot = Join-Path $gradleHome "caches\modules-2\files-2.1\zone.rong\mixinbooter\$mixinBooterVersion"
        if (Test-Path -LiteralPath $cacheRoot -PathType Container) {
            Get-ChildItem -LiteralPath $cacheRoot -Filter "mixinbooter-$mixinBooterVersion.jar" -File -Recurse |
                Sort-Object FullName |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    }

    foreach ($candidate in $candidates) {
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $candidate).Hash
        if ([string]::IsNullOrWhiteSpace($ExpectedMixinBooterHash) -or $hash -eq $ExpectedMixinBooterHash) {
            return [PSCustomObject]@{ Path = $candidate; Hash = $hash; Source = "local" }
        }
        if (![string]::IsNullOrWhiteSpace($PreferredPath)) {
            throw "MixinBooter SHA256 mismatch. Expected $ExpectedMixinBooterHash, found $hash at $candidate"
        }
    }

    $dependencyRoot = Join-Path $testRoot "dependencies"
    $null = New-Item -ItemType Directory -Path $dependencyRoot -Force
    $download = Join-Path $dependencyRoot "mixinbooter-$mixinBooterVersion.jar"
    $null = Invoke-WebRequest -UseBasicParsing -Uri $mixinBooterUri -OutFile $download
    $downloadHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $download).Hash
    if (![string]::IsNullOrWhiteSpace($ExpectedMixinBooterHash) -and
        $downloadHash -ne $ExpectedMixinBooterHash) {
        throw "Downloaded MixinBooter SHA256 mismatch. Expected $ExpectedMixinBooterHash, found $downloadHash"
    }
    return [PSCustomObject]@{ Path = $download; Hash = $downloadHash; Source = "download" }
}

function Get-CraftTweaker {
    param([string]$PreferredPath)
    if (![string]::IsNullOrWhiteSpace($PreferredPath)) {
        return (Resolve-Path -LiteralPath $PreferredPath).Path
    }
    $cacheRoot = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\maven.modrinth\crafttweaker\$craftTweakerVersion"
    if (Test-Path -LiteralPath $cacheRoot -PathType Container) {
        $cached = Get-ChildItem -LiteralPath $cacheRoot -Filter "crafttweaker-$craftTweakerVersion.jar" -File -Recurse |
            Select-Object -First 1
        if ($null -ne $cached) { return $cached.FullName }
    }
    $dependencyRoot = Join-Path $testRoot "dependencies"
    $null = New-Item -ItemType Directory -Path $dependencyRoot -Force
    $download = Join-Path $dependencyRoot "CraftTweaker2-1.12-$craftTweakerVersion.jar"
    $null = Invoke-WebRequest -UseBasicParsing -Uri $craftTweakerUri -OutFile $download
    return $download
}

function Get-JarManifestText {
    param([string]$JarPath)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $archive.GetEntry("META-INF/MANIFEST.MF")
        if ($null -eq $entry) { throw "Jar has no META-INF/MANIFEST.MF: $JarPath" }
        $reader = [IO.StreamReader]::new($entry.Open(), [Text.Encoding]::UTF8, $true)
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } finally {
        $archive.Dispose()
    }
}

function Resolve-Java8 {
    param([string]$PreferredPath)

    $candidate = $PreferredPath
    if ([string]::IsNullOrWhiteSpace($candidate) -and ![string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    }
    if ([string]::IsNullOrWhiteSpace($candidate) -or
        !(Test-Path -LiteralPath $candidate -PathType Leaf)) {
        $command = Get-Command java.exe -ErrorAction SilentlyContinue
        if ($null -ne $command) { $candidate = $command.Source }
    }
    if ([string]::IsNullOrWhiteSpace($candidate) -or
        !(Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw "Standard Forge verification requires a Java 8 java.exe. Set JAVA_HOME or pass -JavaExecutable."
    }

    $candidate = (Resolve-Path -LiteralPath $candidate).Path
    $fileVersion = (Get-Item -LiteralPath $candidate).VersionInfo.ProductVersion
    $versionInfo = [Diagnostics.ProcessStartInfo]::new()
    $versionInfo.FileName = $candidate
    $versionInfo.Arguments = "-version"
    $versionInfo.UseShellExecute = $false
    $versionInfo.RedirectStandardOutput = $true
    $versionInfo.RedirectStandardError = $true
    $versionInfo.CreateNoWindow = $true
    $versionProcess = [Diagnostics.Process]::new()
    $versionProcess.StartInfo = $versionInfo
    try {
        $null = $versionProcess.Start()
        $versionStdout = $versionProcess.StandardOutput.ReadToEnd()
        $versionStderr = $versionProcess.StandardError.ReadToEnd()
        $versionProcess.WaitForExit()
        if ($versionProcess.ExitCode -ne 0) {
            throw "java -version exited with code $($versionProcess.ExitCode)."
        }
        $runtimeVersion = ($versionStderr + "`n" + $versionStdout).Trim()
    } finally {
        $versionProcess.Dispose()
    }
    if ($fileVersion -notmatch "^8\." -or $runtimeVersion -notmatch 'version "1\.8\.') {
        throw "Standard Forge verification requires Java 8. Found file version '$fileVersion' at $candidate. Runtime: $runtimeVersion"
    }
    return [PSCustomObject]@{
        Path = $candidate
        FileVersion = $fileVersion
        RuntimeVersion = ($runtimeVersion -replace "\r?\n", " | ")
    }
}

function Install-ForgeServerSource {
    param(
        [string]$Destination,
        [string]$JavaPath
    )

    $null = New-Item -ItemType Directory -Path $Destination -Force
    $installer = Join-Path $Destination $forgeInstallerFileName
    if (!(Test-Path -LiteralPath $installer -PathType Leaf)) {
        $null = Invoke-WebRequest -UseBasicParsing -Uri $forgeInstallerUri -OutFile $installer
    }
    $installerHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $installer).Hash
    if ($installerHash -ne $expectedForgeInstallerHash) {
        throw "Forge installer SHA256 mismatch. Expected $expectedForgeInstallerHash, found $installerHash"
    }

    Push-Location $Destination
    try {
        & $JavaPath -jar $installer --installServer
        if ($LASTEXITCODE -ne 0) {
            throw "Forge installer exited with code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Write-LibraryHashManifest {
    param(
        [string]$SourceDirectory,
        [string]$Destination
    )

    $rootWithSeparator = $SourceDirectory.TrimEnd('\') + '\'
    $lines = [Collections.Generic.List[string]]::new()
    Get-ChildItem -LiteralPath $SourceDirectory -Filter "*.jar" -File -Recurse |
        Sort-Object FullName |
        ForEach-Object {
            $relative = $_.FullName.Substring($rootWithSeparator.Length).Replace('\', '/')
            $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash
            $lines.Add("$hash  $relative")
        }
    [IO.File]::WriteAllLines($Destination, $lines, [Text.UTF8Encoding]::new($false))
    return $lines.Count
}

function Initialize-ServerDirectory {
    param(
        [string]$Destination,
        [int]$Port,
        [string]$WorldName,
        [bool]$WithPonder
    )

    $null = New-Item -ItemType Directory -Path $Destination -Force
    $null = New-Item -ItemType Directory -Path (Join-Path $Destination "mods") -Force
    Copy-Item -LiteralPath $forgeJar -Destination (Join-Path $Destination $forgeFileName)
    Copy-Item -LiteralPath $minecraftServerJar -Destination (Join-Path $Destination $minecraftServerFileName)
    Copy-Item -LiteralPath $librariesDirectory -Destination (Join-Path $Destination "libraries") -Recurse
    if ($WithPonder) {
        Copy-Item -LiteralPath $MixinBooterJar -Destination `
            (Join-Path $Destination "mods\mixinbooter-$mixinBooterVersion.jar")
        Copy-Item -LiteralPath $CraftTweakerJar -Destination `
            (Join-Path $Destination "mods\CraftTweaker2-1.12-$craftTweakerVersion.jar")
        Copy-Item -LiteralPath $ponderJar -Destination (Join-Path $Destination "mods\$ponderFileName")
        Copy-Item -LiteralPath $ServerHarnessJar -Destination `
            (Join-Path $Destination "mods\$serverHarnessFileName")
        Set-CraftTweakerFixtures -ServerRoot $Destination -FileNames @("00_advanced.zs")
    }

    [IO.File]::WriteAllText((Join-Path $Destination "eula.txt"), "eula=true`n",
        [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllLines((Join-Path $Destination "server.properties"), @(
        "allow-nether=false",
        "difficulty=1",
        "enable-query=false",
        "enable-rcon=false",
        "generate-structures=false",
        "level-name=$WorldName",
        "max-players=1",
        "max-tick-time=-1",
        "motd=Ponder standard Forge verification",
        "online-mode=false",
        "server-ip=127.0.0.1",
        "server-port=$Port",
        "snooper-enabled=false",
        "spawn-animals=false",
        "spawn-monsters=false",
        "spawn-npcs=false",
        "view-distance=4"
    ), [Text.UTF8Encoding]::new($false))
}

function Set-CraftTweakerFixtures {
    param(
        [string]$ServerRoot,
        [string[]]$FileNames
    )

    $serverBoundary = [IO.Path]::GetFullPath($ServerRoot).TrimEnd('\') + '\'
    $fixtureTarget = [IO.Path]::GetFullPath(
        (Join-Path $ServerRoot "scripts\ponder\fixtures"))
    if (!$fixtureTarget.StartsWith($serverBoundary, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Fixture target escaped the server root: $fixtureTarget"
    }
    if (Test-Path -LiteralPath $fixtureTarget) {
        Remove-Item -LiteralPath $fixtureTarget -Recurse -Force
    }
    $null = New-Item -ItemType Directory -Path $fixtureTarget -Force

    foreach ($fileName in $FileNames) {
        if ([IO.Path]::GetFileName($fileName) -ne $fileName -or !$fileName.EndsWith(".zs")) {
            throw "Invalid CraftTweaker fixture file name: $fileName"
        }
        $source = Join-Path $fixtureScripts $fileName
        if (!(Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Missing CraftTweaker fixture: $source"
        }
        Copy-Item -LiteralPath $source -Destination (Join-Path $fixtureTarget $fileName)
    }
}

function Add-ProcessLine {
    param(
        [Collections.Generic.List[string]]$Lines,
        [string]$Phase,
        [string]$Line
    )

    if ($null -eq $Line) { return }
    $Lines.Add($Line)
    if ($Line -notmatch '^\[Loaded ') { Write-Host "[$Phase] $Line" }
}

function Complete-ProcessStreams {
    param(
        [Diagnostics.Process]$Process,
        [object]$StdoutTask,
        [object]$StderrTask,
        [Collections.Generic.List[string]]$Lines,
        [string]$Phase
    )

    if ($null -ne $StdoutTask) {
        if (!$StdoutTask.Wait(5000)) { throw "$Phase stdout did not finish draining." }
        $line = $StdoutTask.Result
        if ($null -ne $line) { Add-ProcessLine -Lines $Lines -Phase $Phase -Line $line }
    }
    if ($null -ne $StderrTask) {
        if (!$StderrTask.Wait(5000)) { throw "$Phase stderr did not finish draining." }
        $line = $StderrTask.Result
        if ($null -ne $line) { Add-ProcessLine -Lines $Lines -Phase $Phase -Line "[stderr] $line" }
    }
    while (!$Process.StandardOutput.EndOfStream) {
        Add-ProcessLine -Lines $Lines -Phase $Phase -Line $Process.StandardOutput.ReadLine()
    }
    while (!$Process.StandardError.EndOfStream) {
        Add-ProcessLine -Lines $Lines -Phase $Phase `
            -Line ("[stderr] " + $Process.StandardError.ReadLine())
    }
}

function Get-FatalLogFindings {
    param(
        [Collections.Generic.List[string]]$ProcessLines,
        [string]$RuntimeLogDirectory
    )

    $pattern = 'Mixin apply failed|InvalidMixinException|InjectionError|MixinTransformerError|' +
        'MixinApplyError|Critical injection failure|NoClassDefFoundError|ClassNotFoundException|' +
        'Exception caught during firing event|Failed to load (?:a )?mod|There was a severe problem during mod loading|' +
        '\bFATAL\b'
    $findings = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($line in $ProcessLines) {
        if ($line -notmatch '^\[Loaded ' -and $line -match $pattern) {
            $null = $findings.Add("process: $line")
        }
    }
    if (Test-Path -LiteralPath $RuntimeLogDirectory -PathType Container) {
        Get-ChildItem -LiteralPath $RuntimeLogDirectory -Filter "*.log" -File -Recurse |
            ForEach-Object {
                $path = $_.FullName
                foreach ($line in [IO.File]::ReadLines($path)) {
                    if ($line -match $pattern) {
                        $null = $findings.Add("$($_.Name): $line")
                    }
                }
            }
    }
    return @($findings | Sort-Object)
}

function Invoke-ForgeServer {
    param(
        [string]$Name,
        [string]$ServerRoot,
        [bool]$TraceClasses
    )

    $consoleLog = Join-Path $ServerRoot "$Name-console.log"
    $runtimeLogSnapshot = Join-Path $ServerRoot "runtime-logs-$Name"
    $craftTweakerLogSnapshot = Join-Path $ServerRoot "$Name-crafttweaker.log"
    $lines = [Collections.Generic.List[string]]::new()
    $started = $false
    $saveRequested = $false
    $saveConfirmed = $false
    $saveFallback = $false
    $stopSent = $false
    $processStarted = $false
    $streamsDrained = $false
    $phaseFailure = ""
    $exitCode = $null
    $stdout = $null
    $stderr = $null
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()

    $startupDeadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $saveDeadline = [DateTime]::MaxValue
    $shutdownDeadline = [DateTime]::MaxValue

    $arguments = [Collections.Generic.List[string]]::new()
    if ($TraceClasses) { $arguments.Add("-verbose:class") }
    $arguments.Add("-Xms512M")
    $arguments.Add("-Xmx1536M")
    $arguments.Add("-jar")
    $arguments.Add($forgeFileName)
    $arguments.Add("nogui")

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $JavaExecutable
    $startInfo.Arguments = $arguments -join " "
    $startInfo.WorkingDirectory = $ServerRoot
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo

    try {
        $null = $process.Start()
        $processStarted = $true
        $stdout = $process.StandardOutput.ReadLineAsync()
        $stderr = $process.StandardError.ReadLineAsync()

        while (!$process.HasExited) {
            if ($stdout.Wait(50)) {
                $line = $stdout.Result
                Add-ProcessLine -Lines $lines -Phase $Name -Line $line
                if ($null -ne $line) {
                    if (!$started -and $line -match 'Done \(.+\)! For help') { $started = $true }
                    if ($line -match 'Saved the world|Saved the game') { $saveConfirmed = $true }
                    if ($saveRequested -and $line -match 'Unknown command') { $saveFallback = $true }
                    if ($saveFallback -and $line -match 'Saving chunks for level') { $saveConfirmed = $true }
                }
                $stdout = $process.StandardOutput.ReadLineAsync()
            }
            if ($stderr.IsCompleted) {
                $line = $stderr.Result
                if ($null -ne $line) {
                    Add-ProcessLine -Lines $lines -Phase $Name -Line "[stderr] $line"
                }
                $stderr = $process.StandardError.ReadLineAsync()
            }

            $now = [DateTime]::UtcNow
            if (!$started) {
                if ($now -gt $startupDeadline) { throw "$Name did not reach the Done state." }
                continue
            }
            if (!$saveRequested) {
                $process.StandardInput.WriteLine("save-all")
                $process.StandardInput.Flush()
                $saveRequested = $true
                $saveDeadline = $now.AddSeconds(120)
                continue
            }
            if (!$saveConfirmed -and !$saveFallback) {
                if ($now -gt $saveDeadline) { throw "$Name did not confirm save-all." }
                continue
            }
            if (!$stopSent) {
                $process.StandardInput.WriteLine("stop")
                $process.StandardInput.Flush()
                $stopSent = $true
                $shutdownDeadline = $now.AddSeconds(120)
                continue
            }
            if ($now -gt $shutdownDeadline) { throw "$Name did not stop cleanly." }
        }

        if (!$process.WaitForExit(5000)) { throw "$Name did not finish exiting." }
        $exitCode = $process.ExitCode
        Complete-ProcessStreams -Process $process -StdoutTask $stdout -StderrTask $stderr `
            -Lines $lines -Phase $Name
        $streamsDrained = $true
        if ($saveFallback -and $exitCode -eq 0) { $saveConfirmed = $true }

        if (!$started) { throw "$Name exited before the Done state." }
        if (!$saveConfirmed) { throw "$Name did not confirm world saving." }
        if (!$stopSent) { throw "$Name exited before the script sent stop." }
        if ($exitCode -ne 0) { throw "$Name exited with code $exitCode." }
    } catch {
        $phaseFailure = $_.Exception.Message
    } finally {
        if ($processStarted -and !$process.HasExited) {
            try {
                $process.StandardInput.WriteLine("stop")
                $process.StandardInput.Flush()
                if (!$process.WaitForExit(30000)) {
                    $process.Kill()
                    $null = $process.WaitForExit(10000)
                }
            } catch {
                try {
                    $process.Kill()
                    $null = $process.WaitForExit(10000)
                } catch { }
            }
        }
        if ($processStarted -and $process.HasExited -and !$streamsDrained) {
            try {
                Complete-ProcessStreams -Process $process -StdoutTask $stdout -StderrTask $stderr `
                    -Lines $lines -Phase $Name
            } catch {
                $message = "$Name could not drain process output: $($_.Exception.Message)"
                $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
            }
        }
        $process.Dispose()
        $stopwatch.Stop()
        [IO.File]::WriteAllLines($consoleLog, $lines, [Text.UTF8Encoding]::new($false))
    }

    $runtimeLogs = Join-Path $ServerRoot "logs"
    if (Test-Path -LiteralPath $runtimeLogs -PathType Container) {
        try {
            Copy-Item -LiteralPath $runtimeLogs -Destination $runtimeLogSnapshot -Recurse
        } catch {
            $message = "$Name could not snapshot runtime logs: $($_.Exception.Message)"
            $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
        }
    }
    $craftTweakerLog = Join-Path $ServerRoot "crafttweaker.log"
    if (Test-Path -LiteralPath $craftTweakerLog -PathType Leaf) {
        try {
            Copy-Item -LiteralPath $craftTweakerLog -Destination $craftTweakerLogSnapshot -Force
        } catch {
            $message = "$Name could not snapshot crafttweaker.log: $($_.Exception.Message)"
            $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
        }
    }

    $fatal = @(Get-FatalLogFindings -ProcessLines $lines -RuntimeLogDirectory $runtimeLogSnapshot)
    if ($fatal.Count -gt 0) {
        $message = "$Name contains $($fatal.Count) fatal Mixin, missing-class, or lifecycle finding(s)."
        $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
    }

    return [PSCustomObject]@{
        Name = $Name
        Status = if ($phaseFailure) { "FAIL" } else { "PASS" }
        TraceClasses = $TraceClasses
        Done = $started
        SaveConfirmed = $saveConfirmed
        ExitCode = $exitCode
        DurationSeconds = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
        ConsoleLog = $consoleLog
        RuntimeLogs = $runtimeLogSnapshot
        CraftTweakerLog = $craftTweakerLogSnapshot
        FatalFindings = $fatal
        Error = $phaseFailure
    }
}

function Get-LoadedClasses {
    param([string]$TraceLog)

    $classes = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($line in [IO.File]::ReadLines($TraceLog)) {
        if ($line -match '^\[Loaded (?<class>[^\s]+) (?:from|by) ') {
            $null = $classes.Add($Matches.class)
        }
    }
    return @($classes | Sort-Object)
}

function Write-TextList {
    param(
        [string]$Path,
        [object[]]$Values
    )
    [IO.File]::WriteAllLines($Path, [string[]]$Values, [Text.UTF8Encoding]::new($false))
}

function Write-VerificationReport {
    $status = if ($scriptFailure) { "FAIL" } else { "PASS" }
    $reportLines = @(
        "# Standard Forge 14.23.5.2847 dedicated-server verification",
        "",
        "- Status: $status",
        "- Release gate satisfied: $(!$scriptFailure)",
        "- Run ID: $runId",
        "- Generated: $([DateTime]::UtcNow.ToString('u')) UTC",
        "- Evidence directory: ``$testRoot``",
        "- Forge source directory: ``$ForgeDirectory``",
        "- Ponder artifact: ``$ponderJar``",
        "- Ponder version: $ponderVersion",
        "- MixinBooter source: ``$mixinBooterSource``",
        "- Java executable: ``$JavaExecutable``",
        "- Java version: $javaVersion",
        "",
        "## SHA-256",
        "",
        "| Input | SHA-256 |",
        "| --- | --- |",
        "| Forge universal | $forgeHash |",
        "| Minecraft server | $minecraftServerHash |",
        "| Ponder $ponderVersion | $ponderHash |",
        "| MixinBooter $mixinBooterVersion | $mixinBooterHash |",
        "| Library hash manifest ($libraryCount jars) | $libraryManifestHash |",
        "| level.dat after initial run | $initialLevelHash |",
        "| level.dat after same-world restart | $restartLevelHash |",
        "",
        "Library hashes: ``$(Join-Path $testRoot 'library-sha256.txt')``",
        "Initial level snapshot: ``$(Join-Path $testRoot 'level-after-initial.dat')``",
        "Restart level snapshot: ``$(Join-Path $testRoot 'level-after-restart.dat')``",
        "",
        "## Server runs",
        "",
        "| Phase | Status | Class trace | Done | Saved | Exit | Seconds | Console log | Runtime logs |",
        "| --- | --- | --- | --- | --- | --- | ---: | --- | --- |"
    )
    foreach ($phase in $phaseResults) {
        $reportLines += "| $($phase.Name) | $($phase.Status) | $($phase.TraceClasses) | $($phase.Done) | " +
            "$($phase.SaveConfirmed) | $($phase.ExitCode) | $($phase.DurationSeconds) | " +
            "``$($phase.ConsoleLog)`` | ``$($phase.RuntimeLogs)`` |"
    }

    $reportLines += @(
        "",
        "## Dedicated-server class audit",
        "",
        "- Baseline loaded classes: $baselineClassCount",
        "- Modded restart loaded classes: $moddedClassCount",
        "- Classes introduced over the empty Forge baseline: $introducedClassCount",
        "- Ponder/Catnip classes introduced: $introducedProjectClassCount",
        "- Baseline ``net.minecraft.client`` classes: $baselineVanillaClientCount",
        "- Modded ``net.minecraft.client`` classes: $moddedVanillaClientCount",
        "- New ``net.minecraft.client`` classes over baseline: $($introducedVanillaClientClasses.Count)",
        "- Forbidden Ponder/Catnip client/render/gui/outliner/ghostblock/foundation.ui classes: $($forbiddenProjectClasses.Count)",
        "- Baseline class list: ``$(Join-Path $testRoot 'classes-baseline.txt')``",
        "- Modded class list: ``$(Join-Path $testRoot 'classes-modded.txt')``",
        "- Introduced class list: ``$(Join-Path $testRoot 'classes-introduced.txt')``",
        "- Forbidden class list: ``$(Join-Path $testRoot 'classes-forbidden.txt')``"
    )

    $allPhaseFindings = @($phaseResults | ForEach-Object { $_.FatalFindings })
    if ($scriptFailure -or $blockingFindings.Count -gt 0 -or $allPhaseFindings.Count -gt 0) {
        $reportLines += @("", "## Blocking findings", "")
        if ($scriptFailure) { $reportLines += "- $(ConvertTo-MarkdownCell $scriptFailure)" }
        foreach ($finding in $blockingFindings) {
            $reportLines += "- $(ConvertTo-MarkdownCell $finding)"
        }
        foreach ($finding in $allPhaseFindings) {
            $reportLines += "- $(ConvertTo-MarkdownCell $finding)"
        }
    }

    $reportLines += @(
        "",
        "> This report covers a Java 8 standard Forge dedicated server. It is not client rendering or OpenGL acceptance evidence."
    )
    [IO.File]::WriteAllLines($report, $reportLines, [Text.UTF8Encoding]::new($false))
}

try {
    $resolvedJava = Resolve-Java8 -PreferredPath $JavaExecutable
    $JavaExecutable = $resolvedJava.Path
    $javaVersion = "$($resolvedJava.FileVersion); $($resolvedJava.RuntimeVersion)"

    $bootstrapForgeDirectory = [string]::IsNullOrWhiteSpace($ForgeDirectory)
    if ([string]::IsNullOrWhiteSpace($ForgeDirectory)) {
        $ForgeDirectory = Join-Path $buildRoot "forge-2847-server-source"
    }
    if (!(Test-Path -LiteralPath $ForgeDirectory -PathType Container)) {
        if (!$bootstrapForgeDirectory) {
            throw "Forge source directory does not exist: $ForgeDirectory"
        }
        $null = New-Item -ItemType Directory -Path $ForgeDirectory -Force
    }
    $ForgeDirectory = (Resolve-Path -LiteralPath $ForgeDirectory).Path

    $forgeJar = Join-Path $ForgeDirectory $forgeFileName
    $minecraftServerJar = Join-Path $ForgeDirectory $minecraftServerFileName
    $librariesDirectory = Join-Path $ForgeDirectory "libraries"
    if ($bootstrapForgeDirectory -and
        (!(Test-Path -LiteralPath $forgeJar -PathType Leaf) -or
         !(Test-Path -LiteralPath $minecraftServerJar -PathType Leaf) -or
         !(Test-Path -LiteralPath $librariesDirectory -PathType Container))) {
        Install-ForgeServerSource -Destination $ForgeDirectory -JavaPath $JavaExecutable
    }
    foreach ($requiredFile in @($forgeJar, $minecraftServerJar)) {
        if (!(Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "Forge source directory is missing required file: $requiredFile"
        }
    }
    if (!(Test-Path -LiteralPath $librariesDirectory -PathType Container)) {
        throw "Forge source directory is missing libraries: $librariesDirectory"
    }
    $forgeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $forgeJar).Hash
    if ($forgeHash -ne $expectedForgeHash) {
        throw "Forge universal SHA256 mismatch. Expected $expectedForgeHash, found $forgeHash"
    }
    $minecraftServerHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $minecraftServerJar).Hash
    if ($minecraftServerHash -ne $expectedMinecraftServerHash) {
        throw "Minecraft server SHA256 mismatch. Expected $expectedMinecraftServerHash, found $minecraftServerHash"
    }
    $launchWrapper = Join-Path $librariesDirectory "net\minecraft\launchwrapper\1.12\launchwrapper-1.12.jar"
    if (!(Test-Path -LiteralPath $launchWrapper -PathType Leaf)) {
        throw "Forge libraries are incomplete; launchwrapper is missing: $launchWrapper"
    }

    if (!(Test-Path -LiteralPath $ponderJar -PathType Leaf)) {
        throw "Build the final 1.3.0-alpha.1 reobf artifact before this test. Required: $ponderJar"
    }
    if ([IO.Path]::GetFileName($ponderJar) -ne $ponderFileName) {
        throw "Only the final $ponderFileName artifact may be tested."
    }
    if (!(Test-Path -LiteralPath $ServerHarnessJar -PathType Leaf)) {
        throw "Build the server fixture harness before this test. Required: $ServerHarnessJar"
    }
    if (!(Test-Path -LiteralPath $fixtureScripts -PathType Container)) {
        throw "CraftTweaker fixture scripts are missing: $fixtureScripts"
    }
    $ponderManifest = Get-JarManifestText -JarPath $ponderJar
    if ($ponderManifest -notmatch "(?m)^Implementation-Version:\s*$([regex]::Escape($ponderVersion))\s*`$") {
        throw "Ponder manifest does not declare Implementation-Version $ponderVersion."
    }

    $resolvedMixinBooter = Get-VerifiedMixinBooter -PreferredPath $MixinBooterJar
    $MixinBooterJar = $resolvedMixinBooter.Path
    $mixinBooterHash = $resolvedMixinBooter.Hash
    $mixinBooterSource = "$($resolvedMixinBooter.Source): $MixinBooterJar"
    $CraftTweakerJar = Get-CraftTweaker -PreferredPath $CraftTweakerJar

    $ponderHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $ponderJar).Hash
    $libraryManifest = Join-Path $testRoot "library-sha256.txt"
    $libraryCount = Write-LibraryHashManifest -SourceDirectory $librariesDirectory -Destination $libraryManifest
    if ($libraryCount -lt 1) { throw "Forge libraries contain no jar files: $librariesDirectory" }
    $libraryManifestHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $libraryManifest).Hash

    Initialize-ServerDirectory -Destination $baselineRoot -Port ($ServerPort + 1) `
        -WorldName "world-baseline" -WithPonder $false
    Initialize-ServerDirectory -Destination $moddedRoot -Port $ServerPort `
        -WorldName "world" -WithPonder $true

    $baseline = Invoke-ForgeServer -Name "baseline-trace" -ServerRoot $baselineRoot -TraceClasses $true
    $phaseResults.Add($baseline)
    if ($baseline.Status -ne "PASS") { throw "Empty Forge baseline failed: $($baseline.Error)" }

    $initial = Invoke-ForgeServer -Name "modded-initial" -ServerRoot $moddedRoot -TraceClasses $false
    $phaseResults.Add($initial)
    if ($initial.Status -ne "PASS") { throw "Ponder initial run failed: $($initial.Error)" }

    $levelDat = Join-Path $moddedRoot "world\level.dat"
    if (!(Test-Path -LiteralPath $levelDat -PathType Leaf)) {
        throw "Ponder initial run did not create world\level.dat."
    }
    $initialLevelSnapshot = Join-Path $testRoot "level-after-initial.dat"
    Copy-Item -LiteralPath $levelDat -Destination $initialLevelSnapshot
    $initialLevelHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $initialLevelSnapshot).Hash

    $negativeFixtures = [ordered]@{
        "90_syntax_error.zs" = "90_syntax_error.zs"
        "91_duplicate_id.zs" = "Duplicate Ponder script scene id"
        "92_invalid_handle.zs" = "Unknown scene handle"
        "93_oversized_nbt.zs" = "Tile NBT exceeds 256 KiB text safety limit"
        "94_unregistered.zs" = "Ponder scene builder was not registered"
    }
    foreach ($fixture in $negativeFixtures.GetEnumerator()) {
        Set-CraftTweakerFixtures -ServerRoot $moddedRoot `
            -FileNames @("00_advanced.zs", $fixture.Key)
        $phaseName = "fixture-" + [IO.Path]::GetFileNameWithoutExtension($fixture.Key)
        $fixturePhase = Invoke-ForgeServer -Name $phaseName -ServerRoot $moddedRoot -TraceClasses $false
        $phaseResults.Add($fixturePhase)
        if ($fixturePhase.Status -ne "PASS") {
            throw "CraftTweaker fixture phase $($fixture.Key) failed: $($fixturePhase.Error)"
        }
        $diagnosticText = [IO.File]::ReadAllText($fixturePhase.ConsoleLog)
        if (Test-Path -LiteralPath $fixturePhase.CraftTweakerLog -PathType Leaf) {
            $diagnosticText += [Environment]::NewLine +
                [IO.File]::ReadAllText($fixturePhase.CraftTweakerLog)
        }
        if ($diagnosticText -notmatch [regex]::Escape($fixture.Value)) {
            throw "CraftTweaker fixture $($fixture.Key) did not report: $($fixture.Value)"
        }
    }

    Set-CraftTweakerFixtures -ServerRoot $moddedRoot -FileNames @("00_advanced.zs")
    $restart = Invoke-ForgeServer -Name "modded-restart-trace" -ServerRoot $moddedRoot -TraceClasses $true
    $phaseResults.Add($restart)
    if ($restart.Status -ne "PASS") { throw "Ponder same-world restart failed: $($restart.Error)" }
    if (!(Test-Path -LiteralPath $levelDat -PathType Leaf)) {
        throw "Ponder restart lost world\level.dat."
    }
    $restartLevelSnapshot = Join-Path $testRoot "level-after-restart.dat"
    Copy-Item -LiteralPath $levelDat -Destination $restartLevelSnapshot
    $restartLevelHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $restartLevelSnapshot).Hash

    $baselineClasses = @(Get-LoadedClasses -TraceLog $baseline.ConsoleLog)
    $moddedClasses = @(Get-LoadedClasses -TraceLog $restart.ConsoleLog)
    $baselineClassCount = $baselineClasses.Count
    $moddedClassCount = $moddedClasses.Count
    $baselineSet = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($className in $baselineClasses) { $null = $baselineSet.Add($className) }
    $introducedClasses = @($moddedClasses | Where-Object { !$baselineSet.Contains($_) })
    $introducedClassCount = $introducedClasses.Count
    $introducedProjectClasses = @($introducedClasses | Where-Object {
        $_ -match '^net\.createmod\.(?:ponder|catnip)\.'
    })
    $introducedProjectClassCount = $introducedProjectClasses.Count
    $baselineVanillaClientCount = @($baselineClasses | Where-Object {
        $_ -match '^net\.minecraft\.client\.'
    }).Count
    $moddedVanillaClientCount = @($moddedClasses | Where-Object {
        $_ -match '^net\.minecraft\.client\.'
    }).Count
    $introducedVanillaClientClasses = @($introducedClasses | Where-Object {
        $_ -match '^net\.minecraft\.client\.'
    })
    $forbiddenProjectPattern = '^net\.createmod\.ponder\.(?:client|render|foundation\.ui)(?:\.|$)|' +
        '^net\.createmod\.catnip\.(?:client|gui|render|outliner|ghostblock|impl\.client|config\.ui)(?:\.|$)'
    $forbiddenProjectClasses = @($moddedClasses | Where-Object { $_ -match $forbiddenProjectPattern })

    Write-TextList -Path (Join-Path $testRoot "classes-baseline.txt") -Values $baselineClasses
    Write-TextList -Path (Join-Path $testRoot "classes-modded.txt") -Values $moddedClasses
    Write-TextList -Path (Join-Path $testRoot "classes-introduced.txt") -Values $introducedClasses
    Write-TextList -Path (Join-Path $testRoot "classes-forbidden.txt") `
        -Values @($introducedVanillaClientClasses + $forbiddenProjectClasses | Sort-Object -Unique)

    if ($moddedClasses -notcontains "net.createmod.ponder.PonderMod") {
        $blockingFindings.Add("The traced restart did not load net.createmod.ponder.PonderMod.")
    }
    if ($moddedClasses -notcontains "net.createmod.ponder.mixin.PonderMixinLoader") {
        $blockingFindings.Add("The traced restart did not load net.createmod.ponder.mixin.PonderMixinLoader.")
    }
    $restartText = [IO.File]::ReadAllText($restart.ConsoleLog)
    if ($restartText -notmatch 'Registered 2 Ponder plugin\(s\) and 9 storyboard\(s\)') {
        $blockingFindings.Add("The traced restart did not confirm eight builtins plus the positive fixture storyboard.")
    }
    $fixtureReport = Join-Path $moddedRoot "ponder-fixture-harness.properties"
    if (!(Test-Path -LiteralPath $fixtureReport -PathType Leaf) -or
        [IO.File]::ReadAllText($fixtureReport) -notmatch '(?m)^status=PASS\s*$') {
        $blockingFindings.Add("The real CraftTweaker server harness did not produce a PASS report.")
    }
    if ($introducedVanillaClientClasses.Count -gt 0) {
        $blockingFindings.Add("The modded server loaded $($introducedVanillaClientClasses.Count) new net.minecraft.client class(es) beyond the empty Forge baseline.")
    }
    if ($forbiddenProjectClasses.Count -gt 0) {
        $blockingFindings.Add("The modded server loaded $($forbiddenProjectClasses.Count) forbidden Ponder/Catnip client-side class(es).")
    }
    if ($blockingFindings.Count -gt 0) {
        throw "Dedicated-server class audit failed with $($blockingFindings.Count) finding(s)."
    }
} catch {
    $scriptFailure = $_.Exception.Message
} finally {
    Write-VerificationReport
}

if ($scriptFailure) {
    throw "Standard Forge verification failed. See $report. $scriptFailure"
}

Write-Host "Standard Forge 14.23.5.2847 dedicated-server verification passed."
Write-Host "Evidence: $testRoot"
Write-Host "Report: $report"
Write-Host "Ponder SHA256: $ponderHash"
Write-Host "MixinBooter SHA256: $mixinBooterHash"
