package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.minecraft.util.ResourceLocation;

public class PonderStoryBoardIdentityTest {
    @Test
    public void declaredSceneIdIsOptionalAndStable() {
        PonderStoryBoardEntry entry = new PonderStoryBoardEntry((scene, util) -> {
        }, "test", new ResourceLocation("test", "structure"),
            new ResourceLocation("test", "component"));
        assertNull(entry.getDeclaredSceneId());
        StoryBoardEntry returned = entry.identifiedBy("scene");
        assertSame(entry, returned);
        assertEquals(new ResourceLocation("test", "scene"), entry.getDeclaredSceneId());
        assertSame(entry, entry.identifiedBy(new ResourceLocation("test", "scene")));
    }

    @Test(expected = IllegalStateException.class)
    public void declaredSceneIdCannotBeChanged() {
        PonderStoryBoardEntry entry = new PonderStoryBoardEntry((scene, util) -> {
        }, "test", new ResourceLocation("test", "structure"),
            new ResourceLocation("test", "component"));
        entry.identifiedBy("first");
        entry.identifiedBy("second");
    }
}
