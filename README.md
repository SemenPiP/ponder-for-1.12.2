# Ponder for Minecraft 1.12.2

[![Build and Verify](https://github.com/SemenPiP/ponder-for-1.12.2/actions/workflows/build.yml/badge.svg)](https://github.com/SemenPiP/ponder-for-1.12.2/actions/workflows/build.yml)

Independent Java 8 and Forge 1.12.2 rewrite of the official Ponder visual
documentation library, including the Catnip support packages used by Ponder.
No third-party Ponder 1.12.2 backport source is copied or adapted by this
project.

The current completion gaps, release priorities, and post-1.3.0 milestones are tracked in the
[project roadmap](docs/ROADMAP.md).

The implementation and automated checks are present, but a build is not a
qualified release until every runtime gate in [docs/TESTING.md](docs/TESTING.md)
has evidence for that exact jar. In particular, a server-only smoke test does
not establish client rendering or full CatServer compatibility.

## Current verification status

Version 1.3.0 Alpha 1 adds reloadable JSON scene packs, a versioned Draft-07 schema,
an offline validator/migrator and installable JSON examples. JSON and
ZenScript compile to the same deterministic IR and continue to use snapshot
protocol v3, codec capability negotiation, synchronized script metadata,
diagnostics and structure dependency manifests. Runtime metadata accepts
MixinBooter 9.1 and newer and requires CraftTweaker 4.1.20 or newer. Every
generated built-in scene runs for exactly 32 seconds. MMCE 2.3.2+ compatibility
is included in the Ponder runtime jar and activates only when Modular
Machinery is installed. This is an Alpha release: automated build, packaging,
and dedicated-server evidence are required, while real-client acceptance
remains explicitly pending.

This workspace cannot create a usable hardware OpenGL context for visual
judgement or real mouse, fullscreen and GUI-scale testing. Standard Forge
client rendering therefore remains the release gate and is still unverified.
The installable harness, evidence checklist and hash-bound report process are
documented in
[docs/PONDER-1.3.0-CLIENT-ACCEPTANCE.md](docs/PONDER-1.3.0-CLIENT-ACCEPTANCE.md).
CatServer client support is experimental for 1.3.0 and does not block the
release line. Server-only results are recorded separately in
[docs/TESTING.md](docs/TESTING.md) and must not be interpreted as client proof.

## Runtime requirements

- Forge mod ID: `ponder_legacy`
- Content, resource and ZenScript namespace: `ponder`
- Minecraft 1.12.2
- Forge 14.23.5.2847 or newer
- Java 8
- MixinBooter 9.1 or newer
- CraftTweaker 4.1.20 or newer

Install `Ponder-1.12.2-1.3.0-alpha.1.jar`, a supported MixinBooter jar, and CraftTweaker
in the same `mods` directory. When both sides run Ponder, the client and server
must use the exact same Ponder version. A client with Ponder can still connect
to a server that does not have Ponder installed. Runtime dependencies are
deliberately not embedded.

Do not install the `-api`, `-sources`, or `-dev` jars as runtime mods. The API
jar is a deobfuscated compile-time dependency for addon development. The
default build and server qualification baseline remains MixinBooter 11.2. The
current Alpha artifact hashes are published in the GitHub Actions build job
artifact summary and the uploaded release artifact bundle; this README does not
pin a static hash for a jar that may be rebuilt.

Use `gradlew.bat build -PmixinBooterVersion=10.7` to compile and verify against
another available MixinBooter runtime API. Refmap generation keeps the 11.2
annotation processor because older releases do not carry its complete build-time
ASM classpath. Runtime metadata accepts MixinBooter 9.1 and newer; compatibility
claims should still name the versions that completed the Forge and client
acceptance matrix, and should treat CatServer as experimental support rather
than a 1.3.0 release gate.

## Build

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-1.8
gradlew.bat clean test build compileClientHarnessJava :ponder-mmce:test :ponder-mmce:build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\verify-release.ps1
```

Development launch tasks are `runClient` and `runServer`. The build uses
RetroFuturaGradle 1.4.9, Gradle 8.14.3, MCP stable_39, and Forge
14.23.5.2847. `build` already runs the tests and release-content checks and
produces the reobfuscated artifacts. Before any release compatibility claim,
the same artifacts must also pass the standard Forge server checks and real
standard Forge client gate in [docs/TESTING.md](docs/TESTING.md). CatServer
server regression remains automated experimental evidence, not a 1.3.0 release
blocker.

`build/libs/Ponder-1.12.2-1.3.0-alpha.1.jar` is the reobfuscated runtime artifact.
Developer jars are isolated under `build/devlibs` and must not be installed on
a production server. `reobfExampleAddonJar` builds the separately installable
example as `build/libs/Ponder-Example-Addon-1.12.2-1.3.0-alpha.1.jar`.

## Built-in demonstrations

Use `/ponder <component id>` with `minecraft:crafting_table`, `minecraft:chest`,
`minecraft:furnace`, `minecraft:piston`, `minecraft:redstone_lamp`,
`minecraft:glass`, `minecraft:water_bucket`, or `minecraft:rail`. Use
`/ponder index` for the component index and `/ponder tags` for categories.
The built-in scenes first reveal the 5x5 floor and then the upper structure.
Scripted books keep a fixed position while retaining their slow item rotation.
In a container screen, hover a registered item and hold the displayed Ponder
key to open its scene. The default is `W` and can be rebound in Controls.

On first launch Ponder writes eight editable scripts to
`scripts/ponder/builtin`. If
`config/ponder/builtin-zs-generated.properties` exists, the installer leaves
that directory untouched. If the marker is deleted, Ponder first moves the
existing builtin folder to a timestamped backup, then atomically restores all
eight scripts from the bundled resources before writing a fresh marker. A
failed copy or marker write rolls the old builtin tree back into place.

ZenScript scenes belong under `scripts/ponder/scenes`. Reloadable JSON packs
belong under `scripts/ponder/packs/**/*.ponder.json`. Custom structure NBT belongs
under `scripts/ponder/structures/<namespace>/<path>.nbt` and is addressed as
`<namespace>:<path>`. External structures override resource-pack and mod-jar
resources. ZenScript changes require a restart; `/ponder reload` rescans JSON
packs, reapplies compiled definitions and invalidates structure caches. Servers
synchronize validated scene instructions, not source files or structure NBT.
See [docs/JSON-PACKS.md](docs/JSON-PACKS.md),
[docs/ZENSCRIPT-API.md](docs/ZENSCRIPT-API.md) and
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Diagnostic commands

`/ponder list [local|server|effective]`, `/ponder inspect <scene>
[local|server|effective]`, `/ponder validate [local|server|effective]`, `/ponder
export <scene> [ir|timeline|all] [local|server|effective]`, and `/ponder sync
status [player]` expose the diagnostic surface. `/ponder dependencies
[local|server|effective]` writes a versioned JSON structure dependency
manifest and summarizes provider, fingerprint and codec compatibility state.

`local` means the client registry assembled from Java plugins, builtin ZS,
local ZS and local JSON. `server` means the validated server snapshot received over sync.
`effective` means the merged active view: same-ID server snapshot script scenes
override local script scenes, while Java plugin scenes stay local.
On a dedicated-server console, `local` is the complete local registration,
`server` is the synchronizable non-client-only ZS set, and `effective` is the
current server registration.

`/ponder reload` and `/ponder sync status` require permission level 2 when run
on the server. In-game player requests for `list`, `inspect`, `validate`, and
`export` are relayed to the client diagnostics service; if a non-player sender
uses `validate` or `export`, the same permission gate applies.

Validation reports and export files are written under
`logs/ponder/diagnostics`. Java storyboard scenes do not have exportable script
IR, so `export ... ir` only works for script-backed scenes; timeline exports
remain available for Java and script scenes.

The checked-in ZenScript examples package is built as
`build/distributions/Ponder-ZenScript-Examples-1.3.0-alpha.1.zip`.
The JSON example and author-tool packages are built as
`Ponder-JSON-Examples-1.3.0-alpha.1.zip` and `Ponder-JSON-Tools-1.3.0-alpha.1.zip`.
`Ponder-Client-Acceptance-Kit-1.3.0-alpha.1.zip` contains the reobfuscated client
harness, JSON fixture and report generator used to bind real-client evidence
to one successful main Actions run and its exact Ponder SHA-256.

## Addon development

Ponder plugins implement `net.createmod.ponder.api.registration.PonderPlugin`
and register Java storyboards during mod initialization. See
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) and the separately built example
plugin under `examples/addon`.

## Integrated MMCE compatibility

The Ponder runtime jar contains an optional MMCE 2.3.2+ compatibility layer.
When `modularmachinery` is absent, none of its MMCE-dependent implementation
classes are loaded. When MMCE is present and its expected ABI is available,
Ponder registers the structure provider, blueprint subject resolver, and
`mods.ponder.mmce.MMCEStructures` ZenScript namespace automatically.

No separate Ponder-MMCE runtime jar is required. The `ponder-mmce` Gradle
subproject remains as an isolated source, fixture, and harness boundary. If
the legacy external `ponder_mmce` addon is also installed, Ponder leaves
compatibility ownership to it and avoids duplicate provider or resolver
registration.

The integrated layer supports static structures, Dynamic Pattern expansion,
negative-coordinate normalization, preview NBT, named groups, fingerprints,
and blueprint-to-component mapping. It does not generate generic scenes and
does not yet provide material lists, recipe overlays, or placed-controller
entry points.

The same build produces the verification and author example pack
`ponder-mmce/build/distributions/Ponder-MMCE-Smoke-Pack-0.1.0-alpha.zip`.
It contains two authored scenes backed by real static/dynamic MMCE machine
definitions and a third machine with no scene. The pack is both an installable
author example and the input used by the real Forge fixture.

The example Java addon also produces
`build/distributions/Ponder-Example-Addon-Smoke-1.3.0-alpha.1.zip`. It contains the
reobfuscated addon, a structure and a ZS scene that invokes the
`ponder_example:pulse` custom codec. The pack verifies ServiceLoader and IMC
plugin discovery, successful protocol-v3 negotiation, and rejection before
snapshot transfer when a client lacks the codec.

## Compatibility policy

The public package and DSL names follow current Ponder where Minecraft 1.12.2
has an equivalent. Minecraft and Forge types are mapped to their MCP 1.12.2
counterparts. Modern-only systems use functional 1.12.2 adapters rather than
empty compatibility stubs. This is source migration compatibility, not binary
compatibility with a 1.21.1 jar or a promise that every scene compiles without
type substitutions. See [docs/API-COMPATIBILITY.md](docs/API-COMPATIBILITY.md).
The checked `api-signatures/ponder-api-1.2.0.sig` baseline and exact 1.3.0
snapshot are generated from the real API jar by JDK 8 `javap`; CI rejects
binary-breaking changes and unreviewed current-signature drift.

Only Forge 1.12.2 is in scope. Fabric, NeoForge, Cleanroom-specific behavior
and unrelated third-party scene formats are not provided. The CatServer path is
experimental for 1.3.0 and does not block the release line. The standard Forge
real-client gate remains the release requirement.
