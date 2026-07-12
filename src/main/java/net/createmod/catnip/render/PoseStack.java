package net.createmod.catnip.render;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Small Java 8 replacement for Mojang's modern PoseStack.
 */
public final class PoseStack {
    private final Deque<Pose> stack = new ArrayDeque<Pose>();

    public PoseStack() {
        Matrix4f pose = new Matrix4f();
        pose.setIdentity();
        Matrix3f normal = new Matrix3f();
        normal.setIdentity();
        stack.push(new Pose(pose, normal));
    }

    public void pushPose() {
        stack.push(new Pose(last()));
    }

    public void popPose() {
        if (stack.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root pose");
        }
        stack.pop();
    }

    public Pose last() {
        return stack.peek();
    }

    public int depth() {
        return stack.size();
    }

    public void clear() {
        while (stack.size() > 1) {
            stack.pop();
        }
        last().pose.setIdentity();
        last().normal.setIdentity();
    }

    public void translate(double x, double y, double z) {
        Matrix4f translation = new Matrix4f();
        translation.setIdentity();
        translation.setTranslation(new Vector3f((float) x, (float) y, (float) z));
        last().pose.mul(translation);
    }

    public void scale(float x, float y, float z) {
        Matrix4f scale = new Matrix4f();
        scale.setIdentity();
        scale.m00 = x;
        scale.m11 = y;
        scale.m22 = z;
        last().pose.mul(scale);

        Matrix3f normalScale = new Matrix3f();
        normalScale.setIdentity();
        normalScale.m00 = x == 0 ? 0 : 1f / x;
        normalScale.m11 = y == 0 ? 0 : 1f / y;
        normalScale.m22 = z == 0 ? 0 : 1f / z;
        last().normal.mul(normalScale);
    }

    public void rotate(float radians, float axisX, float axisY, float axisZ) {
        float lengthSquared = axisX * axisX + axisY * axisY + axisZ * axisZ;
        if (lengthSquared == 0) {
            return;
        }
        float inverseLength = (float) (1d / Math.sqrt(lengthSquared));
        Quat4f quaternion = new Quat4f();
        quaternion.set(new AxisAngle4f(axisX * inverseLength, axisY * inverseLength,
            axisZ * inverseLength, radians));
        rotate(quaternion);
    }

    public void rotate(Quat4f quaternion) {
        Matrix4f rotation = new Matrix4f();
        rotation.set(quaternion);
        last().pose.mul(rotation);
        Matrix3f normalRotation = new Matrix3f();
        normalRotation.set(quaternion);
        last().normal.mul(normalRotation);
    }

    public void mulPose(Matrix4f matrix) {
        last().pose.mul(matrix);
        Matrix3f normal = new Matrix3f();
        matrix.getRotationScale(normal);
        try {
            normal.invert();
            normal.transpose();
            last().normal.mul(normal);
        } catch (RuntimeException ignored) {
            // Singular transforms deliberately collapse normals instead of poisoning the stack.
            last().normal.setZero();
        }
    }

    public static final class Pose {
        private final Matrix4f pose;
        private final Matrix3f normal;

        private Pose(Matrix4f pose, Matrix3f normal) {
            this.pose = pose;
            this.normal = normal;
        }

        private Pose(Pose other) {
            this(new Matrix4f(other.pose), new Matrix3f(other.normal));
        }

        public Matrix4f pose() {
            return pose;
        }

        public Matrix3f normal() {
            return normal;
        }
    }
}
