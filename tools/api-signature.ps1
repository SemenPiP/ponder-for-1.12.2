param(
    [Parameter(Mandatory = $true)]
    [string]$JarPath,
    [Parameter(Mandatory = $true)]
    [string]$OutputPath,
    [string]$CompatibilityBaseline = "",
    [string]$ExpectedSignature = "",
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"
$jar = (Resolve-Path -LiteralPath $JarPath).Path
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $JavaHome = $env:JAVA_HOME
}
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    throw "JAVA_HOME is required to locate JDK 8 javap"
}
$JavaHome = $JavaHome.Trim().Trim('"').TrimEnd('\', '/')
$javap = Join-Path $JavaHome "bin\javap.exe"
if (!(Test-Path -LiteralPath $javap -PathType Leaf)) {
    $javap = Join-Path $JavaHome "bin\javap"
}
if (!(Test-Path -LiteralPath $javap -PathType Leaf) -and
    (Split-Path -Leaf $JavaHome) -ieq "jre") {
    $JavaHome = Split-Path -Parent $JavaHome
    $javap = Join-Path $JavaHome "bin\javap.exe"
    if (!(Test-Path -LiteralPath $javap -PathType Leaf)) {
        $javap = Join-Path $JavaHome "bin\javap"
    }
}
if (!(Test-Path -LiteralPath $javap -PathType Leaf)) {
    throw "Could not find javap under $JavaHome"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $classes = @(
        $archive.Entries |
            Where-Object {
                !$_.FullName.EndsWith("/") -and
                $_.FullName.EndsWith(".class") -and
                $_.FullName -ne "module-info.class"
            } |
            ForEach-Object {
                $_.FullName.Substring(0, $_.FullName.Length - 6).Replace("/", ".")
            } |
            Sort-Object -Unique
    )
} finally {
    $archive.Dispose()
}

$signature = [Collections.Generic.List[string]]::new()
$signature.Add("# ponder-api-signature-format=1")
foreach ($className in $classes) {
    $output = @(& $javap -protected -s -constants -classpath $jar $className 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "javap failed for $className`n$($output -join [Environment]::NewLine)"
    }
    $header = $null
    $pending = $null
    foreach ($raw in $output) {
        $line = ([string]$raw).Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or
            $line.StartsWith("Compiled from ") -or $line -eq "}") {
            continue
        }
        if ($null -eq $header -and $line.EndsWith("{") -and
            ($line.StartsWith("public ") -or $line.StartsWith("protected "))) {
            $header = ($line.Substring(0, $line.Length - 1)).Trim()
            $signature.Add("C|$className|$header")
            continue
        }
        if ($null -eq $header) {
            continue
        }
        if ($line.StartsWith("public ") -or $line.StartsWith("protected ")) {
            $pending = $line
            continue
        }
        if ($line.StartsWith("descriptor:") -and $null -ne $pending) {
            $descriptor = $line.Substring("descriptor:".Length).Trim()
            $signature.Add("M|$className|$pending|$descriptor")
            $pending = $null
        }
    }
}

$body = @($signature | Sort-Object -Unique)
$outputFile = if ([IO.Path]::IsPathRooted($OutputPath)) {
    [IO.Path]::GetFullPath($OutputPath)
} else {
    [IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputPath))
}
$outputDirectory = [IO.Path]::GetDirectoryName($outputFile)
if (!(Test-Path -LiteralPath $outputDirectory -PathType Container)) {
    $null = New-Item -ItemType Directory -Path $outputDirectory -Force
}
[IO.File]::WriteAllLines($outputFile, $body, [Text.UTF8Encoding]::new($false))

function Read-Signature([string]$Path) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing API signature file: $Path"
    }
    return @([IO.File]::ReadAllLines((Resolve-Path -LiteralPath $Path).Path) |
        Where-Object { ![string]::IsNullOrWhiteSpace($_) } |
        Sort-Object -Unique)
}

function Get-CompatibilityKey([string]$Line) {
    if (!$Line.StartsWith("M|")) {
        return $Line
    }
    $parts = $Line.Split("|", 4)
    if ($parts.Count -ne 4) {
        throw "Malformed member signature: $Line"
    }
    $declaration = [regex]::Replace($parts[2], '\s+=\s+.*;$', ';')
    return "M|$($parts[1])|$declaration|$($parts[3])"
}

if (![string]::IsNullOrWhiteSpace($ExpectedSignature)) {
    $expected = Read-Signature $ExpectedSignature
    if (($expected -join "`n") -ne ($body -join "`n")) {
        $missing = @($expected | Where-Object { $_ -notin $body })
        $added = @($body | Where-Object { $_ -notin $expected })
        throw "Current API signature differs from the tracked snapshot." +
            "`nMissing:`n$($missing -join "`n")`nAdded:`n$($added -join "`n")"
    }
}

if (![string]::IsNullOrWhiteSpace($CompatibilityBaseline)) {
    $baseline = Read-Signature $CompatibilityBaseline
    $baselineCompatibility = @(
        $baseline |
            Where-Object { !$_.StartsWith("#") } |
            ForEach-Object { Get-CompatibilityKey $_ } |
            Sort-Object -Unique
    )
    $currentCompatibility = @(
        $body |
            Where-Object { !$_.StartsWith("#") } |
            ForEach-Object { Get-CompatibilityKey $_ } |
            Sort-Object -Unique
    )
    $breaking = [Collections.Generic.List[string]]::new()
    foreach ($line in $baselineCompatibility) {
        if ($line -notin $currentCompatibility) {
            $breaking.Add("Removed or changed: $line")
        }
    }
    $baselineTypes = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($line in $baseline) {
        if ($line.StartsWith("C|")) {
            $parts = $line.Split("|", 3)
            $null = $baselineTypes.Add($parts[1])
        }
    }
    foreach ($line in $body) {
        if (!$line.StartsWith("M|") -or
            (Get-CompatibilityKey $line) -in $baselineCompatibility) {
            continue
        }
        $parts = $line.Split("|", 4)
        if ($baselineTypes.Contains($parts[1]) -and
            $parts[2] -match '(^|\s)(public|protected)\s+abstract\s+') {
            $breaking.Add("Added abstract member to existing API type: $line")
        }
    }
    if ($breaking.Count -gt 0) {
        throw "Ponder API compatibility check failed:`n$($breaking -join "`n")"
    }
}

Write-Host "Wrote $($body.Count - 1) public/protected API signatures to $outputFile"
