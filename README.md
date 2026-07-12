# Ponder for Minecraft 1.12.2

Independent Java 8 and Forge 1.12.2 rewrite of the official Ponder visual
documentation library, including the Catnip support packages used by Ponder.
No third-party Ponder 1.12.2 backport source is copied or adapted by this
project.

The implementation and automated checks are present, but a build is not a
qualified release until every runtime gate in [docs/TESTING.md](docs/TESTING.md)
has evidence for that exact jar. In particular, a server-only smoke test does
not establish client rendering or full CatServer compatibility.

## Current verification status

Version 1.0.2 retains the 1.0.1 depth-buffer fix and eight built-in vanilla
demonstrations, stabilizes scripted item entities, and reveals ordinary base
plates and sections one block at a time. Every built-in scene now runs for
exactly 32 seconds.
Historical 1.0.0/MixinBooter 11.5 reports remain historical and do not qualify
the current 1.0.2 artifact.

This workspace cannot create a usable hardware OpenGL context for visual
judgement or real mouse, fullscreen and GUI-scale testing. Standard Forge
client rendering therefore remains unverified and the complete client sign-off
is not yet satisfied. A real CatServer client connection was not required for
this round. Server-only results are recorded separately in
[docs/TESTING.md](docs/TESTING.md) and must not be interpreted as client proof.

## Runtime requirements

- Minecraft 1.12.2
- Forge 14.23.5.2847 or newer
- Java 8
- MixinBooter 11.2 (exact version)

Install `Ponder-1.12.2-1.0.2.jar` and the separate MixinBooter 11.2 jar in the
same `mods` directory. MixinBooter is deliberately not embedded.

Do not install the `-api`, `-sources`, or `-dev` jars as runtime mods. The API
jar is a deobfuscated compile-time dependency for addon development. The
supported MixinBooter 11.2 artifact has SHA-256
`48667BC07D4F9D54A5C0F808DAA02DEB956128664DB24269EB34460F4CA2462E`.
The build resolves it from the
[CleanroomMC Maven artifact](https://maven.cleanroommc.com/zone/rong/mixinbooter/11.2/mixinbooter-11.2.jar);
verify the hash before installing a manually downloaded copy.

## Build

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-1.8
gradlew.bat clean test build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\verify-release.ps1
```

Development launch tasks are `runClient` and `runServer`. The build uses
RetroFuturaGradle 1.4.9, Gradle 8.14.3, MCP stable_39, and Forge
14.23.5.2847. `build` already runs the tests and release-content checks and
produces the reobfuscated artifacts. Before any release compatibility claim,
the same artifacts must also pass the standard Forge and CatServer gates in
[docs/TESTING.md](docs/TESTING.md).

`build/libs/Ponder-1.12.2-1.0.2.jar` is the reobfuscated runtime artifact.
Developer jars are isolated under `build/devlibs` and must not be installed on
a production server. `reobfExampleAddonJar` builds the separately installable
example as `build/libs/Ponder-Example-Addon-1.12.2-1.0.2.jar`.

## Built-in demonstrations

Use `/ponder <component id>` with `minecraft:crafting_table`, `minecraft:chest`,
`minecraft:furnace`, `minecraft:piston`, `minecraft:redstone_lamp`,
`minecraft:glass`, `minecraft:water_bucket`, or `minecraft:rail`. Use
`/ponder index` for the component index and `/ponder tags` for categories.
The built-in scenes first reveal the 5x5 floor and then the upper structure.
Scripted books keep a fixed position while retaining their slow item rotation.

## Addon development

Ponder plugins implement `net.createmod.ponder.api.registration.PonderPlugin`
and register Java storyboards during mod initialization. See
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) and the separately built example
plugin under `examples/addon`.

## Compatibility policy

The public package and DSL names follow current Ponder where Minecraft 1.12.2
has an equivalent. Minecraft and Forge types are mapped to their MCP 1.12.2
counterparts. Modern-only systems use functional 1.12.2 adapters rather than
empty compatibility stubs. This is source migration compatibility, not binary
compatibility with a 1.21.1 jar or a promise that every scene compiles without
type substitutions. See [docs/API-COMPATIBILITY.md](docs/API-COMPATIBILITY.md).

Only Forge 1.12.2 is in scope. Fabric, NeoForge, Cleanroom-specific behavior,
third-party JSON scene formats and third-party backport source compatibility
are not provided. The CatServer claim is limited to the exact build whose
SHA-256 is documented in [docs/TESTING.md](docs/TESTING.md).
