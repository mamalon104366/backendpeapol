package dev.blendemotes.mc1201.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.blendemotes.core.emote.Emote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Emote icons (PNG rendered by the Blender exporter) as textures. */
public final class Icons {
    private static final Map<UUID, ResourceLocation> CACHE = new HashMap<>();
    private static final ResourceLocation NONE = new ResourceLocation("blendemotes", "icon/none");

    private Icons() {
    }

    public static ResourceLocation get(Emote emote) {
        if (emote == null || emote.info.icon == null) {
            return null;
        }
        ResourceLocation loc = CACHE.get(emote.id);
        if (loc == null) {
            loc = NONE;
            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(emote.info.icon));
                DynamicTexture texture = new DynamicTexture(image);
                texture.setFilter(true, false);
                loc = new ResourceLocation("blendemotes", "icon/" + emote.id);
                Minecraft.getInstance().getTextureManager().register(loc, texture);
            } catch (Exception ignored) {
                loc = NONE;
            }
            CACHE.put(emote.id, loc);
        }
        return loc == NONE ? null : loc;
    }
}
