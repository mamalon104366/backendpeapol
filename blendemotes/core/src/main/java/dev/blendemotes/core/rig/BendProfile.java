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
    /** Elbow: 4 px below the shoulder pivot, the forearm (lower half) moves. */
    public static final BendProfile ARM = limb(4, 0);
    /** Knee: 6 px below the hip pivot, the shin (lower half) moves. */
    public static final BendProfile LEG = limb(6, 0);
    /** Cape: bends at its middle, lower half moves. */
    public static final BendProfile CAPE = limb(8, 0);
    /**
     * Torso: the rig's {@code torso_bend} bone sits at the middle of the torso and moves the
     * <em>upper</em> half; the weights fade smoothly over the whole torso.
     * Measured from the Blender mesh (distance below the neck -> weight of the bend bone).
     */
    public static final BendProfile TORSO = new BendProfile(6, 0,
            new double[]{-0.31, 0.73, 1.77, 2.81, 3.86, 4.90, 5.94, 6.98, 8.02, 9.07, 10.11, 11.15, 12.19},
            new double[]{0.961, 0.844, 0.738, 0.642, 0.555, 0.474, 0.400, 0.332, 0.269, 0.210, 0.155, 0.105, 0.057});

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
