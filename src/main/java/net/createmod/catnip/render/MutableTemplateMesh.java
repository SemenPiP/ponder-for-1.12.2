package net.createmod.catnip.render;

import java.util.Arrays;

public class MutableTemplateMesh extends TemplateMesh {
    public MutableTemplateMesh() {
        super(0);
    }

    public MutableTemplateMesh(int initialCapacity) {
        super(0);
        data = new int[Math.max(0, initialCapacity) * INT_STRIDE];
    }

    public MutableTemplateMesh(TemplateMesh source) {
        super(source.copyData());
    }

    public void ensureCapacity(int vertices) {
        if (vertices < 0) {
            throw new IllegalArgumentException("Negative capacity");
        }
        int required = vertices * INT_STRIDE;
        if (required > data.length) {
            int grown = Math.max(required, Math.max(INT_STRIDE, data.length * 2));
            data = Arrays.copyOf(data, grown);
        }
    }

    public int add(float x, float y, float z, int color, float u, float v, int light,
                   float normalX, float normalY, float normalZ) {
        int index = vertexCount;
        ensureCapacity(index + 1);
        vertexCount++;
        set(index, x, y, z, color, u, v, light, packNormal(normalX, normalY, normalZ));
        return index;
    }

    public void set(int index, float x, float y, float z, int color, float u, float v,
                    int light, int normal) {
        checkIndex(index);
        setValue(index, X_OFFSET, Float.floatToRawIntBits(x));
        setValue(index, Y_OFFSET, Float.floatToRawIntBits(y));
        setValue(index, Z_OFFSET, Float.floatToRawIntBits(z));
        setValue(index, COLOR_OFFSET, color);
        setValue(index, U_OFFSET, Float.floatToRawIntBits(u));
        setValue(index, V_OFFSET, Float.floatToRawIntBits(v));
        setValue(index, LIGHT_OFFSET, light);
        setValue(index, NORMAL_OFFSET, normal);
    }

    public void append(TemplateMesh source) {
        ensureCapacity(vertexCount + source.vertexCount());
        int[] sourceData = source.copyData();
        System.arraycopy(sourceData, 0, data, vertexCount * INT_STRIDE, sourceData.length);
        vertexCount += source.vertexCount();
    }

    public void clear() {
        vertexCount = 0;
    }

    public TemplateMesh toImmutable() {
        return new TemplateMesh(Arrays.copyOf(data, vertexCount * INT_STRIDE));
    }

    private void setValue(int index, int offset, int value) {
        data[index * INT_STRIDE + offset] = value;
    }

    public static int packNormal(float x, float y, float z) {
        int nx = Math.round(clamp(x) * 127f) & 0xff;
        int ny = Math.round(clamp(y) * 127f) & 0xff;
        int nz = Math.round(clamp(z) * 127f) & 0xff;
        return nx | ny << 8 | nz << 16;
    }

    private static float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
