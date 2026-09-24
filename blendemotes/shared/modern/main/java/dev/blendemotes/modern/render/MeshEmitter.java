package dev.blendemotes.modern.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.blendemotes.core.bend.BendMesh;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Sends deformed quads to a vertex consumer (same vertex layout as {@code ModelPart.Cube}). */
public final class MeshEmitter {
    private MeshEmitter() {
    }

    public static void emit(BendMesh.Output mesh, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                            float r, float g, float b, float a) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        Vector4f p = new Vector4f();
        Vector3f nv = new Vector3f();
        for (int q = 0; q < mesh.quadCount; q++) {
            nv.set(mesh.normals[q * 3], mesh.normals[q * 3 + 1], mesh.normals[q * 3 + 2]);
            n.transform(nv);
            for (int v = 0; v < 4; v++) {
                int i = q * 4 + v;
                p.set(mesh.positions[i * 3] / 16.0F, mesh.positions[i * 3 + 1] / 16.0F, mesh.positions[i * 3 + 2] / 16.0F, 1.0F);
                m.transform(p);
                consumer.vertex(p.x(), p.y(), p.z(), r, g, b, a, mesh.uvs[i * 2], mesh.uvs[i * 2 + 1], overlay, light,
                        nv.x(), nv.y(), nv.z());
            }
        }
    }
}
