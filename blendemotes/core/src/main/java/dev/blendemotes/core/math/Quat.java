package dev.blendemotes.core.math;

/** Immutable unit quaternion, used to blend poses smoothly (fade in / fade out). */
public final class Quat {
    public static final Quat IDENTITY = new Quat(0, 0, 0, 1);

    public final double x;
    public final double y;
    public final double z;
    public final double w;

    public Quat(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /** Extracts the rotation of an affine matrix (scale is removed first). */
    public static Quat fromMatrix(Mat4 mat) {
        Vec3 s = mat.getScale();
        double sx = s.x < 1e-12 ? 1 : s.x;
        double sy = s.y < 1e-12 ? 1 : s.y;
        double sz = s.z < 1e-12 ? 1 : s.z;
        double m00 = mat.m[0] / sx, m01 = mat.m[1] / sy, m02 = mat.m[2] / sz;
        double m10 = mat.m[4] / sx, m11 = mat.m[5] / sy, m12 = mat.m[6] / sz;
        double m20 = mat.m[8] / sx, m21 = mat.m[9] / sy, m22 = mat.m[10] / sz;
        double trace = m00 + m11 + m22;
        double qx, qy, qz, qw;
        if (trace > 0) {
            double s4 = Math.sqrt(trace + 1.0) * 2;
            qw = 0.25 * s4;
            qx = (m21 - m12) / s4;
            qy = (m02 - m20) / s4;
            qz = (m10 - m01) / s4;
        } else if (m00 > m11 && m00 > m22) {
            double s4 = Math.sqrt(1.0 + m00 - m11 - m22) * 2;
            qw = (m21 - m12) / s4;
            qx = 0.25 * s4;
            qy = (m01 + m10) / s4;
            qz = (m02 + m20) / s4;
        } else if (m11 > m22) {
            double s4 = Math.sqrt(1.0 + m11 - m00 - m22) * 2;
            qw = (m02 - m20) / s4;
            qx = (m01 + m10) / s4;
            qy = 0.25 * s4;
            qz = (m12 + m21) / s4;
        } else {
            double s4 = Math.sqrt(1.0 + m22 - m00 - m11) * 2;
            qw = (m10 - m01) / s4;
            qx = (m02 + m20) / s4;
            qy = (m12 + m21) / s4;
            qz = 0.25 * s4;
        }
        return new Quat(qx, qy, qz, qw).normalize();
    }

    public Quat normalize() {
        double len = Math.sqrt(x * x + y * y + z * z + w * w);
        if (len < 1e-12) {
            return IDENTITY;
        }
        return new Quat(x / len, y / len, z / len, w / len);
    }

    public Quat slerp(Quat o, double t) {
        double cos = x * o.x + y * o.y + z * o.z + w * o.w;
        double ox = o.x, oy = o.y, oz = o.z, ow = o.w;
        if (cos < 0) {
            cos = -cos;
            ox = -ox;
            oy = -oy;
            oz = -oz;
            ow = -ow;
        }
        double a;
        double b;
        if (cos > 0.9995) {
            a = 1 - t;
            b = t;
        } else {
            double theta = Math.acos(cos);
            double sin = Math.sin(theta);
            a = Math.sin((1 - t) * theta) / sin;
            b = Math.sin(t * theta) / sin;
        }
        return new Quat(x * a + ox * b, y * a + oy * b, z * a + oz * b, w * a + ow * b).normalize();
    }

    /** Rotation matrix (no translation). */
    public Mat4 toMatrix() {
        Mat4 r = new Mat4();
        double xx = x * x, yy = y * y, zz = z * z;
        double xy = x * y, xz = x * z, yz = y * z;
        double wx = w * x, wy = w * y, wz = w * z;
        r.m[0] = 1 - 2 * (yy + zz);
        r.m[1] = 2 * (xy - wz);
        r.m[2] = 2 * (xz + wy);
        r.m[4] = 2 * (xy + wz);
        r.m[5] = 1 - 2 * (xx + zz);
        r.m[6] = 2 * (yz - wx);
        r.m[8] = 2 * (xz - wy);
        r.m[9] = 2 * (yz + wx);
        r.m[10] = 1 - 2 * (xx + yy);
        return r;
    }
}
