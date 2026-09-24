package dev.blendemotes.legacy.gui;

import dev.blendemotes.core.emote.Emote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Emote icons (the PNG rendered by the Blender exporter) as textures. */
public final class Icons {
    private static final Map<UUID, ResourceLocation> CACHE = new HashMap<UUID, ResourceLocation>();
    private static final ResourceLocation NONE = new ResourceLocation("blendemotes", "none");

    private Icons() {
    }

    /** Texture of the emote icon, or null when it has none. */
    public static ResourceLocation get(Emote emote) {
        if (emote == null || emote.info.icon == null) {
            return null;
        }
        ResourceLocation loc = CACHE.get(emote.id);
        if (loc == null) {
            loc = NONE;
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(emote.info.icon));
                if (image != null) {
                    DynamicTexture texture = new DynamicTexture(image);
                    loc = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("blendemotes_icon", texture);
                    GlStateManager.bindTexture(texture.getGlTextureId());
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                }
            } catch (Exception ignored) {
                loc = NONE;
            }
            CACHE.put(emote.id, loc);
        }
        return loc == NONE ? null : loc;
    }
}
