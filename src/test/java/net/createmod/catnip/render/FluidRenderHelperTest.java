package net.createmod.catnip.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import org.junit.Test;
import org.lwjgl.opengl.GL11;

public class FluidRenderHelperTest {
    private static final float EPSILON = 1e-5f;

    @Test
    public void topAndBottomWindingMatchesPackedNormals() {
        BufferBuilder buffer = beginFluidBuffer();
        PoseStack pose = new PoseStack();

        FluidRenderHelper.emitFace(buffer, pose, EnumFacing.UP,
            2, 9, 5, 6, 9, 11, 0xffffffff, 0x00f000f0, 0, 0, 1, 1);
        FluidRenderHelper.emitFace(buffer, pose, EnumFacing.DOWN,
            2, 3, 11, 6, 3, 5, 0xffffffff, 0x00f000f0, 0, 0, 1, 1);

        TemplateMesh mesh = SuperByteBufferBuilder.meshFrom(buffer.getVertexState());
        assertEquals(8, mesh.vertexCount());
        assertFaceMatchesNormal(mesh, 0, EnumFacing.UP);
        assertFaceMatchesNormal(mesh, 4, EnumFacing.DOWN);
    }

    @Test
    public void gasInversionRotatesPositionsAndNormalsAroundBoxCenter() {
        BufferBuilder buffer = beginFluidBuffer();
        PoseStack pose = new PoseStack();
        FluidRenderHelper.applyGasInversion(pose, 2, 3, 5, 6, 9, 11,
            true, true);

        FluidRenderHelper.emitFace(buffer, pose, EnumFacing.UP,
            2, 9, 5, 6, 9, 11, 0xffffffff, 0x00f000f0, 0, 0, 1, 1);

        TemplateMesh mesh = SuperByteBufferBuilder.meshFrom(buffer.getVertexState());
        assertFaceMatchesNormal(mesh, 0, EnumFacing.DOWN);
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(3, mesh.y(vertex), EPSILON);
        }
        assertPoint(mesh, 0, 2, 3, 11);
        assertPoint(mesh, 1, 2, 3, 5);
        assertPoint(mesh, 2, 6, 3, 5);
        assertPoint(mesh, 3, 6, 3, 11);
    }

    @Test
    public void gasInversionRequiresBothOptInAndLighterThanAir() {
        Point3f source = new Point3f(2, 9, 5);

        PoseStack disabled = new PoseStack();
        FluidRenderHelper.applyGasInversion(disabled, 2, 3, 5, 6, 9, 11,
            false, true);
        Point3f disabledResult = new Point3f(source);
        disabled.last().pose().transform(disabledResult);
        assertEquals(source, disabledResult);

        PoseStack liquid = new PoseStack();
        FluidRenderHelper.applyGasInversion(liquid, 2, 3, 5, 6, 9, 11,
            true, false);
        Point3f liquidResult = new Point3f(source);
        liquid.last().pose().transform(liquidResult);
        assertEquals(source, liquidResult);
    }

    private static BufferBuilder beginFluidBuffer() {
        BufferBuilder buffer = new BufferBuilder(4096);
        VertexFormat fluidFormat = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(DefaultVertexFormats.TEX_2F)
            .addElement(DefaultVertexFormats.TEX_2S)
            .addElement(DefaultVertexFormats.NORMAL_3B)
            .addElement(DefaultVertexFormats.PADDING_1B);
        buffer.begin(GL11.GL_QUADS, fluidFormat);
        return buffer;
    }

    private static void assertFaceMatchesNormal(TemplateMesh mesh, int firstVertex,
                                                EnumFacing expected) {
        Vector3f edgeOne = new Vector3f(
            mesh.x(firstVertex + 1) - mesh.x(firstVertex),
            mesh.y(firstVertex + 1) - mesh.y(firstVertex),
            mesh.z(firstVertex + 1) - mesh.z(firstVertex));
        Vector3f edgeTwo = new Vector3f(
            mesh.x(firstVertex + 2) - mesh.x(firstVertex),
            mesh.y(firstVertex + 2) - mesh.y(firstVertex),
            mesh.z(firstVertex + 2) - mesh.z(firstVertex));
        Vector3f geometricNormal = new Vector3f();
        geometricNormal.cross(edgeOne, edgeTwo);
        geometricNormal.normalize();

        Vector3f declaredNormal = unpackNormal(mesh.normal(firstVertex));
        declaredNormal.normalize();
        assertTrue("geometric winding " + geometricNormal + " must agree with packed vertex normal "
                + declaredNormal,
            geometricNormal.dot(declaredNormal) > .999f);
        assertEquals(expected.getXOffset(), declaredNormal.x, EPSILON);
        assertEquals(expected.getYOffset(), declaredNormal.y, EPSILON);
        assertEquals(expected.getZOffset(), declaredNormal.z, EPSILON);

        for (int vertex = firstVertex + 1; vertex < firstVertex + 4; vertex++) {
            Vector3f otherNormal = unpackNormal(mesh.normal(vertex));
            assertEquals(declaredNormal.x, otherNormal.x, EPSILON);
            assertEquals(declaredNormal.y, otherNormal.y, EPSILON);
            assertEquals(declaredNormal.z, otherNormal.z, EPSILON);
        }
    }

    private static Vector3f unpackNormal(int packed) {
        return new Vector3f((byte) packed / 127f, (byte) (packed >> 8) / 127f,
            (byte) (packed >> 16) / 127f);
    }

    private static void assertPoint(TemplateMesh mesh, int vertex,
                                    float x, float y, float z) {
        assertEquals(x, mesh.x(vertex), EPSILON);
        assertEquals(y, mesh.y(vertex), EPSILON);
        assertEquals(z, mesh.z(vertex), EPSILON);
    }
}
