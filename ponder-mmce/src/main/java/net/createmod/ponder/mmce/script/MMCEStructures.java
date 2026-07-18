package net.createmod.ponder.mmce.script;

import crafttweaker.annotations.ZenRegister;
import net.createmod.ponder.mmce.structure.MMCEStructureProvider;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.mmce.MMCEStructures")
public final class MMCEStructures {
    private MMCEStructures() {
    }

    @ZenMethod
    public static MMCEStructureRef machine(String machineId) {
        return staticStructure(machineId, true);
    }

    @ZenMethod
    public static MMCEStructureRef staticStructure(String machineId, boolean includePreviewNbt) {
        return MMCEStructureProvider.INSTANCE.createStaticReference(machineId, includePreviewNbt);
    }

    @ZenMethod
    public static MMCEStructureRef dynamic(String machineId, String dynamicPattern, int repetitions,
                                           String patternOffset, String facing) {
        return dynamic(machineId, dynamicPattern, repetitions, patternOffset, facing, true);
    }

    @ZenMethod
    public static MMCEStructureRef dynamic(String machineId, String dynamicPattern, int repetitions,
                                           String patternOffset, String facing,
                                           boolean includePreviewNbt) {
        return MMCEStructureProvider.INSTANCE.createDynamicReference(machineId, dynamicPattern, repetitions,
            patternOffset, facing, includePreviewNbt);
    }
}
