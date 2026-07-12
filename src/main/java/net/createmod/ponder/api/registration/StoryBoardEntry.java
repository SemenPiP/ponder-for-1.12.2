package net.createmod.ponder.api.registration;

import java.util.List;

import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.util.ResourceLocation;

public interface StoryBoardEntry {
    PonderStoryBoard getBoard();

    String getNamespace();

    ResourceLocation getSchematicLocation();

    ResourceLocation getComponent();

    List<ResourceLocation> getTags();

    List<SceneOrderingEntry> getOrderingEntries();

    default StoryBoardEntry orderBefore(String otherSceneId) {
        return orderBefore(getNamespace(), otherSceneId);
    }

    StoryBoardEntry orderBefore(String namespace, String otherSceneId);

    default StoryBoardEntry orderAfter(String otherSceneId) {
        return orderAfter(getNamespace(), otherSceneId);
    }

    StoryBoardEntry orderAfter(String namespace, String otherSceneId);

    StoryBoardEntry highlightTag(ResourceLocation tag);

    StoryBoardEntry highlightTags(ResourceLocation... tags);

    StoryBoardEntry highlightAllTags();

    enum SceneOrderingType {
        BEFORE, AFTER
    }

    final class SceneOrderingEntry {
        private final SceneOrderingType type;
        private final ResourceLocation sceneId;

        public SceneOrderingEntry(SceneOrderingType type, ResourceLocation sceneId) {
            if (type == null || sceneId == null)
                throw new IllegalArgumentException("Ordering type and scene id are required");
            this.type = type;
            this.sceneId = sceneId;
        }

        public SceneOrderingType getType() {
            return type;
        }

        public SceneOrderingType type() {
            return type;
        }

        public ResourceLocation getSceneId() {
            return sceneId;
        }

        public ResourceLocation sceneId() {
            return sceneId;
        }

        public static SceneOrderingEntry after(String namespace, String sceneId) {
            return new SceneOrderingEntry(SceneOrderingType.AFTER, new ResourceLocation(namespace, sceneId));
        }

        public static SceneOrderingEntry before(String namespace, String sceneId) {
            return new SceneOrderingEntry(SceneOrderingType.BEFORE, new ResourceLocation(namespace, sceneId));
        }
    }
}
