# Ponder JSON scene packs

Ponder Legacy 1.3.0 adds a reloadable JSON authoring frontend. JSON and
ZenScript compile to the same immutable scene IR, use the same validator and
use snapshot protocol v3.

## Directory layout

```text
scripts/ponder/
├─ packs/
│  └─ **/*.ponder.json
└─ structures/
   └─ <namespace>/<path>.nbt
```

The loader scans pack files recursively in normalized path order. A pack may
contain scenes, tags and component associations, shared text and index
exclusions. Structure NBT remains external to the JSON file.

## Reload behavior

- `/ponder reload` reloads JSON packs and structure caches, then resends the
  server snapshot.
- ZenScript remains startup-only. Editing `.zs` files still requires a client
  or server restart.
- Each JSON file has its own last-known-good contribution. If an edited file is
  invalid, its previous valid contribution remains active while unrelated
  valid files update normally.
- Deleting a file removes its contribution.
- JSON may not replace a ZenScript scene, tag or shared-text key with the same
  ID. Conflicting JSON files are rejected per file.
- On a connected client, reload clears the previous server layer, refreshes
  local JSON and then atomically applies the newly received server snapshot.

## Pack envelope

```json
{
  "$schema": "ponder:schemas/ponder-pack-v1",
  "format": 1,
  "id": "mypack:machines",
  "tags": [],
  "sharedText": {},
  "indexExclusions": [],
  "scenes": []
}
```

Scene instructions use stable, human-facing names such as
`scene.idle`, `world.show_section`, `overlay.show_text` and
`effects.indicate_success`. Internal underscore-only IR names are not accepted.
The versioned operation contract is
`schemas/ponder-operations-v1.json`; the Draft-07 schema is
`schemas/ponder-pack-v1.schema.json`.

Selections are JSON objects:

```json
{ "type": "position", "pos": [2, 1, 2] }
{ "type": "from_to", "from": [1, 1, 1], "to": [3, 2, 3] }
{ "type": "layers", "y": 1, "height": 2 }
{ "type": "structure_group", "name": "mmce:controller" }
```

Tile NBT and custom codec payloads are SNBT compound strings. They must use a
compound envelope, for example:

```json
{
  "op": "world.tile_nbt",
  "selection": { "type": "position", "pos": [3, 1, 2] },
  "nbt": "{Lock:\"mypack\"}",
  "replace": false,
  "redraw": true
}
```

## Offline tool

Validate a pack without starting Minecraft:

```powershell
.\tools\ponder-json.ps1 validate .\scripts\ponder\packs\demo.ponder.json
```

Write a separate canonical copy:

```powershell
.\tools\ponder-json.ps1 migrate .\demo.ponder.json `
  -OutputPath .\demo.migrated.ponder.json
```

`migrate` never overwrites the input file. The offline tool validates the pack
envelope, required operation fields, selection shapes, resource IDs, limits and
the SNBT compound envelope. Minecraft remains the authoritative validator for
full SNBT syntax, block states, enum values, handle lifecycles and custom
instruction codecs.

The installable example is built as
`build/distributions/Ponder-JSON-Examples-1.3.0.zip`. The standalone schema and
PowerShell author tools are built as
`build/distributions/Ponder-JSON-Tools-1.3.0.zip`.
