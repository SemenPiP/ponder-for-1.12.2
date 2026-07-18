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
the static pattern and expand one named MMCE dynamic pattern with
`DynamicPattern#addPatternToBlockArray(...)`. The adapter samples each original
position with `getSampleState(pos.toLong())`, copies preview NBT, normalizes
negative coordinates, exports MMCE selector tags as named groups, and does not
invoke machine event handlers, NBT checkers, or CraftTweaker callbacks.

## Core API integration

`MMCEStructureProvider` directly implements
`net.createmod.ponder.api.structure.PonderStructureProvider` and publishes NBT
bytes, a content fingerprint, normalized named groups, and diagnostics through
`PonderStructureProviderResult`.

`MMCEBlueprintResolver` implements the item resolver contract and maps a bound
MMCE blueprint to the same synthetic component ID used by authored scenes.
