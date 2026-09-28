package dev.blendemotes.modern.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.blendemotes.core.anim.EmoteModel;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.modern.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
//#if MC >= 12111
import net.minecraft.client.renderer.rendertype.RenderTypes;
//#endif
//#if MC >= 12109
import net.minecraft.client.renderer.SubmitNodeCollector;
//#else
import net.minecraft.client.renderer.MultiBufferSource;
//#endif
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws the models an emote attaches to the rig's bones (a microphone, a horse...). The pose
 * stack must be in the player's model space with the rig's root applied, like the cape layer's.
 */
public final class EmoteModels {
    /** Texture of each model texture (by its hash); null when the image could not be read. */
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private EmoteModels() {
    }

    private static ResourceLocation texture(EmoteModel model) {
        if (TEXTURES.containsKey(model.textureId)) {
            return TEXTURES.get(model.textureId);
        }
        ResourceLocation id = null;
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(model.texture));
            String path = "model/" + model.textureId;
            //#if MC >= 12105
            DynamicTexture texture = new DynamicTexture(() -> "blendemotes:" + path, image);
            //#else
            DynamicTexture texture = new DynamicTexture(image);
            //#endif
            id = Compat.id("blendemotes", path);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        } catch (Exception e) {
            id = null;
        }
        TEXTURES.put(model.textureId, id);
        return id;
    }

    /** No face culling (open meshes and planes stay visible from behind) and alpha blending. */
    private static RenderType renderType(ResourceLocation texture) {
        //#if MC >= 12111
        return RenderTypes.entityTranslucent(texture);
        //#else
        return RenderType.entityTranslucent(texture);
        //#endif
    }

    //#if MC >= 12109
    /** Queues the models of the pose (drawing is deferred; the pose stack is copied now). */
    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, PlayerPose pose) {
        for (EmoteModel model : pose.models()) {
            Mat4 bone = pose.boneMatrix(model.bone);
            ResourceLocation texture = bone == null ? null : texture(model);
            if (texture == null) {
                continue;
            }
            poseStack.pushPose();
            PoseMath.mul(poseStack, bone);
            collector.submitCustomGeometry(poseStack, renderType(texture), (p, consumer) -> emit(model, p, consumer, light));
            poseStack.popPose();
        }
    }
    //#else
    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light, PlayerPose pose) {
        for (EmoteModel model : pose.models()) {
            Mat4 bone = pose.boneMatrix(model.bone);
            ResourceLocation texture = bone == null ? null : texture(model);
            if (texture == null) {
                continue;
            }
            poseStack.pushPose();
            PoseMath.mul(poseStack, bone);
            emit(model, poseStack.last(), buffers.getBuffer(renderType(texture)), light);
            poseStack.popPose();
        }
    }
    //#endif

    /** Triangles as quads with the last corner repeated (entity render types draw quads). */
    private static void emit(EmoteModel model, PoseStack.Pose pose, VertexConsumer consumer, int light) {
        int overlay = OverlayTexture.NO_OVERLAY;
        int corners = model.positions.length / 3;
        for (int t = 0; t + 2 < corners; t += 3) {
            for (int k = 0; k < 4; k++) {
                int i = t + Math.min(k, 2);
                float x = model.positions[i * 3] / 16.0F;
                float y = model.positions[i * 3 + 1] / 16.0F;
                float z = model.positions[i * 3 + 2] / 16.0F;
                float u = model.uvs[i * 2];
                float v = model.uvs[i * 2 + 1];
                float nx = model.normals[i * 3];
                float ny = model.normals[i * 3 + 1];
                float nz = model.normals[i * 3 + 2];
                //#if MC >= 12100
                consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(overlay).setLight(light)
                        .setNormal(pose, nx, ny, nz);
                //#else
                consumer.vertex(pose.pose(), x, y, z).color(1.0F, 1.0F, 1.0F, 1.0F).uv(u, v).overlayCoords(overlay)
                        .uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
                //#endif
            }
        }
    }
}
