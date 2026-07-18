package net.createmod.ponder.script.net;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import net.createmod.ponder.api.script.ScriptInstructionCodecDescriptor;
import net.createmod.ponder.script.ScriptSceneSnapshot;
import net.createmod.ponder.script.ScriptSceneSync;
import net.minecraft.util.ResourceLocation;

public class ScriptSnapshotReceiverTest {
    private static final int TRANSFER_ID = 17;

    @After
    public void clearReceiver() {
        ScriptSnapshotReceiver.reset();
    }

    @Test
    public void acceptsChunksInAnyOrder() throws Exception {
        byte[] compressed = new byte[] { 1, 2 };
        begin(2, compressed.length, compressed);

        ScriptSnapshotReceiver.accept(TRANSFER_ID, 1, new byte[] { compressed[1] });
        assertNotNull(activeTransfer());
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { compressed[0] });
        assertNotNull(activeTransfer());
        ScriptSnapshotReceiver.complete(TRANSFER_ID);

        assertNull(activeTransfer());
    }

    @Test
    public void duplicateChunkInvalidatesTransfer() throws Exception {
        begin(2, 2, new byte[] { 1, 2 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { 1 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { 1 });

        assertNull(activeTransfer());
    }

    @Test
    public void missingChunkKeepsTransferIncomplete() throws Exception {
        begin(3, 3, new byte[] { 1, 2, 3 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { 1 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 2, new byte[] { 3 });

        assertNotNull(activeTransfer());
    }

    @Test
    public void hashMismatchRejectsCompletedTransfer() throws Exception {
        byte[] wrongHash = new byte[32];
        begin(1, 1, wrongHash);
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { 1 });
        ScriptSnapshotReceiver.complete(TRANSFER_ID);

        assertNull(activeTransfer());
    }

    @Test
    public void earlyCompleteRejectsTransferWithoutAffectingLaterTransfer() throws Exception {
        begin(2, 2, new byte[] { 1, 2 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { 1 });
        ScriptSnapshotReceiver.complete(TRANSFER_ID);
        assertNull(activeTransfer());

        begin(1, 1, new byte[] { 3 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID + 1, 0, new byte[] { 9 });
        assertNotNull(activeTransfer());
    }

    @Test
    public void sizeOverrunRejectsCompletedTransfer() throws Exception {
        begin(2, 1, new byte[] { 1 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { 1 });
        assertNotNull(activeTransfer());

        begin(2, 1, new byte[] { 1 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[] { 1, 2 });
        assertNull(activeTransfer());
    }

    @Test
    public void invalidNewHeadersDoNotClearPreviousTransfer() throws Exception {
        begin(1, 1, new byte[] { 1 });
        assertNotNull(activeTransfer());

        ScriptSnapshotReceiver.begin(TRANSFER_ID + 1, ScriptSceneSnapshot.PROTOCOL, 0, 0, 0, new byte[32],
            Collections.emptyList());

        assertNotNull(activeTransfer());
    }

    @Test
    public void oversizedHeadersAreRejected() throws Exception {
        ScriptSnapshotReceiver.begin(TRANSFER_ID, ScriptSceneSnapshot.PROTOCOL, 1,
            ScriptSceneSnapshot.MAX_COMPRESSED_BYTES + 1, 0, new byte[32], Collections.emptyList());
        assertNull(activeTransfer());

        ScriptSnapshotReceiver.begin(TRANSFER_ID, ScriptSceneSnapshot.PROTOCOL, 1, 0,
            ScriptSceneSnapshot.MAX_UNCOMPRESSED_BYTES + 1, new byte[32], Collections.emptyList());
        assertNull(activeTransfer());
    }

    @Test
    public void oversizedChunkAndNullChunkAreRejected() throws Exception {
        begin(1, 1, new byte[] { 1 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, null);
        assertNull(activeTransfer());

        begin(1, 1, new byte[] { 1 });
        ScriptSnapshotReceiver.accept(TRANSFER_ID, 0, new byte[ScriptSceneSync.CHUNK_BYTES + 1]);
        assertNull(activeTransfer());
    }

    @Test
    public void protocolMismatchAndMissingCodecAreRejectedBeforeChunks() throws Exception {
        ScriptSnapshotReceiver.begin(TRANSFER_ID, ScriptSceneSnapshot.PROTOCOL + 1, 1, 0, 0, new byte[32],
            Collections.emptyList());
        assertNull(activeTransfer());

        ScriptSnapshotReceiver.begin(TRANSFER_ID, ScriptSceneSnapshot.PROTOCOL, 1, 0, 0, new byte[32],
            Collections.singletonList(new ScriptInstructionCodecDescriptor(
                new ResourceLocation("missing", "codec"), 1, Collections.emptyList())));
        assertNull(activeTransfer());
    }

    private static void begin(int chunks, int compressedBytes, byte[] hashInput) throws Exception {
        ScriptSnapshotReceiver.begin(TRANSFER_ID, ScriptSceneSnapshot.PROTOCOL, chunks, compressedBytes, 0,
            hashInput.length == 32 ? hashInput : ScriptSceneSnapshot.sha256(hashInput), Collections.emptyList());
    }

    private static Object activeTransfer() throws Exception {
        Field active = ScriptSnapshotReceiver.class.getDeclaredField("active");
        active.setAccessible(true);
        return active.get(null);
    }
}
