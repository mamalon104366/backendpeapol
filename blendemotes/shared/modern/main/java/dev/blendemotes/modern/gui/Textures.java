package dev.blendemotes.modern.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.blendemotes.core.client.WheelImage;
import dev.blendemotes.modern.Compat;
import net.minecraft.client.Minecraft;
//#if MC >= 12111
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
//#endif
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Textures made at runtime: the emote wheel and the emote icons. */
final class Textures {
    private Textures() {
    }

    static ResourceLocation register(String path, NativeImage image) {
        //#if MC >= 12111
        // smooth scaling: textures pick their sampler since 1.21.11
        DynamicTexture texture = new DynamicTexture(() -> "blendemotes:" + path, image) {
            {
                sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            }
        };
        //#elseif MC >= 12105
        DynamicTexture texture = new DynamicTexture(() -> "blendemotes:" + path, image);
        texture.setFilter(true, false);
        //#else
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(true, false);
        //#endif
        ResourceLocation id = Compat.id("blendemotes", path);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    /** ARGB pixels to a new image. */
    static NativeImage image(int size, int[] argb) {
        NativeImage image = new NativeImage(size, size, true);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int c = argb[y * size + x];
                //#if MC >= 12102
                image.setPixel(x, y, c);
                //#else
                image.setPixelRGBA(x, y, WheelImage.toAbgr(c));
                //#endif
            }
        }
        return image;
    }

    private static final int WHEEL_SIZE = 256;
    private static final int WHEEL_COLOR = 0x90101014;
    private static final int WHEEL_HOVER = 0xC04A90E2;
    private static ResourceLocation[] wheel;
    private static int wheelSlots;

    /** The wheel with the given slot highlighted (-1: none). */
    static ResourceLocation wheel(int slots, int hovered) {
        if (wheel == null || wheelSlots != slots) {
            wheel = new ResourceLocation[slots + 1];
            wheelSlots = slots;
        }
        int index = hovered < 0 || hovered >= slots ? slots : hovered;
        if (wheel[index] == null) {
            int[] pixels = WheelImage.render(WHEEL_SIZE, slots, index == slots ? -1 : index, WHEEL_COLOR, WHEEL_HOVER);
            wheel[index] = register("wheel/" + slots + "_" + index, image(WHEEL_SIZE, pixels));
        }
        return wheel[index];
    }
}
