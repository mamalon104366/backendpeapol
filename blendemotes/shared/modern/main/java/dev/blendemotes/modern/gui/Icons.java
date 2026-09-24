package dev.blendemotes.modern.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.blendemotes.core.emote.Emote;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Emote icons (PNG rendered by the Blender exporter) as textures. */
public final class Icons {
    private static final Map<UUID, ResourceLocation> CACHE = new HashMap<>();
    /** Marks icons that could not be read. */
    private static final Map<UUID, Boolean> FAILED = new HashMap<>();

    private Icons() {
    }

    public static ResourceLocation get(Emote emote) {
        if (emote == null || emote.info.icon == null || FAILED.containsKey(emote.id)) {
            return null;
        }
        ResourceLocation loc = CACHE.get(emote.id);
        if (loc == null) {
            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(emote.info.icon));
                loc = Textures.register("icon/" + emote.id, image);
                CACHE.put(emote.id, loc);
            } catch (Exception e) {
                FAILED.put(emote.id, Boolean.TRUE);
                return null;
            }
        }
        return loc;
    }
}
