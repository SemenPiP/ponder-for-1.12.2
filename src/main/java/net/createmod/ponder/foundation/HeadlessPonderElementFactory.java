package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/** State-complete element implementation used for server-side compilation and tests. */
final class HeadlessPonderElementFactory implements PonderElementFactory {
    @Override public WorldSectionElement createWorldSection(Selection selection) { return new Section(selection); }
    @Override public EntityElement createEntity(Entity entity) { return new TrackedEntity(entity); }
    @Override public TextElementBuilder createText(PonderScene scene, int duration) { return new Text(scene, duration); }
    @Override public InputElementBuilder createInput(PonderScene scene, Vec3d location, Pointing direction, int duration) {
        return new Input(location, direction, duration);
    }
    @Override public ParrotElement createParrot(Vec3d location, Supplier<? extends ParrotPose> pose) {
        return new Parrot(location, pose);
    }
    @Override public MinecartElement createMinecart(Vec3d location, float angle, MinecartElement.MinecartConstructor constructor) {
        return new Minecart(location, angle, constructor);
    }

    private abstract static class Base implements PonderElement {
        boolean visible;
        @Override public boolean isVisible() { return visible; }
        @Override public void setVisible(boolean visible) { this.visible = visible; }
    }

    private abstract static class Animated extends Base {
        float fade = 1;
        Vec3d fadeVector = Vec3d.ZERO;
        public void forceApplyFade(float fade) { this.fade = clamp(fade); visible = fade > 0; }
        public void setFade(float fade) { forceApplyFade(fade); }
        public void setFadeVec(Vec3d fadeVector) { this.fadeVector = fadeVector == null ? Vec3d.ZERO : fadeVector; }
        static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    }

    private static final class Section extends Animated implements WorldSectionElement {
        Selection selection;
        Vec3d center = Vec3d.ZERO;
        Vec3d stableAnchor;
        Vec3d rotation = Vec3d.ZERO;
        Vec3d offset = Vec3d.ZERO;
        BlockPos selected;
        boolean redraw = true;

        Section(Selection selection) {
            this.selection = selection == null ? SelectionImpl.empty() : selection.copy();
            this.center = this.selection.getCenter();
        }

        @Override public void mergeOnto(WorldSectionElement other) { if (other != null) other.add(selection); setEmpty(); }
        @Override public void set(Selection selection) { this.selection = selection.copy(); center = selection.getCenter(); redraw = true; }
        @Override public void add(Selection selection) { this.selection.add(selection); center = this.selection.getCenter(); redraw = true; }
        @Override public void erase(Selection selection) { this.selection.substract(selection); center = this.selection.getCenter(); redraw = true; }
        @Override public void setCenterOfRotation(Vec3d center) { this.center = center == null ? Vec3d.ZERO : center; }
        @Override public void stabilizeRotation(Vec3d anchor) { stableAnchor = anchor; }
        @Override public void selectBlock(BlockPos pos) { selected = pos == null ? null : pos.toImmutable(); }
        @Override public void resetSelectedBlock() { selected = null; }
        @Override public void queueRedraw() { redraw = true; }
        @Override public boolean isEmpty() { return selection.isEmpty(); }
        @Override public void setEmpty() { selection = SelectionImpl.empty(); selected = null; redraw = true; }
        @Override public void setAnimatedRotation(Vec3d angles, boolean force) { rotation = angles == null ? Vec3d.ZERO : angles; }
        @Override public Vec3d getAnimatedRotation() { return rotation; }
        @Override public void setAnimatedOffset(Vec3d value, boolean force) { offset = value == null ? Vec3d.ZERO : value; }
        @Override public Vec3d getAnimatedOffset() { return offset; }

        @Override
        public Pair<Vec3d, RayTraceResult> rayTrace(PonderWorld world, Vec3d source, Vec3d target) {
            Vec3d localSource = inverseTransform(source);
            Vec3d localTarget = inverseTransform(target);
            RayTraceResult best = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (BlockPos pos : selection) {
                if (world != null && world.getBlockState(pos).getBlock() == Blocks.AIR) continue;
                AxisAlignedBB bounds = new AxisAlignedBB(pos);
                RayTraceResult hit = null;
                if (world != null) {
                    try {
                        hit = world.getBlockState(pos).collisionRayTrace(world, pos, localSource, localTarget);
                    } catch (Throwable ignored) {
                        // Use the selected outline fallback below.
                    }
                    try {
                        bounds = world.getBlockState(pos).getSelectedBoundingBox(world, pos);
                    } catch (Throwable ignored) {
                        bounds = new AxisAlignedBB(pos);
                    }
                }
                if (hit == null && bounds != null) hit = bounds.calculateIntercept(localSource, localTarget);
                if (hit == null)
                    continue;
                double distance = hit.hitVec.squareDistanceTo(localSource);
                if (distance >= bestDistance)
                    continue;
                bestDistance = distance;
                Vec3d transformed = transform(hit.hitVec);
                EnumFacing side = hit.sideHit == null ? EnumFacing.UP : hit.sideHit;
                Vec3d normal = rotate(new Vec3d(side.getDirectionVec()), rotation);
                best = new RayTraceResult(transformed,
                    EnumFacing.getFacingFromVector((float) normal.x, (float) normal.y, (float) normal.z), pos);
            }
            return best == null ? null : Pair.of(best.hitVec, best);
        }

        private Vec3d transform(Vec3d point) {
            Vec3d correction = Vec3d.ZERO;
            if (stableAnchor != null) {
                Vec3d rotatedAnchor = rotate(stableAnchor.subtract(center), rotation).add(center);
                correction = stableAnchor.subtract(rotatedAnchor);
            }
            return rotate(point.subtract(center), rotation).add(center).add(offset).add(correction);
        }

        private Vec3d inverseTransform(Vec3d point) {
            Vec3d correction = Vec3d.ZERO;
            if (stableAnchor != null) {
                Vec3d rotatedAnchor = rotate(stableAnchor.subtract(center), rotation).add(center);
                correction = stableAnchor.subtract(rotatedAnchor);
            }
            Vec3d adjusted = point.subtract(offset).subtract(correction).subtract(center);
            adjusted = rotateAxis(adjusted, -rotation.z, 2);
            adjusted = rotateAxis(adjusted, -rotation.y, 1);
            adjusted = rotateAxis(adjusted, -rotation.x, 0);
            return adjusted.add(center);
        }

        private static Vec3d rotate(Vec3d value, Vec3d euler) {
            return rotateAxis(rotateAxis(rotateAxis(value, euler.x, 0), euler.y, 1), euler.z, 2);
        }

        private static Vec3d rotateAxis(Vec3d value, double degrees, int axis) {
            double radians = Math.toRadians(degrees);
            double c = Math.cos(radians);
            double s = Math.sin(radians);
            if (axis == 0) return new Vec3d(value.x, value.y * c - value.z * s, value.y * s + value.z * c);
            if (axis == 1) return new Vec3d(value.x * c + value.z * s, value.y, -value.x * s + value.z * c);
            return new Vec3d(value.x * c - value.y * s, value.x * s + value.y * c, value.z);
        }

        @Override public Object captureState() {
            return new SectionState(visible, fade, fadeVector, selection.copy(), center, stableAnchor, rotation, offset, selected);
        }
        @Override public void restoreState(Object value) {
            if (!(value instanceof SectionState)) { super.restoreState(value); return; }
            SectionState state=(SectionState)value;visible=state.visible;fade=state.fade;fadeVector=state.fadeVector;
            selection=state.selection.copy();center=state.center;stableAnchor=state.stableAnchor;rotation=state.rotation;
            offset=state.offset;selected=state.selected;redraw=true;
        }
        private static final class SectionState {
            final boolean visible;final float fade;final Vec3d fadeVector;final Selection selection;
            final Vec3d center,stableAnchor,rotation,offset;final BlockPos selected;
            SectionState(boolean visible,float fade,Vec3d fadeVector,Selection selection,Vec3d center,Vec3d stableAnchor,
                         Vec3d rotation,Vec3d offset,BlockPos selected){this.visible=visible;this.fade=fade;this.fadeVector=fadeVector;
                this.selection=selection;this.center=center;this.stableAnchor=stableAnchor;this.rotation=rotation;this.offset=offset;this.selected=selected;}
        }
    }

    private static final class TrackedEntity extends Base implements EntityElement {
        @Nullable Entity entity;
        final java.util.UUID entityId;
        TrackedEntity(@Nullable Entity entity) { this.entity = entity; entityId=entity==null?null:entity.getUniqueID(); visible = true; }
        @Override public void ifPresent(Consumer<Entity> consumer) {
            if (entity != null && isStillValid(entity)) consumer.accept(entity);
        }
        @Override public boolean isStillValid(Entity entity) { return !entity.isDead; }
        @Override public void tick(PonderScene scene) {
            if (entityId == null || entity != null && !entity.isDead || scene.getWorld() == null) return;
            for (Entity candidate : scene.getWorld().getEntities())
                if (entityId.equals(candidate.getUniqueID())) { entity = candidate; return; }
        }
    }

    private static final class Text extends Base implements TextElementBuilder, PonderOverlayElement {
        final PonderScene scene;
        final int duration;
        PonderPalette color = PonderPalette.WHITE;
        Vec3d target;
        int independentY;
        boolean independent;
        boolean nearTarget;
        boolean keyframe;
        Supplier<String> text = new Supplier<String>() { @Override public String get() { return ""; } };

        Text(PonderScene scene, int duration) { this.scene = scene; this.duration = Math.max(1, duration); visible = true; }
        @Override public TextElementBuilder colored(PonderPalette value) { color = value; return this; }
        @Override public TextElementBuilder pointAt(Vec3d value) { target = value; return this; }
        @Override public TextElementBuilder independent(int y) { independent = true; independentY = y; return this; }
        @Override public TextElementBuilder text(String value) { text = scene.registerText(value); return this; }
        @Override public TextElementBuilder text(String value, Object... params) { text = scene.registerText(value, params); return this; }
        @Override public TextElementBuilder sharedText(final ResourceLocation key) { text = () -> PonderIndex.getLangAccess().getShared(key); return this; }
        @Override public TextElementBuilder sharedText(final ResourceLocation key, final Object... params) { text = () -> PonderIndex.getLangAccess().getShared(key, params); return this; }
        @Override public TextElementBuilder sharedText(String key) { return sharedText(new ResourceLocation(scene.getNamespace(), key)); }
        @Override public TextElementBuilder sharedText(String key, Object... params) { return sharedText(new ResourceLocation(scene.getNamespace(), key), params); }
        @Override public TextElementBuilder placeNearTarget() { nearTarget = true; return this; }
        @Override public TextElementBuilder attachKeyFrame() { keyframe = true; scene.declareKeyframe(scene.getBuildCursor()); return this; }
        @Override public void render(PonderScene scene, int mouseX, int mouseY, float partialTicks) { }
    }

    private static final class Input extends Base implements InputElementBuilder, PonderOverlayElement {
        final Vec3d location;
        final Pointing direction;
        final int duration;
        ItemStack stack = ItemStack.EMPTY;
        ScreenElement icon;
        int input;
        boolean sneaking;
        boolean control;
        Input(Vec3d location, Pointing direction, int duration) { this.location = location; this.direction = direction; this.duration = Math.max(1, duration); visible = true; }
        @Override public InputElementBuilder withItem(ItemStack value) { stack = value == null ? ItemStack.EMPTY : value.copy(); return this; }
        @Override public InputElementBuilder leftClick() { input = 1; return this; }
        @Override public InputElementBuilder rightClick() { input = 2; return this; }
        @Override public InputElementBuilder scroll() { input = 3; return this; }
        @Override public InputElementBuilder showing(ScreenElement value) { icon = value; return this; }
        @Override public InputElementBuilder whileSneaking() { sneaking = true; return this; }
        @Override public InputElementBuilder whileCTRL() { control = true; return this; }
        @Override public void render(PonderScene scene, int mouseX, int mouseY, float partialTicks) { }
    }

    private static final class Parrot extends Animated implements ParrotElement {
        Vec3d position;
        Vec3d rotation = Vec3d.ZERO;
        ParrotPose pose;
        Parrot(Vec3d location, Supplier<? extends ParrotPose> supplier) { position = location; pose = supplier.get(); visible = true; }
        @Override public void setPositionOffset(Vec3d value, boolean immediate) { position = value; }
        @Override public void setRotation(Vec3d value, boolean immediate) { rotation = value; }
        @Override public Vec3d getPositionOffset() { return position; }
        @Override public Vec3d getRotation() { return rotation; }
        @Override public void setPose(ParrotPose value) { if (value != null) pose = value; }
        @Override public Object captureState(){return new Object[]{Boolean.valueOf(visible),Float.valueOf(fade),position,rotation,pose};}
        @Override public void restoreState(Object value){if(!(value instanceof Object[])){super.restoreState(value);return;}Object[] s=(Object[])value;
            visible=((Boolean)s[0]).booleanValue();fade=((Float)s[1]).floatValue();position=(Vec3d)s[2];rotation=(Vec3d)s[3];pose=(ParrotPose)s[4];}
    }

    private static final class Minecart extends Animated implements MinecartElement {
        Vec3d position;
        float yaw;
        final MinecartConstructor constructor;
        Minecart(Vec3d location, float angle, MinecartConstructor constructor) { position = location; yaw = angle; this.constructor = constructor; visible = true; }
        @Override public void setPositionOffset(Vec3d value, boolean immediate) { position = value; }
        @Override public void setRotation(float value, boolean immediate) { yaw = value; }
        @Override public Vec3d getPositionOffset() { return position; }
        @Override public Vec3d getRotation() { return new Vec3d(0, yaw, 0); }
        @Override public Object captureState(){return new Object[]{Boolean.valueOf(visible),Float.valueOf(fade),position,Float.valueOf(yaw)};}
        @Override public void restoreState(Object value){if(!(value instanceof Object[])){super.restoreState(value);return;}Object[] s=(Object[])value;
            visible=((Boolean)s[0]).booleanValue();fade=((Float)s[1]).floatValue();position=(Vec3d)s[2];yaw=((Float)s[3]).floatValue();}
    }
}
