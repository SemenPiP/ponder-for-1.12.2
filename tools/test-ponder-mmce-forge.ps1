param(
    [string]$ForgeDirectory = "",
    [string]$PonderJar = "",
    [string]$PonderMMCEJar = "",
    [string]$SmokePack = "",
    [string]$ServerHarnessJar = "",
    [string]$MixinBooterJar = "",
    [string]$CraftTweakerJar = "",
    [string]$MMCEJar = "",
    [string]$JavaExecutable = "",
    [int]$TimeoutSeconds = 600,
    [int]$ServerPort = 25578
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2.0

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$buildRoot = Join-Path $projectRoot "build"
$addonBuild = Join-Path $projectRoot "ponder-mmce\build"
$runId = [DateTime]::UtcNow.ToString("yyyyMMdd-HHmmssfff") + "-" +
    ([Guid]::NewGuid().ToString("N").Substring(0, 8))
$testRoot = Join-Path $buildRoot "ponder-mmce-forge-smoke\$runId"
$runtimeRoot = Join-Path $testRoot "runtime"
$reportRoot = Join-Path $buildRoot "reports"
$reportPath = Join-Path $reportRoot "ponder-mmce-forge-verification-$runId.md"
$forgeVersion = "14.23.5.2847"
$forgeName = "forge-1.12.2-$forgeVersion-universal.jar"
$minecraftServerName = "minecraft_server.1.12.2.jar"

function Resolve-FirstFile {
    param(
        [string]$Preferred,
        [string]$SearchRoot,
        [string]$Filter,
        [string]$Label
    )
    if (![string]::IsNullOrWhiteSpace($Preferred)) {
        $resolved = (Resolve-Path -LiteralPath $Preferred).Path
        if (!(Test-Path -LiteralPath $resolved -PathType Leaf)) {
            throw "$Label is not a file: $resolved"
        }
        return $resolved
    }
    $found = Get-ChildItem -LiteralPath $SearchRoot -Filter $Filter -File -Recurse -ErrorAction SilentlyContinue |
        Sort-Object FullName |
        Select-Object -First 1
    if ($null -eq $found) { throw "Could not resolve $Label under $SearchRoot with filter $Filter" }
    return $found.FullName
}

function Resolve-Java8 {
    param([string]$Preferred)
    $candidate = $Preferred
    if ([string]::IsNullOrWhiteSpace($candidate) -and ![string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    }
    if ([string]::IsNullOrWhiteSpace($candidate)) {
        $command = Get-Command java.exe -ErrorAction SilentlyContinue
        if ($null -ne $command) { $candidate = $command.Source }
    }
    if ([string]::IsNullOrWhiteSpace($candidate) -or !(Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw "Ponder-MMCE Forge fixture requires Java 8."
    }
    $candidate = (Resolve-Path -LiteralPath $candidate).Path
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $candidate
    $startInfo.Arguments = "-version"
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    try {
        $null = $process.Start()
        $version = ($process.StandardError.ReadToEnd() + "`n" +
            $process.StandardOutput.ReadToEnd()).Trim()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0 -or $version -notmatch 'version "1\.8\.') {
            throw "Ponder-MMCE Forge fixture requires Java 8. Found: $version"
        }
    } finally {
        $process.Dispose()
    }
    return $candidate
}

function Copy-ForgeRuntime {
    param([string]$Source, [string]$Destination)
    $null = New-Item -ItemType Directory -Path $Destination -Force
    $null = New-Item -ItemType Directory -Path (Join-Path $Destination "mods") -Force
    Copy-Item -LiteralPath (Join-Path $Source $forgeName) -Destination (Join-Path $Destination $forgeName)
    Copy-Item -LiteralPath (Join-Path $Source $minecraftServerName) `
        -Destination (Join-Path $Destination $minecraftServerName)
    Copy-Item -LiteralPath (Join-Path $Source "libraries") `
        -Destination (Join-Path $Destination "libraries") -Recurse
}

function Invoke-Server {
    param(
        [string]$Phase,
        [string]$JvmArguments = ""
    )
    $lines = [Collections.Generic.List[string]]::new()
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $JavaExecutable
    $startInfo.Arguments = "$JvmArguments -Xms512M -Xmx1536M -jar `"$forgeName`" nogui".Trim()
    $startInfo.WorkingDirectory = $runtimeRoot
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $stdout = $null
    $stderr = $null
    $started = $false
    $stopSent = $false
    $stopAt = [DateTime]::MaxValue
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    try {
        $null = $process.Start()
        $stdout = $process.StandardOutput.ReadLineAsync()
        $stderr = $process.StandardError.ReadLineAsync()
        while (!$process.HasExited) {
            if ([DateTime]::UtcNow -gt $deadline) {
                try { $process.Kill() } catch {}
                throw "$Phase timed out after $TimeoutSeconds seconds"
            }
            if ($stdout.Wait(50)) {
                $line = $stdout.Result
                if ($null -ne $line) {
                    $lines.Add($line)
                    Write-Host "[$Phase] $line"
                    if (!$started -and $line -match 'Done \(.+\)! For help') {
                        $started = $true
                        $stopAt = [DateTime]::UtcNow.AddSeconds(2)
                    }
                }
                $stdout = $process.StandardOutput.ReadLineAsync()
            }
            if ($stderr.IsCompleted) {
                $line = $stderr.Result
                if ($null -ne $line) {
                    $lines.Add("[stderr] $line")
                    Write-Host "[$Phase] [stderr] $line"
                }
                $stderr = $process.StandardError.ReadLineAsync()
            }
            if ($started -and !$stopSent -and [DateTime]::UtcNow -ge $stopAt) {
                $process.StandardInput.WriteLine("save-all")
                $process.StandardInput.WriteLine("stop")
                $process.StandardInput.Flush()
                $stopSent = $true
            }
        }
        while (!$process.StandardOutput.EndOfStream) { $lines.Add($process.StandardOutput.ReadLine()) }
        while (!$process.StandardError.EndOfStream) {
            $lines.Add("[stderr] " + $process.StandardError.ReadLine())
        }
        if (!$started) { throw "$Phase never reached the Forge Done marker" }
        if ($process.ExitCode -ne 0) { throw "$Phase exited with code $($process.ExitCode)" }
    } finally {
        $process.Dispose()
    }
    $console = Join-Path $testRoot "$Phase-console.log"
    [IO.File]::WriteAllLines($console, $lines, [Text.UTF8Encoding]::new($false))
    return [PSCustomObject]@{ Phase = $Phase; Console = $console; Lines = $lines }
}

function Require-HarnessPass {
    param([string]$ExpectedProperty = "")
    $harnessReport = Join-Path $runtimeRoot "ponder-mmce-fixture-harness.properties"
    if (!(Test-Path -LiteralPath $harnessReport -PathType Leaf)) {
        throw "Ponder-MMCE harness report is missing: $harnessReport"
    }
    $text = [IO.File]::ReadAllText($harnessReport)
    if ($text -notmatch '(?m)^status=PASS\s*$') {
        throw "Ponder-MMCE harness did not report PASS"
    }
    if (![string]::IsNullOrWhiteSpace($ExpectedProperty) -and
        $text -notmatch "(?m)^$([regex]::Escape($ExpectedProperty))=PASS\s*`$") {
        throw "Ponder-MMCE harness did not report $ExpectedProperty=PASS"
    }
    return $text
}

function Get-PropertyValue {
    param([string]$Text, [string]$Name)
    $match = [regex]::Match($Text, "(?m)^$([regex]::Escape($Name))=(.+)\s*$")
    if (!$match.Success) { throw "Harness report has no $Name property" }
    return $match.Groups[1].Value.Trim().
        Replace('\:', ':').
        Replace('\=', '=').
        Replace('\\', '\')
}

function Write-FingerprintFixture {
    param([string]$FrozenStructure)
    $machinePath = Join-Path $runtimeRoot `
        "config\modularmachinery\machinery\ponder_mmce_static_demo.json"
    $machine = Get-Content -LiteralPath $machinePath -Raw | ConvertFrom-Json
    $machine.parts[0].elements[0] = "minecraft:gold_block@0"
    [IO.File]::WriteAllText($machinePath, ($machine | ConvertTo-Json -Depth 20),
        [Text.UTF8Encoding]::new($false))

    $marker = Join-Path $runtimeRoot "ponder-mmce-fingerprint-phase.properties"
    [IO.File]::WriteAllText($marker, "mode=fingerprint-isolation`n",
        [Text.UTF8Encoding]::new($false))

    $script = @"
import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.mmce.MMCEStructures;

val stale = SceneRegistry.create(
    "ponder_mmce:machine/modularmachinery/ponder_mmce_static_demo",
    "ponder_mmce:stale_static",
    "Stale MMCE Structure",
    "$FrozenStructure"
);
stale.idle(1);
stale.markAsFinished();
stale.register();

val extended = MMCEStructures.dynamic(
    "modularmachinery:ponder_mmce_dynamic_demo",
    "line",
    3,
    "north",
    "north"
);
val dynamicScene = SceneRegistry.create(
    extended.component,
    "ponder_mmce:dynamic_demo",
    "MMCE Dynamic Pattern",
    extended.structure
);
dynamicScene.configureBasePlate(0, 0, extended.basePlateSize);
dynamicScene.showBasePlate();
dynamicScene.world.showSection(Selection.structureGroup("mmce:all"), "down");
dynamicScene.idle(20);
dynamicScene.markAsFinished();
dynamicScene.register();
"@
    $scriptPath = Join-Path $runtimeRoot "scripts\ponder\scenes\ponder_mmce_smoke.zs"
    [IO.File]::WriteAllText($scriptPath, $script, [Text.UTF8Encoding]::new($false))
}

$phaseResults = [Collections.Generic.List[object]]::new()
$failure = ""
$hashes = [ordered]@{}
try {
    $JavaExecutable = Resolve-Java8 -Preferred $JavaExecutable
    if ([string]::IsNullOrWhiteSpace($ForgeDirectory)) {
        $ForgeDirectory = Join-Path $buildRoot "forge-2847-server-source"
    }
    $ForgeDirectory = (Resolve-Path -LiteralPath $ForgeDirectory).Path
    foreach ($required in @(
        (Join-Path $ForgeDirectory $forgeName),
        (Join-Path $ForgeDirectory $minecraftServerName),
        (Join-Path $ForgeDirectory "libraries")
    )) {
        if (!(Test-Path -LiteralPath $required)) { throw "Forge source is missing $required" }
    }

    $PonderJar = Resolve-FirstFile -Preferred $PonderJar -SearchRoot (Join-Path $buildRoot "libs") `
        -Filter "Ponder-1.12.2-1.2.0.jar" -Label "Ponder jar"
    $PonderMMCEJar = Resolve-FirstFile -Preferred $PonderMMCEJar `
        -SearchRoot (Join-Path $addonBuild "libs") -Filter "Ponder-MMCE-1.12.2-0.1.0-alpha.jar" `
        -Label "Ponder-MMCE jar"
    $SmokePack = Resolve-FirstFile -Preferred $SmokePack `
        -SearchRoot (Join-Path $addonBuild "distributions") -Filter "Ponder-MMCE-Smoke-Pack-0.1.0-alpha.zip" `
        -Label "Ponder-MMCE Smoke Pack"
    $ServerHarnessJar = Resolve-FirstFile -Preferred $ServerHarnessJar `
        -SearchRoot (Join-Path $addonBuild "verification\server-harness") `
        -Filter "Ponder-MMCE-Server-Harness-1.12.2-0.1.0-alpha.jar" -Label "Ponder-MMCE server harness"

    $gradleCache = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1"
    $MixinBooterJar = Resolve-FirstFile -Preferred $MixinBooterJar `
        -SearchRoot (Join-Path $gradleCache "zone.rong\mixinbooter\11.2") `
        -Filter "mixinbooter-11.2.jar" -Label "MixinBooter 11.2"
    $CraftTweakerJar = Resolve-FirstFile -Preferred $CraftTweakerJar `
        -SearchRoot (Join-Path $gradleCache "maven.modrinth\crafttweaker\4.1.20.698") `
        -Filter "crafttweaker-4.1.20.698.jar" -Label "CraftTweaker 4.1.20.698"
    $MMCEJar = Resolve-FirstFile -Preferred $MMCEJar `
        -SearchRoot (Join-Path $gradleCache `
            "curse.maven\modularmachinery-community-edition-817377\7372951") `
        -Filter "*.jar" -Label "MMCE 2.3.2"

    $null = New-Item -ItemType Directory -Path $testRoot -Force
    $null = New-Item -ItemType Directory -Path $reportRoot -Force
    Copy-ForgeRuntime -Source $ForgeDirectory -Destination $runtimeRoot
    Expand-Archive -LiteralPath $SmokePack -DestinationPath $runtimeRoot
    foreach ($mod in @($PonderJar, $PonderMMCEJar, $MixinBooterJar, $CraftTweakerJar,
            $MMCEJar, $ServerHarnessJar)) {
        Copy-Item -LiteralPath $mod -Destination (Join-Path $runtimeRoot "mods")
        $hashes[[IO.Path]::GetFileName($mod)] = (Get-FileHash -LiteralPath $mod -Algorithm SHA256).Hash
    }
    $hashes[[IO.Path]::GetFileName($SmokePack)] =
        (Get-FileHash -LiteralPath $SmokePack -Algorithm SHA256).Hash

    [IO.File]::WriteAllText((Join-Path $runtimeRoot "eula.txt"), "eula=true`n",
        [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllLines((Join-Path $runtimeRoot "server.properties"), @(
        "allow-nether=false",
        "enable-query=false",
        "enable-rcon=false",
        "generate-structures=false",
        "level-name=world",
        "max-players=1",
        "max-tick-time=-1",
        "online-mode=false",
        "server-ip=127.0.0.1",
        "server-port=$ServerPort",
        "snooper-enabled=false",
        "spawn-animals=false",
        "spawn-monsters=false",
        "spawn-npcs=false",
        "view-distance=4"
    ), [Text.UTF8Encoding]::new($false))

    $initial = Invoke-Server -Phase "initial"
    $phaseResults.Add($initial)
    $initialReport = Require-HarnessPass
    $frozenStructure = Get-PropertyValue -Text $initialReport -Name "static.structure"

    Write-FingerprintFixture -FrozenStructure $frozenStructure
    $fingerprint = Invoke-Server -Phase "fingerprint-isolation"
    $phaseResults.Add($fingerprint)
    $null = Require-HarnessPass -ExpectedProperty "fingerprint.isolation"

    $allText = ($fingerprint.Lines -join "`n")
    $latestLog = Join-Path $runtimeRoot "logs\latest.log"
    if (Test-Path -LiteralPath $latestLog -PathType Leaf) {
        $allText += "`n" + [IO.File]::ReadAllText($latestLog)
    }
    if ($allText -notmatch 'fingerprint mismatch') {
        throw "Fingerprint phase did not report the expected mismatch diagnostic"
    }
    if ($allText -match 'Mixin apply failed|InvalidMixinException|NoClassDefFoundError|\bFATAL\b') {
        throw "Ponder-MMCE Forge fixture found a fatal runtime diagnostic"
    }

    Remove-Item -LiteralPath (Join-Path $runtimeRoot "scripts\ponder\scenes\ponder_mmce_smoke.zs")
    $abi = Invoke-Server -Phase "abi-incompatible" `
        -JvmArguments "-Dponder.mmce.verification.forceIncompatibleAbi=true"
    $phaseResults.Add($abi)
    $null = Require-HarnessPass -ExpectedProperty "abi.incompatible"
    $abiText = ($abi.Lines -join "`n")
    if ($abiText -notmatch 'compatibility disabled') {
        throw "ABI phase did not report that Ponder-MMCE compatibility was disabled"
    }
    if ($abiText -match 'Mixin apply failed|InvalidMixinException|NoClassDefFoundError|\bFATAL\b') {
        throw "Ponder-MMCE ABI fixture found a fatal runtime diagnostic"
    }
} catch {
    $failure = $_.Exception.Message
} finally {
    $status = if ([string]::IsNullOrWhiteSpace($failure)) { "PASS" } else { "FAIL" }
    $lines = [Collections.Generic.List[string]]::new()
    $lines.Add("# Ponder-MMCE real Forge verification")
    $lines.Add("")
    $lines.Add("- Status: $status")
    $lines.Add("- Generated: $([DateTime]::UtcNow.ToString('u')) UTC")
    $lines.Add("- Java: $JavaExecutable")
    $lines.Add("- Forge: $forgeVersion")
    $lines.Add("- Phases: $($phaseResults.Count)")
    $lines.Add("")
    $lines.Add("## Artifact SHA-256")
    $lines.Add("")
    foreach ($entry in $hashes.GetEnumerator()) {
        $lines.Add("- ``$($entry.Key)``: ``$($entry.Value)``")
    }
    if (![string]::IsNullOrWhiteSpace($failure)) {
        $lines.Add("")
        $lines.Add("## Blocking finding")
        $lines.Add("")
        $lines.Add("- $failure")
    }
    $null = New-Item -ItemType Directory -Path $reportRoot -Force
    [IO.File]::WriteAllLines($reportPath, $lines, [Text.UTF8Encoding]::new($false))
}

if (![string]::IsNullOrWhiteSpace($failure)) {
    throw "Ponder-MMCE Forge verification failed. See $reportPath. $failure"
}

Write-Host "Ponder-MMCE real Forge verification passed."
Write-Host "Evidence: $testRoot"
Write-Host "Report: $reportPath"
