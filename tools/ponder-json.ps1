[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("validate", "migrate")]
    [string]$Command,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$Path,

    [string]$OutputPath,

    [string]$SchemaDirectory
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($SchemaDirectory)) {
    $scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
    $SchemaDirectory = Join-Path $scriptDirectory "..\schemas"
}

function Get-Properties {
    param([object]$Value)
    if ($null -eq $Value) {
        return @()
    }
    return @($Value.PSObject.Properties)
}

function Has-Property {
    param([object]$Value, [string]$Name)
    return $null -ne $Value -and $null -ne $Value.PSObject.Properties[$Name]
}

function Require-Object {
    param([object]$Value, [string]$Location)
    if ($null -eq $Value -or $Value -is [string] -or $Value -is [System.Array] -or
        $Value -is [ValueType]) {
        throw "$Location must be an object"
    }
}

function Require-Array {
    param([object]$Value, [string]$Location)
    if ($null -eq $Value) {
        return @()
    }
    if ($Value -is [string] -or -not ($Value -is [System.Collections.IEnumerable])) {
        throw "$Location must be an array"
    }
    return @($Value)
}

function Require-String {
    param([object]$Value, [string]$Location, [int]$Maximum = 8192)
    if (-not ($Value -is [string])) {
        throw "$Location must be a string"
    }
    if ($Value.Length -gt $Maximum) {
        throw "$Location exceeds $Maximum characters"
    }
    return [string]$Value
}

function Require-ResourceId {
    param([object]$Value, [string]$Location)
    $text = Require-String $Value $Location 256
    if ($text -notmatch "^[a-z0-9_.-]+:[a-z0-9_./-]+$") {
        throw "$Location is not a valid resource ID: $text"
    }
    return $text
}

function Require-Integer {
    param([object]$Value, [string]$Location)
    if ($Value -is [bool] -or $null -eq $Value -or
        [math]::Floor([double]$Value) -ne [double]$Value) {
        throw "$Location must be an integer"
    }
}

function Require-Number {
    param([object]$Value, [string]$Location)
    if ($Value -is [bool] -or $null -eq $Value) {
        throw "$Location must be a finite number"
    }
    $number = [double]$Value
    if ([double]::IsNaN($number) -or [double]::IsInfinity($number)) {
        throw "$Location must be a finite number"
    }
}

function Require-Boolean {
    param([object]$Value, [string]$Location)
    if (-not ($Value -is [bool])) {
        throw "$Location must be a boolean"
    }
}

function Reject-UnknownFields {
    param([object]$Value, [string[]]$Allowed, [string]$Location)
    foreach ($property in Get-Properties $Value) {
        if ($Allowed -notcontains $property.Name) {
            throw "$Location contains unknown field '$($property.Name)'"
        }
    }
}

function Require-Fields {
    param([object]$Value, [string[]]$Required, [string]$Location)
    foreach ($name in $Required) {
        if (-not (Has-Property $Value $name) -or $null -eq $Value.$name) {
            throw "$Location is missing required field '$name'"
        }
    }
}

function Test-Vector3 {
    param([object]$Value, [string]$Location)
    $entries = @(Require-Array $Value $Location)
    if ($entries.Count -ne 3) {
        throw "$Location must contain exactly three integers"
    }
    for ($index = 0; $index -lt 3; $index++) {
        Require-Integer $entries[$index] "$Location/$index"
    }
}

function Test-Selection {
    param([object]$Value, [string]$Location)
    Require-Object $Value $Location
    Require-Fields $Value @("type") $Location
    $type = Require-String $Value.type "$Location/type" 64
    switch ($type) {
        "position" {
            Reject-UnknownFields $Value @("type", "pos") $Location
            Require-Fields $Value @("type", "pos") $Location
            Test-Vector3 $Value.pos "$Location/pos"
        }
        "from_to" {
            Reject-UnknownFields $Value @("type", "from", "to") $Location
            Require-Fields $Value @("type", "from", "to") $Location
            Test-Vector3 $Value.from "$Location/from"
            Test-Vector3 $Value.to "$Location/to"
        }
        "column" {
            Reject-UnknownFields $Value @("type", "x", "z") $Location
            Require-Fields $Value @("type", "x", "z") $Location
            Require-Integer $Value.x "$Location/x"
            Require-Integer $Value.z "$Location/z"
        }
        { $_ -eq "layer" -or $_ -eq "layers_from" } {
            Reject-UnknownFields $Value @("type", "y") $Location
            Require-Fields $Value @("type", "y") $Location
            Require-Integer $Value.y "$Location/y"
        }
        "layers" {
            Reject-UnknownFields $Value @("type", "y", "height") $Location
            Require-Fields $Value @("type", "y", "height") $Location
            Require-Integer $Value.y "$Location/y"
            Require-Integer $Value.height "$Location/height"
            if ([int]$Value.height -le 0) {
                throw "$Location/height must be greater than zero"
            }
        }
        "cuboid" {
            Reject-UnknownFields $Value @("type", "origin", "offset") $Location
            Require-Fields $Value @("type", "origin", "offset") $Location
            Test-Vector3 $Value.origin "$Location/origin"
            Test-Vector3 $Value.offset "$Location/offset"
        }
        "everywhere" {
            Reject-UnknownFields $Value @("type") $Location
        }
        "structure_group" {
            Reject-UnknownFields $Value @("type", "name") $Location
            Require-Fields $Value @("type", "name") $Location
            $name = Require-String $Value.name "$Location/name" 256
            if ([string]::IsNullOrWhiteSpace($name)) {
                throw "$Location/name may not be blank"
            }
        }
        default {
            throw "$Location has unknown selection type '$type'"
        }
    }
}

function Test-SnbtEnvelope {
    param([object]$Value, [string]$Location)
    $text = (Require-String $Value $Location 262144).Trim()
    if ($text.Length -lt 2 -or $text[0] -ne "{" -or $text[$text.Length - 1] -ne "}") {
        throw "$Location must be an SNBT compound enclosed by braces"
    }
}

function Read-OperationContract {
    $contractPath = Join-Path $SchemaDirectory "ponder-operations-v1.json"
    if (-not (Test-Path -LiteralPath $contractPath -PathType Leaf)) {
        throw "Missing operation contract: $contractPath"
    }
    $contract = Get-Content -Raw -LiteralPath $contractPath | ConvertFrom-Json
    if ($contract.format -ne 1 -or $null -eq $contract.operations) {
        throw "Unsupported Ponder JSON operation contract"
    }
    $result = [ordered]@{}
    foreach ($operation in Get-Properties $contract.operations) {
        if ($operation.Value -is [System.Array]) {
            $fields = @($operation.Value)
            $optional = @()
        } else {
            Require-Object $operation.Value "operation contract/$($operation.Name)"
            $fields = @($operation.Value.fields)
            $optional = @($operation.Value.optional)
        }
        $required = @($fields | Where-Object { $optional -notcontains $_ })
        $result[$operation.Name] = [pscustomobject]@{
            Fields = $fields
            Optional = $optional
            Required = $required
        }
    }
    return $result
}

$IntegerFields = @("size", "ticks", "duration", "count", "meta", "cycles")
$BooleanFields = @("enabled", "particles", "replace", "redraw", "near", "keyframe", "big", "visible")
$ResourceFields = @("item", "codec")
$StringFields = @("handle", "direction", "side", "state", "property", "type", "pose",
    "text", "key", "slot", "pointing", "action")

function Test-Instruction {
    param([object]$Value, [string]$Location, [System.Collections.IDictionary]$Operations)
    Require-Object $Value $Location
    Require-Fields $Value @("op") $Location
    $operation = Require-String $Value.op "$Location/op" 256
    if (-not $Operations.Contains($operation)) {
        throw "$Location uses unknown operation '$operation'"
    }
    $spec = $Operations[$operation]
    Reject-UnknownFields $Value (@("op") + @($spec.Fields)) $Location
    Require-Fields $Value @($spec.Required) $Location

    foreach ($field in Get-Properties $Value) {
        if ($field.Name -eq "op") {
            continue
        }
        $fieldLocation = "$Location/$($field.Name)"
        if ($field.Name -eq "selection") {
            Test-Selection $field.Value $fieldLocation
        } elseif ($field.Name -eq "nbt" -or $field.Name -eq "payload") {
            Test-SnbtEnvelope $field.Value $fieldLocation
        } elseif ($field.Name -eq "params") {
            $parameters = @(Require-Array $field.Value $fieldLocation)
            for ($index = 0; $index -lt $parameters.Count; $index++) {
                [void](Require-String $parameters[$index] "$fieldLocation/$index" 8192)
            }
        } elseif ($IntegerFields -contains $field.Name) {
            Require-Integer $field.Value $fieldLocation
        } elseif ($BooleanFields -contains $field.Name) {
            Require-Boolean $field.Value $fieldLocation
        } elseif ($ResourceFields -contains $field.Name) {
            [void](Require-ResourceId $field.Value $fieldLocation)
        } elseif ($field.Name -eq "color") {
            if ($field.Value -is [string]) {
                [void](Require-String $field.Value $fieldLocation 64)
            } else {
                Require-Integer $field.Value $fieldLocation
            }
        } elseif ($StringFields -contains $field.Name) {
            [void](Require-String $field.Value $fieldLocation 8192)
        } else {
            Require-Number $field.Value $fieldLocation
        }
    }
}

function Test-Pack {
    param([object]$Pack, [System.Collections.IDictionary]$Operations)
    Require-Object $Pack "pack"
    Reject-UnknownFields $Pack @('$schema', "format", "id", "scenes", "tags",
        "sharedText", "indexExclusions") "pack"
    Require-Fields $Pack @("format", "id") "pack"
    Require-Integer $Pack.format "pack/format"
    if ([int]$Pack.format -ne 1) {
        throw "pack/format must be 1"
    }
    [void](Require-ResourceId $Pack.id "pack/id")

    $scenes = @(if (Has-Property $Pack "scenes") {
        Require-Array $Pack.scenes "pack/scenes"
    })
    if ($scenes.Count -gt 2048) {
        throw "pack/scenes exceeds 2048 entries"
    }
    for ($sceneIndex = 0; $sceneIndex -lt $scenes.Count; $sceneIndex++) {
        $scene = $scenes[$sceneIndex]
        $location = "pack/scenes/$sceneIndex"
        Require-Object $scene $location
        Reject-UnknownFields $scene @("id", "component", "title", "structure", "tags",
            "clientOnly", "instructions") $location
        Require-Fields $scene @("id", "component", "title", "structure", "instructions") $location
        [void](Require-ResourceId $scene.id "$location/id")
        [void](Require-ResourceId $scene.component "$location/component")
        $title = Require-String $scene.title "$location/title" 8192
        if ([string]::IsNullOrWhiteSpace($title)) {
            throw "$location/title may not be blank"
        }
        [void](Require-ResourceId $scene.structure "$location/structure")
        if (Has-Property $scene "clientOnly") {
            Require-Boolean $scene.clientOnly "$location/clientOnly"
        }
        if (Has-Property $scene "tags") {
            $tags = @(Require-Array $scene.tags "$location/tags")
            for ($tagIndex = 0; $tagIndex -lt $tags.Count; $tagIndex++) {
                [void](Require-ResourceId $tags[$tagIndex] "$location/tags/$tagIndex")
            }
        }
        $instructions = @(Require-Array $scene.instructions "$location/instructions")
        if ($instructions.Count -lt 1 -or $instructions.Count -gt 4096) {
            throw "$location/instructions must contain 1 to 4096 entries"
        }
        for ($instructionIndex = 0; $instructionIndex -lt $instructions.Count; $instructionIndex++) {
            Test-Instruction $instructions[$instructionIndex] `
                "$location/instructions/$instructionIndex" $Operations
        }
    }

    $tags = @(if (Has-Property $Pack "tags") {
        Require-Array $Pack.tags "pack/tags"
    })
    if ($tags.Count -gt 1024) {
        throw "pack/tags exceeds 1024 entries"
    }
    for ($tagIndex = 0; $tagIndex -lt $tags.Count; $tagIndex++) {
        $tag = $tags[$tagIndex]
        $location = "pack/tags/$tagIndex"
        Require-Object $tag $location
        Reject-UnknownFields $tag @("id", "icon", "title", "description", "indexed", "components") $location
        Require-Fields $tag @("id", "icon", "title", "description") $location
        [void](Require-ResourceId $tag.id "$location/id")
        [void](Require-ResourceId $tag.icon "$location/icon")
        [void](Require-String $tag.title "$location/title" 8192)
        [void](Require-String $tag.description "$location/description" 8192)
        if (Has-Property $tag "indexed") {
            Require-Boolean $tag.indexed "$location/indexed"
        }
        if (Has-Property $tag "components") {
            $components = @(Require-Array $tag.components "$location/components")
            for ($componentIndex = 0; $componentIndex -lt $components.Count; $componentIndex++) {
                [void](Require-ResourceId $components[$componentIndex] "$location/components/$componentIndex")
            }
        }
    }

    if (Has-Property $Pack "sharedText") {
        Require-Object $Pack.sharedText "pack/sharedText"
        foreach ($entry in Get-Properties $Pack.sharedText) {
            if ([string]::IsNullOrWhiteSpace($entry.Name) -or $entry.Name.Length -gt 256) {
                throw "pack/sharedText contains a blank or oversized key"
            }
            [void](Require-String $entry.Value "pack/sharedText/$($entry.Name)" 8192)
        }
    }
    if (Has-Property $Pack "indexExclusions") {
        $exclusions = @(Require-Array $Pack.indexExclusions "pack/indexExclusions")
        for ($index = 0; $index -lt $exclusions.Count; $index++) {
            [void](Require-ResourceId $exclusions[$index] "pack/indexExclusions/$index")
        }
    }
}

function Convert-Instruction {
    param([object]$Instruction, [System.Collections.IDictionary]$Operations)
    $result = [ordered]@{ op = $Instruction.op }
    foreach ($field in @($Operations[$Instruction.op].Fields)) {
        if (Has-Property $Instruction $field) {
            $result[$field] = $Instruction.$field
        }
    }
    return $result
}

function Convert-Pack {
    param([object]$Pack, [System.Collections.IDictionary]$Operations)
    $result = [ordered]@{}
    if (Has-Property $Pack '$schema') {
        $result['$schema'] = $Pack.'$schema'
    }
    $result["format"] = 1
    $result["id"] = $Pack.id
    if (Has-Property $Pack "scenes") {
        $result["scenes"] = @($Pack.scenes | ForEach-Object {
            $scene = [ordered]@{
                id = $_.id
                component = $_.component
                title = $_.title
                structure = $_.structure
            }
            if (Has-Property $_ "tags") { $scene["tags"] = @($_.tags) }
            if (Has-Property $_ "clientOnly") { $scene["clientOnly"] = [bool]$_.clientOnly }
            $scene["instructions"] = @($_.instructions | ForEach-Object {
                Convert-Instruction $_ $Operations
            })
            $scene
        })
    }
    if (Has-Property $Pack "tags") {
        $result["tags"] = @($Pack.tags | ForEach-Object {
            $tag = [ordered]@{
                id = $_.id
                icon = $_.icon
                title = $_.title
                description = $_.description
            }
            if (Has-Property $_ "indexed") { $tag["indexed"] = [bool]$_.indexed }
            if (Has-Property $_ "components") { $tag["components"] = @($_.components) }
            $tag
        })
    }
    if (Has-Property $Pack "sharedText") {
        $shared = [ordered]@{}
        foreach ($entry in @(Get-Properties $Pack.sharedText | Sort-Object Name)) {
            $shared[$entry.Name] = $entry.Value
        }
        $result["sharedText"] = $shared
    }
    if (Has-Property $Pack "indexExclusions") {
        $result["indexExclusions"] = @($Pack.indexExclusions)
    }
    return $result
}

$inputFile = (Resolve-Path -LiteralPath $Path).Path
if (-not [IO.File]::Exists($inputFile)) {
    throw "Ponder JSON pack does not exist: $Path"
}
if (-not $inputFile.EndsWith(".ponder.json", [StringComparison]::OrdinalIgnoreCase)) {
    throw "Ponder JSON pack must use the .ponder.json extension"
}
if ((Get-Item -LiteralPath $inputFile).Length -gt 1MB) {
    throw "Ponder JSON pack exceeds 1 MiB: $inputFile"
}

$operations = Read-OperationContract
$pack = Get-Content -Raw -LiteralPath $inputFile | ConvertFrom-Json
Test-Pack $pack $operations

if ($Command -eq "validate") {
    Write-Output "Valid Ponder JSON pack: $inputFile"
    return
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $directory = [IO.Path]::GetDirectoryName($inputFile)
    $name = [IO.Path]::GetFileNameWithoutExtension(
        [IO.Path]::GetFileNameWithoutExtension($inputFile))
    $OutputPath = Join-Path $directory "$name.migrated.ponder.json"
}
$outputFile = [IO.Path]::GetFullPath($OutputPath)
if ([string]::Equals($inputFile, $outputFile, [StringComparison]::OrdinalIgnoreCase)) {
    throw "migrate refuses to overwrite the input pack"
}
$outputDirectory = [IO.Path]::GetDirectoryName($outputFile)
if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
    [IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
}
$canonical = Convert-Pack $pack $operations
$json = $canonical | ConvertTo-Json -Depth 100
[IO.File]::WriteAllText($outputFile, $json + [Environment]::NewLine,
    (New-Object Text.UTF8Encoding($false)))
Write-Output "Migrated Ponder JSON pack: $outputFile"
