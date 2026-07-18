[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$tool = Join-Path $PSScriptRoot "ponder-json.ps1"
$fixtures = Join-Path $PSScriptRoot "fixtures\json"
$positive = Join-Path $fixtures "valid.ponder.json"
$temporary = Join-Path ([IO.Path]::GetTempPath()) (
    "ponder-json-tool-" + [guid]::NewGuid().ToString("N"))
[IO.Directory]::CreateDirectory($temporary) | Out-Null

try {
    & $tool validate $positive | Out-Null
    $originalHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $positive).Hash
    $migrated = Join-Path $temporary "canonical.ponder.json"
    & $tool migrate $positive -OutputPath $migrated | Out-Null
    & $tool validate $migrated | Out-Null
    if ($originalHash -ne (Get-FileHash -Algorithm SHA256 -LiteralPath $positive).Hash) {
        throw "migrate modified its input file"
    }

    $negative = @(
        "missing-required.ponder.json",
        "invalid-selection.ponder.json",
        "invalid-snbt.ponder.json",
        "unknown-operation.ponder.json"
    )
    foreach ($name in $negative) {
        $failed = $false
        try {
            & $tool validate (Join-Path $fixtures $name) | Out-Null
        } catch {
            $failed = $true
        }
        if (-not $failed) {
            throw "negative fixture unexpectedly passed: $name"
        }
    }

    $overwriteFailed = $false
    try {
        & $tool migrate $positive -OutputPath $positive | Out-Null
    } catch {
        $overwriteFailed = $true
    }
    if (-not $overwriteFailed) {
        throw "migrate unexpectedly overwrote its input"
    }

    Write-Output "Ponder JSON author tool fixtures passed."
} finally {
    if (Test-Path -LiteralPath $temporary) {
        Remove-Item -LiteralPath $temporary -Recurse -Force
    }
}
