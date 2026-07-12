param(
    [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $PSScriptRoot "..\src\main\resources\assets\ponder\ponder\demo"
}
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)

function Write-BigEndianBytes([IO.BinaryWriter]$Writer, [byte[]]$Bytes) {
    [Array]::Reverse($Bytes)
    $Writer.Write($Bytes)
}

function Write-Int16([IO.BinaryWriter]$Writer, [int]$Value) {
    Write-BigEndianBytes $Writer ([BitConverter]::GetBytes([int16]$Value))
}

function Write-Int32([IO.BinaryWriter]$Writer, [int]$Value) {
    Write-BigEndianBytes $Writer ([BitConverter]::GetBytes([int32]$Value))
}

function Write-Int64([IO.BinaryWriter]$Writer, [long]$Value) {
    Write-BigEndianBytes $Writer ([BitConverter]::GetBytes([int64]$Value))
}

function Write-Single([IO.BinaryWriter]$Writer, [single]$Value) {
    Write-BigEndianBytes $Writer ([BitConverter]::GetBytes($Value))
}

function Write-Double([IO.BinaryWriter]$Writer, [double]$Value) {
    Write-BigEndianBytes $Writer ([BitConverter]::GetBytes($Value))
}

function Write-NbtString([IO.BinaryWriter]$Writer, [string]$Value) {
    $bytes = [Text.Encoding]::UTF8.GetBytes($Value)
    if ($bytes.Length -gt 32767) { throw "NBT string is too long" }
    Write-Int16 $Writer $bytes.Length
    $Writer.Write($bytes)
}

function Write-Header([IO.BinaryWriter]$Writer, [byte]$Type, [string]$Name) {
    $Writer.Write($Type)
    Write-NbtString $Writer $Name
}

function Write-StringTag([IO.BinaryWriter]$Writer, [string]$Name, [string]$Value) {
    Write-Header $Writer 8 $Name
    Write-NbtString $Writer $Value
}

function Write-ByteTag([IO.BinaryWriter]$Writer, [string]$Name, [byte]$Value) {
    Write-Header $Writer 1 $Name
    $Writer.Write($Value)
}

function Write-ShortTag([IO.BinaryWriter]$Writer, [string]$Name, [int]$Value) {
    Write-Header $Writer 2 $Name
    Write-Int16 $Writer $Value
}

function Write-IntTag([IO.BinaryWriter]$Writer, [string]$Name, [int]$Value) {
    Write-Header $Writer 3 $Name
    Write-Int32 $Writer $Value
}

function Write-LongTag([IO.BinaryWriter]$Writer, [string]$Name, [long]$Value) {
    Write-Header $Writer 4 $Name
    Write-Int64 $Writer $Value
}

function Write-FloatTag([IO.BinaryWriter]$Writer, [string]$Name, [single]$Value) {
    Write-Header $Writer 5 $Name
    Write-Single $Writer $Value
}

function Write-IntListTag([IO.BinaryWriter]$Writer, [string]$Name, [int[]]$Values) {
    Write-Header $Writer 9 $Name
    $Writer.Write([byte]3)
    Write-Int32 $Writer $Values.Length
    foreach ($value in $Values) { Write-Int32 $Writer $value }
}

function Write-DoubleListTag([IO.BinaryWriter]$Writer, [string]$Name, [double[]]$Values) {
    Write-Header $Writer 9 $Name
    $Writer.Write([byte]6)
    Write-Int32 $Writer $Values.Length
    foreach ($value in $Values) { Write-Double $Writer $value }
}

function Write-FloatListTag([IO.BinaryWriter]$Writer, [string]$Name, [single[]]$Values) {
    Write-Header $Writer 9 $Name
    $Writer.Write([byte]5)
    Write-Int32 $Writer $Values.Length
    foreach ($value in $Values) { Write-Single $Writer $value }
}

function New-State([string]$Name, [Collections.IDictionary]$Properties = $null) {
    if ($null -eq $Properties) { $Properties = [ordered]@{} }
    [pscustomobject]@{ Name = $Name; Properties = $Properties }
}

function New-Block([int]$X, [int]$Y, [int]$Z, [string]$State, $Tile = $null) {
    [pscustomobject]@{ X = $X; Y = $Y; Z = $Z; State = $State; Tile = $Tile }
}

function New-Item([int]$Slot, [string]$Id, [int]$Count = 1, [int]$Damage = 0) {
    [pscustomobject]@{ Slot = $Slot; Id = $Id; Count = $Count; Damage = $Damage }
}

function New-ChestTile([string]$CustomName, [object[]]$Items) {
    [pscustomobject]@{ Kind = "Chest"; CustomName = $CustomName; Items = $Items }
}

function New-FurnaceTile([string]$CustomName, [object[]]$Items, [int]$BurnTime, [int]$CookTime) {
    [pscustomobject]@{
        Kind = "Furnace"; CustomName = $CustomName; Items = $Items
        BurnTime = $BurnTime; CookTime = $CookTime; CookTimeTotal = 200
    }
}

function New-ArmorStand([double]$X, [double]$Y, [double]$Z) {
    [pscustomobject]@{ Kind = "ArmorStand"; X = $X; Y = $Y; Z = $Z }
}

function Add-Floor([Collections.ArrayList]$Blocks, [scriptblock]$StateAt) {
    for ($z = 0; $z -lt 5; $z++) {
        for ($x = 0; $x -lt 5; $x++) {
            $state = & $StateAt $x $z
            $null = $Blocks.Add((New-Block $x 0 $z $state))
        }
    }
}

function New-BasicsStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks { param($x, $z) "stone" }
    $null = $blocks.Add((New-Block 1 1 2 "glass"))
    $null = $blocks.Add((New-Block 2 1 2 "crafting"))
    $null = $blocks.Add((New-Block 3 1 2 "chest" (New-ChestTile "Ponder Supplies" @(
        New-Item 0 "minecraft:book"
    ))))
    $null = $blocks.Add((New-Block 1 1 3 "water"))
    $null = $blocks.Add((New-Block 1 2 3 "glass"))
    $null = $blocks.Add((New-Block 4 1 2 "lamp"))
    $null = $blocks.Add((New-Block 0 1 2 "torch"))
    $null = $blocks.Add((New-Block 4 1 3 "leaves"))
    $null = $blocks.Add((New-Block 4 1 4 "grass"))
    [pscustomobject]@{
        Palette = [ordered]@{
            stone = (New-State "minecraft:stone")
            glass = (New-State "minecraft:glass")
            water = (New-State "minecraft:water" ([ordered]@{ level = "0" }))
            crafting = (New-State "minecraft:crafting_table")
            chest = (New-State "minecraft:chest" ([ordered]@{ facing = "north" }))
            lamp = (New-State "minecraft:redstone_lamp")
            torch = (New-State "minecraft:torch" ([ordered]@{ facing = "up" }))
            leaves = (New-State "minecraft:leaves" ([ordered]@{
                variant = "oak"; decayable = "false"; check_decay = "false"
            }))
            grass = (New-State "minecraft:grass" ([ordered]@{ snowy = "false" }))
        }
        Blocks = $blocks
        Entities = @((New-ArmorStand 2.5 1.0 1.5))
    }
}

function New-StorageStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks {
        param($x, $z)
        if ($x -eq 0 -or $x -eq 4 -or $z -eq 0 -or $z -eq 4) { "stonebrick" } else { "planks" }
    }
    $null = $blocks.Add((New-Block 2 1 2 "chest" (New-ChestTile "" @(
        New-Item 0 "minecraft:book" 4
        New-Item 1 "minecraft:compass"
        New-Item 8 "minecraft:chest"
    ))))
    foreach ($x in @(1, 2, 3)) { $null = $blocks.Add((New-Block $x 1 4 "bookshelf")) }
    $null = $blocks.Add((New-Block 1 2 4 "bookshelf"))
    $null = $blocks.Add((New-Block 3 2 4 "bookshelf"))
    $null = $blocks.Add((New-Block 2 2 4 "glass"))
    [pscustomobject]@{
        Palette = [ordered]@{
            stonebrick = (New-State "minecraft:stonebrick")
            planks = (New-State "minecraft:planks" ([ordered]@{ variant = "oak" }))
            chest = (New-State "minecraft:chest" ([ordered]@{ facing = "north" }))
            bookshelf = (New-State "minecraft:bookshelf")
            glass = (New-State "minecraft:glass")
        }
        Blocks = $blocks
        Entities = @()
    }
}

function New-SmeltingStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks {
        param($x, $z)
        if ($x -eq 2 -or $z -eq 2) { "brick" } else { "cobblestone" }
    }
    $null = $blocks.Add((New-Block 1 1 2 "iron_ore"))
    $null = $blocks.Add((New-Block 2 1 2 "furnace" (New-FurnaceTile "Ponder Furnace" @() 0 0)))
    $null = $blocks.Add((New-Block 3 1 2 "coal_block"))
    $null = $blocks.Add((New-Block 2 1 3 "iron_block"))
    [pscustomobject]@{
        Palette = [ordered]@{
            cobblestone = (New-State "minecraft:cobblestone")
            brick = (New-State "minecraft:brick_block")
            iron_ore = (New-State "minecraft:iron_ore")
            furnace = (New-State "minecraft:furnace" ([ordered]@{ facing = "north" }))
            coal_block = (New-State "minecraft:coal_block")
            iron_block = (New-State "minecraft:iron_block")
        }
        Blocks = $blocks
        Entities = @()
    }
}

function New-PistonStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks {
        param($x, $z)
        if ($z -eq 2) { "iron_block" } elseif (($x + $z) % 2 -eq 0) { "stone" } else { "andesite" }
    }
    $null = $blocks.Add((New-Block 1 1 2 "piston"))
    $null = $blocks.Add((New-Block 2 1 2 "slime"))
    $null = $blocks.Add((New-Block 4 1 2 "obsidian"))
    $null = $blocks.Add((New-Block 1 1 4 "redstone"))
    [pscustomobject]@{
        Palette = [ordered]@{
            stone = (New-State "minecraft:stone" ([ordered]@{ variant = "stone" }))
            andesite = (New-State "minecraft:stone" ([ordered]@{ variant = "andesite" }))
            iron_block = (New-State "minecraft:iron_block")
            piston = (New-State "minecraft:piston" ([ordered]@{ facing = "east"; extended = "false" }))
            slime = (New-State "minecraft:slime")
            obsidian = (New-State "minecraft:obsidian")
            redstone = (New-State "minecraft:redstone_block")
        }
        Blocks = $blocks
        Entities = @()
    }
}

function New-RedstoneStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks {
        param($x, $z)
        if ($z -eq 2) { "quartz" } elseif ($x -eq 0 -or $x -eq 4 -or $z -eq 0 -or $z -eq 4) { "stonebrick" } else { "stone" }
    }
    $null = $blocks.Add((New-Block 1 1 2 "source"))
    $null = $blocks.Add((New-Block 2 1 2 "wire"))
    $null = $blocks.Add((New-Block 3 1 2 "wire"))
    $null = $blocks.Add((New-Block 4 1 2 "lamp"))
    [pscustomobject]@{
        Palette = [ordered]@{
            stone = (New-State "minecraft:stone")
            stonebrick = (New-State "minecraft:stonebrick")
            quartz = (New-State "minecraft:quartz_block")
            source = (New-State "minecraft:redstone_block")
            wire = (New-State "minecraft:redstone_wire" ([ordered]@{ power = "0" }))
            lamp = (New-State "minecraft:redstone_lamp")
        }
        Blocks = $blocks
        Entities = @()
    }
}

function New-RenderLayersStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks {
        param($x, $z)
        if ($x -eq $z -or ($x + $z) -eq 4) { "quartz" } else { "stonebrick" }
    }
    $null = $blocks.Add((New-Block 1 1 2 "solid"))
    $null = $blocks.Add((New-Block 2 1 2 "cutout"))
    $null = $blocks.Add((New-Block 3 1 2 "mipped"))
    $null = $blocks.Add((New-Block 4 1 2 "translucent"))
    [pscustomobject]@{
        Palette = [ordered]@{
            stonebrick = (New-State "minecraft:stonebrick")
            quartz = (New-State "minecraft:quartz_block")
            solid = (New-State "minecraft:stone")
            cutout = (New-State "minecraft:glass")
            mipped = (New-State "minecraft:grass" ([ordered]@{ snowy = "false" }))
            translucent = (New-State "minecraft:stained_glass" ([ordered]@{ color = "light_blue" }))
        }
        Blocks = $blocks
        Entities = @()
    }
}

function New-FluidsStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks {
        param($x, $z)
        if ($x -eq 0 -or $x -eq 4 -or $z -eq 0 -or $z -eq 4) { "stonebrick" } else { "prismarine" }
    }
    $null = $blocks.Add((New-Block 2 1 2 "source"))
    $null = $blocks.Add((New-Block 1 1 2 "flow4"))
    $null = $blocks.Add((New-Block 3 1 2 "flow7"))
    foreach ($x in @(1, 2, 3)) {
        $null = $blocks.Add((New-Block $x 1 1 "glass"))
        $null = $blocks.Add((New-Block $x 1 3 "glass"))
    }
    [pscustomobject]@{
        Palette = [ordered]@{
            stonebrick = (New-State "minecraft:stonebrick")
            prismarine = (New-State "minecraft:prismarine" ([ordered]@{ variant = "prismarine" }))
            source = (New-State "minecraft:water" ([ordered]@{ level = "0" }))
            flow4 = (New-State "minecraft:flowing_water" ([ordered]@{ level = "4" }))
            flow7 = (New-State "minecraft:flowing_water" ([ordered]@{ level = "7" }))
            glass = (New-State "minecraft:glass")
        }
        Blocks = $blocks
        Entities = @()
    }
}

function New-RailStructure {
    $blocks = [Collections.ArrayList]::new()
    Add-Floor $blocks {
        param($x, $z)
        if ($x -eq 0 -or $z -eq 0 -or $x -eq 4 -or $z -eq 4) { "stonebrick" } else { "planks" }
    }
    $null = $blocks.Add((New-Block 1 1 1 "north_south"))
    $null = $blocks.Add((New-Block 1 1 2 "north_south"))
    $null = $blocks.Add((New-Block 1 1 3 "north_east"))
    $null = $blocks.Add((New-Block 2 1 3 "east_west"))
    $null = $blocks.Add((New-Block 3 1 3 "east_west"))
    [pscustomobject]@{
        Palette = [ordered]@{
            stonebrick = (New-State "minecraft:stonebrick")
            planks = (New-State "minecraft:planks" ([ordered]@{ variant = "spruce" }))
            north_south = (New-State "minecraft:rail" ([ordered]@{ shape = "north_south" }))
            north_east = (New-State "minecraft:rail" ([ordered]@{ shape = "north_east" }))
            east_west = (New-State "minecraft:rail" ([ordered]@{ shape = "east_west" }))
        }
        Blocks = $blocks
        Entities = @()
    }
}

function Write-PaletteEntry([IO.BinaryWriter]$Writer, $State) {
    Write-StringTag $Writer "Name" $State.Name
    if ($State.Properties.Count -gt 0) {
        Write-Header $Writer 10 "Properties"
        foreach ($key in $State.Properties.Keys) {
            Write-StringTag $Writer $key $State.Properties[$key]
        }
        $Writer.Write([byte]0)
    }
    $Writer.Write([byte]0)
}

function Write-ItemList([IO.BinaryWriter]$Writer, [object[]]$Items) {
    Write-Header $Writer 9 "Items"
    $Writer.Write([byte]10)
    Write-Int32 $Writer $Items.Count
    foreach ($item in $Items) {
        Write-ByteTag $Writer "Slot" ([byte]$item.Slot)
        Write-StringTag $Writer "id" $item.Id
        Write-ByteTag $Writer "Count" ([byte]$item.Count)
        Write-ShortTag $Writer "Damage" $item.Damage
        $Writer.Write([byte]0)
    }
}

function Write-TileData([IO.BinaryWriter]$Writer, $Tile) {
    Write-Header $Writer 10 "nbt"
    if ($Tile.Kind -eq "Chest") {
        Write-StringTag $Writer "id" "minecraft:chest"
        if (![string]::IsNullOrEmpty($Tile.CustomName)) {
            Write-StringTag $Writer "CustomName" $Tile.CustomName
        }
        Write-StringTag $Writer "Lock" ""
        Write-ItemList $Writer $Tile.Items
    } elseif ($Tile.Kind -eq "Furnace") {
        Write-StringTag $Writer "id" "minecraft:furnace"
        Write-StringTag $Writer "CustomName" $Tile.CustomName
        Write-StringTag $Writer "Lock" ""
        Write-IntTag $Writer "BurnTime" $Tile.BurnTime
        Write-IntTag $Writer "CookTime" $Tile.CookTime
        Write-IntTag $Writer "CookTimeTotal" $Tile.CookTimeTotal
        Write-ItemList $Writer $Tile.Items
    } else {
        throw "Unknown tile payload kind: $($Tile.Kind)"
    }
    $Writer.Write([byte]0)
}

function Write-Block([IO.BinaryWriter]$Writer, $Block, [Collections.IDictionary]$PaletteIndices) {
    Write-IntListTag $Writer "pos" @($Block.X, $Block.Y, $Block.Z)
    if (-not $PaletteIndices.Contains($Block.State)) { throw "Unknown palette key: $($Block.State)" }
    Write-IntTag $Writer "state" $PaletteIndices[$Block.State]
    if ($null -ne $Block.Tile) { Write-TileData $Writer $Block.Tile }
    $Writer.Write([byte]0)
}

function Write-Entity([IO.BinaryWriter]$Writer, $Entity) {
    if ($Entity.Kind -ne "ArmorStand") { throw "Unknown entity payload kind: $($Entity.Kind)" }
    Write-DoubleListTag $Writer "pos" @([double]$Entity.X, [double]$Entity.Y, [double]$Entity.Z)
    Write-IntListTag $Writer "blockPos" @([int][Math]::Floor($Entity.X), [int][Math]::Floor($Entity.Y), [int][Math]::Floor($Entity.Z))
    Write-Header $Writer 10 "nbt"
    Write-StringTag $Writer "id" "minecraft:armor_stand"
    Write-DoubleListTag $Writer "Pos" @([double]$Entity.X, [double]$Entity.Y, [double]$Entity.Z)
    Write-DoubleListTag $Writer "Motion" @([double]0.0, [double]0.0, [double]0.0)
    Write-FloatListTag $Writer "Rotation" @([single]180.0, [single]0.0)
    Write-FloatTag $Writer "Health" ([single]20.0)
    Write-LongTag $Writer "UUIDMost" 123456789
    Write-LongTag $Writer "UUIDLeast" 987654321
    Write-ByteTag $Writer "NoGravity" 1
    Write-ByteTag $Writer "ShowArms" 1
    Write-ByteTag $Writer "PersistenceRequired" 1
    $Writer.Write([byte]0)
    $Writer.Write([byte]0)
}

function Write-Structure([string]$Name, $Definition) {
    $paletteIndices = [ordered]@{}
    $paletteIndex = 0
    foreach ($key in $Definition.Palette.Keys) {
        $paletteIndices[$key] = $paletteIndex
        $paletteIndex++
    }

    $raw = [IO.MemoryStream]::new()
    $writer = [IO.BinaryWriter]::new($raw, [Text.Encoding]::UTF8, $true)
    try {
        Write-Header $writer 10 ""
        Write-IntTag $writer "DataVersion" 1343
        Write-StringTag $writer "author" "Ponder 1.12.2 independent rewrite"
        Write-IntListTag $writer "size" @(5, 3, 5)

        Write-Header $writer 9 "palette"
        $writer.Write([byte]10)
        Write-Int32 $writer $Definition.Palette.Count
        foreach ($key in $Definition.Palette.Keys) {
            Write-PaletteEntry $writer $Definition.Palette[$key]
        }

        Write-Header $writer 9 "blocks"
        $writer.Write([byte]10)
        Write-Int32 $writer $Definition.Blocks.Count
        foreach ($block in $Definition.Blocks) {
            Write-Block $writer $block $paletteIndices
        }

        Write-Header $writer 9 "entities"
        $writer.Write([byte]10)
        Write-Int32 $writer $Definition.Entities.Count
        foreach ($entity in $Definition.Entities) {
            Write-Entity $writer $entity
        }

        $writer.Write([byte]0)
        $writer.Flush()

        $output = Join-Path $OutputDirectory ($Name + ".nbt")
        $file = [IO.File]::Open($output, [IO.FileMode]::Create, [IO.FileAccess]::Write, [IO.FileShare]::None)
        try {
            $gzip = [IO.Compression.GZipStream]::new($file, [IO.Compression.CompressionMode]::Compress, $true)
            try {
                $bytes = $raw.ToArray()
                $gzip.Write($bytes, 0, $bytes.Length)
            } finally {
                $gzip.Dispose()
            }
        } finally {
            $file.Dispose()
        }
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $output).Hash
        Write-Host ("Generated {0} ({1} blocks, {2} entities) SHA256 {3}" -f $output, $Definition.Blocks.Count, $Definition.Entities.Count, $hash)
    } finally {
        $writer.Dispose()
        $raw.Dispose()
    }
}

$definitions = [ordered]@{
    basics = (New-BasicsStructure)
    storage = (New-StorageStructure)
    smelting = (New-SmeltingStructure)
    piston = (New-PistonStructure)
    redstone = (New-RedstoneStructure)
    render_layers = (New-RenderLayersStructure)
    fluids = (New-FluidsStructure)
    rail = (New-RailStructure)
}

$null = [IO.Directory]::CreateDirectory($OutputDirectory)
foreach ($name in $definitions.Keys) {
    Write-Structure $name $definitions[$name]
}
