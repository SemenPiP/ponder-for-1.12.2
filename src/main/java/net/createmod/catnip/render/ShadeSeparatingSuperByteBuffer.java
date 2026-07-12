package net.createmod.catnip.render;

import java.util.Arrays;

/** Super buffer that retains transitions between shaded and unshaded baked quads. */
public class ShadeSeparatingSuperByteBuffer extends DefaultSuperByteBuffer {
    private final int[] shadeSwapVertices;

    public ShadeSeparatingSuperByteBuffer(TemplateMesh mesh, int... shadeSwapVertices) {
        super(mesh);
        this.shadeSwapVertices = shadeSwapVertices == null ? new int[0] : shadeSwapVertices.clone();
        validateTransitions(mesh.vertexCount());
    }

    public int[] getShadeSwapVertices() {
        return shadeSwapVertices.clone();
    }

    public boolean isVertexShaded(int vertexIndex) {
        TemplateMesh activeMesh = mesh;
        if (activeMesh == null || vertexIndex < 0 || vertexIndex >= activeMesh.vertexCount()) {
            throw new IndexOutOfBoundsException("vertex " + vertexIndex);
        }
        int insertion = Arrays.binarySearch(shadeSwapVertices, vertexIndex);
        int transitions = insertion >= 0 ? insertion + 1 : -insertion - 1;
        return (transitions & 1) == 0;
    }

    @Override
    protected boolean shouldApplyDiffuse(int vertexIndex) {
        return isVertexShaded(vertexIndex);
    }

    private void validateTransitions(int vertexCount) {
        int previous = -1;
        for (int transition : shadeSwapVertices) {
            if (transition < 0 || transition > vertexCount || transition <= previous) {
                throw new IllegalArgumentException("Shade transitions must be sorted unique vertex offsets");
            }
            previous = transition;
        }
    }
}
