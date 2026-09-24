package dev.blendemotes.core.pose;

import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;

/**
 * Values of one Minecraft {@code ModelPart}: pivot, rotation (radians, applied X, Y, Z like
 * {@code ModelPart#translateAndRotate}) and scale. Mutable so platform code can reuse it.
 */
public final class PartTransform {
    public double x, y, z;
    public double pitch, yaw, roll;
    public double scaleX = 1, scaleY = 1, scaleZ = 1;

    public PartTransform() {
    }

    public PartTransform(double x, double y, double z, double pitch, double yaw, double roll) {
        set(x, y, z, pitch, yaw, roll);
    }

    public PartTransform set(double x, double y, double z, double pitch, double yaw, double roll) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
        return this;
    }

    public PartTransform setScale(double sx, double sy, double sz) {
        this.scaleX = sx;
        this.scaleY = sy;
        this.scaleZ = sz;
        return this;
    }

    public PartTransform copy() {
        return new PartTransform(x, y, z, pitch, yaw, roll).setScale(scaleX, scaleY, scaleZ);
    }

    public Vec3 pivot() {
        return new Vec3(x, y, z);
    }

    /** Matrix mapping part-local coordinates to model space: T(pivot) Rz Ry Rx S. */
    public Mat4 toMatrix() {
        Mat4 m = Mat4.translation(x, y, z);
        m.mulLocal(Mat4.rotationZYX(pitch, yaw, roll));
        if (scaleX != 1 || scaleY != 1 || scaleZ != 1) {
            m.mulLocal(Mat4.scaling(scaleX, scaleY, scaleZ));
        }
        return m;
    }

    /** Inverse of {@link #toMatrix()} decomposition (no shear). */
    public static PartTransform fromMatrix(Mat4 m) {
        double[] e = m.toEulerZYX();
        Vec3 s = m.getScale();
        return new PartTransform(m.m[3], m.m[7], m.m[11], e[0], e[1], e[2]).setScale(s.x, s.y, s.z);
    }

    public boolean hasScale() {
        return Math.abs(scaleX - 1) > 1e-6 || Math.abs(scaleY - 1) > 1e-6 || Math.abs(scaleZ - 1) > 1e-6;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "Part[pivot=(%.3f, %.3f, %.3f) rot=(%.2f, %.2f, %.2f)deg scale=(%.3f, %.3f, %.3f)]",
                x, y, z, Math.toDegrees(pitch), Math.toDegrees(yaw), Math.toDegrees(roll), scaleX, scaleY, scaleZ);
    }
}
