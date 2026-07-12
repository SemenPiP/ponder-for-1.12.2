package net.createmod.ponder.foundation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class PonderSceneTimelineTest {
    @Test
    public void seekBackRestoresInstructionAndSceneState() {
        PonderScene scene = new PonderScene(null, new PonderLocalization(), "test",
            new ResourceLocation("test", "component"), Collections.emptyList(), Collections.emptyList());
        PonderSceneBuilder builder = (PonderSceneBuilder) scene.builder();
        builder.title("timeline", "Timeline");
        builder.addInstruction(value -> value.setPointOfInterest(new Vec3d(3, 4, 5)));
        builder.idle(3);
        builder.addKeyframe();
        builder.idle(2);
        scene.begin();
        scene.seek(5);
        assertEquals(new Vec3d(3, 4, 5), scene.getPointOfInterest());
        assertTrue(scene.isFinished());
        scene.seek(0);
        assertEquals(new Vec3d(0, 4, 0), scene.getPointOfInterest());
        assertFalse(scene.isFinished());
        scene.seek(5);
        assertEquals(new Vec3d(3, 4, 5), scene.getPointOfInterest());
        assertEquals(Collections.singletonList(3), scene.getKeyframes());
    }
}
