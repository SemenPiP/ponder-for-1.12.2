param(
    [string]$ProjectRoot = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2.0

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
} else {
    $ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
}

$fixtureRoot = Join-Path $ProjectRoot (
    "build\tmp\ponder-client-acceptance-" + [Guid]::NewGuid().ToString("N"))
$evidence = Join-Path $fixtureRoot "evidence"
$null = New-Item -ItemType Directory -Path $evidence -Force

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
$checks = @($requiredChecks | ForEach-Object {
    [ordered]@{ name = $_; status = "PASS"; detail = "fixture" }
})
$screenshots = @()
for ($index = 0; $index -lt 15; $index++) {
    $screenshots += [ordered]@{
        file = "screenshots/$index.png"
        sampledColors = 32
        sha256 = ("A" * 64)
    }
}
$harness = [ordered]@{
    schemaVersion = 2
    status = "PASS"
    doesNotReplaceManualVisualAcceptance = $true
    doesNotReplaceRealMouseAcceptance = $true
    includesProgrammaticFullscreenRoundTrip = $true
    includesFullAutoplayForAllEightScenes = $true
    includesPerSceneTargetContentAssertions = $true
    includesJsonRuntimeReloadChecks = $true
    includesJsonLastKnownGoodChecks = $true
    includesJsonZenScriptConflictChecks = $true
    includesJsonServerOverrideChecks = $true
    includesJsonSnbtPlaybackCheck = $true
    minecraft = "1.12.2"
    forge = "14.23.5.2847"
    mixinBooter = "11.2"
    craftTweaker = "4.1.20.698"
    ponder = "1.3.0-mc1.12.2"
    clientHarness = "1.3.0"
    checks = $checks
    screenshots = $screenshots
}

$harnessPath = Join-Path $fixtureRoot "report.json"
[IO.File]::WriteAllText($harnessPath, ($harness | ConvertTo-Json -Depth 10),
    [Text.UTF8Encoding]::new($false))
for ($index = 1; $index -le 8; $index++) {
    [IO.File]::WriteAllBytes(
        (Join-Path $evidence ("manual-$index.png")),
        [byte[]](137, 80, 78, 71, $index))
}
[IO.File]::WriteAllText((Join-Path $evidence "latest.log"), "fixture log",
    [Text.UTF8Encoding]::new($false))

$ponderJar = Join-Path $fixtureRoot "Ponder-1.12.2-1.3.0.jar"
$clientHarnessJar = Join-Path $fixtureRoot "Ponder-Client-Harness-1.12.2-1.3.0.jar"
[IO.File]::WriteAllBytes($ponderJar, [byte[]](1, 3, 0))
[IO.File]::WriteAllBytes($clientHarnessJar, [byte[]](1, 3, 1))
$output = Join-Path $fixtureRoot "completed.json"

& (Join-Path $PSScriptRoot "complete-ponder-client-acceptance.ps1") `
    -HarnessReport $harnessPath `
    -EvidenceDirectory $evidence `
    -PonderJar $ponderJar `
    -ClientHarnessJar $clientHarnessJar `
    -ActionsRunId 123456 `
    -SourceCommit ("a" * 40) `
    -Output $output `
    -ConfirmedEightBuiltinScenes `
    -ConfirmedJsonReloadCommand `
    -ConfirmedWEntry `
    -ConfirmedRealMouse `
    -ConfirmedGuiScales1To4 `
    -ConfirmedFullscreenRoundTrip `
    -ConfirmedResourceReload `
    -ConfirmedOrdinaryWorldAfterClose `
    -Notes "fixture"

$completed = Get-Content -LiteralPath $output -Raw | ConvertFrom-Json
if ($completed.status -ne "PASS" -or
    $completed.source.runId -ne 123456 -or
    $completed.source.commit -ne ("a" * 40) -or
    $completed.manualAcceptance.status -ne "PASS") {
    throw "Completed client acceptance fixture is invalid"
}
foreach ($hash in @(
    [string]$completed.sha256.ponder_legacy,
    [string]$completed.sha256.client_harness,
    [string]$completed.sha256.automated_report
)) {
    if ($hash -notmatch '^[0-9A-F]{64}$') {
        throw "Completed client acceptance fixture contains an invalid hash"
    }
}

$reportDirectory = Join-Path $ProjectRoot "build\reports"
$null = New-Item -ItemType Directory -Path $reportDirectory -Force
$resultPath = Join-Path $reportDirectory "ponder-client-acceptance-tool-fixture.txt"
[IO.File]::WriteAllText($resultPath,
    "PASS`nFixture=$fixtureRoot`nOutput=$output`n",
    [Text.UTF8Encoding]::new($false))
Write-Host "Ponder client acceptance tool fixture passed."
