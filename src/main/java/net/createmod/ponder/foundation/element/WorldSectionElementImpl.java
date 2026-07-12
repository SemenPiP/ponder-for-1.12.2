package net.createmod.ponder.foundation.element;

import net.createmod.catnip.data.Pair;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.foundation.SelectionImpl;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.createmod.ponder.render.SectionRenderCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WorldSectionElementImpl extends AnimatedSceneElementBase implements WorldSectionElement {
    private static final PonderWorldRenderer AUXILIARY_RENDERER = new PonderWorldRenderer();

    private final SectionRenderCache cache = new SectionRenderCache();
    private Selection selection;
    private Vec3d center;
    private Vec3d stableAnchor;
    private Vec3d previousRotation = Vec3d.ZERO;
    private Vec3d rotation = Vec3d.ZERO;
    private Vec3d previousOffset = Vec3d.ZERO;
    private Vec3d offset = Vec3d.ZERO;
    private BlockPos selectedBlock;
    private float lastRenderPartialTicks = 1;

    public WorldSectionElementImpl() {
        this(SelectionImpl.empty());
    }

    public WorldSectionElementImpl(Selection selection) {
        this.selection = selection == null ? SelectionImpl.empty() : selection.copy();
        center = this.selection.getCenter();
    }

    @Override
    public void mergeOnto(WorldSectionElement other) {
        if (other != null) {
            other.add(selection);
        }
        setEmpty();
    }

    @Override
    public void set(Selection value) {
        selection = value == null ? SelectionImpl.empty() : value.copy();
        center = selection.getCenter();
        cache.invalidate();
    }

    @Override
    public void add(Selection value) {
        if (value != null) {
            selection.add(value);
        }
        center = selection.getCenter();
        cache.invalidate();
    }

    @Override
    public void erase(Selection value) {
        if (value != null) {
            selection.substract(value);
        }
        center = selection.getCenter();
        cache.invalidate();
    }

    @Override
    public void setCenterOfRotation(Vec3d value) {
        center = value == null ? Vec3d.ZERO : value;
    }

    @Override
    public void stabilizeRotation(Vec3d anchor) {
        stableAnchor = anchor;
    }

    @Override
    public void selectBlock(BlockPos pos) {
        selectedBlock = pos == null ? null : pos.toImmutable();
    }

    @Override
    public void resetSelectedBlock() {
        selectedBlock = null;
    }

    @Override
    public void queueRedraw() {
        cache.invalidate();
    }

    @Override
    public boolean isEmpty() {
        return selection.isEmpty();
    }

    @Override
    public void setEmpty() {
        selection = SelectionImpl.empty();
        selectedBlock = null;
        cache.clear();
    }

    @Override
    public void setAnimatedRotation(Vec3d value, boolean force) {
        Vec3d next = value == null ? Vec3d.ZERO : value;
        previousRotation = force ? next : rotation;
        rotation = next;
    }

    @Override
    public Vec3d getAnimatedRotation() {
        return rotation;
    }

    @Override
    public void setAnimatedOffset(Vec3d value, boolean force) {
        Vec3d next = value == null ? Vec3d.ZERO : value;
        previousOffset = force ? next : offset;
        offset = next;
    }

    @Override
    public Vec3d getAnimatedOffset() {
        return offset;
    }

    @Override
    public void renderFirst(PonderWorld world, float partialTicks) {
        renderFirst(world, partialTicks, Vec3d.ZERO);
    }

    public void renderFirst(PonderWorld world, float partialTicks, Vec3d sortingCamera) {
        if (!visible || selection.isEmpty()) {
            return;
        }
        lastRenderPartialTicks = partialTicks;
        cache.ensureBuilt(world, selection);
        RenderTransform transform = getTransform(partialTicks);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            transform.applyGl();
            AUXILIARY_RENDERER.renderTileEntities(world, cache.getTiles(), Vec3d.ZERO,
                transform.inverse(sortingCamera), partialTicks);
        } finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void renderLayer(PonderWorld world, BlockRenderLayer layer, float partialTicks) {
        renderLayer(world, layer, partialTicks, Vec3d.ZERO);
    }

    public void renderLayer(PonderWorld world, BlockRenderLayer layer, float partialTicks, Vec3d sortingCamera) {
        if (!visible || selection.isEmpty()) {
            return;
        }
        lastRenderPartialTicks = partialTicks;
        cache.ensureBuilt(world, selection);
        RenderTransform transform = getTransform(partialTicks);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            transform.applyGl();
            cache.render(layer, transform.inverse(sortingCamera));
        } finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void renderLast(PonderWorld world, float partialTicks) {
        renderLast(world, partialTicks, Vec3d.ZERO);
    }

    public void renderLast(PonderWorld world, float partialTicks, Vec3d sortingCamera) {
        if (!visible || selection.isEmpty()) {
            return;
        }
        lastRenderPartialTicks = partialTicks;
        RenderTransform transform = getTransform(partialTicks);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            transform.applyGl();
            AUXILIARY_RENDERER.renderBreaking(world, selection, Vec3d.ZERO);
            renderSelectedBlock(world);
        } finally {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
        }
    }

    private void renderSelectedBlock(PonderWorld world) {
        if (selectedBlock == null) return;
        IBlockState state = world.getBlockState(selectedBlock);
        if (state.getBlock() == Blocks.AIR) return;
        AxisAlignedBB bounds;
        try {
            bounds = state.getSelectedBoundingBox(world, selectedBlock);
        } catch (Throwable ignored) {
            bounds = new AxisAlignedBB(selectedBlock);
        }
        if (bounds == null) return;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2);
        GlStateManager.depthMask(false);
        RenderGlobal.drawSelectionBoundingBox(bounds.grow(.002), .94f, .94f, .94f, .95f);
        GlStateManager.depthMask(true);
        GlStateManager.glLineWidth(1);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /** Squared world-space distance used to draw independently transformed translucent sections back-to-front. */
    public double getSortDistance(Vec3d sortingCamera, float partialTicks) {
        return getTransform(partialTicks).transform(center).squareDistanceTo(sortingCamera);
    }

    @Override
    public Pair<Vec3d, RayTraceResult> rayTrace(PonderWorld world, Vec3d source, Vec3d target) {
        return rayTrace(world, source, target, lastRenderPartialTicks);
    }

    @Override
    public Pair<Vec3d, RayTraceResult> rayTrace(PonderWorld world, Vec3d source, Vec3d target,
                                                float partialTicks) {
        RenderTransform transform = getTransform(partialTicks);
        Vec3d localSource = transform.inverse(source);
        Vec3d localTarget = transform.inverse(target);
        RayTraceResult closest = null;
        double best = Double.POSITIVE_INFINITY;
        for (BlockPos pos : selection) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blocks.AIR) continue;
            RayTraceResult hit = null;
            try {
                hit = state.collisionRayTrace(world, pos, localSource, localTarget);
            } catch (Throwable ignored) {
                // Fall through to the selected outline used by vanilla targeting.
            }
            if (hit == null) {
                AxisAlignedBB bounds;
                try {
                    bounds = state.getSelectedBoundingBox(world, pos);
                } catch (Throwable ignored) {
                    bounds = null;
                }
                if (bounds != null) hit = bounds.calculateIntercept(localSource, localTarget);
            }
            if (hit == null || hit.hitVec.squareDistanceTo(localSource) >= best) {
                continue;
            }
            best = hit.hitVec.squareDistanceTo(localSource);
            Vec3d transformed = transform.transform(hit.hitVec);
            EnumFacing side = transform.transformFacing(hit.sideHit == null ? EnumFacing.UP : hit.sideHit);
            closest = new RayTraceResult(transformed, side, pos);
        }
        return closest == null ? null : Pair.of(closest.hitVec, closest);
    }

    private RenderTransform getTransform(float partialTicks) {
        Vec3d angles = lerp(previousRotation, rotation, partialTicks);
        Vec3d movement = lerp(previousOffset, offset, partialTicks)
            .add(fadeVector.scale(1 - getFade(partialTicks)));
        Vec3d anchorCorrection = Vec3d.ZERO;
        if (stableAnchor != null) {
            Vec3d rotatedAnchor = rotate(stableAnchor.subtract(center), angles).add(center);
            // Stabilization pins only the rotational displacement. Explicit section translation
            // and fade translation must continue to move the anchor.
            anchorCorrection = stableAnchor.subtract(rotatedAnchor);
        }
        return new RenderTransform(center, angles, movement.add(anchorCorrection));
    }

    private static Vec3d rotate(Vec3d point, Vec3d angles) {
        return rotateAxis(rotateAxis(rotateAxis(point, angles.x, 0), angles.y, 1), angles.z, 2);
    }

    private static Vec3d rotateAxis(Vec3d point, double degrees, int axis) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        if (axis == 0) {
            return new Vec3d(point.x, point.y * cosine - point.z * sine,
                point.y * sine + point.z * cosine);
        }
        if (axis == 1) {
            return new Vec3d(point.x * cosine + point.z * sine, point.y,
                -point.x * sine + point.z * cosine);
        }
        return new Vec3d(point.x * cosine - point.y * sine,
            point.x * sine + point.y * cosine, point.z);
    }

    private static Vec3d lerp(Vec3d first, Vec3d second, float partialTicks) {
        return new Vec3d(first.x + (second.x - first.x) * partialTicks,
            first.y + (second.y - first.y) * partialTicks,
            first.z + (second.z - first.z) * partialTicks);
    }

    @Override
    public Object captureState() {
        return new State(super.captureState(), selection.copy(), center, stableAnchor, previousRotation, rotation,
            previousOffset, offset, selectedBlock);
    }

    @Override
    public void restoreState(Object value) {
        if (!(value instanceof State)) {
            super.restoreState(value);
            return;
        }
        State state = (State) value;
        super.restoreState(state.animation);
        selection = state.selection.copy();
        center = state.center;
        stableAnchor = state.stableAnchor;
        previousRotation = state.previousRotation;
        rotation = state.rotation;
        previousOffset = state.previousOffset;
        offset = state.offset;
        selectedBlock = state.selectedBlock;
        cache.invalidate();
    }

    private static final class RenderTransform {
        private final Vec3d center;
        private final Vec3d angles;
        private final Vec3d translation;

        private RenderTransform(Vec3d center, Vec3d angles, Vec3d translation) {
            this.center = center;
            this.angles = angles;
            this.translation = translation;
        }

        private void applyGl() {
            GlStateManager.translate(translation.x, translation.y, translation.z);
            GlStateManager.translate(center.x, center.y, center.z);
            GlStateManager.rotate((float) angles.z, 0, 0, 1);
            GlStateManager.rotate((float) angles.y, 0, 1, 0);
            GlStateManager.rotate((float) angles.x, 1, 0, 0);
            GlStateManager.translate(-center.x, -center.y, -center.z);
        }

        private Vec3d transform(Vec3d point) {
            return rotate(point.subtract(center), angles).add(center).add(translation);
        }

        private Vec3d inverse(Vec3d point) {
            Vec3d adjusted = point.subtract(translation).subtract(center);
            adjusted = rotateAxis(adjusted, -angles.z, 2);
            adjusted = rotateAxis(adjusted, -angles.y, 1);
            adjusted = rotateAxis(adjusted, -angles.x, 0);
            return adjusted.add(center);
        }

        private EnumFacing transformFacing(EnumFacing facing) {
            Vec3d normal = rotate(new Vec3d(facing.getDirectionVec()), angles);
            EnumFacing closest = facing;
            double best = -Double.MAX_VALUE;
            for (EnumFacing candidate : EnumFacing.values()) {
                Vec3d direction = new Vec3d(candidate.getDirectionVec());
                double alignment = normal.dotProduct(direction);
                if (alignment > best) {
                    best = alignment;
                    closest = candidate;
                }
            }
            return closest;
        }
    }

    private static final class State {
        private final Object animation;
        private final Selection selection;
        private final Vec3d center;
        private final Vec3d stableAnchor;
        private final Vec3d previousRotation;
        private final Vec3d rotation;
        private final Vec3d previousOffset;
        private final Vec3d offset;
        private final BlockPos selectedBlock;

        private State(Object animation, Selection selection, Vec3d center, Vec3d stableAnchor,
                      Vec3d previousRotation, Vec3d rotation, Vec3d previousOffset, Vec3d offset,
                      BlockPos selectedBlock) {
            this.animation = animation;
            this.selection = selection;
            this.center = center;
            this.stableAnchor = stableAnchor;
            this.previousRotation = previousRotation;
            this.rotation = rotation;
            this.previousOffset = previousOffset;
            this.offset = offset;
            this.selectedBlock = selectedBlock;
        }
    }
}
