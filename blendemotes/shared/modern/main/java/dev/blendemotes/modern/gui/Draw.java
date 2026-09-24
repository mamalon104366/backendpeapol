package dev.blendemotes.modern.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Radial menu drawing. */
final class Draw {
    private Draw() {
    }

    /** Ring segment between two angles (radians, 0 = up, clockwise), ARGB colour. */
    static void ring(GuiGraphics g, double cx, double cy, double inner, double outer, double from, double to, int argb) {
        float a = (argb >>> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float gr = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;
        Matrix4f m = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int steps = Math.max(4, (int) Math.ceil((to - from) / 0.05));
        for (int i = 0; i <= steps; i++) {
            double ang = from + (to - from) * i / steps;
            float sx = (float) Math.sin(ang);
            float sy = (float) -Math.cos(ang);
            bb.vertex(m, (float) (cx + sx * outer), (float) (cy + sy * outer), 0).color(r, gr, b, a).endVertex();
            bb.vertex(m, (float) (cx + sx * inner), (float) (cy + sy * inner), 0).color(r, gr, b, a).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
