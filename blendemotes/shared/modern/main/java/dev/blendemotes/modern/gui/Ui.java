package dev.blendemotes.modern.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
//#if MC >= 12106
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
//#elseif MC >= 12102
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
//#elseif MC >= 12000
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
//#else
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
//#if MC < 11700
import net.minecraft.client.Minecraft;
//#endif
//#endif

/** The drawing calls the emote screens need, for every GUI API since 1.16. Colours are ARGB. */
public final class Ui {
    //#if MC >= 12000
    private final GuiGraphics g;

    Ui(GuiGraphics g) {
        this.g = g;
    }
    //#else
    private final PoseStack pose;

    Ui(PoseStack pose) {
        this.pose = pose;
    }
    //#endif

    public void centered(Font font, String text, int x, int y, int argb) {
        //#if MC >= 12000
        g.drawCenteredString(font, text, x, y, argb);
        //#else
        GuiComponent.drawCenteredString(pose, font, text, x, y, argb);
        //#endif
    }

    public void centered(Font font, Component text, int x, int y, int argb) {
        //#if MC >= 12000
        g.drawCenteredString(font, text, x, y, argb);
        //#else
        GuiComponent.drawCenteredString(pose, font, text, x, y, argb);
        //#endif
    }

    /** Draws a whole texture stretched to {@code w x h}, blended by its alpha. */
    public void image(ResourceLocation texture, int x, int y, int w, int h) {
        //#if MC >= 12106
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, w, h, w, h);
        //#elseif MC >= 12102
        g.blit(RenderType::guiTextured, texture, x, y, 0, 0, w, h, w, h);
        //#elseif MC >= 12000
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(texture, x, y, 0, 0, w, h, w, h);
        RenderSystem.disableBlend();
        //#else
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //#if MC >= 11700
        RenderSystem.setShaderTexture(0, texture);
        //#else
        Minecraft.getInstance().getTextureManager().bind(texture);
        //#endif
        GuiComponent.blit(pose, x, y, 0, 0, w, h, w, h);
        RenderSystem.disableBlend();
        //#endif
    }
}
