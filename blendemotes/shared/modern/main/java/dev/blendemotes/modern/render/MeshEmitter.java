package dev.blendemotes.modern.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.blendemotes.core.bend.BendMesh;

/** Sends deformed quads to a vertex consumer (same vertex layout as {@code ModelPart.Cube}). */
public final class MeshEmitter {
    private MeshEmitter() {
    }

    /**
     * @param color ARGB tint (vanilla passes white for players)
     */
    public static void emit(BendMesh.Output mesh, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                            int color) {
        //#if MC < 12100
        float a = (color >>> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;
        //#endif
        for (int q = 0; q < mesh.quadCount; q++) {
            float nx = mesh.normals[q * 3];
            float ny = mesh.normals[q * 3 + 1];
            float nz = mesh.normals[q * 3 + 2];
            for (int v = 0; v < 4; v++) {
                int i = q * 4 + v;
                float x = mesh.positions[i * 3] / 16.0F;
                float y = mesh.positions[i * 3 + 1] / 16.0F;
                float z = mesh.positions[i * 3 + 2] / 16.0F;
                float u = mesh.uvs[i * 2];
                float t = mesh.uvs[i * 2 + 1];
                //#if MC >= 12100
                consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, t).setOverlay(overlay).setLight(light)
                        .setNormal(pose, nx, ny, nz);
                //#else
                consumer.vertex(pose.pose(), x, y, z).color(r, g, b, a).uv(u, t).overlayCoords(overlay).uv2(light)
                        .normal(pose.normal(), nx, ny, nz).endVertex();
                //#endif
            }
        }
    }

    /** Packs float colour channels (0..1) into ARGB. */
    public static int argb(float r, float g, float b, float a) {
        return ((int) (a * 255) & 255) << 24 | ((int) (r * 255) & 255) << 16 | ((int) (g * 255) & 255) << 8
                | ((int) (b * 255) & 255);
    }
}
