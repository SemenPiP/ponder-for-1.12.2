package net.createmod.ponder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import net.minecraft.util.ResourceLocation;

public class ScriptSnapshotMetadataTest {
    @After
    public void clearServerLayers() {
        ScriptSceneRegistry.replaceServerScenes(Collections.<ScriptSceneDefinition>emptyList());
        ScriptTagRegistry.clearServer();
        ScriptSharedText.clearServer();
    }

    @Test
    public void serverMetadataOverridesAndDisconnectRestoresLocalMetadata() {
        ResourceLocation id = new ResourceLocation("snapshot_metadata", "override");
        ScriptTagDefinition local = tag(id, "Local");
        ScriptTagDefinition server = tag(id, "Server");
        ScriptTagRegistry.register(local);
        ScriptSharedText.register("snapshot_metadata.override", "Local text");

        ScriptTagRegistry.replaceServer(Collections.singletonList(server));
        Map<String, String> serverText = new LinkedHashMap<String, String>();
        serverText.put("snapshot_metadata.override", "Server text");
        ScriptSharedText.replaceServer(serverText);

        assertEquals("Server", find(ScriptTagRegistry.snapshot(), id).title);
        assertEquals("Server text", ScriptSharedText.snapshot().get("snapshot_metadata.override"));

        ScriptTagRegistry.clearServer();
        ScriptSharedText.clearServer();
        assertEquals("Local", find(ScriptTagRegistry.snapshot(), id).title);
        assertEquals("Local text", ScriptSharedText.snapshot().get("snapshot_metadata.override"));
    }

    @Test
    public void snapshotRoundTripsTagsComponentsAndSharedText() throws Exception {
        ResourceLocation id = new ResourceLocation("snapshot_metadata", "roundtrip");
        ScriptTagDefinition tag = tag(id, "Round trip");
        Map<String, String> shared = new LinkedHashMap<String, String>();
        shared.put("snapshot_metadata.roundtrip", "Shared");

        ScriptSceneSnapshot.Encoded encoded = ScriptSceneSnapshot.encode(
            Collections.<ScriptSceneDefinition>emptyList(),
            Collections.singletonList(tag), shared);
        ScriptSceneSnapshot.Decoded decoded =
            ScriptSceneSnapshot.decodeContent(encoded.bytes, encoded.uncompressedBytes);

        assertEquals(1, decoded.getTagCount());
        assertEquals(1, decoded.getSharedTextCount());
        assertEquals(id, decoded.tags.get(0).id);
        assertEquals(tag.components, decoded.tags.get(0).components);
        assertEquals(shared, decoded.sharedText);
    }

    @Test
    public void failedReloadRollsBackScenesTagsAndSharedText() {
        ScriptSceneDefinition oldScene = scene("old");
        ScriptTagDefinition oldTag = tag(new ResourceLocation("snapshot_metadata", "old"), "Old");
        Map<String, String> oldText = Collections.singletonMap("snapshot_metadata.state", "Old");
        ScriptSceneSnapshot.Decoded oldSnapshot = new ScriptSceneSnapshot.Decoded(
            Collections.singletonList(oldScene), Collections.emptyList(),
            Collections.singletonList(oldTag), oldText);
        ScriptSceneRegistry.replaceServerSnapshotAndReload(oldSnapshot, () -> {
        });

        ScriptSceneDefinition replacementScene = scene("replacement");
        ScriptTagDefinition replacementTag =
            tag(new ResourceLocation("snapshot_metadata", "replacement"), "Replacement");
        ScriptSceneSnapshot.Decoded replacement = new ScriptSceneSnapshot.Decoded(
            Collections.singletonList(replacementScene), Collections.emptyList(),
            Collections.singletonList(replacementTag),
            Collections.singletonMap("snapshot_metadata.state", "Replacement"));
        try {
            ScriptSceneRegistry.replaceServerSnapshotAndReload(replacement,
                () -> { throw new IllegalStateException("reload failed"); });
            throw new AssertionError("Failed reload was accepted");
        } catch (IllegalStateException expected) {
            assertEquals("reload failed", expected.getMessage());
        }

        assertEquals(oldScene.getSceneId(),
            ScriptSceneRegistry.serverSnapshot().get(0).getSceneId());
        assertEquals(oldTag.id, ScriptTagRegistry.serverSnapshot().iterator().next().id);
        assertEquals("Old", ScriptSharedText.serverSnapshot().get("snapshot_metadata.state"));
        assertTrue(ScriptSceneRegistry.serverSnapshot().stream()
            .noneMatch(scene -> replacementScene.getSceneId().equals(scene.getSceneId())));
    }

    private static ScriptTagDefinition tag(ResourceLocation id, String title) {
        return new ScriptTagDefinition(id, new ResourceLocation("minecraft", "paper"), title,
            title + " description", true, Arrays.asList(
                new ResourceLocation("minecraft", "paper"),
                new ResourceLocation("minecraft", "book")));
    }

    private static ScriptSceneDefinition scene(String path) {
        ResourceLocation id = new ResourceLocation("snapshot_metadata", path);
        return new ScriptSceneDefinition(new ResourceLocation("minecraft", "paper"), id, path,
            id, Collections.<ResourceLocation>emptyList(),
            Collections.singletonList(new ScriptInstruction("finish", null)), false);
    }

    private static ScriptTagDefinition find(Collection<ScriptTagDefinition> tags, ResourceLocation id) {
        for (ScriptTagDefinition tag : tags)
            if (id.equals(tag.id))
                return tag;
        throw new AssertionError("Missing tag " + id);
    }
}
