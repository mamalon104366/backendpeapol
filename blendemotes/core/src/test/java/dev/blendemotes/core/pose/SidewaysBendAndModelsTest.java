package dev.blendemotes.core.pose;

import dev.blendemotes.core.TestRunner;
import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.EmoteModel;
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
 * The "cantar" emote of the upgraded rig (tools/blender/upgrade_rig.py): elbow and knee bent
 * sideways, and a microphone model in the right hand. Compared with what Blender computes for
 * the same frames (tools/blender/sample_ground_truth.py --models).
 */
public class SidewaysBendAndModelsTest {
    private static final Mat4 C = Mat4.fromRows(new double[][]{
            {4, 0, 0, 0},
            {0, 0, -4, 24},
            {0, 4, 0, 0},
            {0, 0, 0, 1}});
    private static final Mat4 C_INV = C.invertAffine();
    private static final String[] BONES = {
            "body", "body_control", "waist", "torso", "torso_bend", "head", "cape", "cape_bend",
            "left_arm", "left_arm_bend", "left_item", "right_arm", "right_arm_bend", "right_item",
            "left_leg", "left_leg_bend", "right_leg", "right_leg_bend"};

    private static Map<String, Object> truth;
    private static Animation anim;

    private static synchronized Map<String, Object> action() {
        if (truth == null) {
            truth = JsonUtil.asObject(Json.parse(TestRunner.resource("/blender/cantar_truth.json")), "truth");
            Map<String, Object> root = JsonUtil.asObject(Json.parse(TestRunner.resource("/blender/exact/cantar.json")), "root");
            anim = BedrockAnimationLoader.load(root).get(0).animation;
        }
        return JsonUtil.getObject(JsonUtil.getObject(truth, "actions"), "cantar");
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

    public void testFileHasSidewaysBendsAndAModel() {
        action();
        BoneAnimation left = anim.bone("left_arm");
        TestRunner.check(left != null && !left.bendAxes[BoneAnimation.Z].isEmpty(), "left arm has no sideways bend");
        TestRunner.check(!anim.bone("right_leg").bendAxes[BoneAnimation.Z].isEmpty(), "right leg has no sideways bend");
        TestRunner.check(anim.bone("right_arm").bendAxes[BoneAnimation.Z].isEmpty(), "right arm should only bend forwards");
        TestRunner.check(anim.models.size() == 1, "expected the microphone model");
        EmoteModel mic = anim.models.get(0);
        TestRunner.check(mic.bone.equals("right_item"), "microphone bone: " + mic.bone);
        TestRunner.check(mic.triangleCount() > 50, "microphone triangles: " + mic.triangleCount());
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
                Mat4 mine;
                if (bone.equals("body")) {
                    mine = new Mat4(root);
                } else if (bone.endsWith("_bend")) {
                    String part = bone.substring(0, bone.length() - 5);
                    mine = root.mul(e.world(part)).mul(e.bendSegment(part));
                } else {
                    mine = root.mul(e.world(bone));
                }
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
        System.out.printf(Locale.ROOT, "    cantar bones: max error %.4f px %s%n", max, worst);
        TestRunner.check(max < 0.05, "sideways bends deviate from Blender by " + max + " px");
    }

    public void testSidewaysBentMeshMatchesBlender() {
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
                Vec3 deformed = BendMesh.deformPoint(rest.sub(pivot), part.bend, e.bendVector(bone), rig.joint(part));
                Vec3 mine = root.mul(e.world(bone)).transformPoint(deformed.add(pivot));
                double err = mine.sub(expected).length();
                Double prev = worst.get(bone);
                if (prev == null || err > prev) {
                    worst.put(bone, err);
                }
                worstAll = Math.max(worstAll, err);
            }
            System.out.printf(Locale.ROOT, "    cantar frame %5.1f mesh %s%n", f, worst);
        }
        TestRunner.check(worstAll < 0.1, "sideways bent mesh deviates from Blender by " + worstAll + " px");
    }

    public void testModelFollowsItsBone() {
        Map<String, Object> act = action();
        double fps = JsonUtil.getDouble(truth, "fps", 24);
        EmoteModel mic = anim.models.get(0);
        double worst = 0;
        for (Object fo : JsonUtil.asArray(act.get("models"), "models")) {
            Map<String, Object> frame = JsonUtil.asObject(fo, "frame");
            double f = JsonUtil.getDouble(frame, "frame", 0);
            PlayerPose pose = PoseEvaluator.evaluate(anim, f / fps, new VanillaPose().reset(RigDefinition.BLENDER),
                    RigDefinition.BLENDER, null, new PlayerPose());
            TestRunner.check(pose.models().size() == 1 && pose.models().get(0) == mic, "the pose does not carry the model");
            PlayerPose fading = PlayerPose.blend(pose, new PlayerPose(), 0.5, new PlayerPose());
            TestRunner.check(fading.models().size() == 1 && fading.boneMatrix(mic.bone) != null,
                    "the model disappeared while the emote fades out");
            Mat4 m = pose.root.mul(pose.boneMatrix(mic.bone));
            List<Object> models = JsonUtil.asArray(frame.get("models"), "models");
            List<Object> verts = JsonUtil.asArray(JsonUtil.asObject(models.get(0), "model").get("vertices"), "vertices");
            TestRunner.check(verts.size() * 3 == mic.positions.length, "vertex count differs");
            double frameWorst = 0;
            for (int i = 0; i < verts.size(); i++) {
                Vec3 rest = new Vec3(mic.positions[i * 3], mic.positions[i * 3 + 1], mic.positions[i * 3 + 2]);
                Vec3 expected = C.transformPoint(vec(verts.get(i)));
                frameWorst = Math.max(frameWorst, m.transformPoint(rest).sub(expected).length());
            }
            System.out.printf(Locale.ROOT, "    cantar frame %5.1f microphone max error %.4f px%n", f, frameWorst);
            worst = Math.max(worst, frameWorst);
        }
        TestRunner.check(worst < 0.05, "the microphone deviates from Blender by " + worst + " px");
    }

    public void testCodecKeepsSidewaysBendsAndModels() throws Exception {
        action();
        Emote emote = new Emote(UUID.randomUUID(), new EmoteInfo("Cantar", "", "", new ArrayList<EmoteInfo.Badge>(), null), anim, "test");
        byte[] data = EmoteCodec.encode(emote);
        Emote back = EmoteCodec.decode(data, "net");
        System.out.println("    cantar encoded: " + data.length + " bytes");
        TestRunner.check(back.animation.models.size() == 1, "model lost on the network");
        EmoteModel a = anim.models.get(0);
        EmoteModel b = back.animation.models.get(0);
        TestRunner.check(a.textureId.equals(b.textureId) && a.positions.length == b.positions.length, "model changed on the network");
        PlayerPose pa = PoseEvaluator.evaluate(anim, 0.3, new VanillaPose(), new PlayerPose());
        PlayerPose pb = PoseEvaluator.evaluate(back.animation, 0.3, new VanillaPose(), new PlayerPose());
        for (PlayerPart p : PlayerPart.VALUES) {
            TestRunner.check(pa.bendVector(p).sub(pb.bendVector(p)).length() < 1e-4, "bend of " + p + " changed on the network");
        }
    }
}
