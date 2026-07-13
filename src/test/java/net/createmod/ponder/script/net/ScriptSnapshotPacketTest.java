package net.createmod.ponder.script.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.minecraft.util.ResourceLocation;

public class ScriptSnapshotPacketTest {
    @Test
    public void beginPacketRoundTripsProtocolAndRequiredCodecs() throws Exception {
        ClientboundScriptSnapshotBeginPacket source = new ClientboundScriptSnapshotBeginPacket(
            42, ScriptSceneSnapshot.PROTOCOL, 3, 1234, 5678, new byte[32],
            Arrays.asList(new ResourceLocation("example", "first"), new ResourceLocation("example", "second")));
        ByteBuf bytes = Unpooled.buffer();
        source.toBytes(bytes);
        ClientboundScriptSnapshotBeginPacket decoded = new ClientboundScriptSnapshotBeginPacket();
        decoded.fromBytes(bytes);

        assertEquals(42, integer(decoded, "transferId"));
        assertEquals(ScriptSceneSnapshot.PROTOCOL, integer(decoded, "protocol"));
        assertEquals(2, ((java.util.List<?>) field(decoded, "requiredCodecs")).size());
    }

    @Test
    public void resultPacketSanitizesAndBoundsMessage() throws Exception {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < 1200; i++) message.append('x');
        message.append('\n');
        ServerboundScriptSnapshotResultPacket source =
            new ServerboundScriptSnapshotResultPacket(9, false, message.toString());
        ByteBuf bytes = Unpooled.buffer();
        source.toBytes(bytes);
        ServerboundScriptSnapshotResultPacket decoded = new ServerboundScriptSnapshotResultPacket();
        decoded.fromBytes(bytes);

        assertEquals(9, integer(decoded, "transferId"));
        assertFalse((Boolean) field(decoded, "accepted"));
        String decodedMessage = (String) field(decoded, "message");
        assertEquals(1024, decodedMessage.length());
        assertTrue(decodedMessage.indexOf('\n') < 0);
    }

    private static int integer(Object target, String name) throws Exception {
        return ((Integer) field(target, name)).intValue();
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
