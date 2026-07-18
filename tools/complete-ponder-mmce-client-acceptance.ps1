param(
    [Parameter(Mandatory = $true)]
    [string]$HarnessReport,
    [Parameter(Mandatory = $true)]
    [string]$EvidenceDirectory,
    [string]$Output = "",
    [switch]$ConfirmedBlueprintWEntry,
    [switch]$ConfirmedRealMouse,
    [switch]$ConfirmedGuiScales1To4,
    [switch]$ConfirmedFullscreenRoundTrip,
    [switch]$ConfirmedResourceReload,
    [string]$Notes = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2.0

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$HarnessReport = (Resolve-Path -LiteralPath $HarnessReport).Path
$EvidenceDirectory = (Resolve-Path -LiteralPath $EvidenceDirectory).Path
if ([string]::IsNullOrWhiteSpace($Output)) {
    $Output = Join-Path $projectRoot "build\reports\ponder-mmce-client-acceptance.json"
} else {
    $Output = [IO.Path]::GetFullPath($Output)
}

$harness = Get-Content -LiteralPath $HarnessReport -Raw | ConvertFrom-Json
if ($harness.status -ne "PASS") {
    throw "Automated Ponder-MMCE client harness did not report PASS"
}
$requiredChecks = @(
    "blueprints",
    "fingerprint.isolation",
    "static.open",
    "static.screenshot",
    "dynamic.open",
    "dynamic.screenshot",
    "complete"
)
foreach ($name in $requiredChecks) {
    $check = @($harness.checks | Where-Object { $_.name -eq $name -and $_.status -eq "PASS" })
    if ($check.Count -ne 1) {
        throw "Automated harness is missing PASS check: $name"
    }
}
$automatedScreenshots = @($harness.screenshots)
if ($automatedScreenshots.Count -lt 2) {
    throw "Automated harness report contains fewer than two screenshots"
}
foreach ($screenshot in $automatedScreenshots) {
    if ([int]$screenshot.sampledColors -lt 8 -or
        [string]$screenshot.sha256 -notmatch '^[0-9A-Fa-f]{64}$') {
        throw "Automated screenshot evidence is incomplete: $($screenshot.file)"
    }
}
foreach ($modId in @("ponder_legacy", "ponder_mmce")) {
    $value = [string]$harness.sha256.$modId
    if ($value -notmatch '^[0-9A-Fa-f]{64}$') {
        throw "Harness report has no valid SHA-256 for $modId"
    }
}

$screenshots = @(Get-ChildItem -LiteralPath $EvidenceDirectory -Filter "*.png" -File -Recurse)
$logs = @(Get-ChildItem -LiteralPath $EvidenceDirectory -Filter "latest.log" -File -Recurse)
if ($screenshots.Count -lt 4) {
    throw "Manual evidence requires at least four PNG screenshots, found $($screenshots.Count)"
}
if ($logs.Count -lt 1) {
    throw "Manual evidence requires a retained latest.log"
}

$confirmations = [ordered]@{
    blueprintWEntry = [bool]$ConfirmedBlueprintWEntry
    realMouse = [bool]$ConfirmedRealMouse
    guiScales1To4 = [bool]$ConfirmedGuiScales1To4
    fullscreenRoundTrip = [bool]$ConfirmedFullscreenRoundTrip
    resourceReload = [bool]$ConfirmedResourceReload
}
$missing = @($confirmations.GetEnumerator() | Where-Object { !$_.Value } | ForEach-Object Key)
if ($missing.Count -gt 0) {
    throw "Manual acceptance is incomplete: $($missing -join ', ')"
}

$evidenceFiles = [Collections.Generic.List[object]]::new()
foreach ($file in @($screenshots + $logs | Sort-Object FullName)) {
    $relative = $file.FullName.Substring($EvidenceDirectory.TrimEnd('\').Length)
    $evidenceFiles.Add([ordered]@{
        file = $relative.TrimStart('\').Replace('\', '/')
        sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
        bytes = $file.Length
    })
}

$result = [ordered]@{
    schemaVersion = 1
    status = "PASS"
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    automatedHarness = [ordered]@{
        status = $harness.status
        reportSha256 = (Get-FileHash -LiteralPath $HarnessReport -Algorithm SHA256).Hash
        reportFile = [IO.Path]::GetFileName($HarnessReport)
        checks = $requiredChecks
        screenshots = $automatedScreenshots
    }
    versions = $harness.versions
    sha256 = $harness.sha256
    manualAcceptance = [ordered]@{
        status = "PASS"
        blueprintWEntry = $confirmations.blueprintWEntry
        realMouse = $confirmations.realMouse
        guiScales1To4 = $confirmations.guiScales1To4
        fullscreenRoundTrip = $confirmations.fullscreenRoundTrip
        resourceReload = $confirmations.resourceReload
        notes = $Notes
    }
    evidence = $evidenceFiles
}

$parent = Split-Path -Parent $Output
if (!(Test-Path -LiteralPath $parent -PathType Container)) {
    $null = New-Item -ItemType Directory -Path $parent -Force
}
[IO.File]::WriteAllText($Output, ($result | ConvertTo-Json -Depth 12),
    [Text.UTF8Encoding]::new($false))

Write-Host "Ponder-MMCE client acceptance completed."
Write-Host "Report: $Output"
Write-Host "Ponder SHA256: $($harness.sha256.ponder_legacy)"
Write-Host "Ponder-MMCE SHA256: $($harness.sha256.ponder_mmce)"
