package net.createmod.catnip.render;

import static org.junit.Assert.assertEquals;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.junit.Test;

public class PoseStackTest {
    @Test
    public void nestedPoseRestoresParentTransform() {
        PoseStack stack = new PoseStack();
        stack.translate(1, 2, 3);
        stack.pushPose();
        stack.scale(2, 3, 4);
        Point3f nested = new Point3f(1, 1, 1);
        stack.last().pose().transform(nested);
        assertEquals(new Point3f(3, 5, 7), nested);

        stack.popPose();
        Point3f parent = new Point3f(1, 1, 1);
        stack.last().pose().transform(parent);
        assertEquals(new Point3f(2, 3, 4), parent);
    }

    @Test
    public void nonUniformScaleUsesInverseNormalScale() {
        PoseStack stack = new PoseStack();
        stack.scale(2, 4, 8);
        Vector3f normal = new Vector3f(1, 1, 1);
        stack.last().normal().transform(normal);
        assertEquals(.5f, normal.x, 0);
        assertEquals(.25f, normal.y, 0);
        assertEquals(.125f, normal.z, 0);
    }

    @Test(expected = IllegalStateException.class)
    public void rootPoseCannotBePopped() {
        new PoseStack().popPose();
    }
}
