package dev.blendemotes.mc1201.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.client.EmoteWheel;
import dev.blendemotes.core.config.EmoteConfig;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.mc1201.ModernEmotes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/** Radial emote menu: hold the key, point at an emote, release to play it. */
public class EmoteWheelScreen extends Screen {
    private static final int SLOTS = EmoteConfig.WHEEL_SLOTS;
    private final KeyMapping key;
    private int hovered = -1;
    private boolean done;

    public EmoteWheelScreen(KeyMapping key) {
        super(Component.translatable("blendemotes.wheel.title"));
        this.key = key;
    }

    private boolean keyHeld() {
        InputConstants.Key k = ((dev.blendemotes.mc1201.mixin.KeyMappingAccessor) key).blendemotes$getKey();
        long window = minecraft.getWindow().getWindow();
        if (k.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
        }
        if (k.getValue() == InputConstants.UNKNOWN.getValue()) {
            return true;
        }
        return InputConstants.isKeyDown(window, k.getValue());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ClientEmotes client = ModernEmotes.client();
        if (client == null) {
            onClose();
            return;
        }
        if (!keyHeld()) {
            select();
            return;
        }
        int cx = width / 2;
        int cy = height / 2;
        double outer = Math.min(width, height) * 0.40;
        double inner = outer * 0.32;
        hovered = EmoteWheel.slotAt(mouseX - cx, mouseY - cy, inner, SLOTS);
        double step = Math.PI * 2 / SLOTS;
        for (int i = 0; i < SLOTS; i++) {
            Draw.ring(g, cx, cy, inner, outer, i * step - step / 2 + 0.02, i * step + step / 2 - 0.02,
                    i == hovered ? 0xC04A90E2 : 0x90101014);
        }
        int iconSize = (int) Math.max(16, (outer - inner) * 0.55);
        int labelWidth = (int) (outer - inner);
        for (int i = 0; i < SLOTS; i++) {
            Emote e = client.wheelEmote(i);
            double[] c = EmoteWheel.slotCenter(i, SLOTS, (inner + outer) / 2);
            int x = (int) (cx + c[0]);
            int y = (int) (cy + c[1]);
            if (e == null) {
                g.drawCenteredString(font, "-", x, y - 4, 0x777777);
                continue;
            }
            String name = font.plainSubstrByWidth(e.info.name, labelWidth);
            ResourceLocation icon = Icons.get(e);
            if (icon != null) {
                g.blit(icon, x - iconSize / 2, y - iconSize / 2 - 5, 0, 0, iconSize, iconSize, iconSize, iconSize);
                g.drawCenteredString(font, name, x, y + iconSize / 2 - 3, 0xFFFFFF);
            } else {
                g.drawCenteredString(font, name, x, y - 4, 0xFFFFFF);
            }
        }
        Emote current = hovered >= 0 ? client.wheelEmote(hovered) : null;
        if (current != null) {
            g.drawCenteredString(font, current.info.name, cx, cy - 10, 0xFFFFFF);
            if (!current.info.author.isEmpty()) {
                g.drawCenteredString(font, current.info.author, cx, cy + 2, 0xAAAAAA);
            }
        } else {
            g.drawCenteredString(font, Component.translatable("blendemotes.wheel.title"), cx, cy - 10, 0xFFFFFF);
            g.drawCenteredString(font, Component.translatable("blendemotes.wheel.edit"), cx, cy + 2, 0xAAAAAA);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void select() {
        if (done) {
            return;
        }
        done = true;
        ClientEmotes client = ModernEmotes.client();
        Emote e = hovered >= 0 && client != null ? client.wheelEmote(hovered) : null;
        minecraft.setScreen(null);
        if (e != null) {
            client.playLocal(e);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            select();
            return true;
        }
        if (button == 1) {
            done = true;
            minecraft.setScreen(new EmoteMenuScreen(null));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            done = true;
            minecraft.setScreen(new EmoteMenuScreen(null));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
