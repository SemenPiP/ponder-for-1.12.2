param(
    [string]$CatServerJar = "",
    [string]$MixinBooterJar = "",
    [string]$MixinBooterVersion = "11.2",
    [string]$ExpectedMixinBooterHash = "",
    [string]$CraftTweakerJar = "",
    [string]$LibrariesDirectory = "",
    [int]$TimeoutSeconds = 600,
    [switch]$WaitForClient,
    [int]$ClientTimeoutSeconds = 300,
    [int]$DemoTimeoutSeconds = 600
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$buildRoot = Join-Path $projectRoot "build"
$smokeRoot = Join-Path $buildRoot "catserver-smoke"
$reportRoot = Join-Path $buildRoot "reports"
$runId = [DateTime]::UtcNow.ToString("yyyyMMdd-HHmmssfff") + "-" +
    (([Guid]::NewGuid().ToString("N")).Substring(0, 8))
$testRoot = Join-Path $smokeRoot $runId
$report = Join-Path $reportRoot "catserver-verification-$runId.md"
$demoFlag = Join-Path $testRoot "client-demo-ok.flag"

$expectedCatServerHash = "EAF575310ACBB48D535212CFB88D93DE69F90F2A81879A26F88457713A25952E"
if ([string]::IsNullOrWhiteSpace($ExpectedMixinBooterHash) -and $MixinBooterVersion -eq "11.2") {
    $ExpectedMixinBooterHash = "48667BC07D4F9D54A5C0F808DAA02DEB956128664DB24269EB34460F4CA2462E"
}
$mixinBooterUri = "https://maven.cleanroommc.com/zone/rong/mixinbooter/$mixinBooterVersion/mixinbooter-$mixinBooterVersion.jar"
$craftTweakerVersion = "4.1.20.698"
$craftTweakerUri = "https://api.modrinth.com/maven/maven/modrinth/crafttweaker/$craftTweakerVersion/crafttweaker-$craftTweakerVersion.jar"

$null = New-Item -ItemType Directory -Path $testRoot -Force
$null = New-Item -ItemType Directory -Path $reportRoot -Force

function New-LayerResult {
    param(
        [string]$Name,
        [int]$ModCount,
        [bool]$ClientRequired,
        [bool]$RestartRequired
    )

    return [PSCustomObject]@{
        Name = $Name
        ModCount = $ModCount
        ClientRequired = $ClientRequired
        ClientObserved = $false
        DemoRequired = $ClientRequired
        DemoConfirmed = $false
        SaveConfirmed = $false
        RestartRequired = $RestartRequired
        RestartPassed = $false
        Status = "NOT_RUN"
        Error = ""
        InitialLog = ""
        RestartLog = ""
        InitialRuntimeLogs = ""
        RestartRuntimeLogs = ""
    }
}

$results = [Collections.Generic.List[object]]::new()
$results.Add((New-LayerResult -Name "01-empty" -ModCount 0 -ClientRequired $false -RestartRequired $false))
$results.Add((New-LayerResult -Name "02-mixinbooter" -ModCount 1 -ClientRequired $false -RestartRequired $false))
$results.Add((New-LayerResult -Name "03-ponder" -ModCount 3 -ClientRequired $false -RestartRequired $false))
$results.Add((New-LayerResult -Name "04-example-addon" -ModCount 4 `
    -ClientRequired $WaitForClient.IsPresent -RestartRequired $true))

$catServerHash = "not checked"
$mixinBooterHash = "not checked"
$mixinBooterSource = "not resolved"
$releaseHash = "not checked"
$exampleHash = "not checked"
$java = "not resolved"
$javaVersion = "not checked"
$librariesSource = "CatServer self-bootstrap"
$scriptFailure = ""

function Get-VerifiedMixinBooter {
    param([string]$PreferredPath)

    $candidates = [Collections.Generic.List[string]]::new()
    if (![string]::IsNullOrWhiteSpace($PreferredPath)) {
        $candidates.Add((Resolve-Path -LiteralPath $PreferredPath).Path)
    } else {
        $cacheRoot = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\zone.rong\mixinbooter\$mixinBooterVersion"
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

function Add-ProcessLine {
    param(
        [Collections.Generic.List[string]]$Lines,
        [string]$Name,
        [string]$Phase,
        [string]$Line
    )

    if ($null -eq $Line) { return }
    $Lines.Add($Line)
    if ($Line -notmatch '^\[Loaded ') { Write-Host "[$Name/$Phase] $Line" }
}

function Complete-ProcessStreams {
    param(
        [Diagnostics.Process]$Process,
        [object]$StdoutTask,
        [object]$StderrTask,
        [Collections.Generic.List[string]]$Lines,
        [string]$Name,
        [string]$Phase
    )

    if ($null -ne $StdoutTask) {
        if (!$StdoutTask.Wait(5000)) { throw "$Name/$Phase stdout did not finish draining." }
        $line = $StdoutTask.Result
        if ($null -ne $line) {
            Add-ProcessLine -Lines $Lines -Name $Name -Phase $Phase -Line $line
        }
    }
    if ($null -ne $StderrTask) {
        if (!$StderrTask.Wait(5000)) { throw "$Name/$Phase stderr did not finish draining." }
        $line = $StderrTask.Result
        if ($null -ne $line) {
            Add-ProcessLine -Lines $Lines -Name $Name -Phase $Phase -Line "[stderr] $line"
        }
    }
    while (!$Process.StandardOutput.EndOfStream) {
        Add-ProcessLine -Lines $Lines -Name $Name -Phase $Phase `
            -Line $Process.StandardOutput.ReadLine()
    }
    while (!$Process.StandardError.EndOfStream) {
        Add-ProcessLine -Lines $Lines -Name $Name -Phase $Phase `
            -Line ("[stderr] " + $Process.StandardError.ReadLine())
    }
}

function Invoke-CatServerProcess {
    param(
        [string]$Name,
        [string]$LayerRoot,
        [string]$Phase,
        [bool]$RequireClient,
        [bool]$RequireDemo,
        [bool]$TraceClasses
    )

    $logName = if ($Phase -eq "restart") { "server-restart.log" } else { "server.log" }
    $logPath = Join-Path $LayerRoot $logName
    $runtimeLogSnapshot = Join-Path $LayerRoot "logs-$Phase"
    $lines = [Collections.Generic.List[string]]::new()
    $started = $false
    $clientObserved = $false
    $demoConfirmed = $false
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

    $startupDeadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $clientDeadline = [DateTime]::MaxValue
    $demoDeadline = [DateTime]::MaxValue
    $saveDeadline = [DateTime]::MaxValue
    $shutdownDeadline = [DateTime]::MaxValue

    $arguments = [Collections.Generic.List[string]]::new()
    if ($TraceClasses) { $arguments.Add("-verbose:class") }
    $arguments.Add("-Xms512M")
    $arguments.Add("-Xmx1536M")
    $arguments.Add("-jar")
    $arguments.Add("catserver.jar")
    $arguments.Add("nogui")

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $java
    $startInfo.Arguments = $arguments -join " "
    $startInfo.WorkingDirectory = $LayerRoot
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
                Add-ProcessLine -Lines $lines -Name $Name -Phase $Phase -Line $line
                if ($null -ne $line) {
                    if (!$started -and $line -match "Done \(.+\)! For help") {
                        $started = $true
                        if ($RequireClient) {
                            $clientDeadline = [DateTime]::UtcNow.AddSeconds($ClientTimeoutSeconds)
                            Write-Host "[$Name/$Phase] Waiting for a real client login on 127.0.0.1:25567"
                        }
                    }
                    if ($line -match "logged in with entity id") {
                        $clientObserved = $true
                        if ($RequireDemo) {
                            $demoDeadline = [DateTime]::UtcNow.AddSeconds($DemoTimeoutSeconds)
                            Write-Host "[$Name/$Phase] Client observed. Open the bundled demo, finish the checks, then create:"
                            Write-Host "[$Name/$Phase] $demoFlag"
                        }
                    }
                    if ($line -match "Saved the world|Saved the game") { $saveConfirmed = $true }
                    if ($saveRequested -and $line -match "Unknown command") { $saveFallback = $true }
                    if ($saveFallback -and $line -match "Saving chunks for level") { $saveConfirmed = $true }
                }
                $stdout = $process.StandardOutput.ReadLineAsync()
            }
            if ($stderr.IsCompleted) {
                $line = $stderr.Result
                if ($null -ne $line) {
                    Add-ProcessLine -Lines $lines -Name $Name -Phase $Phase -Line "[stderr] $line"
                }
                $stderr = $process.StandardError.ReadLineAsync()
            }

            $now = [DateTime]::UtcNow
            if (!$started) {
                if ($now -gt $startupDeadline) { throw "$Name/$Phase did not reach the Done state." }
                continue
            }
            if ($RequireClient -and !$clientObserved) {
                if ($now -gt $clientDeadline) {
                    throw "No real client connected within $ClientTimeoutSeconds seconds."
                }
                continue
            }
            if ($RequireDemo -and !$demoConfirmed) {
                if (Test-Path -LiteralPath $demoFlag -PathType Leaf) {
                    $demoConfirmed = $true
                    Write-Host "[$Name/$Phase] Demo confirmation flag observed."
                } elseif ($now -gt $demoDeadline) {
                    throw "The client connected, but $demoFlag was not created within $DemoTimeoutSeconds seconds."
                } else {
                    continue
                }
            }
            if (!$saveRequested) {
                $process.StandardInput.WriteLine("save-all")
                $process.StandardInput.Flush()
                $saveRequested = $true
                $saveDeadline = $now.AddSeconds(120)
                continue
            }
            if (!$saveConfirmed -and !$saveFallback) {
                if ($now -gt $saveDeadline) { throw "$Name/$Phase did not confirm save-all." }
                continue
            }
            if (!$stopSent) {
                $process.StandardInput.WriteLine("stop")
                $process.StandardInput.Flush()
                $stopSent = $true
                $shutdownDeadline = $now.AddSeconds(120)
                continue
            }
            if ($now -gt $shutdownDeadline) { throw "$Name/$Phase did not stop cleanly." }
        }

        if (!$process.WaitForExit(5000)) { throw "$Name/$Phase did not finish exiting." }
        $exitCode = $process.ExitCode
        Complete-ProcessStreams -Process $process -StdoutTask $stdout -StderrTask $stderr `
            -Lines $lines -Name $Name -Phase $Phase
        $streamsDrained = $true
        if ($saveFallback -and $exitCode -eq 0) { $saveConfirmed = $true }

        if (!$started) { throw "$Name/$Phase exited before the Done state." }
        if ($RequireClient -and !$clientObserved) { throw "$Name/$Phase did not observe a real client login." }
        if ($RequireDemo -and !$demoConfirmed) { throw "$Name/$Phase did not observe the demo confirmation flag." }
        if (!$saveConfirmed) { throw "$Name/$Phase did not confirm world saving." }
        if (!$stopSent) { throw "$Name/$Phase exited before the script sent stop." }
        if ($exitCode -ne 0) { throw "$Name/$Phase exited with code $exitCode." }
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
                    -Lines $lines -Name $Name -Phase $Phase
                $streamsDrained = $true
            } catch {
                $message = "$Name/$Phase could not drain its process output: $($_.Exception.Message)"
                $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
            }
        }
        $process.Dispose()
        [IO.File]::WriteAllLines($logPath, $lines, [Text.UTF8Encoding]::new($false))
    }

    $runtimeLogs = Join-Path $LayerRoot "logs"
    if (Test-Path -LiteralPath $runtimeLogs -PathType Container) {
        try {
            Copy-Item -LiteralPath $runtimeLogs -Destination $runtimeLogSnapshot -Recurse
        } catch {
            $message = "$Name/$Phase could not snapshot its runtime logs: $($_.Exception.Message)"
            $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
        }
    }

    $fatal = @($lines | Where-Object {
        $_ -notmatch '^\[Loaded ' -and
        $_ -match "Mixin apply failed|InvalidMixinException|InjectionError|MixinTransformerError|MixinApplyError|Critical injection failure|NoClassDefFoundError|ClassNotFoundException|Exception caught during firing event|Failed to load (a )?mod|\[FATAL\]"
    })
    $clientClassLoads = @($lines | Where-Object {
        $_ -match "\[Loaded (net\.minecraft\.client\.|net\.createmod\.ponder\.(client|render|foundation\.ui)\.|net\.createmod\.catnip\.(client|gui|render|outliner|ghostblock)\.)"
    })

    if ($fatal.Count -gt 0) {
        $message = "$Name/$Phase contains $($fatal.Count) fatal Mixin, class-loading, or lifecycle line(s)."
        $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
    }
    if ($clientClassLoads.Count -gt 0) {
        $message = "$Name/$Phase loaded $($clientClassLoads.Count) client-only class(es) on the dedicated server."
        $phaseFailure = if ($phaseFailure) { "$phaseFailure $message" } else { $message }
    }
    if ($phaseFailure) { throw $phaseFailure }

    return [PSCustomObject]@{
        ClientObserved = $clientObserved
        DemoConfirmed = $demoConfirmed
        SaveConfirmed = $saveConfirmed
        Log = $logPath
        RuntimeLogs = $runtimeLogSnapshot
    }
}

function Invoke-CatServerLayer {
    param(
        [string]$Name,
        [string[]]$ModFiles,
        [bool]$RequireClient,
        [bool]$RequireRestart,
        [bool]$TraceClasses
    )

    $result = New-LayerResult -Name $Name -ModCount $ModFiles.Count `
        -ClientRequired $RequireClient -RestartRequired $RequireRestart
    $layerRoot = Join-Path $testRoot $Name
    $modsRoot = Join-Path $layerRoot "mods"
    $result.InitialLog = Join-Path $layerRoot "server.log"
    $result.InitialRuntimeLogs = Join-Path $layerRoot "logs-initial"
    if ($RequireRestart) { $result.RestartLog = Join-Path $layerRoot "server-restart.log" }
    if ($RequireRestart) { $result.RestartRuntimeLogs = Join-Path $layerRoot "logs-restart" }

    try {
        $null = New-Item -ItemType Directory -Path $modsRoot -Force
        Copy-Item -LiteralPath $CatServerJar -Destination (Join-Path $layerRoot "catserver.jar")
        if (![string]::IsNullOrWhiteSpace($LibrariesDirectory)) {
            Copy-Item -LiteralPath $LibrariesDirectory -Destination (Join-Path $layerRoot "libraries") -Recurse
        }
        foreach ($mod in $ModFiles) {
            Copy-Item -LiteralPath $mod -Destination (Join-Path $modsRoot ([IO.Path]::GetFileName($mod)))
        }
        [IO.File]::WriteAllText((Join-Path $layerRoot "eula.txt"), "eula=true`n",
            [Text.UTF8Encoding]::new($false))
        [IO.File]::WriteAllLines((Join-Path $layerRoot "server.properties"), @(
            "online-mode=false",
            "server-port=25567",
            "motd=Ponder CatServer verification",
            "max-tick-time=-1",
            "view-distance=4",
            "level-name=world"
        ), [Text.UTF8Encoding]::new($false))

        $initial = Invoke-CatServerProcess -Name $Name -LayerRoot $layerRoot -Phase "initial" `
            -RequireClient $RequireClient -RequireDemo $RequireClient -TraceClasses $TraceClasses
        $result.ClientObserved = $initial.ClientObserved
        $result.DemoConfirmed = $initial.DemoConfirmed
        $result.SaveConfirmed = $initial.SaveConfirmed

        if ($RequireRestart) {
            $levelDat = Join-Path $layerRoot "world\level.dat"
            if (!(Test-Path -LiteralPath $levelDat -PathType Leaf)) {
                throw "$Name did not create world\level.dat before restart."
            }
            $restart = Invoke-CatServerProcess -Name $Name -LayerRoot $layerRoot -Phase "restart" `
                -RequireClient $false -RequireDemo $false -TraceClasses $TraceClasses
            if (!$restart.SaveConfirmed) { throw "$Name restart did not save the world." }
            $result.RestartPassed = $true
        }

        $result.Status = "PASS"
    } catch {
        $result.Status = "FAIL"
        $result.Error = $_.Exception.Message
    }
    return $result
}

function ConvertTo-MarkdownCell {
    param([string]$Value)
    if ($null -eq $Value) { return "" }
    return (($Value -replace "\r?\n", " ") -replace "\|", "&#124;")
}

function Write-VerificationReport {
    $overallStatus = if ($scriptFailure) {
        "FAIL"
    } elseif ($WaitForClient.IsPresent) {
        "PASS"
    } else {
        "PASS_SERVER_ONLY"
    }
    $releaseGate = (!$scriptFailure -and $WaitForClient.IsPresent)

    $reportLines = @(
        "# CatServer layered verification",
        "",
        "- Status: $overallStatus",
        "- Release gate satisfied: $releaseGate",
        "- Run ID: $runId",
        "- Generated: $([DateTime]::UtcNow.ToString('u')) UTC",
        "- Evidence directory: ``$testRoot``",
        "- CatServer SHA256: $catServerHash",
        "- MixinBooter version: $mixinBooterVersion",
        "- MixinBooter SHA256: $mixinBooterHash",
        "- MixinBooter source: ``$mixinBooterSource``",
        "- Ponder SHA256: $releaseHash",
        "- Example addon SHA256: $exampleHash",
        "- Java executable: ``$java``",
        "- Java file version: $javaVersion",
        "- Runtime libraries: ``$librariesSource``",
        "- Real client required: $($WaitForClient.IsPresent)",
        "- Demo confirmation flag: ``$demoFlag``",
        "",
        "| Layer | Mods | Client required | Client observed | Demo confirmed | Save confirmed | Restart required | Restart passed | Status |",
        "| --- | ---: | --- | --- | --- | --- | --- | --- | --- |"
    )
    foreach ($result in $results) {
        $reportLines += "| $($result.Name) | $($result.ModCount) | $($result.ClientRequired) | $($result.ClientObserved) | $($result.DemoConfirmed) | $($result.SaveConfirmed) | $($result.RestartRequired) | $($result.RestartPassed) | $($result.Status) |"
    }

    $reportLines += ""
    $reportLines += "## Evidence"
    $reportLines += ""
    foreach ($result in $results) {
        if ($result.InitialLog) { $reportLines += "- $($result.Name) initial: ``$($result.InitialLog)``" }
        if ($result.InitialRuntimeLogs) { $reportLines += "- $($result.Name) initial runtime logs: ``$($result.InitialRuntimeLogs)``" }
        if ($result.RestartLog) { $reportLines += "- $($result.Name) restart: ``$($result.RestartLog)``" }
        if ($result.RestartRuntimeLogs) { $reportLines += "- $($result.Name) restart runtime logs: ``$($result.RestartRuntimeLogs)``" }
    }

    $failures = @($results | Where-Object { $_.Status -eq "FAIL" })
    if ($scriptFailure -or $failures.Count -gt 0) {
        $reportLines += ""
        $reportLines += "## Blocking findings"
        $reportLines += ""
        if ($scriptFailure) { $reportLines += "- $(ConvertTo-MarkdownCell $scriptFailure)" }
        foreach ($failure in $failures) {
            $reportLines += "- $($failure.Name): $(ConvertTo-MarkdownCell $failure.Error)"
        }
    }
    if (!$WaitForClient.IsPresent) {
        $reportLines += ""
        $reportLines += "> This was a server-only layered smoke test. It is not evidence of a real client login or demo playback."
    }

    [IO.File]::WriteAllLines($report, $reportLines, [Text.UTF8Encoding]::new($false))
}

try {
    if ([string]::IsNullOrWhiteSpace($CatServerJar)) {
        $CatServerJar = Join-Path $projectRoot "..\CatServer-4168d848-universal.jar"
    }
    $CatServerJar = (Resolve-Path -LiteralPath $CatServerJar).Path
    $catServerHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $CatServerJar).Hash
    if ($catServerHash -ne $expectedCatServerHash) {
        throw "The CatServer jar is not the supported SHA256 $expectedCatServerHash build. Found $catServerHash"
    }

    $releaseJar = Join-Path $buildRoot "libs\Ponder-1.12.2-1.1.3.jar"
    $exampleJar = Join-Path $buildRoot "libs\Ponder-Example-Addon-1.12.2-1.1.3.jar"
    foreach ($artifact in @($releaseJar, $exampleJar)) {
        if (!(Test-Path -LiteralPath $artifact -PathType Leaf)) {
            throw "Build all release and example artifacts before this test: $artifact"
        }
    }
    $releaseHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $releaseJar).Hash
    $exampleHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $exampleJar).Hash

    $resolvedMixinBooter = Get-VerifiedMixinBooter -PreferredPath $MixinBooterJar
    $MixinBooterJar = $resolvedMixinBooter.Path
    $mixinBooterHash = $resolvedMixinBooter.Hash
    $mixinBooterSource = "$($resolvedMixinBooter.Source): $MixinBooterJar"
    $CraftTweakerJar = Get-CraftTweaker -PreferredPath $CraftTweakerJar

    if (![string]::IsNullOrWhiteSpace($LibrariesDirectory)) {
        $LibrariesDirectory = (Resolve-Path -LiteralPath $LibrariesDirectory).Path
        if (!(Test-Path -LiteralPath $LibrariesDirectory -PathType Container)) {
            throw "CatServer libraries directory does not exist: $LibrariesDirectory"
        }
        $requiredLibraries = @("launchwrapper-1.12.jar", "minecraft_server.1.12.2.jar")
        foreach ($requiredLibrary in $requiredLibraries) {
            if (!(Test-Path -LiteralPath (Join-Path $LibrariesDirectory $requiredLibrary) -PathType Leaf)) {
                throw "CatServer libraries directory is missing ${requiredLibrary}: $LibrariesDirectory"
            }
        }
        $libraryCount = (Get-ChildItem -LiteralPath $LibrariesDirectory -File -Recurse | Measure-Object).Count
        $librariesSource = "local copy: $LibrariesDirectory ($libraryCount files)"
    }

    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "" }
    if (!(Test-Path -LiteralPath $java -PathType Leaf)) {
        $java = (Get-Command java.exe).Source
    }
    $javaVersion = (Get-Item -LiteralPath $java).VersionInfo.ProductVersion
    if ($javaVersion -notmatch "^8\.") {
        throw "CatServer verification requires Java 8. Found file version $javaVersion at $java"
    }

    $layerMods = @(
        [string[]]@(),
        [string[]]@($MixinBooterJar),
        [string[]]@($MixinBooterJar, $CraftTweakerJar, $releaseJar),
        [string[]]@($MixinBooterJar, $CraftTweakerJar, $releaseJar, $exampleJar)
    )
    for ($i = 0; $i -lt $results.Count; $i++) {
        $pending = $results[$i]
        $layerResult = Invoke-CatServerLayer -Name $pending.Name -ModFiles $layerMods[$i] `
            -RequireClient $pending.ClientRequired -RequireRestart $pending.RestartRequired `
            -TraceClasses ($i -ge 2)
        $results[$i] = $layerResult
        if ($layerResult.Status -ne "PASS") {
            throw "$($layerResult.Name) failed: $($layerResult.Error)"
        }
    }
} catch {
    $scriptFailure = $_.Exception.Message
} finally {
    Write-VerificationReport
}

if ($scriptFailure) {
    throw "CatServer layered verification failed. See $report. $scriptFailure"
}

$mode = if ($WaitForClient.IsPresent) { "full client gate" } else { "server-only smoke test" }
Write-Host "CatServer layered verification passed ($mode)."
Write-Host "Evidence: $testRoot"
Write-Host "Report: $report"
Write-Host "Ponder SHA256: $releaseHash"
