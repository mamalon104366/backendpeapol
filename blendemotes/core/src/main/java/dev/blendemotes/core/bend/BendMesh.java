package dev.blendemotes.core.bend;

import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.BendProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Bendable version of a Minecraft model part.
 * <p>
 * The vanilla quads of the part (arm, sleeve, leg, pants, torso, jacket, cape) are cut into
 * horizontal strips where the skin weights change, then every vertex is skinned between the
 * straight and the bent transform exactly like the rig's Armature modifier, which uses
 * "Preserve Volume" (dual quaternion skinning): a vertex with weight {@code w} is rotated
 * around the joint by {@link #blendAngle(double, double)}. The surface stays closed (no gaps or
 * holes at the elbow/knee), keeps its thickness, and the texture keeps its pixel mapping.
 * <p>
 * Arms and legs also have a hand/foot ({@link BendProfile#tip}): the last 3 px turn about the
 * wrist/ankle first, then the whole lower half turns about the elbow/knee, which is exactly how
 * the rig composes its hand bone (a child of the bend bone) when the weights do not overlap.
 * <p>
 * Build once per part geometry (it only depends on the model), then call {@link #deform} every
 * frame. All coordinates are part-local pixels (Y down from the part pivot), like
 * {@code ModelPart} cubes.
 */
public final class BendMesh {
    /** Largest strip height inside a weight ramp, in pixels. */
    private static final double MAX_STRIP = 0.5;

    private final BendProfile profile;
    private final int quadCount;
    /** quadCount * 4 * 3 rest positions. */
    private final float[] positions;
    /** quadCount * 4 * 2 texture coordinates. */
    private final float[] uvs;
    /** quadCount * 3 rest normals. */
    private final float[] normals;
    /** quadCount * 4 weights. */
    private final float[] weights;
    /** quadCount * 4 weights of the hand/foot, or null when the part has none. */
    private final float[] tipWeights;

    private BendMesh(BendProfile profile, float[] positions, float[] uvs, float[] normals) {
        this.profile = profile;
        this.quadCount = normals.length / 3;
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
        this.weights = new float[quadCount * 4];
        for (int v = 0; v < quadCount * 4; v++) {
            weights[v] = (float) profile.weight(positions[v * 3 + 1]);
        }
        if (profile.tip != null) {
            tipWeights = new float[quadCount * 4];
            for (int v = 0; v < quadCount * 4; v++) {
                tipWeights[v] = (float) profile.tip.weight(positions[v * 3 + 1]);
            }
        } else {
            tipWeights = null;
        }
    }

    public int quadCount() {
        return quadCount;
    }

    public BendProfile profile() {
        return profile;
    }

    /**
     * Builds the bendable mesh.
     *
     * @param quadPositions n * 4 * 3 floats: the part's quads (vertex order preserved)
     * @param quadUvs       n * 4 * 2 floats
     * @param quadNormals   n * 3 floats (one normal per quad)
     */
    public static BendMesh build(float[] quadPositions, float[] quadUvs, float[] quadNormals, BendProfile profile) {
        int n = quadNormals.length / 3;
        if (quadPositions.length != n * 12 || quadUvs.length != n * 8) {
            throw new IllegalArgumentException("inconsistent quad arrays");
        }
        double[] cuts = cutLevels(profile);
        List<float[]> outPos = new ArrayList<float[]>();
        List<float[]> outUv = new ArrayList<float[]>();
        List<float[]> outNormal = new ArrayList<float[]>();
        for (int q = 0; q < n; q++) {
            float[] p = Arrays.copyOfRange(quadPositions, q * 12, q * 12 + 12);
            float[] uv = Arrays.copyOfRange(quadUvs, q * 8, q * 8 + 8);
            float[] nrm = Arrays.copyOfRange(quadNormals, q * 3, q * 3 + 3);
            split(p, uv, nrm, cuts, outPos, outUv, outNormal);
        }
        float[] pos = new float[outPos.size() * 12];
        float[] uv = new float[outUv.size() * 8];
        float[] nrm = new float[outNormal.size() * 3];
        for (int i = 0; i < outPos.size(); i++) {
            System.arraycopy(outPos.get(i), 0, pos, i * 12, 12);
            System.arraycopy(outUv.get(i), 0, uv, i * 8, 8);
            System.arraycopy(outNormal.get(i), 0, nrm, i * 3, 3);
        }
        return new BendMesh(profile, pos, uv, nrm);
    }

    /** Heights where the geometry is cut: profile breakpoints plus a fine grid inside ramps. */
    static double[] cutLevels(BendProfile profile) {
        TreeSet<Double> set = new TreeSet<Double>();
        addCuts(profile, set);
        if (profile.tip != null) {
            addCuts(profile.tip, set);
        }
        double[] r = new double[set.size()];
        int i = 0;
        for (Double d : set) {
            r[i++] = d;
        }
        return r;
    }

    private static void addCuts(BendProfile profile, TreeSet<Double> set) {
        double[] bp = profile.breakpoints();
        for (double b : bp) {
            set.add(b);
        }
        for (int i = 1; i < bp.length; i++) {
            double a = bp[i - 1];
            double b = bp[i];
            int steps = (int) Math.ceil((b - a) / MAX_STRIP);
            for (int s = 1; s < steps; s++) {
                set.add(a + (b - a) * s / steps);
            }
        }
    }

    private static void split(float[] p, float[] uv, float[] nrm, double[] cuts,
                              List<float[]> outPos, List<float[]> outUv, List<float[]> outNormal) {
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int v = 0; v < 4; v++) {
            minY = Math.min(minY, p[v * 3 + 1]);
            maxY = Math.max(maxY, p[v * 3 + 1]);
        }
        int[] partner = verticalPartners(p, minY, maxY);
        if (maxY - minY < 1e-4f || partner == null) {
            outPos.add(p);
            outUv.add(uv);
            outNormal.add(nrm);
            return;
        }
        List<Double> levels = new ArrayList<Double>();
        levels.add((double) minY);
        for (double c : cuts) {
            if (c > minY + 1e-4 && c < maxY - 1e-4) {
                levels.add(c);
            }
        }
        levels.add((double) maxY);
        for (int s = 0; s + 1 < levels.size(); s++) {
            double top = levels.get(s);
            double bottom = levels.get(s + 1);
            float[] sp = new float[12];
            float[] suv = new float[8];
            for (int v = 0; v < 4; v++) {
                int o = partner[v];
                float vy = p[v * 3 + 1];
                float oy = p[o * 3 + 1];
                double level = Math.abs(vy - minY) < Math.abs(vy - maxY) ? top : bottom;
                double f = (level - vy) / (oy - vy);
                for (int k = 0; k < 3; k++) {
                    sp[v * 3 + k] = (float) (p[v * 3 + k] + (p[o * 3 + k] - p[v * 3 + k]) * f);
                }
                for (int k = 0; k < 2; k++) {
                    suv[v * 2 + k] = (float) (uv[v * 2 + k] + (uv[o * 2 + k] - uv[v * 2 + k]) * f);
                }
            }
            outPos.add(sp);
            outUv.add(suv);
            outNormal.add(nrm.clone());
        }
    }

    /**
     * For a quad spanning Y, returns for every vertex the neighbouring vertex at the other end
     * of its vertical edge; null when the quad is not a Y-spanning rectangle.
     */
    private static int[] verticalPartners(float[] p, float minY, float maxY) {
        int[] partner = new int[4];
        for (int v = 0; v < 4; v++) {
            float vy = p[v * 3 + 1];
            boolean top = Math.abs(vy - minY) < 1e-4f;
            boolean bottom = Math.abs(vy - maxY) < 1e-4f;
            if (!top && !bottom) {
                return null;
            }
            int prev = (v + 3) % 4;
            int next = (v + 1) % 4;
            float py = p[prev * 3 + 1];
            float ny = p[next * 3 + 1];
            boolean prevOther = Math.abs(py - vy) > 1e-4f;
            boolean nextOther = Math.abs(ny - vy) > 1e-4f;
            if (prevOther == nextOther) {
                return null;
            }
            partner[v] = prevOther ? prev : next;
        }
        return partner;
    }

    /** Output buffer, reused between frames. */
    public static final class Output {
        public int quadCount;
        public float[] positions = new float[0];
        public float[] uvs = new float[0];
        public float[] normals = new float[0];

        void ensure(int quads) {
            quadCount = quads;
            if (positions.length < quads * 12) {
                positions = new float[quads * 12];
                uvs = new float[quads * 8];
                normals = new float[quads * 3];
            }
        }
    }

    /**
     * Bends the mesh forwards/backwards.
     *
     * @param angle radians around the part-local X axis
     * @param joint joint position (part-local)
     */
    public Output deform(double angle, Vec3 joint, Output out) {
        return deform(new Vec3(angle, 0, 0), joint, out);
    }

    /**
     * Bends the mesh in any direction: the lower half turns about an axis through the joint.
     *
     * @param bend  rotation vector (radians; direction = axis, length = angle), part-local;
     *              {@code (a, 0, 0)} is the classic forwards/backwards bend
     * @param joint joint position (part-local)
     */
    public Output deform(Vec3 bend, Vec3 joint, Output out) {
        return deform(bend, joint, Vec3.ZERO, Vec3.ZERO, out);
    }

    /**
     * Bends the lower half and turns the hand/foot.
     *
     * @param tip      rotation vector of the hand/foot about the wrist/ankle (radians, part-local,
     *                 rest pose); zero when it does not move or the part has no hand/foot
     * @param tipJoint wrist/ankle position (part-local, rest pose)
     */
    public Output deform(Vec3 bend, Vec3 joint, Vec3 tip, Vec3 tipJoint, Output out) {
        out.ensure(quadCount);
        Axis axis = new Axis(bend);
        Axis tipAxis = tipWeights != null && tip != null ? new Axis(tip) : null;
        if (tipAxis != null && tipAxis.angle == 0) {
            tipAxis = null;
        }
        float[] dst = out.positions;
        for (int v = 0; v < quadCount * 4; v++) {
            double x = positions[v * 3];
            double y = positions[v * 3 + 1];
            double z = positions[v * 3 + 2];
            if (tipAxis != null && tipWeights[v] > 0) {
                tipAxis.rotate(x - tipJoint.x, y - tipJoint.y, z - tipJoint.z, blendAngle(tipAxis.angle, tipWeights[v]));
                x = tipJoint.x + tipAxis.rx;
                y = tipJoint.y + tipAxis.ry;
                z = tipJoint.z + tipAxis.rz;
            }
            double phi = blendAngle(axis.angle, weights[v]);
            axis.rotate(x - joint.x, y - joint.y, z - joint.z, phi);
            dst[v * 3] = (float) (joint.x + axis.rx);
            dst[v * 3 + 1] = (float) (joint.y + axis.ry);
            dst[v * 3 + 2] = (float) (joint.z + axis.rz);
        }
        System.arraycopy(uvs, 0, out.uvs, 0, quadCount * 8);
        for (int q = 0; q < quadCount; q++) {
            computeNormal(q, axis, tipAxis, out);
        }
        return out;
    }

    /** A bend axis (unit vector) and angle, with a Rodrigues rotation that keeps its result. */
    private static final class Axis {
        final double angle;
        final double kx;
        final double ky;
        final double kz;
        double rx;
        double ry;
        double rz;

        Axis(Vec3 bend) {
            double len = bend.length();
            angle = len;
            if (len < 1e-12) {
                kx = 1;
                ky = 0;
                kz = 0;
            } else {
                kx = bend.x / len;
                ky = bend.y / len;
                kz = bend.z / len;
            }
        }

        /** Rotates (x, y, z) about the axis by phi into rx, ry, rz. */
        void rotate(double x, double y, double z, double phi) {
            if (phi == 0) {
                rx = x;
                ry = y;
                rz = z;
                return;
            }
            double c = Math.cos(phi);
            double s = Math.sin(phi);
            double dot = kx * x + ky * y + kz * z;
            double cx = ky * z - kz * y;
            double cy = kz * x - kx * z;
            double cz = kx * y - ky * x;
            rx = x * c + cx * s + kx * dot * (1 - c);
            ry = y * c + cy * s + ky * dot * (1 - c);
            rz = z * c + cz * s + kz * dot * (1 - c);
        }
    }

    /**
     * Dual quaternion blend of "not bent" (weight {@code 1 - w}) and "bent by {@code angle}"
     * (weight {@code w}) around the same axis: a rotation by the returned angle. Matches
     * Blender's Preserve Volume skinning (normalised linear blend of the quaternions, with the
     * same shortest-path sign rule).
     */
    public static double blendAngle(double angle, double w) {
        if (w <= 0) {
            return 0;
        }
        if (w >= 1) {
            return angle;
        }
        double half = angle / 2;
        double cw = Math.cos(half);
        double sw = Math.sin(half);
        if (cw < 0) {
            // Blender flips the quaternion to the same hemisphere before blending
            cw = -cw;
            sw = -sw;
        }
        return 2 * Math.atan2(w * sw, (1 - w) + w * cw);
    }

    private void computeNormal(int q, Axis axis, Axis tipAxis, Output out) {
        float[] p = out.positions;
        int b = q * 12;
        // diagonals of the deformed quad
        double ax = p[b + 6] - p[b];
        double ay = p[b + 7] - p[b + 1];
        double az = p[b + 8] - p[b + 2];
        double bx = p[b + 9] - p[b + 3];
        double by = p[b + 10] - p[b + 4];
        double bz = p[b + 11] - p[b + 5];
        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;
        // reference: rest normal rotated like the middle of the quad
        double w = (weights[q * 4] + weights[q * 4 + 1] + weights[q * 4 + 2] + weights[q * 4 + 3]) / 4.0;
        double n0 = normals[q * 3];
        double n1 = normals[q * 3 + 1];
        double n2 = normals[q * 3 + 2];
        if (tipAxis != null) {
            double tw = (tipWeights[q * 4] + tipWeights[q * 4 + 1] + tipWeights[q * 4 + 2] + tipWeights[q * 4 + 3]) / 4.0;
            tipAxis.rotate(n0, n1, n2, blendAngle(tipAxis.angle, tw));
            n0 = tipAxis.rx;
            n1 = tipAxis.ry;
            n2 = tipAxis.rz;
        }
        axis.rotate(n0, n1, n2, blendAngle(axis.angle, w));
        double rx = axis.rx;
        double ry = axis.ry;
        double rz = axis.rz;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) {
            nx = rx;
            ny = ry;
            nz = rz;
        } else {
            nx /= len;
            ny /= len;
            nz /= len;
            if (nx * rx + ny * ry + nz * rz < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
        }
        out.normals[q * 3] = (float) nx;
        out.normals[q * 3 + 1] = (float) ny;
        out.normals[q * 3 + 2] = (float) nz;
    }

    /** Where a single rest point ends up (part-local), used by tests and attachment code. */
    public static Vec3 deformPoint(Vec3 p, BendProfile profile, double angle, Vec3 joint) {
        return deformPoint(p, profile, new Vec3(angle, 0, 0), joint);
    }

    /** {@link #deformPoint(Vec3, BendProfile, double, Vec3)} for a bend in any direction. */
    public static Vec3 deformPoint(Vec3 p, BendProfile profile, Vec3 bend, Vec3 joint) {
        return deformPoint(p, profile, bend, joint, Vec3.ZERO, Vec3.ZERO);
    }

    /** Where a rest point ends up with the hand/foot turned too (see {@link #deform(Vec3, Vec3, Vec3, Vec3, Output)}). */
    public static Vec3 deformPoint(Vec3 p, BendProfile profile, Vec3 bend, Vec3 joint, Vec3 tip, Vec3 tipJoint) {
        double x = p.x;
        double y = p.y;
        double z = p.z;
        if (profile.tip != null && tip != null && tip.length() > 0) {
            Axis t = new Axis(tip);
            t.rotate(x - tipJoint.x, y - tipJoint.y, z - tipJoint.z, blendAngle(t.angle, profile.tip.weight(p.y)));
            x = tipJoint.x + t.rx;
            y = tipJoint.y + t.ry;
            z = tipJoint.z + t.rz;
        }
        Axis axis = new Axis(bend);
        axis.rotate(x - joint.x, y - joint.y, z - joint.z, blendAngle(axis.angle, profile.weight(p.y)));
        return new Vec3(joint.x + axis.rx, joint.y + axis.ry, joint.z + axis.rz);
    }

    // accessors for tests
    float[] restPositions() {
        return positions;
    }

    float[] restUvs() {
        return uvs;
    }
}
