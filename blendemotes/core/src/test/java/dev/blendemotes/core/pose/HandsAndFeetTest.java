package dev.blendemotes.core.pose;

import dev.blendemotes.core.TestRunner;
import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.io.BedrockAnimationLoader;
import dev.blendemotes.core.bend.BendMesh;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.emote.EmoteInfo;
import dev.blendemotes.core.json.Json;
import dev.blendemotes.core.json.JsonUtil;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.net.EmoteCodec;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The "manos_pies" action of the rig (tools/blender/test_fixtures.py): palms and feet turning
 * on every axis, elbows and knees bent forwards and sideways, one leg driven by IK. Compared
 * with what Blender computes for the same frames (tools/blender/sample_ground_truth.py).
 */
public class HandsAndFeetTest {
    private static final Mat4 C = Mat4.fromRows(new double[][]{
            {4, 0, 0, 0},
            {0, 0, -4, 24},
            {0, 4, 0, 0},
            {0, 0, 0, 1}});
    private static final Mat4 C_INV = C.invertAffine();
    private static final String[] BONES = {
            "body", "body_control", "waist", "torso", "head",
            "left_arm", "left_arm_bend", "left_hand", "left_item",
            "right_arm", "right_arm_bend", "right_hand", "right_item",
            "left_leg", "left_leg_bend", "left_foot",
            "right_leg", "right_leg_bend", "right_foot"};

    private static Map<String, Object> truth;
    private static Animation anim;

    private static synchronized Map<String, Object> action() {
        if (truth == null) {
            truth = JsonUtil.asObject(Json.parse(TestRunner.resource("/blender/manos_pies_truth.json")), "truth");
            Map<String, Object> root = JsonUtil.asObject(Json.parse(TestRunner.resource("/blender/exact/manos_pies.json")), "root");
            anim = BedrockAnimationLoader.load(root).get(0).animation;
        }
        return JsonUtil.getObject(JsonUtil.getObject(truth, "actions"), "manos_pies");
    }

    private static Mat4 matrix(Object rows) {
        List<Object> r = JsonUtil.asArray(rows, "matrix");
        double[][] m = new double[4][4];
        for (int i = 0; i < 4; i++) {
            List<Object> row = JsonUtil.asArray(r.get(i), "row");
            for (int j = 0; j < 4; j++) {
                m[i][j] = ((Number) row.get(j)).doubleValue();
            }
        }
        return Mat4.fromRows(m);
    }

    private static Vec3 vec(Object o) {
        List<Object> v = JsonUtil.asArray(o, "vec");
        return new Vec3(((Number) v.get(0)).doubleValue(), ((Number) v.get(1)).doubleValue(), ((Number) v.get(2)).doubleValue());
    }

    /** Our transform for a Blender bone. */
    private static Mat4 ours(PoseEvaluator e, Mat4 root, String bone) {
        if (bone.equals("body")) {
            return new Mat4(root);
        }
        PlayerPart limb = PlayerPart.byTipBone(bone);
        if (limb != null) {
            return root.mul(e.world(limb.bone)).mul(e.bendSegment(limb.bone)).mul(e.tipSegment(limb.bone));
        }
        if (bone.endsWith("_bend")) {
            String part = bone.substring(0, bone.length() - 5);
            return root.mul(e.world(part)).mul(e.bendSegment(part));
        }
        return root.mul(e.world(bone));
    }

    public void testFileHasHandsAndFeet() {
        action();
        for (PlayerPart p : new PlayerPart[]{PlayerPart.RIGHT_ARM, PlayerPart.LEFT_ARM, PlayerPart.RIGHT_LEG, PlayerPart.LEFT_LEG}) {
            TestRunner.check(anim.bone(p.bone) != null && anim.bone(p.bone).hasTip(), p + " has no hand/foot channel");
        }
        TestRunner.check(anim.bone("right_hand") == null && anim.bone("left_foot") == null,
                "the palm and foot bones must travel in the tip channel, not as loose bones");
        TestRunner.check(!anim.bone("left_leg").rotation[0].isEmpty(), "the IK leg was exported frozen");
    }

    public void testBonesMatchBlender() {
        Map<String, Object> act = action();
        Map<String, Object> rest = JsonUtil.getObject(truth, "rest");
        double fps = JsonUtil.getDouble(truth, "fps", 24);
        VanillaPose vanilla = new VanillaPose().reset(RigDefinition.BLENDER);
        Map<String, Double> worst = new HashMap<String, Double>();
        double max = 0;
        for (Object frameObj : JsonUtil.asArray(act.get("export"), "export")) {
            Map<String, Object> frame = JsonUtil.asObject(frameObj, "frame");
            double t = JsonUtil.getDouble(frame, "frame", 0) / fps;
            PoseEvaluator e = PoseEvaluator.create(anim, t, vanilla, RigDefinition.BLENDER);
            Mat4 root = e.local(PoseEvaluator.BODY);
            Map<String, Object> bones = JsonUtil.getObject(frame, "bones");
            for (String bone : BONES) {
                Mat4 restBl = matrix(rest.get(bone));
                Mat4 blender = C.mul(matrix(bones.get(bone)).mul(restBl.invertAffine())).mul(C_INV);
                Mat4 mine = ours(e, root, bone);
                Mat4 restMc = C.mul(restBl);
                double err = 0;
                double[][] local = {{0, 0, 0}, {0, 1, 0}, {0.5, 0, 0}, {0, 0, 0.5}, {-0.5, 0.5, -0.5}};
                for (double[] p : local) {
                    Vec3 rp = restMc.transformPoint(p[0], p[1], p[2]);
                    err = Math.max(err, blender.transformPoint(rp).sub(mine.transformPoint(rp)).length());
                }
                Double prev = worst.get(bone);
                if (prev == null || err > prev) {
                    worst.put(bone, err);
                }
                max = Math.max(max, err);
            }
        }
        System.out.printf(Locale.ROOT, "    manos_pies bones: max error %.4f px %s%n", max, worst);
        TestRunner.check(max < 0.05, "hands and feet deviate from Blender by " + max + " px");
    }

    public void testMeshMatchesBlender() {
        Map<String, Object> act = action();
        double fps = JsonUtil.getDouble(truth, "fps", 24);
        RigDefinition rig = RigDefinition.BLENDER;
        double worstAll = 0;
        for (Object fo : JsonUtil.asArray(act.get("mesh"), "mesh")) {
            Map<String, Object> frame = JsonUtil.asObject(fo, "frame");
            double f = JsonUtil.getDouble(frame, "frame", 0);
            PoseEvaluator e = PoseEvaluator.create(anim, f / fps, new VanillaPose().reset(rig), rig);
            Mat4 root = e.local(PoseEvaluator.BODY);
            Map<String, Double> worst = new HashMap<String, Double>();
            for (Object vo : JsonUtil.asArray(frame.get("vertices"), "vertices")) {
                List<Object> v = JsonUtil.asArray(vo, "vertex");
                String bone = (String) v.get(0);
                PlayerPart part = PlayerPart.byBone(bone);
                Vec3 rest = C.transformPoint(vec(v.get(1)));
                Vec3 expected = C.transformPoint(vec(v.get(2)));
                Vec3 pivot = rig.pivot(part);
                Vec3 deformed = BendMesh.deformPoint(rest.sub(pivot), part.bend, e.bendVector(bone), rig.joint(part),
                        e.tipVector(bone), rig.tipJoint(part));
                Vec3 mine = root.mul(e.world(bone)).transformPoint(deformed.add(pivot));
                double err = mine.sub(expected).length();
                Double prev = worst.get(bone);
                if (prev == null || err > prev) {
                    worst.put(bone, err);
                }
                worstAll = Math.max(worstAll, err);
            }
            System.out.printf(Locale.ROOT, "    manos_pies frame %5.1f mesh %s%n", f, worst);
        }
        TestRunner.check(worstAll < 0.1, "the mesh with hands and feet deviates from Blender by " + worstAll + " px");
    }

    public void testPoseAndCodecKeepHandsAndFeet() throws Exception {
        action();
        PlayerPose pose = PoseEvaluator.evaluate(anim, 0.2, new VanillaPose(), new PlayerPose());
        TestRunner.check(pose.tipVector(PlayerPart.RIGHT_ARM).length() > 0.1, "the pose lost the palm");
        TestRunner.check(pose.tipVector(PlayerPart.HEAD).length() == 0, "only arms and legs have a hand/foot");
        PlayerPose half = PlayerPose.blend(pose, new PlayerPose(), 0.5, new PlayerPose());
        TestRunner.near(pose.tipVector(PlayerPart.RIGHT_ARM).length() / 2, half.tipVector(PlayerPart.RIGHT_ARM).length(), 1e-9,
                "the palm does not fade with the emote");
        Emote emote = new Emote(UUID.randomUUID(), new EmoteInfo("Manos", "", "", new ArrayList<EmoteInfo.Badge>(), null), anim, "test");
        Emote back = EmoteCodec.decode(EmoteCodec.encode(emote), "net");
        PlayerPose other = PoseEvaluator.evaluate(back.animation, 0.2, new VanillaPose(), new PlayerPose());
        for (PlayerPart p : PlayerPart.VALUES) {
            TestRunner.check(pose.tipVector(p).sub(other.tipVector(p)).length() < 1e-4, "hand/foot of " + p + " changed on the network");
        }
    }
}
