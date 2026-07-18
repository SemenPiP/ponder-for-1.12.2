package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.util.ResourceLocation;

public final class PonderStoryBoardEntry implements StoryBoardEntry {
    private final PonderStoryBoard board;
    private final String namespace;
    private final ResourceLocation schematicLocation;
    private final ResourceLocation component;
    private final List<ResourceLocation> tags = new ArrayList<ResourceLocation>();
    private final List<SceneOrderingEntry> orderingEntries = new ArrayList<SceneOrderingEntry>();
    private ResourceLocation declaredSceneId;
    private String pluginClass = "";

    public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, ResourceLocation schematicLocation,
                                 ResourceLocation component) {
        if (board == null || namespace == null || schematicLocation == null || component == null)
            throw new IllegalArgumentException("Storyboard registration arguments may not be null");
        this.board = board;
        this.namespace = namespace;
        this.schematicLocation = schematicLocation;
        this.component = component;
    }

    public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, String schematicPath,
                                 ResourceLocation component) {
        this(board, namespace, new ResourceLocation(namespace, schematicPath), component);
    }

    @Override public PonderStoryBoard getBoard() { return board; }
    @Override public String getNamespace() { return namespace; }
    @Override public ResourceLocation getSchematicLocation() { return schematicLocation; }
    @Override public ResourceLocation getComponent() { return component; }
    @Override public List<ResourceLocation> getTags() { return Collections.unmodifiableList(tags); }
    @Override public List<SceneOrderingEntry> getOrderingEntries() { return Collections.unmodifiableList(orderingEntries); }
    @Override public ResourceLocation getDeclaredSceneId() { return declaredSceneId; }

    @Override
    public StoryBoardEntry identifiedBy(ResourceLocation sceneId) {
        if (sceneId == null)
            throw new IllegalArgumentException("Declared Ponder scene id is required");
        if (declaredSceneId != null && !declaredSceneId.equals(sceneId))
            throw new IllegalStateException("Ponder storyboard already declares scene id " + declaredSceneId);
        declaredSceneId = sceneId;
        return this;
    }

    public void setPluginClass(String pluginClass) {
        this.pluginClass = pluginClass == null ? "" : pluginClass;
    }

    public String getPluginClass() {
        return pluginClass;
    }

    @Override
    public StoryBoardEntry orderBefore(String namespace, String otherSceneId) {
        orderingEntries.add(SceneOrderingEntry.before(namespace, otherSceneId));
        return this;
    }

    @Override
    public StoryBoardEntry orderAfter(String namespace, String otherSceneId) {
        orderingEntries.add(SceneOrderingEntry.after(namespace, otherSceneId));
        return this;
    }

    @Override
    public StoryBoardEntry highlightTag(ResourceLocation tag) {
        if (tag != null && !tags.contains(tag))
            tags.add(tag);
        return this;
    }

    @Override
    public StoryBoardEntry highlightTags(ResourceLocation... tags) {
        if (tags != null)
            for (ResourceLocation tag : tags)
                highlightTag(tag);
        return this;
    }

    @Override
    public StoryBoardEntry highlightAllTags() {
        return highlightTag(PonderTag.Highlight.ALL);
    }
}
