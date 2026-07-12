package net.createmod.catnip.animation;

import static org.junit.Assert.*;

import org.junit.Test;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.math.VoxelShape;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class AnimationMathTest {
    @Test public void angularChaserUsesShortestPath(){
        LerpedFloat value=LerpedFloat.angular().startWithValue(350).chase(10,5,LerpedFloat.Chaser.LINEAR);value.tickChaser();assertEquals(355,value.getValue(),.0001f);
    }
    @Test public void forceOverTimePreservesTotalImpulse(){
        PhysicalFloat value=PhysicalFloat.create().bump(4,8);for(int i=0;i<4;i++)value.tick();assertEquals(8,value.getSpeed(),.0001f);
    }
    @Test public void angleWrappingHandlesNegativeMultiples(){
        assertEquals(-20,AngleHelper.getShortestAngleDiff(10,350),.0001f);assertEquals(-180,AngleHelper.wrapAngle180(180),.0001f);
    }
    @Test public void vectorRotationUsesDegreesAndStableCenter(){
        Vec3d rotated=VecHelper.rotate(new Vec3d(1,0,0),90,EnumFacing.Axis.Y);assertEquals(0,rotated.x,1e-9);assertEquals(-1,rotated.z,1e-9);
        Vec3d center=VecHelper.rotateCentered(new Vec3d(.5,.5,.5),90,EnumFacing.Axis.Z);assertEquals(new Vec3d(.5,.5,.5),center);
    }
    @Test public void voxelRotationTransformsAllCorners(){
        VoxelShape source=VoxelShape.box(0,0,0,4,8,16);
        VoxelShape east=VoxelShaper.forHorizontal(source,EnumFacing.SOUTH).get(EnumFacing.EAST);
        AxisAlignedBB bounds=east.getBounds();
        assertEquals(0,bounds.minX,1e-9);assertEquals(.75,bounds.minZ,1e-9);assertEquals(1,bounds.maxX,1e-9);assertEquals(1,bounds.maxZ,1e-9);
    }
}
