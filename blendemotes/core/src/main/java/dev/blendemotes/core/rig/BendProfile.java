package dev.blendemotes.core.rig;

/**
 * Describes how a part bends, reproducing the skin weights of the Blender rig's player mesh
 * ({@code player_mesh} in {@code emote_creator.blend}).
 * <p>
 * The bend is a rotation about the part-local X axis through the joint. Vertices are blended
 * between the unbent and the bent transform (linear blend skinning, exactly like Blender's
 * Armature modifier) with a weight that depends on how far below the part pivot they are.
 */
public final class BendProfile {
    /**
     * Elbow: 4 px below the shoulder pivot, the forearm (lower half) moves.
     * Weights measured on the Blender rig's mesh (distance below the shoulder -> forearm weight).
     */
    public static final BendProfile ARM = new BendProfile(4, 0,
            new double[]{1.954, 2.848, 2.947, 3.121, 3.195, 3.394, 3.443, 3.667, 3.691, 3.940,
                    4.188, 4.213, 4.436, 4.486, 4.684, 4.758, 4.932},
            new double[]{0.0, 0.187, 0.187, 0.265, 0.265, 0.343, 0.343, 0.422, 0.422, 0.500,
                    0.578, 0.578, 0.657, 0.657, 0.735, 0.735, 1.0});
    /**
     * Knee: 6 px below the hip pivot, the shin (lower half) moves.
     * Weights measured on the Blender rig's mesh.
     */
    public static final BendProfile LEG = new BendProfile(6, 0,
            new double[]{3.868, 4.761, 4.860, 5.034, 5.109, 5.307, 5.357, 5.580, 5.605, 5.853,
                    6.101, 6.126, 6.349, 6.399, 6.597, 6.672, 6.846, 6.945, 7.838},
            new double[]{0.0, 0.187, 0.187, 0.265, 0.265, 0.343, 0.343, 0.422, 0.422, 0.500,
                    0.578, 0.578, 0.657, 0.657, 0.735, 0.735, 0.813, 0.813, 1.0});
    /** Cape: bends at its middle, lower half moves. */
    public static final BendProfile CAPE = limb(8, 0);
    /**
     * Torso: the rig's {@code torso_bend} bone sits at the middle of the torso and moves the
     * <em>upper</em> half; the weights fade smoothly over the whole torso.
     * Measured from the Blender mesh (distance below the neck -> weight of the bend bone).
     */
    public static final BendProfile TORSO = new BendProfile(6, 0,
            new double[]{-0.611, -0.015, 0.481, 0.977, 1.573, 1.970, 2.665, 2.962, 3.757, 3.955, 4.848, 4.948,
                    5.940, 6.933, 7.032, 7.925, 8.124, 8.918, 9.216, 9.910, 10.307, 10.903, 11.399, 11.896, 12.491},
            new double[]{0.961, 0.961, 0.844, 0.844, 0.738, 0.738, 0.642, 0.642, 0.555, 0.555, 0.474, 0.474,
                    0.400, 0.332, 0.332, 0.269, 0.269, 0.210, 0.210, 0.155, 0.155, 0.105, 0.105, 0.057, 0.057});

    /** Joint position in part-local space (Y down from the pivot). */
    public final double jointY;
    public final double jointZ;
    /** Piecewise linear weight curve: distance below the pivot -> weight of the bent transform. */
    private final double[] ds;
    private final double[] ws;

    public BendProfile(double jointY, double jointZ, double[] ds, double[] ws) {
        if (ds.length != ws.length || ds.length == 0) {
            throw new IllegalArgumentException("bad bend profile");
        }
        this.jointY = jointY;
        this.jointZ = jointZ;
        this.ds = ds.clone();
        this.ws = ws.clone();
    }

    /**
     * Limb profile: rigid above and below the joint with a smooth 4 px transition, as the
     * rig's mesh weights (0 / 0.187 / 0.813 / 1 around the elbow and knee).
     */
    public static BendProfile limb(double jointY, double jointZ) {
        return new BendProfile(jointY, jointZ,
                new double[]{jointY - 2.0, jointY - 1.1, jointY + 1.1, jointY + 2.0},
                new double[]{0.0, 0.187, 0.813, 1.0});
    }

    /** Weight of the bent transform for a vertex {@code y} pixels below the pivot. */
    public double weight(double y) {
        if (y <= ds[0]) {
            return ws[0];
        }
        int n = ds.length;
        if (y >= ds[n - 1]) {
            return ws[n - 1];
        }
        for (int i = 1; i < n; i++) {
            if (y <= ds[i]) {
                double f = (y - ds[i - 1]) / (ds[i] - ds[i - 1]);
                return ws[i - 1] + (ws[i] - ws[i - 1]) * f;
            }
        }
        return ws[n - 1];
    }

    /** Positions where the weight curve changes slope; geometry is split there. */
    public double[] breakpoints() {
        return ds.clone();
    }

    /** Range over which the weight is not constant. */
    public double rampStart() {
        return ds[0];
    }

    public double rampEnd() {
        return ds[ds.length - 1];
    }

    /** True when the moving half is below the joint (limbs); false for the torso. */
    public boolean lowerHalfMoves() {
        return ws[ws.length - 1] > ws[0];
    }
}
