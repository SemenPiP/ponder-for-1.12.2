package com.example.ponderaddon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;

public class ExamplePulseCodecTest {
    @Test
    public void codecDeclaresStableVersionAndCapability() {
        ExamplePulseCodec codec = new ExamplePulseCodec();
        assertEquals(1, codec.getProtocolVersion());
        assertTrue(codec.getCapabilities().contains(ExamplePulseCodec.OUTLINE_CAPABILITY));
        assertEquals(codec.getCapabilities(),
            codec.getRequiredCapabilities(validPayload()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void codecRejectsUnknownPalette() {
        NBTTagCompound payload = validPayload();
        payload.setString("color", "ultraviolet");
        new ExamplePulseCodec().validate(payload);
    }

    private static NBTTagCompound validPayload() {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("x", 2);
        payload.setInteger("y", 1);
        payload.setInteger("z", 2);
        payload.setInteger("duration", 40);
        payload.setString("color", "green");
        return payload;
    }
}
