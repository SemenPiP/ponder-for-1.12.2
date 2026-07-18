param(
    [Parameter(Mandatory = $true)]
    [string]$HarnessReport,
    [Parameter(Mandatory = $true)]
    [string]$EvidenceDirectory,
    [Parameter(Mandatory = $true)]
    [string]$PonderJar,
    [Parameter(Mandatory = $true)]
    [string]$ClientHarnessJar,
    [Parameter(Mandatory = $true)]
    [long]$ActionsRunId,
    [Parameter(Mandatory = $true)]
    [string]$SourceCommit,
    [string]$Output = "",
    [switch]$ConfirmedEightBuiltinScenes,
    [switch]$ConfirmedJsonReloadCommand,
    [switch]$ConfirmedWEntry,
    [switch]$ConfirmedRealMouse,
    [switch]$ConfirmedGuiScales1To4,
    [switch]$ConfirmedFullscreenRoundTrip,
    [switch]$ConfirmedResourceReload,
    [switch]$ConfirmedOrdinaryWorldAfterClose,
    [string]$Notes = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2.0

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$HarnessReport = (Resolve-Path -LiteralPath $HarnessReport).Path
$EvidenceDirectory = (Resolve-Path -LiteralPath $EvidenceDirectory).Path
$PonderJar = (Resolve-Path -LiteralPath $PonderJar).Path
$ClientHarnessJar = (Resolve-Path -LiteralPath $ClientHarnessJar).Path
if ([string]::IsNullOrWhiteSpace($Output)) {
    $Output = Join-Path $projectRoot "build\reports\ponder-1.3.0-client-acceptance.json"
} else {
    $Output = [IO.Path]::GetFullPath($Output)
}

if ($ActionsRunId -le 0) {
    throw "ActionsRunId must be a positive GitHub Actions run id"
}
$SourceCommit = $SourceCommit.Trim().ToLowerInvariant()
if ($SourceCommit -notmatch '^[0-9a-f]{40}$') {
    throw "SourceCommit must be a full 40-character Git commit"
}
if ([IO.Path]::GetFileName($PonderJar) -ne "Ponder-1.12.2-1.3.0.jar") {
    throw "PonderJar must be Ponder-1.12.2-1.3.0.jar"
}
if ([IO.Path]::GetFileName($ClientHarnessJar) -ne
    "Ponder-Client-Harness-1.12.2-1.3.0.jar") {
    throw "ClientHarnessJar must be Ponder-Client-Harness-1.12.2-1.3.0.jar"
}

$harness = Get-Content -LiteralPath $HarnessReport -Raw | ConvertFrom-Json
if ($harness.status -ne "PASS") {
    throw "Automated Ponder client harness did not report PASS"
}
foreach ($flag in @(
    "doesNotReplaceManualVisualAcceptance",
    "doesNotReplaceRealMouseAcceptance",
    "includesProgrammaticFullscreenRoundTrip",
    "includesFullAutoplayForAllEightScenes",
    "includesPerSceneTargetContentAssertions",
    "includesJsonRuntimeReloadChecks",
    "includesJsonLastKnownGoodChecks",
    "includesJsonZenScriptConflictChecks",
    "includesJsonServerOverrideChecks",
    "includesJsonSnbtPlaybackCheck"
)) {
    if ($harness.$flag -ne $true) {
        throw "Automated harness report is missing required capability flag: $flag"
    }
}
if ([string]$harness.minecraft -ne "1.12.2") {
    throw "Automated harness used unexpected Minecraft version: $($harness.minecraft)"
}
if ([string]$harness.forge -ne "14.23.5.2847") {
    throw "Automated harness used unexpected Forge version: $($harness.forge)"
}
if ([string]$harness.ponder -ne "1.3.0-mc1.12.2") {
    throw "Automated harness used unexpected Ponder version: $($harness.ponder)"
}
if ([string]$harness.clientHarness -ne "1.3.0") {
    throw "Automated harness used unexpected client harness version: $($harness.clientHarness)"
}
if ([string]$harness.mixinBooter -ne "11.2") {
    throw "Automated harness used unexpected MixinBooter version: $($harness.mixinBooter)"
}
if ([string]$harness.craftTweaker -notmatch '^4\.1\.20(?:\.698)?$') {
    throw "Automated harness used unexpected CraftTweaker version: $($harness.craftTweaker)"
}

$requiredChecks = @(
    "json.install",
    "json.open",
    "json.snbt_playback",
    "json.last_known_good",
    "json.update",
    "json.zenscript_conflict",
    "json.server_override",
    "json.reload_clears_server",
    "json.delete_pack",
    "resources.rebuilt",
    "gui_scale.1",
    "gui_scale.2",
    "gui_scale.3",
    "gui_scale.4",
    "fullscreen.restored_state",
    "component.crafting_table.full_autoplay_result",
    "component.chest.full_autoplay_result",
    "component.furnace.full_autoplay_result",
    "component.piston.full_autoplay_result",
    "component.redstone_lamp.full_autoplay_result",
    "component.glass.full_autoplay_result",
    "component.water_bucket.full_autoplay_result",
    "component.rail.full_autoplay_result",
    "ponder.close_stays_closed"
)
foreach ($name in $requiredChecks) {
    $matches = @($harness.checks |
        Where-Object { $_.name -eq $name -and $_.status -eq "PASS" })
    if ($matches.Count -ne 1) {
        throw "Automated harness is missing PASS check: $name"
    }
}

$automatedScreenshots = @($harness.screenshots)
if ($automatedScreenshots.Count -lt 15) {
    throw "Automated harness contains fewer than 15 screenshots"
}
foreach ($screenshot in $automatedScreenshots) {
    if ([int]$screenshot.sampledColors -lt 8 -or
        [string]$screenshot.sha256 -notmatch '^[0-9A-Fa-f]{64}$') {
        throw "Automated screenshot evidence is incomplete: $($screenshot.file)"
    }
}

$manualScreenshots = @(Get-ChildItem -LiteralPath $EvidenceDirectory -Filter "*.png" -File -Recurse)
$logs = @(Get-ChildItem -LiteralPath $EvidenceDirectory -Filter "latest.log" -File -Recurse)
if ($manualScreenshots.Count -lt 8) {
    throw "Manual evidence requires at least eight PNG screenshots, found $($manualScreenshots.Count)"
}
if ($logs.Count -lt 1) {
    throw "Manual evidence requires a retained latest.log"
}

$confirmations = [ordered]@{
    eightBuiltinScenes = [bool]$ConfirmedEightBuiltinScenes
    jsonReloadCommand = [bool]$ConfirmedJsonReloadCommand
    wEntry = [bool]$ConfirmedWEntry
    realMouse = [bool]$ConfirmedRealMouse
    guiScales1To4 = [bool]$ConfirmedGuiScales1To4
    fullscreenRoundTrip = [bool]$ConfirmedFullscreenRoundTrip
    resourceReload = [bool]$ConfirmedResourceReload
    ordinaryWorldAfterClose = [bool]$ConfirmedOrdinaryWorldAfterClose
}
$missing = @($confirmations.GetEnumerator() |
    Where-Object { !$_.Value } | ForEach-Object Key)
if ($missing.Count -gt 0) {
    throw "Manual acceptance is incomplete: $($missing -join ', ')"
}

$evidenceFiles = [Collections.Generic.List[object]]::new()
foreach ($file in @($manualScreenshots + $logs | Sort-Object FullName)) {
    $relative = $file.FullName.Substring($EvidenceDirectory.TrimEnd('\').Length)
    $evidenceFiles.Add([ordered]@{
        file = $relative.TrimStart('\').Replace('\', '/')
        sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
        bytes = $file.Length
    })
}

$result = [ordered]@{
    schemaVersion = 1
    scope = "ponder_legacy_1.3.0_client_acceptance"
    status = "PASS"
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    source = [ordered]@{
        repository = "SemenPiP/ponder-for-1.12.2"
        workflow = "Build and Verify"
        runId = $ActionsRunId
        commit = $SourceCommit
    }
    versions = [ordered]@{
        minecraft = [string]$harness.minecraft
        forge = [string]$harness.forge
        ponder_legacy = [string]$harness.ponder
        client_harness = [string]$harness.clientHarness
        crafttweaker = [string]$harness.craftTweaker
        mixinbooter = [string]$harness.mixinBooter
    }
    sha256 = [ordered]@{
        ponder_legacy = (Get-FileHash -LiteralPath $PonderJar -Algorithm SHA256).Hash
        client_harness = (Get-FileHash -LiteralPath $ClientHarnessJar -Algorithm SHA256).Hash
        automated_report = (Get-FileHash -LiteralPath $HarnessReport -Algorithm SHA256).Hash
    }
    automatedHarness = [ordered]@{
        status = $harness.status
        reportFile = [IO.Path]::GetFileName($HarnessReport)
        requiredChecks = $requiredChecks
        screenshots = $automatedScreenshots
    }
    manualAcceptance = [ordered]@{
        status = "PASS"
        eightBuiltinScenes = $confirmations.eightBuiltinScenes
        jsonReloadCommand = $confirmations.jsonReloadCommand
        wEntry = $confirmations.wEntry
        realMouse = $confirmations.realMouse
        guiScales1To4 = $confirmations.guiScales1To4
        fullscreenRoundTrip = $confirmations.fullscreenRoundTrip
        resourceReload = $confirmations.resourceReload
        ordinaryWorldAfterClose = $confirmations.ordinaryWorldAfterClose
        notes = $Notes
    }
    evidence = $evidenceFiles
}

$parent = Split-Path -Parent $Output
if (!(Test-Path -LiteralPath $parent -PathType Container)) {
    $null = New-Item -ItemType Directory -Path $parent -Force
}
[IO.File]::WriteAllText($Output, ($result | ConvertTo-Json -Depth 16),
    [Text.UTF8Encoding]::new($false))

Write-Host "Ponder 1.3.0 client acceptance completed."
Write-Host "Report: $Output"
Write-Host "Actions run: $ActionsRunId"
Write-Host "Source commit: $SourceCommit"
Write-Host "Ponder SHA256: $($result.sha256.ponder_legacy)"
Write-Host "Client harness SHA256: $($result.sha256.client_harness)"
