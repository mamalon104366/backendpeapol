package dev.blendemotes.core.bend;

import dev.blendemotes.core.TestRunner;
import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.io.BedrockAnimationLoader;
import dev.blendemotes.core.json.Json;
import dev.blendemotes.core.json.JsonUtil;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.MeshAccess;
import dev.blendemotes.core.rig.BendProfile;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BendMeshTest {
    private static final Mat4 C = Mat4.fromRows(new double[][]{
            {4, 0, 0, 0},
            {0, 0, -4, 24},
            {0, 4, 0, 0},
            {0, 0, 0, 1}});

    /** Vertices of Blender's deformed player mesh vs our skinning of the same rest points. */
    public void testSkinningMatchesBlenderMesh() {
        Map<String, Object> root = JsonUtil.asObject(Json.parse(TestRunner.resource("/blender/inchworm.json")), "root");
        Animation anim = BedrockAnimationLoader.load(root).get(0).animation;
        Map<String, Object> truth = JsonUtil.asObject(Json.parse(TestRunner.resource("/blender/mesh_truth.json")), "t");
        RigDefinition rig = RigDefinition.BLENDER;
        double worstAll = 0;
        for (Object fo : JsonUtil.asArray(truth.get("mesh"), "mesh")) {
            Map<String, Object> frame = JsonUtil.asObject(fo, "frame");
            double f = JsonUtil.getDouble(frame, "frame", 0);
            MeshAccess access = new MeshAccess(anim, f / 24.0, rig);
            Map<String, Double> worst = new HashMap<String, Double>();
            double sum = 0;
            int n = 0;
            for (Object vo : JsonUtil.asArray(frame.get("vertices"), "vertices")) {
                List<Object> v = JsonUtil.asArray(vo, "vertex");
                String bone = (String) v.get(0);
                PlayerPart part = PlayerPart.byBone(bone);
                Vec3 restBl = vec(v.get(1));
                Vec3 defBl = vec(v.get(2));
                Vec3 rest = C.transformPoint(restBl);
                Vec3 expected = C.transformPoint(defBl);
                Vec3 pivot = rig.pivot(part);
                Vec3 deformed = BendMesh.deformPoint(rest.sub(pivot), part.bend, access.bendAngle(bone), rig.joint(part));
                Vec3 mine = access.world(bone).transformPoint(deformed.add(pivot));
                double err = mine.sub(expected).length();
                sum += err;
                n++;
                Double prev = worst.get(bone);
                if (prev == null || err > prev) {
                    worst.put(bone, err);
                }
            }
            double max = 0;
            for (double d : worst.values()) {
                max = Math.max(max, d);
            }
            worstAll = Math.max(worstAll, max);
            System.out.printf(Locale.ROOT, "    frame %5.1f: mesh error max %.4f px mean %.4f px %s%n", f, max, sum / n, worst);
        }
        TestRunner.check(worstAll < 0.1, "skinning deviates from Blender's mesh by " + worstAll + " px");
    }

    public void testStripsKeepTextureAndClosure() {
        CubeGeometry arm = CubeGeometry.box(40, 16, -3, -2, -2, 4, 12, 4, 0, false, 64, 64);
        BendMesh mesh = arm.toBendMesh(BendProfile.ARM);
        TestRunner.check(mesh.quadCount() > 6, "arm was not subdivided");
        // straight: output equals rest
        BendMesh.Output out = mesh.deform(0, new Vec3(0, 4, 0), new BendMesh.Output());
        float[] rest = mesh.restPositions();
        for (int i = 0; i < mesh.quadCount() * 12; i++) {
            TestRunner.near(rest[i], out.positions[i], 1e-5, "unbent vertex moved");
        }
        // bent 90 degrees: every rest position maps to one deformed position (no gaps)
        out = mesh.deform(Math.PI / 2, new Vec3(0, 4, 0), new BendMesh.Output());
        Map<String, float[]> seen = new HashMap<String, float[]>();
        for (int v = 0; v < mesh.quadCount() * 4; v++) {
            String key = String.format(Locale.ROOT, "%.4f,%.4f,%.4f", rest[v * 3], rest[v * 3 + 1], rest[v * 3 + 2]);
            float[] p = {out.positions[v * 3], out.positions[v * 3 + 1], out.positions[v * 3 + 2]};
            float[] other = seen.get(key);
            if (other != null) {
                for (int k = 0; k < 3; k++) {
                    TestRunner.near(other[k], p[k], 1e-5, "shared vertex split apart (gap)");
                }
            } else {
                seen.put(key, p);
            }
        }
        // the hand end moved by the full rotation, the shoulder did not move
        Vec3 hand = BendMesh.deformPoint(new Vec3(0, 10, 0), BendProfile.ARM, Math.PI / 2, new Vec3(0, 4, 0));
        TestRunner.near(4, hand.y, 1e-9, "hand y");
        TestRunner.near(6, hand.z, 1e-9, "hand z");
        Vec3 shoulder = BendMesh.deformPoint(new Vec3(0, -2, 0), BendProfile.ARM, Math.PI / 2, new Vec3(0, 4, 0));
        TestRunner.near(-2, shoulder.y, 1e-9, "shoulder y");
        // texture coordinates stay inside the arm's area of the skin
        float[] uv = mesh.restUvs();
        for (int i = 0; i < mesh.quadCount() * 4; i++) {
            float u = uv[i * 2] * 64;
            float vv = uv[i * 2 + 1] * 64;
            TestRunner.check(u >= 40 - 1e-3 && u <= 56 + 1e-3 && vv >= 16 - 1e-3 && vv <= 32 + 1e-3, "uv outside the arm: " + u + "," + vv);
        }
    }

    public void testCubeLayoutMatchesMinecraft() {
        // right arm, wide: texture (40,16), box (-3,-2,-2) 4x12x4
        CubeGeometry c = CubeGeometry.box(40, 16, -3, -2, -2, 4, 12, 4, 0, false, 64, 64);
        // north face (index 3): vertices 1,0,3,2 -> (x2,y1,z1),(x1,y1,z1),(x1,y2,z1),(x2,y2,z1)
        float[] p = c.positions;
        int f = 3 * 12;
        TestRunner.near(1, p[f], 1e-6, "north v0 x");
        TestRunner.near(-2, p[f + 1], 1e-6, "north v0 y");
        TestRunner.near(-2, p[f + 2], 1e-6, "north v0 z");
        // uv of north v0 = (u2, v1) = (40+4+4, 16+4)
        TestRunner.near(48 / 64f, c.uvs[3 * 8], 1e-6, "north v0 u");
        TestRunner.near(20 / 64f, c.uvs[3 * 8 + 1], 1e-6, "north v0 v");
        TestRunner.near(-1, c.normals[3 * 3 + 2], 1e-6, "north normal");
        // mirrored boxes flip the X normal and reverse the vertex order
        CubeGeometry m = CubeGeometry.box(40, 16, -1, -2, -2, 4, 12, 4, 0, true, 64, 64);
        TestRunner.near(1, m.normals[2 * 3], 1e-6, "mirrored west normal");
    }

    private static Vec3 vec(Object o) {
        List<Object> l = JsonUtil.asArray(o, "vec");
        return new Vec3(((Number) l.get(0)).doubleValue(), ((Number) l.get(1)).doubleValue(), ((Number) l.get(2)).doubleValue());
    }
}
