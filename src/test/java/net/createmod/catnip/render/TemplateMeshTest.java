package net.createmod.catnip.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TemplateMeshTest {
    @Test
    public void immutableMeshDefensivelyCopiesVertexData() {
        MutableTemplateMesh mutable = new MutableTemplateMesh();
        mutable.add(1, 2, 3, 0xff123456, .25f, .75f, 0x00f000f0, 0, 1, 0);
        TemplateMesh immutable = mutable.toImmutable();
        mutable.set(0, 9, 8, 7, 0xffffffff, 0, 0, 0, 0);

        assertEquals(1f, immutable.x(0), 0);
        assertEquals(2f, immutable.y(0), 0);
        assertEquals(0xff123456, immutable.color(0));

        int[] copy = immutable.copyData();
        copy[TemplateMesh.X_OFFSET] = Float.floatToRawIntBits(42);
        assertNotEquals(42f, immutable.x(0), 0);
    }

    @Test
    public void mutableMeshGrowsAndClampsPackedNormals() {
        MutableTemplateMesh mesh = new MutableTemplateMesh(1);
        for (int i = 0; i < 32; i++)
            mesh.add(i, 0, 0, 0xffffffff, 0, 0, 0, 2, -2, .5f);
        assertEquals(32, mesh.vertexCount());
        int normal = mesh.normal(0);
        assertEquals(127, (byte) normal);
        assertEquals(-127, (byte) (normal >> 8));
        assertTrue((byte) (normal >> 16) > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCanonicalStride() {
        new TemplateMesh(new int[TemplateMesh.INT_STRIDE - 1]);
    }
}
