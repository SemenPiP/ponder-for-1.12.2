package net.createmod.ponder.mmce.subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import net.createmod.ponder.api.subject.SubjectResolverResult;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class MMCEBlueprintResolverTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void resolvesBlueprintNbtWithoutLookingUpOrExecutingTheMachine() {
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setString(ItemBlueprint.DYNAMIC_MACHINE_NBT_KEY,
            "modularmachinery:assembly_line");

        SubjectResolverResult result = MMCEBlueprintResolver.resolveBlueprintTag(tag);
        assertTrue(result.isHandled());
        assertEquals(new ResourceLocation("ponder_mmce", "machine/modularmachinery/assembly_line"),
            result.getComponent());
    }

    @Test
    public void ignoresNonBlueprintAndUnboundBlueprintStacks() {
        assertFalse(MMCEBlueprintResolver.INSTANCE.supports(new ItemStack(Items.STICK)));
        assertFalse(MMCEBlueprintResolver.resolveBlueprintTag(null).isHandled());
        assertFalse(MMCEBlueprintResolver.resolveBlueprintTag(
            new net.minecraft.nbt.NBTTagCompound()).isHandled());
    }
}
