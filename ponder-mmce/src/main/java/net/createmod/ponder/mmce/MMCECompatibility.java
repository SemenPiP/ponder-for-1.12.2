package net.createmod.ponder.mmce;

import java.lang.reflect.Method;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

final class MMCECompatibility {
    static final String FORCE_INCOMPATIBLE_ABI_PROPERTY =
        "ponder.mmce.verification.forceIncompatibleAbi";

    private MMCECompatibility() {
    }

    static boolean isSupported() {
        if (Boolean.getBoolean(FORCE_INCOMPATIBLE_ABI_PROPERTY)) {
            return check(() -> {
                throw new NoSuchMethodException("forced incompatible MMCE ABI verification");
            }, true);
        }
        return check(MMCECompatibility::verifyRuntime, true);
    }

    static boolean check(CompatibilityProbe probe, boolean logFailure) {
        try {
            probe.verify();
            return true;
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            if (logFailure) {
                PonderMMCE.LOGGER.error(
                    "Ponder-MMCE requires the MMCE 2.3.2 structure API; compatibility disabled",
                    incompatible);
            }
            return false;
        }
    }

    private static void verifyRuntime() throws ReflectiveOperationException {
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
        Class<?> machineLoader = Class.forName(
            "hellfirepvp.modularmachinery.common.machine.MachineLoader");
        Class<?> commonProxy = Class.forName(
            "hellfirepvp.modularmachinery.common.CommonProxy");
        Class<?> dataHolder = Class.forName(
            "hellfirepvp.modularmachinery.common.data.ModDataHolder");

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
        require(dynamicPattern, "getName");
        require(dynamicPattern, "getPattern");
        require(dynamicPattern, "getPatternEnd");
        require(dynamicPattern, "getStructureSizeOffsetStart", EnumFacing.class);
        require(dynamicPattern, "getStructureSizeOffset", EnumFacing.class);
        require(taggedArray, "rotateYCCW");
        require(taggedArray, "addBlock",
            net.minecraft.util.math.BlockPos.class, blockInformation);
        require(taggedArray, "setTag", net.minecraft.util.math.BlockPos.class,
            Class.forName("hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag"));
        require(blueprint, "getAssociatedMachineKey", ItemStack.class);
        require(machineLoader, "discoverDirectory", File.class);
        require(machineLoader, "prepareContext", List.class);
        require(machineLoader, "registerMachines", Collection.class);
        require(machineLoader, "loadMachines", Collection.class);
        require(machineLoader, "captureFailedAttempts");
        requireField(machineLoader, "VARIABLE_CONTEXT", Map.class);
        requireField(commonProxy, "dataHolder", dataHolder);
        require(dataHolder, "getMachineryDirectory");
    }

    private static Method require(Class<?> owner, String name, Class<?>... parameters)
        throws NoSuchMethodException {
        return owner.getMethod(name, parameters);
    }

    private static void requireField(Class<?> owner, String name, Class<?> type)
        throws NoSuchFieldException {
        if (!type.isAssignableFrom(owner.getField(name).getType()))
            throw new NoSuchFieldException(owner.getName() + "." + name + " has an incompatible type");
    }

    interface CompatibilityProbe {
        void verify() throws ReflectiveOperationException;
    }
}
