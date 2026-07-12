package net.createmod.catnip.outliner;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class ChasingAABBOutline extends AABBOutline {
    protected AxisAlignedBB previous;
    protected AxisAlignedBB target;

    public ChasingAABBOutline(AxisAlignedBB bounds) {
        super(bounds);
        previous = target = bounds;
    }
    public void target(AxisAlignedBB target) { this.target = target; }
    public void setImmediately(AxisAlignedBB value) { previous = target = bounds = value; }
    @Override public void tick() { previous = bounds; bounds = interpolate(bounds, target, .5f); }
    @Override public void render(Vec3d camera, float partialTicks) {
        AxisAlignedBB current = bounds; bounds = interpolate(previous, current, partialTicks);
        super.render(camera, partialTicks); bounds = current;
    }
    private static AxisAlignedBB interpolate(AxisAlignedBB a, AxisAlignedBB b, float t) {
        return new AxisAlignedBB(lerp(a.minX,b.minX,t),lerp(a.minY,b.minY,t),lerp(a.minZ,b.minZ,t),
            lerp(a.maxX,b.maxX,t),lerp(a.maxY,b.maxY,t),lerp(a.maxZ,b.maxZ,t));
    }
    private static double lerp(double a,double b,float t){return a+(b-a)*t;}
}
