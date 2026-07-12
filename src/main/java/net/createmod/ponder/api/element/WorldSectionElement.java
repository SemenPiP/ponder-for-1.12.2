package net.createmod.ponder.api.element;

import net.createmod.catnip.data.Pair;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public interface WorldSectionElement extends AnimatedSceneElement {
    void mergeOnto(WorldSectionElement other);
    void set(Selection selection);
    void add(Selection selection);
    void erase(Selection selection);
    void setCenterOfRotation(Vec3d center);
    void stabilizeRotation(Vec3d anchor);
    void selectBlock(BlockPos pos);
    void resetSelectedBlock();
    void queueRedraw();
    boolean isEmpty();
    void setEmpty();
    void setAnimatedRotation(Vec3d eulerAngles, boolean force);
    Vec3d getAnimatedRotation();
    void setAnimatedOffset(Vec3d offset, boolean force);
    Vec3d getAnimatedOffset();
    Pair<Vec3d, RayTraceResult> rayTrace(PonderWorld world, Vec3d source, Vec3d target);
    default Pair<Vec3d, RayTraceResult> rayTrace(PonderWorld world, Vec3d source, Vec3d target,
                                                 float partialTicks) {
        return rayTrace(world, source, target);
    }
}
