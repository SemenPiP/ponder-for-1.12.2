package net.createmod.catnip.math;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public final class BBHelper {
    private BBHelper() {}
    public static StructureBoundingBox encapsulate(StructureBoundingBox box, BlockPos pos) {
        return new StructureBoundingBox(
            Math.min(box.minX, pos.getX()), Math.min(box.minY, pos.getY()), Math.min(box.minZ, pos.getZ()),
            Math.max(box.maxX, pos.getX()), Math.max(box.maxY, pos.getY()), Math.max(box.maxZ, pos.getZ()));
    }
    public static StructureBoundingBox encapsulate(StructureBoundingBox first, StructureBoundingBox second) {
        return new StructureBoundingBox(
            Math.min(first.minX, second.minX), Math.min(first.minY, second.minY), Math.min(first.minZ, second.minZ),
            Math.max(first.maxX, second.maxX), Math.max(first.maxY, second.maxY), Math.max(first.maxZ, second.maxZ));
    }
}
