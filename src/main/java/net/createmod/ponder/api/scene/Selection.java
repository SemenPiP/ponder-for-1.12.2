package net.createmod.ponder.api.scene;

import java.util.function.Predicate;

import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface Selection extends Iterable<BlockPos>, Predicate<BlockPos> {
    Selection add(Selection other);

    Selection substract(Selection other);

    /** Correctly-spelled alias retained alongside the historical API spelling. */
    default Selection subtract(Selection other) {
        return substract(other);
    }

    Selection copy();

    Vec3d getCenter();

    boolean isEmpty();

    int size();

    Outline.OutlineParams makeOutline(Outliner outliner, Object slot);

    default Outline.OutlineParams makeOutline(Outliner outliner) {
        return makeOutline(outliner, this);
    }
}
