package net.createmod.catnip.outliner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BlockClusterOutline extends Outline {
    private final List<BlockPos> positions = new ArrayList<BlockPos>();
    public BlockClusterOutline(Iterable<BlockPos> positions) {
        for (BlockPos pos : positions) this.positions.add(pos.toImmutable());
    }
    @Override public void render(Vec3d camera, float partialTicks) {
        for (BlockPos pos : positions) {
            AABBOutline outline = new AABBOutline(new AxisAlignedBB(pos));
            copyParams(outline.getParams());
            outline.render(camera, partialTicks);
        }
    }
    private void copyParams(OutlineParams target) {
        target.colored(params.getColor()).alpha(params.getAlpha()).lineWidth(params.getLineWidth())
            .lightmap(params.getLightmap()).withFaceTextures(params.getFaceTexture(), params.getHighlightedFaceTexture())
            .highlightFace(params.getHighlightedFace());
        if (params.isCullDisabled()) target.disableCull();
    }
}
