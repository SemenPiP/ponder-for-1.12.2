package net.createmod.ponder.mmce.subject;

import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import net.createmod.ponder.api.subject.ItemSubjectResolver;
import net.createmod.ponder.api.subject.SubjectResolverResult;
import net.createmod.ponder.mmce.PonderMMCE;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public final class MMCEBlueprintResolver implements ItemSubjectResolver {
    public static final MMCEBlueprintResolver INSTANCE = new MMCEBlueprintResolver();
    public static final int PRIORITY = 200;

    private MMCEBlueprintResolver() {
    }

    public ResourceLocation getId() {
        return PonderMMCE.BLUEPRINT_RESOLVER_ID;
    }

    public int getPriority() {
        return PRIORITY;
    }

    public boolean supports(ItemStack subject) {
        return subject != null && subject.getItem() instanceof ItemBlueprint;
    }

    @Override
    public SubjectResolverResult resolve(ItemStack subject) {
        if (!supports(subject)) return SubjectResolverResult.pass();
        try {
            return resolveMachine(ItemBlueprint.getAssociatedMachineKey(subject));
        } catch (RuntimeException | LinkageError invalid) {
            PonderMMCE.LOGGER.warn("Could not resolve MMCE blueprint Ponder subject", invalid);
            return SubjectResolverResult.pass();
        }
    }

    static SubjectResolverResult resolveBlueprintTag(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(ItemBlueprint.DYNAMIC_MACHINE_NBT_KEY, 8))
            return SubjectResolverResult.pass();
        String machine = tag.getString(ItemBlueprint.DYNAMIC_MACHINE_NBT_KEY);
        if (machine == null || machine.trim().isEmpty()) return SubjectResolverResult.pass();
        try {
            return resolveMachine(new ResourceLocation(machine));
        } catch (IllegalArgumentException invalid) {
            return SubjectResolverResult.pass();
        }
    }

    private static SubjectResolverResult resolveMachine(ResourceLocation machine) {
        return machine == null
            ? SubjectResolverResult.pass()
            : SubjectResolverResult.handled(MMCEStructureRef.componentId(machine));
    }
}
