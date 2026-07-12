package net.createmod.catnip.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.packets.ClientboundConfigPacket;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.createmod.catnip.net.packets.ServerboundConfigPacket;

public class PacketSerializationTest {
    @Test
    public void configPacketsRoundTripTheirWireRepresentation() {
        assertRoundTrip(new ClientboundConfigPacket("ponder:client.client.editing_mode", "true"),
            new ClientboundConfigPacket());
        assertRoundTrip(new ServerboundConfigPacket("ponder", "ponder:common.general.speed", "1.25"),
            new ServerboundConfigPacket());
    }

    @Test
    public void simpleActionRoundTripsActionAndValue() {
        ClientboundSimpleActionPacket source = new ClientboundSimpleActionPacket("ponder.reload", "all");
        ByteBuf encoded = Unpooled.buffer();
        ByteBuf input = null;
        ByteBuf reencoded = Unpooled.buffer();
        try {
            source.toBytes(encoded);
            input = encoded.copy();
            ClientboundSimpleActionPacket decoded = new ClientboundSimpleActionPacket();
            decoded.fromBytes(input);
            assertFalse(input.isReadable());
            assertEquals("ponder.reload", decoded.getAction());
            assertEquals("all", decoded.getValue());
            decoded.toBytes(reencoded);
            assertEquals(encoded, reencoded);
        } finally {
            if (input != null) input.release();
            encoded.release();
            reencoded.release();
        }
    }

    @Test
    public void packetDecodersRejectOversizedAndTruncatedStrings() {
        assertRejectedLength(new ClientboundConfigPacket(), 513);
        assertRejectedLength(new ServerboundConfigPacket(), 65);
        assertRejectedLength(new ClientboundSimpleActionPacket(), 65);

        assertRejectedClientboundConfigValue();
        assertRejectedServerboundConfigPath();
        assertRejectedServerboundConfigValue();
        assertRejectedSimpleActionValue();

        ByteBuf truncated = Unpooled.buffer();
        try {
            CatnipStreamCodecBuilders.writeVarInt(truncated, 3);
            truncated.writeByte('x');
            try {
                new ClientboundSimpleActionPacket().fromBytes(truncated);
                fail("Expected truncated packet data to be rejected");
            } catch (IllegalArgumentException expected) {
                // Expected: the declared UTF-8 payload is longer than the readable bytes.
            }
        } finally {
            truncated.release();
        }
    }

    @Test
    public void packetEncodersRejectEveryOversizedField() {
        assertEncodeRejected(new ClientboundConfigPacket(repeat('p', 513), "x"));
        assertEncodeRejected(new ClientboundConfigPacket("ponder:client", repeat('v', 4097)));
        assertEncodeRejected(new ServerboundConfigPacket(repeat('m', 65), "ponder:common.x", "x"));
        assertEncodeRejected(new ServerboundConfigPacket("ponder", repeat('p', 513), "x"));
        assertEncodeRejected(new ServerboundConfigPacket("ponder", "ponder:common.x", repeat('v', 4097)));
        try {
            new ClientboundSimpleActionPacket("ponder.reload", repeat('v', 4097));
            fail("Oversized action value was accepted by the constructor");
        } catch (IllegalArgumentException expected) {
            // Constructor validation prevents retaining an invalid packet instance.
        }
    }

    private static void assertRoundTrip(BasePacketPayload source, BasePacketPayload decoded) {
        ByteBuf encoded = Unpooled.buffer();
        ByteBuf input = null;
        ByteBuf reencoded = Unpooled.buffer();
        try {
            source.toBytes(encoded);
            input = encoded.copy();
            decoded.fromBytes(input);
            assertFalse(input.isReadable());
            decoded.toBytes(reencoded);
            assertEquals(encoded, reencoded);
        } finally {
            if (input != null) input.release();
            encoded.release();
            reencoded.release();
        }
    }

    private static void assertRejectedLength(BasePacketPayload packet, int declaredLength) {
        ByteBuf malicious = Unpooled.buffer();
        try {
            CatnipStreamCodecBuilders.writeVarInt(malicious, declaredLength);
            try {
                packet.fromBytes(malicious);
                fail("Expected declared string length " + declaredLength + " to be rejected");
            } catch (IllegalArgumentException expected) {
                // Expected: every packet string has a fixed byte limit.
            }
        } finally {
            malicious.release();
        }
    }

    private static void assertRejectedClientboundConfigValue() {
        ByteBuf malicious = Unpooled.buffer();
        try {
            CatnipStreamCodecBuilders.string(512).encode(malicious, "ponder:client");
            CatnipStreamCodecBuilders.writeVarInt(malicious, 4097);
            assertDecodeRejected(new ClientboundConfigPacket(), malicious);
        } finally {
            malicious.release();
        }
    }

    private static void assertRejectedServerboundConfigPath() {
        ByteBuf malicious = Unpooled.buffer();
        try {
            CatnipStreamCodecBuilders.string(64).encode(malicious, "ponder");
            CatnipStreamCodecBuilders.writeVarInt(malicious, 513);
            assertDecodeRejected(new ServerboundConfigPacket(), malicious);
        } finally {
            malicious.release();
        }
    }

    private static void assertRejectedServerboundConfigValue() {
        ByteBuf malicious = Unpooled.buffer();
        try {
            CatnipStreamCodecBuilders.string(64).encode(malicious, "ponder");
            CatnipStreamCodecBuilders.string(512).encode(malicious, "ponder:common.x");
            CatnipStreamCodecBuilders.writeVarInt(malicious, 4097);
            assertDecodeRejected(new ServerboundConfigPacket(), malicious);
        } finally {
            malicious.release();
        }
    }

    private static void assertRejectedSimpleActionValue() {
        ByteBuf malicious = Unpooled.buffer();
        try {
            CatnipStreamCodecBuilders.string(64).encode(malicious, "ponder.reload");
            CatnipStreamCodecBuilders.writeVarInt(malicious, 4097);
            assertDecodeRejected(new ClientboundSimpleActionPacket(), malicious);
        } finally {
            malicious.release();
        }
    }

    private static void assertDecodeRejected(BasePacketPayload packet, ByteBuf malicious) {
        try {
            packet.fromBytes(malicious);
            fail("Oversized non-leading packet field was accepted");
        } catch (IllegalArgumentException expected) {
            // Every packet field is decoded through its own fixed bound.
        }
    }

    private static void assertEncodeRejected(BasePacketPayload packet) {
        ByteBuf encoded = Unpooled.buffer();
        try {
            packet.toBytes(encoded);
            fail("Oversized packet field was encoded");
        } catch (IllegalArgumentException expected) {
            // Every packet field is encoded through its own fixed bound.
        } finally {
            encoded.release();
        }
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, value);
        return new String(chars);
    }
}
