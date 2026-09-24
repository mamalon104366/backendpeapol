package dev.blendemotes.legacy.gui;

import dev.blendemotes.legacy.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/** Small drawing helpers for the emote screens. */
final class Draw {
    private Draw() {
    }

    /** Ring segment between two angles (radians, 0 = up, clockwise), ARGB colour. */
    static void ring(double cx, double cy, double inner, double outer, double from, double to, int argb) {
        float a = (argb >>> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float g = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Compat.begin(5, DefaultVertexFormats.POSITION_COLOR);
        int steps = Math.max(4, (int) Math.ceil((to - from) / 0.05));
        for (int i = 0; i <= steps; i++) {
            double ang = from + (to - from) * i / steps;
            double sx = Math.sin(ang);
            double sy = -Math.cos(ang);
            Compat.vertex(cx + sx * outer, cy + sy * outer, 0, r, g, b, a);
            Compat.vertex(cx + sx * inner, cy + sy * inner, 0, r, g, b, a);
        }
        Compat.draw();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    static void icon(ResourceLocation texture, int x, int y, int size) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
        GlStateManager.disableBlend();
    }
}
