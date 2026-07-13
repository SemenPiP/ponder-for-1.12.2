package net.createmod.ponder.script;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ScriptSceneLimitsTest {
    @Test
    public void rejectsUnknownInstructionOperation() {
        try {
            new ScriptInstruction("not_a_real_instruction", new NBTTagCompound());
            fail("Unknown instructions must not survive snapshot decoding");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Unknown"));
        }
    }

    @Test
    public void rejectsOversizedInstructionData() {
        NBTTagCompound data = new NBTTagCompound();
        data.setByteArray("nbt", new byte[ScriptInstruction.MAX_DATA_BYTES + 1]);
        try {
            new ScriptInstruction("tile_nbt", data);
            fail("Oversized instruction data must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("exceeds"));
        }
    }

    @Test
    public void rejectsOversizedIndividualScene() throws Exception {
        List<ScriptInstruction> instructions = new ArrayList<ScriptInstruction>();
        for (int i = 0; i < 5; i++) {
            NBTTagCompound data = new NBTTagCompound();
            data.setTag("selection", ScriptSelection.position(0, 0, 0).serialize());
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setByteArray("payload", new byte[240 * 1024]);
            data.setTag("nbt", nbt);
            data.setBoolean("replace", false);
            data.setBoolean("redraw", false);
            instructions.add(new ScriptInstruction("tile_nbt", data));
        }
        ScriptSceneDefinition scene = new ScriptSceneDefinition(
            new ResourceLocation("minecraft", "stone"),
            new ResourceLocation("test", "oversized"),
            "Oversized",
            new ResourceLocation("test", "oversized"),
            Collections.<ResourceLocation>emptyList(),
            instructions,
            false);
        try {
            ScriptSceneSnapshot.encode(Collections.singletonList(scene));
            fail("A scene larger than one MiB must be rejected");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("Scene test:oversized"));
        }
    }
}
