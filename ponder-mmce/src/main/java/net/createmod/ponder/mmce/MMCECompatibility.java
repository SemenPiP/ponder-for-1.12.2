package net.createmod.ponder.mmce;

import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

final class MMCECompatibility {
    private MMCECompatibility() {
    }

    static boolean isSupported() {
        try {
            Class<?> registry = Class.forName(
                "hellfirepvp.modularmachinery.common.machine.MachineRegistry");
            Class<?> machine = Class.forName(
                "hellfirepvp.modularmachinery.common.machine.DynamicMachine");
            Class<?> taggedArray = Class.forName(
                "hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray");
            Class<?> blockArray = Class.forName(
                "hellfirepvp.modularmachinery.common.util.BlockArray");
            Class<?> blockInformation = Class.forName(
                "hellfirepvp.modularmachinery.common.util.BlockArray$BlockInformation");
            Class<?> dynamicPattern = Class.forName(
                "github.kasuminova.mmce.common.util.DynamicPattern");
            Class<?> blueprint = Class.forName(
                "hellfirepvp.modularmachinery.common.item.ItemBlueprint");

            require(registry, "getRegistry");
            require(registry, "getMachine", ResourceLocation.class);
            require(machine, "getPattern");
            require(machine, "getDynamicPatternByName", String.class);
            require(taggedArray, "getTaggedPositions");
            require(blockArray, "getPattern");
            require(blockInformation, "getSampleState", long.class);
            require(blockInformation, "getPreviewTag");
            require(dynamicPattern, "getMinSize");
            require(dynamicPattern, "getMaxSize");
            require(dynamicPattern, "getFaces");
            require(dynamicPattern, "addPatternToBlockArray",
                blockArray, int.class, EnumFacing.class, EnumFacing.class);
            require(blueprint, "getAssociatedMachineKey", ItemStack.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            PonderMMCE.LOGGER.error(
                "Ponder-MMCE requires the MMCE 2.3.2 structure API; compatibility disabled",
                incompatible);
            return false;
        }
    }

    private static Method require(Class<?> owner, String name, Class<?>... parameters)
        throws NoSuchMethodException {
        return owner.getMethod(name, parameters);
    }
}
