package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.createmod.ponder.api.subject.ItemSubjectResolver;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.api.subject.SubjectResolverResult;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class PonderTooltipHandlerTest {
    @Test
    public void heldInputAdvancesAndClampsProgress() {
        assertEquals(.09f, PonderTooltipHandler.advanceProgress(0, true), .0001f);
        assertEquals(1, PonderTooltipHandler.advanceProgress(.96f, true), .0001f);
    }

    @Test
    public void releasedInputDecaysAndClampsProgress() {
        assertEquals(.38f, PonderTooltipHandler.advanceProgress(.5f, false), .0001f);
        assertEquals(0, PonderTooltipHandler.advanceProgress(.05f, false), .0001f);
    }

    @Test
    public void progressBarAlwaysHasTenSegments() {
        String bar = PonderTooltipHandler.progressBar(.5f);
        assertEquals(10, bar.length() - bar.replace("|", "").length());
        assertTrue(bar.endsWith("]"));
    }

    @Test
    public void missingSceneMessageOnlyAppliesToCustomHandledSubjects() {
        Bootstrap.register();
        PonderSubjectResolvers.register(new ResourceLocation("test", "tooltip_missing"), 0,
            new ItemSubjectResolver() {
                @Override
                public SubjectResolverResult resolve(ItemStack stack) {
                    return stack.getItem() == Items.FEATHER
                        ? SubjectResolverResult.handled(new ResourceLocation("test", "missing_scene"))
                        : SubjectResolverResult.pass();
                }
            });

        ResolvedPonderSubject custom =
            PonderSubjectResolvers.resolve(new ItemStack(Items.FEATHER));
        ResolvedPonderSubject ordinary =
            PonderSubjectResolvers.resolve(new ItemStack(Items.COAL));

        assertTrue(PonderTooltipHandler.shouldShowMissingSceneMessage(custom, false));
        assertFalse(PonderTooltipHandler.shouldShowMissingSceneMessage(custom, true));
        assertFalse(PonderTooltipHandler.shouldShowMissingSceneMessage(ordinary, false));
    }

    @Test
    public void trackedSubjectIdentityIncludesComponentMetadataAndNbt() {
        Bootstrap.register();
        ResourceLocation component = new ResourceLocation("test", "component");
        ItemStack original = new ItemStack(Items.DYE, 1, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("subject", "first");
        original.setTagCompound(tag);

        assertTrue(PonderTooltipHandler.isSameSubject(original, component, original.copy(), component));
        assertFalse(PonderTooltipHandler.isSameSubject(original, component,
            new ItemStack(Items.DYE, 1, 2), component));
        assertFalse(PonderTooltipHandler.isSameSubject(original, component,
            original.copy(), new ResourceLocation("test", "other")));

        ItemStack differentNbt = original.copy();
        differentNbt.getTagCompound().setString("subject", "second");
        assertFalse(PonderTooltipHandler.isSameSubject(original, component, differentNbt, component));
    }
}
