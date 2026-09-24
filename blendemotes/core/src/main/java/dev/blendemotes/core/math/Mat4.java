package dev.blendemotes.core.math;

import java.util.Locale;

/**
 * Mutable 4x4 affine matrix, row-major, column-vector convention ({@code p' = M * p}).
 * <p>
 * {@code a.mul(b)} returns {@code a * b}, i.e. {@code b} is applied first.
 */
public final class Mat4 {
    /** Row-major storage: {@code m[row * 4 + col]}. */
    public final double[] m = new double[16];

    public Mat4() {
        setIdentity();
    }

    public Mat4(Mat4 other) {
        System.arraycopy(other.m, 0, m, 0, 16);
    }

    public static Mat4 identity() {
        return new Mat4();
    }

    public static Mat4 fromRows(double[][] rows) {
        Mat4 r = new Mat4();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r.m[i * 4 + j] = rows[i][j];
            }
        }
        return r;
    }

    public Mat4 set(Mat4 other) {
        System.arraycopy(other.m, 0, m, 0, 16);
        return this;
    }

    public Mat4 setIdentity() {
        for (int i = 0; i < 16; i++) {
            m[i] = (i % 5 == 0) ? 1 : 0;
        }
        return this;
    }

    public double get(int row, int col) {
        return m[row * 4 + col];
    }

    public static Mat4 translation(double x, double y, double z) {
        Mat4 r = new Mat4();
        r.m[3] = x;
        r.m[7] = y;
        r.m[11] = z;
        return r;
    }

    public static Mat4 scaling(double x, double y, double z) {
        Mat4 r = new Mat4();
        r.m[0] = x;
        r.m[5] = y;
        r.m[10] = z;
        return r;
    }

    /** Right-handed rotation about +X (radians). */
    public static Mat4 rotationX(double a) {
        Mat4 r = new Mat4();
        double c = Math.cos(a);
        double s = Math.sin(a);
        r.m[5] = c;
        r.m[6] = -s;
        r.m[9] = s;
        r.m[10] = c;
        return r;
    }

    /** Right-handed rotation about +Y (radians). */
    public static Mat4 rotationY(double a) {
        Mat4 r = new Mat4();
        double c = Math.cos(a);
        double s = Math.sin(a);
        r.m[0] = c;
        r.m[2] = s;
        r.m[8] = -s;
        r.m[10] = c;
        return r;
    }

    /** Right-handed rotation about +Z (radians). */
    public static Mat4 rotationZ(double a) {
        Mat4 r = new Mat4();
        double c = Math.cos(a);
        double s = Math.sin(a);
        r.m[0] = c;
        r.m[1] = -s;
        r.m[4] = s;
        r.m[5] = c;
        return r;
    }

    public static Mat4 rotationAxis(int axis, double a) {
        return axis == 0 ? rotationX(a) : axis == 1 ? rotationY(a) : rotationZ(a);
    }

    /**
     * Minecraft {@code ModelPart} rotation: {@code Rz * Ry * Rx} (X applied first).
     */
    public static Mat4 rotationZYX(double x, double y, double z) {
        double cx = Math.cos(x), sx = Math.sin(x);
        double cy = Math.cos(y), sy = Math.sin(y);
        double cz = Math.cos(z), sz = Math.sin(z);
        Mat4 r = new Mat4();
        r.m[0] = cz * cy;
        r.m[1] = cz * sy * sx - sz * cx;
        r.m[2] = cz * sy * cx + sz * sx;
        r.m[4] = sz * cy;
        r.m[5] = sz * sy * sx + cz * cx;
        r.m[6] = sz * sy * cx - cz * sx;
        r.m[8] = -sy;
        r.m[9] = cy * sx;
        r.m[10] = cy * cx;
        return r;
    }

    /** Returns {@code this * o} as a new matrix. */
    public Mat4 mul(Mat4 o) {
        Mat4 r = new Mat4();
        mul(this, o, r);
        return r;
    }

    /** {@code this = this * o}. */
    public Mat4 mulLocal(Mat4 o) {
        mul(this, o, this);
        return this;
    }

    /** {@code out = a * b}; {@code out} may alias {@code a} or {@code b}. */
    public static void mul(Mat4 a, Mat4 b, Mat4 out) {
        double[] x = a.m;
        double[] y = b.m;
        double[] r = new double[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r[i * 4 + j] = x[i * 4] * y[j] + x[i * 4 + 1] * y[4 + j] + x[i * 4 + 2] * y[8 + j] + x[i * 4 + 3] * y[12 + j];
            }
        }
        System.arraycopy(r, 0, out.m, 0, 16);
    }

    public Vec3 transformPoint(double x, double y, double z) {
        return new Vec3(
                m[0] * x + m[1] * y + m[2] * z + m[3],
                m[4] * x + m[5] * y + m[6] * z + m[7],
                m[8] * x + m[9] * y + m[10] * z + m[11]);
    }

    public Vec3 transformPoint(Vec3 p) {
        return transformPoint(p.x, p.y, p.z);
    }

    public Vec3 transformDirection(double x, double y, double z) {
        return new Vec3(
                m[0] * x + m[1] * y + m[2] * z,
                m[4] * x + m[5] * y + m[6] * z,
                m[8] * x + m[9] * y + m[10] * z);
    }

    public Vec3 getTranslation() {
        return new Vec3(m[3], m[7], m[11]);
    }

    public Vec3 getColumn(int c) {
        return new Vec3(m[c], m[4 + c], m[8 + c]);
    }

    /** Length of each basis column (the scale of an affine transform). */
    public Vec3 getScale() {
        return new Vec3(getColumn(0).length(), getColumn(1).length(), getColumn(2).length());
    }

    /** General inverse of the affine part (last row assumed 0,0,0,1). */
    public Mat4 invertAffine() {
        double a = m[0], b = m[1], c = m[2];
        double d = m[4], e = m[5], f = m[6];
        double g = m[8], h = m[9], i = m[10];
        double A = e * i - f * h;
        double B = -(d * i - f * g);
        double C = d * h - e * g;
        double det = a * A + b * B + c * C;
        if (Math.abs(det) < 1e-18) {
            return new Mat4();
        }
        double inv = 1.0 / det;
        Mat4 r = new Mat4();
        r.m[0] = A * inv;
        r.m[1] = -(b * i - c * h) * inv;
        r.m[2] = (b * f - c * e) * inv;
        r.m[4] = B * inv;
        r.m[5] = (a * i - c * g) * inv;
        r.m[6] = -(a * f - c * d) * inv;
        r.m[8] = C * inv;
        r.m[9] = -(a * h - b * g) * inv;
        r.m[10] = (a * e - b * d) * inv;
        double tx = m[3], ty = m[7], tz = m[11];
        r.m[3] = -(r.m[0] * tx + r.m[1] * ty + r.m[2] * tz);
        r.m[7] = -(r.m[4] * tx + r.m[5] * ty + r.m[6] * tz);
        r.m[11] = -(r.m[8] * tx + r.m[9] * ty + r.m[10] * tz);
        return r;
    }

    /**
     * Decomposes the rotation part into Minecraft {@code ModelPart} angles
     * ({@code Rz * Ry * Rx}). Scale is removed first. Returns {x, y, z} in radians.
     */
    public double[] toEulerZYX() {
        Vec3 s = getScale();
        double sx = s.x < 1e-12 ? 1 : s.x;
        double sy = s.y < 1e-12 ? 1 : s.y;
        double sz = s.z < 1e-12 ? 1 : s.z;
        double r00 = m[0] / sx, r10 = m[4] / sx, r20 = m[8] / sx;
        double r01 = m[1] / sy, r11 = m[5] / sy, r21 = m[9] / sy;
        double r22 = m[10] / sz;
        double ny = -r20;
        if (ny > 1) {
            ny = 1;
        } else if (ny < -1) {
            ny = -1;
        }
        double y = Math.asin(ny);
        double x;
        double z;
        if (Math.abs(ny) < 0.9999999) {
            x = Math.atan2(r21, r22);
            z = Math.atan2(r10, r00);
        } else {
            // gimbal lock: X and Z rotate about the same axis
            x = 0;
            z = Math.atan2(-r01, r11);
        }
        return new double[]{x, y, z};
    }

    public boolean approxEquals(Mat4 o, double eps) {
        for (int i = 0; i < 16; i++) {
            if (Math.abs(m[i] - o.m[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    public float[] toFloatArrayRowMajor() {
        float[] r = new float[16];
        for (int i = 0; i < 16; i++) {
            r[i] = (float) m[i];
        }
        return r;
    }

    /** Column-major copy, as expected by OpenGL / JOML / Minecraft's {@code Matrix4f} loaders. */
    public float[] toFloatArrayColumnMajor() {
        float[] r = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                r[col * 4 + row] = (float) m[row * 4 + col];
            }
        }
        return r;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mat4[");
        for (int i = 0; i < 4; i++) {
            sb.append(String.format(Locale.ROOT, "%n  %9.4f %9.4f %9.4f %9.4f", m[i * 4], m[i * 4 + 1], m[i * 4 + 2], m[i * 4 + 3]));
        }
        return sb.append(" ]").toString();
    }
}
