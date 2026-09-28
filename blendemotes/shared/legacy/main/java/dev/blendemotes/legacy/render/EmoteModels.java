package dev.blendemotes.legacy.render;

import dev.blendemotes.core.anim.EmoteModel;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.legacy.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws the models an emote attaches to the rig's bones (a microphone, a horse...). The OpenGL
 * matrix must be the player's model space with the rig's root applied, like in the layers.
 */
public final class EmoteModels {
    /** Texture of each model texture (by its hash); null when the image could not be read. */
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<String, ResourceLocation>();

    private EmoteModels() {
    }

    private static ResourceLocation texture(EmoteModel model) {
        if (TEXTURES.containsKey(model.textureId)) {
            return TEXTURES.get(model.textureId);
        }
        ResourceLocation id = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(model.texture));
            if (image != null) {
                id = new ResourceLocation("blendemotes", "model/" + model.textureId);
                Minecraft.getMinecraft().getTextureManager().loadTexture(id, new DynamicTexture(image));
            }
        } catch (Exception e) {
            id = null;
        }
        TEXTURES.put(model.textureId, id);
        return id;
    }

    /** @param scale pixels to OpenGL units (0.0625 for entity models) */
    public static void render(PlayerPose pose, float scale) {
        if (pose.models().isEmpty()) {
            return;
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableCull(); // open meshes and planes stay visible from behind
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (EmoteModel model : pose.models()) {
            Mat4 bone = pose.boneMatrix(model.bone);
            ResourceLocation texture = bone == null ? null : texture(model);
            if (texture == null) {
                continue;
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            GlStateManager.pushMatrix();
            GlMatrix.mult(bone, scale);
            Compat.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
            int corners = model.positions.length / 3;
            for (int i = 0; i < corners - corners % 3; i++) {
                Compat.vertex(model.positions[i * 3] * scale, model.positions[i * 3 + 1] * scale,
                        model.positions[i * 3 + 2] * scale, model.uvs[i * 2], model.uvs[i * 2 + 1],
                        model.normals[i * 3], model.normals[i * 3 + 1], model.normals[i * 3 + 2]);
            }
            Compat.draw();
            GlStateManager.popMatrix();
        }
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
    }
}
