# Ponder-MMCE

Ponder-MMCE is an independent Minecraft 1.12.2 addon that exposes Modular
Machinery: Community Edition machine structures to Ponder Legacy.

## Build dependency

The normal build resolves MMCE 2.3.2 from Curse Maven:

```text
curse.maven:modularmachinery-community-edition-817377:7372951
```

For offline inspection or a patched development jar, override it explicitly:

```powershell
.\gradlew.bat :ponder-mmce:check "-PmmceJar=D:\dependencies\ModularMachinery-CE-2.3.2.jar"
```

Keep an override outside the repository `build` directories when running
`clean`. The MMCE and CraftTweaker jars are compile-only and are never embedded.

## ZenScript

The short forms enable preview NBT. Explicit overloads remain available when
an author needs to disable it.

```zenscript
val fixed = mods.ponder.mmce.MMCEStructures.machine(
    "modularmachinery:alloy_furnace"
);

val extended = mods.ponder.mmce.MMCEStructures.dynamic(
    "modularmachinery:assembly_line",
    "middle",
    4,
    "north",
    "north"
);
```

`MMCEStructureRef.component` is the synthetic scene component and
`MMCEStructureRef.structure` is the stable structure ID. The final path segment
is a SHA-256 fingerprint of the sampled structure, preview NBT, selector groups,
and explicit dynamic parameters.

Static structures use `DynamicMachine#getPattern()`. Dynamic structures copy
the static pattern and deterministically expand one named MMCE dynamic pattern
from its pattern, end pattern, offsets, facing, and size limits. This avoids
depending on MMCE's late-built rotation cache during CraftTweaker execution.
The adapter samples each original position with `getSampleState(pos.toLong())`,
copies preview NBT, normalizes negative coordinates, exports MMCE selector tags
as named groups, and does not invoke machine event handlers, NBT checkers, or
CraftTweaker callbacks.

## Core API integration

`MMCEStructureProvider` directly implements
`net.createmod.ponder.api.structure.PonderStructureProvider` and publishes NBT
bytes, a content fingerprint, normalized named groups, and diagnostics through
`PonderStructureProviderResult`.

`MMCEBlueprintResolver` implements the item resolver contract and maps a bound
MMCE blueprint to the same synthetic component ID used by authored scenes.

## Installable smoke pack

`:ponder-mmce:build` also creates:

```text
ponder-mmce/build/distributions/Ponder-MMCE-Smoke-Pack-0.1.0-alpha.zip
```

The archive contains three real MMCE 2.3.2 machine definitions and one Ponder
ZenScript file. The static and dynamic machines have authored scenes. The
third machine deliberately has no scene so the blueprint path can verify the
localized no-scene message. The example files under `examples/smoke` are the
same files installed by the automated Forge fixture.

## Verification harnesses

The build produces development-only reobfuscated harness jars under:

```text
ponder-mmce/build/verification/server-harness
ponder-mmce/build/verification/client-harness
```

The server harness validates real MMCE registration, named groups, preview NBT,
blueprint resolution, fingerprint mismatch isolation, and an ABI-incompatible
startup where the addon disables its Provider and Resolver without crashing the
server. The client harness opens both authored scenes, checks their structure
groups, rejects a stale fingerprint, captures nonblank screenshots, and records
exact Ponder and Ponder-MMCE hashes. It does not replace the manual keyboard,
mouse, GUI scale, fullscreen, and resource reload acceptance described in
`docs/PONDER-MMCE-ALPHA-ACCEPTANCE.md`.
