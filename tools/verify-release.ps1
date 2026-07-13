param(
    [string]$ProjectRoot = "",
    [string]$MixinBooterVersion = "11.2",
    [string]$ExpectedMixinBooterHash = ""
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
} else {
    $ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-ZipEntryBytes($Entry) {
    $stream = $Entry.Open()
    try {
        $memory = [IO.MemoryStream]::new()
        try {
            $stream.CopyTo($memory)
            return $memory.ToArray()
        } finally {
            $memory.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

function Read-ZipEntryText($Entry) {
    $reader = [IO.StreamReader]::new($Entry.Open(), [Text.Encoding]::UTF8, $true)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Read-U1([byte[]]$Bytes, [ref]$Offset) {
    $value = [int]$Bytes[$Offset.Value]
    $Offset.Value++
    return $value
}

function Read-U2([byte[]]$Bytes, [ref]$Offset) {
    $index = $Offset.Value
    $value = ([int]$Bytes[$index] -shl 8) -bor [int]$Bytes[$index + 1]
    $Offset.Value += 2
    return $value
}

function Skip-Bytes([byte[]]$Bytes, [ref]$Offset, [int]$Count) {
    if ($Offset.Value + $Count -gt $Bytes.Length) {
        throw "Unexpected end of class file"
    }
    $Offset.Value += $Count
}

function Get-ClassMemberReferences([byte[]]$Bytes) {
    if ($Bytes.Length -lt 10 -or $Bytes[0] -ne 0xCA -or $Bytes[1] -ne 0xFE -or
        $Bytes[2] -ne 0xBA -or $Bytes[3] -ne 0xBE) {
        throw "Invalid class file"
    }

    $offset = 8
    $constantCount = Read-U2 $Bytes ([ref]$offset)
    $constants = New-Object object[] $constantCount
    for ($index = 1; $index -lt $constantCount; $index++) {
        $tag = Read-U1 $Bytes ([ref]$offset)
        switch ($tag) {
            1 {
                $length = Read-U2 $Bytes ([ref]$offset)
                if ($offset + $length -gt $Bytes.Length) { throw "Truncated UTF-8 constant" }
                $value = [Text.Encoding]::UTF8.GetString($Bytes, $offset, $length)
                $offset += $length
                $constants[$index] = [pscustomobject]@{ Tag = $tag; Value = $value }
            }
            { $_ -in 3, 4 } { Skip-Bytes $Bytes ([ref]$offset) 4 }
            { $_ -in 5, 6 } {
                Skip-Bytes $Bytes ([ref]$offset) 8
                $index++
            }
            { $_ -in 7, 8, 16, 19, 20 } {
                $constants[$index] = [pscustomobject]@{
                    Tag = $tag
                    A = Read-U2 $Bytes ([ref]$offset)
                }
            }
            { $_ -in 9, 10, 11, 12, 18 } {
                $constants[$index] = [pscustomobject]@{
                    Tag = $tag
                    A = Read-U2 $Bytes ([ref]$offset)
                    B = Read-U2 $Bytes ([ref]$offset)
                }
            }
            15 { Skip-Bytes $Bytes ([ref]$offset) 3 }
            default { throw "Unsupported constant-pool tag $tag" }
        }
    }

    for ($index = 1; $index -lt $constantCount; $index++) {
        $reference = $constants[$index]
        if ($null -eq $reference -or $reference.Tag -notin 9, 10, 11) { continue }
        $classConstant = $constants[[int]$reference.A]
        $nameAndType = $constants[[int]$reference.B]
        if ($null -eq $classConstant -or $null -eq $nameAndType) { continue }
        $ownerUtf8 = $constants[[int]$classConstant.A]
        $nameUtf8 = $constants[[int]$nameAndType.A]
        $descriptorUtf8 = $constants[[int]$nameAndType.B]
        if ($null -eq $ownerUtf8 -or $null -eq $nameUtf8 -or $null -eq $descriptorUtf8) { continue }
        [pscustomobject]@{
            Owner = [string]$ownerUtf8.Value
            Name = [string]$nameUtf8.Value
            Descriptor = [string]$descriptorUtf8.Value
            Kind = [int]$reference.Tag
        }
    }
}

function Get-McpMemberSet([string]$MappingPath) {
    $members = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($line in [IO.File]::ReadLines($MappingPath)) {
        $srgMemberPath = $null
        $memberPath = $null
        $descriptor = $null
        $kind = 9
        if ($line -match '^FD:\s+(\S+)\s+(\S+)$') {
            $srgMemberPath = $Matches[1]
            $memberPath = $Matches[2]
        } elseif ($line -match '^MD:\s+(\S+)\s+\S+\s+(\S+)\s+(\S+)$') {
            $srgMemberPath = $Matches[1]
            $memberPath = $Matches[2]
            $descriptor = $Matches[3]
            $kind = 10
        }
        if ($null -eq $memberPath -or $null -eq $srgMemberPath) { continue }
        $separator = $memberPath.LastIndexOf('/')
        $srgSeparator = $srgMemberPath.LastIndexOf('/')
        if ($separator -le 0 -or $srgSeparator -le 0) { continue }
        $owner = $memberPath.Substring(0, $separator)
        $name = $memberPath.Substring($separator + 1)
        $srgName = $srgMemberPath.Substring($srgSeparator + 1)
        if ($srgName -eq $name) { continue }
        if ($kind -eq 9) {
            $null = $members.Add("9|$owner|$name")
        } else {
            $null = $members.Add("10|$owner|$name|$descriptor")
            $null = $members.Add("11|$owner|$name|$descriptor")
        }
    }
    return $members
}

$libs = Join-Path $ProjectRoot "build\libs"
$artifacts = [ordered]@{
    Ponder = Join-Path $libs "Ponder-1.12.2-1.1.0.jar"
    Sources = Join-Path $libs "Ponder-1.12.2-1.1.0-sources.jar"
    API = Join-Path $libs "Ponder-1.12.2-1.1.0-api.jar"
    Example = Join-Path $libs "Ponder-Example-Addon-1.12.2-1.1.0.jar"
}
foreach ($artifact in $artifacts.Values) {
    if (!(Test-Path -LiteralPath $artifact -PathType Leaf)) {
        throw "Missing release artifact: $artifact"
    }
}

$errors = [Collections.Generic.List[string]]::new()
$classCounts = [ordered]@{ Ponder = 0; API = 0; Example = 0 }
$languageCount = 0
$packMetadata = $null
$mcmodText = $null
$ponderConstants = $null
$ponderModConstants = $null
$exampleMcmodText = $null
$exampleAddonConstants = $null
$manifestText = $null
$refmapText = $null
$requiredDemoStructures = @(
    "assets/ponder/ponder/demo/basics.nbt",
    "assets/ponder/ponder/demo/storage.nbt",
    "assets/ponder/ponder/demo/smelting.nbt",
    "assets/ponder/ponder/demo/piston.nbt",
    "assets/ponder/ponder/demo/redstone.nbt",
    "assets/ponder/ponder/demo/render_layers.nbt",
    "assets/ponder/ponder/demo/fluids.nbt",
    "assets/ponder/ponder/demo/rail.nbt"
)
$requiredBuiltinScripts = @(
    "assets/ponder/scripts/builtin/basics.zs",
    "assets/ponder/scripts/builtin/fluids.zs",
    "assets/ponder/scripts/builtin/piston.zs",
    "assets/ponder/scripts/builtin/rail.zs",
    "assets/ponder/scripts/builtin/redstone.zs",
    "assets/ponder/scripts/builtin/render_layers.zs",
    "assets/ponder/scripts/builtin/smelting.zs",
    "assets/ponder/scripts/builtin/storage.zs"
)
$ponderEntries = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$forbiddenEntryPrefixes = @(
    "zone/rong/mixinbooter/",
    "crafttweaker/",
    "stanhebben/zenscript/",
    "org/spongepowered/asm/",
    "com/llamalad7/mixinextras/",
    "com/cleanroommc/",
    "com/jozufozu/flywheel/",
    "dev/engine_room/flywheel/",
    "org/joml/",
    "org/lwjgl/glfw/",
    "net/fabricmc/",
    "net/neoforged/"
)
$forbiddenReferences = @(
    "com/jozufozu/flywheel/",
    "dev/engine_room/flywheel/",
    "org/joml/",
    "org/lwjgl/glfw/",
    "net/fabricmc/",
    "net/neoforged/"
)

$gradleHome = if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
    Join-Path $env:USERPROFILE ".gradle"
} else {
    $env:GRADLE_USER_HOME
}
$mappingPath = Join-Path $gradleHome "caches\minecraft\de\oceanlabs\mcp\mcp_stable\39\rfg_srgs\srg-mcp.srg"
$mcpMembers = $null
if (Test-Path -LiteralPath $mappingPath -PathType Leaf) {
    $mcpMembers = Get-McpMemberSet $mappingPath
} else {
    $errors.Add("stable_39 mapping is unavailable: $mappingPath")
}

foreach ($artifactEntry in $artifacts.GetEnumerator()) {
    $label = [string]$artifactEntry.Key
    $path = [string]$artifactEntry.Value
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($entry in $archive.Entries) {
            $name = $entry.FullName
            if ($name.StartsWith("assets/ponder/ponder/debug/", [StringComparison]::Ordinal)) {
                $errors.Add("development debug structure in $label artifact: $name")
            }
            foreach ($prefix in $forbiddenEntryPrefixes) {
                if ($name.StartsWith($prefix, [StringComparison]::Ordinal)) {
                    $errors.Add("forbidden embedded entry in $label artifact: $name")
                }
            }

            if ($label -eq "Ponder") {
                $null = $ponderEntries.Add($name)
                if ($name -eq "META-INF/MANIFEST.MF") { $manifestText = Read-ZipEntryText $entry }
                if ($name -eq "mcmod.info") { $mcmodText = Read-ZipEntryText $entry }
                if ($name -eq "mixins.ponder.refmap.json") { $refmapText = Read-ZipEntryText $entry }
                if ($name -like "assets/ponder/lang/*.lang") { $languageCount++ }
                if ($name -eq "pack.mcmeta") { $packMetadata = Read-ZipEntryText $entry }
                if ($name.StartsWith("com/example/ponderaddon/", [StringComparison]::Ordinal)) {
                    $errors.Add("development addon was packaged in Ponder: $name")
                }
            }

            if ($label -eq "Example" -and $name -eq "mcmod.info") {
                $exampleMcmodText = Read-ZipEntryText $entry
            }

            if ($label -eq "API" -and $name -ne "META-INF/MANIFEST.MF" -and
                !$name.EndsWith("/", [StringComparison]::Ordinal) -and
                !$name.StartsWith("net/createmod/ponder/api/", [StringComparison]::Ordinal) -and
                !$name.StartsWith("net/createmod/catnip/", [StringComparison]::Ordinal) -and
                $name -ne "net/createmod/ponder/Ponder.class" -and
                $name -ne "net/createmod/ponder/foundation/PonderIndex.class") {
                $errors.Add("non-API entry in API classifier: $name")
            }

            if (!$name.EndsWith(".class", [StringComparison]::Ordinal)) { continue }
            if ($label -eq "Sources") {
                $errors.Add("compiled class in sources artifact: $name")
                continue
            }

            $classCounts[$label]++
            $bytes = Read-ZipEntryBytes $entry
            if ($bytes.Length -lt 8 -or $bytes[6] -ne 0 -or $bytes[7] -ne 52) {
                $errors.Add("non-Java-8 class in $label artifact: $name")
                continue
            }
            $constants = [Text.Encoding]::GetEncoding(28591).GetString($bytes)
            if ($label -eq "Ponder" -and $name -eq "net/createmod/ponder/PonderMod.class") {
                $ponderModConstants = $constants
            }
            if ($label -eq "Ponder" -and $name -eq "net/createmod/ponder/Ponder.class") {
                $ponderConstants = $constants
            }
            if ($label -eq "Example" -and $name -eq "com/example/ponderaddon/ExampleAddon.class") {
                $exampleAddonConstants = $constants
            }
            foreach ($reference in $forbiddenReferences) {
                if ($constants.Contains($reference)) {
                    $errors.Add("forbidden reference $reference in $label artifact: $name")
                }
            }

            if (($label -eq "Ponder" -or $label -eq "Example") -and $null -ne $mcpMembers) {
                foreach ($reference in (Get-ClassMemberReferences $bytes)) {
                    if (!$reference.Owner.StartsWith("net/minecraft/", [StringComparison]::Ordinal)) { continue }
                    $memberKey = if ($reference.Kind -eq 9) {
                        "9|$($reference.Owner)|$($reference.Name)"
                    } else {
                        "$($reference.Kind)|$($reference.Owner)|$($reference.Name)|$($reference.Descriptor)"
                    }
                    if ($mcpMembers.Contains($memberKey)) {
                        $errors.Add("non-reobfuscated Minecraft member $($reference.Owner).$($reference.Name) in $label artifact: $name")
                    }
                }
            }
        }
    } finally {
        $archive.Dispose()
    }
}

foreach ($structure in $requiredDemoStructures) {
    if (!$ponderEntries.Contains($structure)) {
        $errors.Add("required demo structure is missing from Ponder artifact: $structure")
    }
}
foreach ($script in $requiredBuiltinScripts) {
    if (!$ponderEntries.Contains($script)) {
        $errors.Add("required builtin ZenScript is missing from Ponder artifact: $script")
    }
}

if ($null -eq $manifestText) {
    $errors.Add("release manifest is missing")
} else {
    $requiredManifest = [ordered]@{
        FMLCorePlugin = "net.createmod.ponder.mixin.PonderMixinLoader"
        ForceLoadAsMod = "true"
        FMLCorePluginContainsFMLMod = "true"
    }
    foreach ($attribute in $requiredManifest.GetEnumerator()) {
        $pattern = "(?m)^$([regex]::Escape($attribute.Key)):\s*$([regex]::Escape($attribute.Value))\s*`$"
        if ($manifestText -notmatch $pattern) {
            $errors.Add("release manifest has no $($attribute.Key)=$($attribute.Value)")
        }
    }
    if ($manifestText -match '(?m)^MixinConfigs:') {
        $errors.Add("legacy manifest MixinConfigs attribute must not be present")
    }
}

if ($null -eq $refmapText) {
    $errors.Add("mixins.ponder.refmap.json is missing")
} else {
    try {
        $refmap = $refmapText | ConvertFrom-Json
        $requiredMixins = @(
            "net/createmod/ponder/mixin/BufferBuilderAccessor",
            "net/createmod/ponder/mixin/GuiContainerAccessor",
            "net/createmod/ponder/mixin/MinecraftResizeMixin",
            "net/createmod/ponder/mixin/ParticleManagerAccessor"
        )
        foreach ($mixin in $requiredMixins) {
            $property = $refmap.mappings.PSObject.Properties[$mixin]
            if ($null -eq $property -or $property.Value.PSObject.Properties.Count -eq 0) {
                $errors.Add("refmap has no mappings for $mixin")
                continue
            }
            foreach ($mapping in $property.Value.PSObject.Properties) {
                $value = [string]$mapping.Value
                if (!$value.Contains("field_") -and !$value.Contains("func_")) {
                    $errors.Add("refmap mapping is not SRG for $mixin.$($mapping.Name): $value")
                }
            }
        }
    } catch {
        $errors.Add("refmap is not valid JSON: $($_.Exception.Message)")
    }
}

if ($languageCount -lt 34) { $errors.Add("expected at least 34 language files, found $languageCount") }
$mixinBooterRuntimeDependency = "required-after:mixinbooter@[9.1,)"
$craftTweakerRuntimeDependency = "required-after:crafttweaker@[4.1.20,)"
if ($null -eq $mcmodText -or !$mcmodText.Contains('"modid": "ponder_legacy"')) {
    $errors.Add("Ponder mcmod.info does not declare Forge modid ponder_legacy")
}
if ($null -eq $mcmodText -or !$mcmodText.Contains($mixinBooterRuntimeDependency)) {
    $errors.Add("mcmod.info does not declare supported MixinBooter range [9.1,)")
}
if ($null -eq $mcmodText -or !$mcmodText.Contains($craftTweakerRuntimeDependency)) {
    $errors.Add("mcmod.info does not declare CraftTweaker 4.1.20+")
}
if ($null -eq $mcmodText -or !$mcmodText.Contains('"version": "1.1.0-mc1.12.2"')) {
    $errors.Add("Ponder mcmod.info does not declare version 1.1.0-mc1.12.2")
}
if ($null -eq $ponderConstants -or !$ponderConstants.Contains("1.1.0-mc1.12.2")) {
    $errors.Add("Ponder.VERSION is not 1.1.0-mc1.12.2")
}
if ($null -eq $ponderConstants -or !$ponderConstants.Contains("ponder_legacy") -or
    !$ponderConstants.Contains("ponder")) {
    $errors.Add("Ponder identity constants do not preserve ponder_legacy modid and ponder content namespace")
}
if ($null -eq $ponderModConstants -or
    !$ponderModConstants.Contains("ponder_legacy") -or
    !$ponderModConstants.Contains($mixinBooterRuntimeDependency) -or
    !$ponderModConstants.Contains($craftTweakerRuntimeDependency)) {
    $errors.Add("PonderMod @Mod metadata does not declare ponder_legacy and required runtime dependencies")
}
if ($null -eq $exampleMcmodText -or !$exampleMcmodText.Contains('"version": "1.1.0"') -or
    !$exampleMcmodText.Contains("required-after:ponder_legacy@[1.1.0-mc1.12.2]")) {
    $errors.Add("example mcmod.info does not declare 1.1.0 and require Ponder 1.1.0-mc1.12.2")
}
if ($null -eq $exampleAddonConstants -or !$exampleAddonConstants.Contains("1.1.0") -or
    !$exampleAddonConstants.Contains("required-after:ponder_legacy@[1.1.0-mc1.12.2]")) {
    $errors.Add("ExampleAddon @Mod metadata does not declare and require Ponder 1.1.0")
}
if ($null -eq $packMetadata) {
    $errors.Add("pack.mcmeta is missing")
} else {
    try {
        $pack = $packMetadata | ConvertFrom-Json
        if ($pack.pack.pack_format -ne 3) {
            $errors.Add("pack.mcmeta must use pack_format 3")
        }
    } catch {
        $errors.Add("pack.mcmeta is not valid JSON: $($_.Exception.Message)")
    }
}

$mixinBooterCache = Join-Path $gradleHome "caches\modules-2\files-2.1\zone.rong\mixinbooter\$mixinBooterVersion"
$mixinBooterArtifact = Get-ChildItem -LiteralPath $mixinBooterCache -Filter "mixinbooter-$mixinBooterVersion.jar" `
    -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
$mixinBooterHash = "not present"
if ($null -eq $mixinBooterArtifact) {
    $errors.Add("MixinBooter $mixinBooterVersion is unavailable in the Gradle dependency cache")
} else {
    $mixinBooterHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $mixinBooterArtifact.FullName).Hash
    if (![string]::IsNullOrWhiteSpace($ExpectedMixinBooterHash) -and
        $mixinBooterHash -ne $ExpectedMixinBooterHash) {
        $errors.Add("MixinBooter $MixinBooterVersion hash mismatch: $mixinBooterHash")
    }
}

$catServer = Join-Path $ProjectRoot "..\CatServer-4168d848-universal.jar"
$catServerHash = "not present"
if (Test-Path -LiteralPath $catServer -PathType Leaf) {
    $catServerHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $catServer).Hash
    if ($catServerHash -ne "EAF575310ACBB48D535212CFB88D93DE69F90F2A81879A26F88457713A25952E") {
        $errors.Add("CatServer hash does not match the supported build: $catServerHash")
    }
}

$reportDirectory = Join-Path $ProjectRoot "build\reports"
$null = New-Item -ItemType Directory -Path $reportDirectory -Force
$report = Join-Path $reportDirectory "release-verification.md"
$hashes = [ordered]@{}
foreach ($artifact in $artifacts.GetEnumerator()) {
    $hashes[$artifact.Key] = (Get-FileHash -Algorithm SHA256 -LiteralPath $artifact.Value).Hash
}
$status = if ($errors.Count -eq 0) { "PASS" } else { "FAIL" }
$totalClasses = ($classCounts.Values | Measure-Object -Sum).Sum
$lines = @(
    "# Ponder 1.12.2 release verification",
    "",
    "- Status: $status",
    "- Generated: $([DateTime]::UtcNow.ToString('u')) UTC",
    "- Java 8 classes checked: $totalClasses (Ponder $($classCounts.Ponder), API $($classCounts.API), Example $($classCounts.Example))",
    "- Language files: $languageCount",
    "- Supported MixinBooter range: [9.1,)",
    "- Required CraftTweaker range: [4.1.20,)",
    "- Build MixinBooter version: $MixinBooterVersion",
    "- MixinBooter SHA256: $mixinBooterHash",
    "- Ponder SHA256: $($hashes.Ponder)",
    "- API SHA256: $($hashes.API)",
    "- Sources SHA256: $($hashes.Sources)",
    "- Example addon SHA256: $($hashes.Example)",
    "- CatServer SHA256: $catServerHash"
)
if ($errors.Count -gt 0) {
    $lines += ""
    $lines += "## Blocking findings"
    $lines += ""
    foreach ($finding in $errors) { $lines += "- $finding" }
}
[IO.File]::WriteAllLines($report, $lines, [Text.UTF8Encoding]::new($false))

if ($errors.Count -gt 0) {
    throw "Release verification failed with $($errors.Count) finding(s). See $report"
}
Write-Host "Release verification passed."
Write-Host "Report: $report"
Write-Host "Ponder SHA256: $($hashes.Ponder)"
