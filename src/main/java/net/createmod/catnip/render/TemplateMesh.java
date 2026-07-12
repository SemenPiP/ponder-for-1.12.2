package net.createmod.catnip.render;

import java.util.Arrays;

/** Immutable, renderer-independent vertex data used by the 1.12 fixed pipeline. */
public class TemplateMesh {
    public static final int INT_STRIDE = 8;
    public static final int X_OFFSET = 0;
    public static final int Y_OFFSET = 1;
    public static final int Z_OFFSET = 2;
    public static final int COLOR_OFFSET = 3;
    public static final int U_OFFSET = 4;
    public static final int V_OFFSET = 5;
    public static final int LIGHT_OFFSET = 6;
    public static final int NORMAL_OFFSET = 7;

    protected int[] data;
    protected int vertexCount;

    public TemplateMesh(int[] data) {
        if (data == null || data.length % INT_STRIDE != 0) {
            throw new IllegalArgumentException("Vertex data must use the canonical eight-int stride");
        }
        this.data = Arrays.copyOf(data, data.length);
        this.vertexCount = data.length / INT_STRIDE;
    }

    protected TemplateMesh(int vertexCount) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("Negative vertex count");
        }
        data = new int[vertexCount * INT_STRIDE];
        this.vertexCount = vertexCount;
    }

    public int vertexCount() { return vertexCount; }
    public boolean isEmpty() { return vertexCount == 0; }
    public float x(int index) { return Float.intBitsToFloat(value(index, X_OFFSET)); }
    public float y(int index) { return Float.intBitsToFloat(value(index, Y_OFFSET)); }
    public float z(int index) { return Float.intBitsToFloat(value(index, Z_OFFSET)); }
    /** Color is encoded as 0xAARRGGBB. */
    public int color(int index) { return value(index, COLOR_OFFSET); }
    public float u(int index) { return Float.intBitsToFloat(value(index, U_OFFSET)); }
    public float v(int index) { return Float.intBitsToFloat(value(index, V_OFFSET)); }
    public int light(int index) { return value(index, LIGHT_OFFSET); }
    /** Three signed normalized bytes packed as x | y << 8 | z << 16. */
    public int normal(int index) { return value(index, NORMAL_OFFSET); }

    public int[] copyData() {
        return Arrays.copyOf(data, vertexCount * INT_STRIDE);
    }

    protected int value(int index, int offset) {
        checkIndex(index);
        return data[index * INT_STRIDE + offset];
    }

    protected void checkIndex(int index) {
        if (index < 0 || index >= vertexCount) {
            throw new IndexOutOfBoundsException("vertex " + index + " of " + vertexCount);
        }
    }
}
