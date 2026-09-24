package dev.blendemotes.core.pose;

import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.Track;
import dev.blendemotes.core.anim.molang.Molang;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates an {@link Animation} on the player rig, reproducing Blender's armature
 * hierarchy: {@code body -> body_control -> waist -> torso/head/arms}, {@code torso_bend ->
 * cape}, {@code arm_bend -> item}...
 * <p>
 * Channel conventions (as written by the Blender exporter, Bedrock style):
 * rotation in degrees applied X then Y then Z around Minecraft model axes; position in pixels
 * with Y up; scale factors; bend in degrees around the part's X axis.
 */
public final class PoseEvaluator {
    public static final String BODY = "body";

    private final Animation anim;
    private final double time;
    private final VanillaPose vanilla;
    private final RigDefinition rig;
    private final Molang.Context ctx;
    private final Map<String, Mat4> world = new HashMap<String, Mat4>();
    private final Set<String> visiting = new HashSet<String>();

    private PoseEvaluator(Animation anim, double time, VanillaPose vanilla, RigDefinition rig, Molang.Context ctx) {
        this.anim = anim;
        this.time = time;
        this.vanilla = vanilla;
        this.rig = rig;
        this.ctx = ctx;
    }

    /**
     * @param anim    animation, or null for the plain vanilla pose
     * @param time    seconds inside the animation (loop already applied)
     * @param vanilla the vanilla pose of this frame (used for axes the emote does not animate)
     */
    public static PlayerPose evaluate(Animation anim, double time, VanillaPose vanilla, RigDefinition rig,
                                      Molang.Context ctx, PlayerPose out) {
        PoseEvaluator e = new PoseEvaluator(anim, time, vanilla == null ? new VanillaPose() : vanilla, rig,
                ctx == null ? Molang.ZERO_CONTEXT : ctx);
        out.setRig(rig);
        out.root.set(e.local(BODY));
        for (PlayerPart p : PlayerPart.VALUES) {
            out.setPart(p, e.world(p.bone), e.bendAngle(p.bone));
        }
        return out;
    }

    /** Transform of the "body" root bone only. */
    public static Mat4 rootTransform(Animation anim, double time, RigDefinition rig) {
        return new PoseEvaluator(anim, time, new VanillaPose(), rig, Molang.ZERO_CONTEXT).local(BODY);
    }

    /** For tests and tools: access to intermediate bone transforms. */
    static PoseEvaluator create(Animation anim, double time, VanillaPose vanilla, RigDefinition rig) {
        return new PoseEvaluator(anim, time, vanilla, rig, Molang.ZERO_CONTEXT);
    }

    public static PlayerPose evaluate(Animation anim, double time, VanillaPose vanilla, PlayerPose out) {
        return evaluate(anim, time, vanilla, RigDefinition.MINECRAFT, Molang.ZERO_CONTEXT, out);
    }

    // ------------------------------------------------------------------ hierarchy

    private String parentOf(String bone) {
        if (anim != null) {
            String explicit = anim.parents.get(bone);
            if (explicit != null) {
                if (explicit.equals(BODY) || explicit.equals(bone)) {
                    return null;
                }
                return explicit;
            }
        }
        PlayerPart part = PlayerPart.byBone(bone);
        if (part != null && part.holder() != null) {
            return part.holder().bone;
        }
        return null;
    }

    /** Items hang from the forearm and the cape from the upper torso, like in the Blender rig. */
    private boolean attachesToBendSegment(String bone, String parent) {
        if (anim != null && anim.parents.containsKey(bone)) {
            return false;
        }
        PlayerPart part = PlayerPart.byBone(bone);
        return part != null && part.holder() != null && part.holder().bone.equals(parent);
    }

    Mat4 world(String bone) {
        Mat4 cached = world.get(bone);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(bone)) {
            return new Mat4(); // cycle in "parents": break it
        }
        Mat4 result;
        String parent = parentOf(bone);
        if (parent == null) {
            result = new Mat4();
        } else {
            result = new Mat4(world(parent));
            if (attachesToBendSegment(bone, parent)) {
                result.mulLocal(bendSegment(parent));
            }
        }
        result.mulLocal(local(bone));
        if (anim != null && anim.applyBendToOtherBones && isUpperBodyFollower(bone)) {
            Mat4 torso = world(PlayerPart.TORSO.bone);
            Mat4 follow = torso.mul(bendSegment(PlayerPart.TORSO.bone)).mul(torso.invertAffine());
            result = follow.mul(result);
        }
        visiting.remove(bone);
        world.put(bone, result);
        return result;
    }

    private static boolean isUpperBodyFollower(String bone) {
        return bone.equals(PlayerPart.HEAD.bone) || bone.equals(PlayerPart.RIGHT_ARM.bone) || bone.equals(PlayerPart.LEFT_ARM.bone);
    }

    /** Rotation of the moving half of a bendable part, in the part's rest model space. */
    Mat4 bendSegment(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        if (part == null || part.bend == null) {
            return new Mat4();
        }
        double angle = bendAngle(bone);
        if (angle == 0) {
            return new Mat4();
        }
        Vec3 j = rig.pivot(part).add(rig.joint(part));
        return Mat4.translation(j.x, j.y, j.z).mulLocal(Mat4.rotationX(angle)).mulLocal(Mat4.translation(-j.x, -j.y, -j.z));
    }

    double bendAngle(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        if (anim == null || part == null || part.bend == null) {
            return 0;
        }
        BoneAnimation ba = anim.bone(bone);
        if (ba == null || ba.bend.isEmpty()) {
            return 0;
        }
        return Math.toRadians(ba.bend.evaluate(time, ctx));
    }

    private Vec3 pivotOf(String bone, PlayerPart part) {
        if (part != null) {
            return rig.pivot(part);
        }
        if (bone.equals(BODY)) {
            return rig.bodyPivot;
        }
        if (anim != null) {
            Vec3 p = anim.pivots.get(bone);
            if (p != null) {
                return RigDefinition.bedrockPivotToModel(p);
            }
        }
        return new Vec3(0, 24, 0);
    }

    /** Local delta of one bone: T(offset) * T(pivot) * Rz Ry Rx * S * T(-pivot). */
    Mat4 local(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        Vec3 pivot = pivotOf(bone, part);
        BoneAnimation ba = anim == null ? null : anim.bone(bone);

        double[] offset = new double[3];
        double[] rot = new double[3];
        double[] scale = {1, 1, 1};
        if (part != null && !part.isItem()) {
            PartTransform v = vanilla.get(part);
            offset[0] = v.x - pivot.x;
            offset[1] = v.y - pivot.y;
            offset[2] = v.z - pivot.z;
            rot[0] = v.pitch;
            rot[1] = v.yaw;
            rot[2] = v.roll;
            scale[0] = v.scaleX;
            scale[1] = v.scaleY;
            scale[2] = v.scaleZ;
        }
        // Blender rig files: values are relative to the rig's (slightly tilted) bone frame
        double tilt = anim != null && anim.blenderRig && part != null ? rig.blenderTilt(part) : 0;
        boolean xzy = anim != null && anim.blenderRig && part != null && rig.blenderXzyOrder(part);
        double axisScale = tilt != 0 ? 1.0 / Math.cos(tilt) : 1.0;
        boolean animatedOffset = false;
        boolean animatedRot = false;
        boolean animatedScale = false;
        if (ba != null) {
            for (int a = 0; a < 3; a++) {
                double k = a == 0 ? 1 : axisScale;
                Track pos = ba.position[a];
                if (!pos.isEmpty()) {
                    double v = pos.evaluate(time, ctx) * k;
                    offset[a] = a == 1 ? -v : v; // Bedrock Y up -> model Y down
                    animatedOffset = true;
                }
                Track r = ba.rotation[a];
                if (!r.isEmpty()) {
                    rot[a] = Math.toRadians(r.evaluate(time, ctx)) * k;
                    animatedRot = true;
                }
                Track s = ba.scale[a];
                if (!s.isEmpty()) {
                    scale[a] = s.evaluate(time, ctx) * k;
                    animatedScale = true;
                }
            }
        }
        Mat4 tiltM = tilt != 0 ? Mat4.rotationX(tilt) : null;
        Mat4 tiltInv = tilt != 0 ? Mat4.rotationX(-tilt) : null;

        double ox = offset[0];
        double oy = offset[1];
        double oz = offset[2];
        if (tiltM != null && animatedOffset) {
            Vec3 o = tiltM.transformDirection(ox, oy, oz);
            ox = o.x;
            oy = o.y;
            oz = o.z;
        }
        Mat4 m = Mat4.translation(ox + pivot.x, oy + pivot.y, oz + pivot.z);
        boolean frame = tiltM != null && (animatedRot || animatedScale);
        if (frame) {
            m.mulLocal(tiltM);
        }
        if (xzy) {
            m.mulLocal(Mat4.rotationY(rot[1])).mulLocal(Mat4.rotationZ(rot[2])).mulLocal(Mat4.rotationX(rot[0]));
        } else {
            m.mulLocal(Mat4.rotationZYX(rot[0], rot[1], rot[2]));
        }
        if (scale[0] != 1 || scale[1] != 1 || scale[2] != 1) {
            m.mulLocal(Mat4.scaling(scale[0], scale[1], scale[2]));
        }
        if (frame) {
            m.mulLocal(tiltInv);
        }
        m.mulLocal(Mat4.translation(-pivot.x, -pivot.y, -pivot.z));
        return m;
    }
}
