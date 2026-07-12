package net.createmod.ponder.foundation.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

public class LegacyStateResolverTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    public void mapsFlattenedWoodVariant() {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("Name", "minecraft:spruce_planks");
        LegacyStateResolver.Resolution resolution = new LegacyStateResolver().resolve(entry);
        assertEquals(Blocks.PLANKS, resolution.getState().getBlock());
        assertFalse(resolution.isSubstituted());
    }

    @Test
    public void unknownModernBlockIsVisibleBarrier() {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("Name", "minecraft:nonexistent_future_block");
        LegacyStateResolver.Resolution resolution = new LegacyStateResolver().resolve(entry);
        assertEquals(Blocks.BARRIER, resolution.getState().getBlock());
        assertTrue(resolution.isSubstituted());
    }
}
