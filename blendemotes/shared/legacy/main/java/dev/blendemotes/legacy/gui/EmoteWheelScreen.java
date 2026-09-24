package dev.blendemotes.legacy.gui;

import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.client.EmoteWheel;
import dev.blendemotes.core.config.EmoteConfig;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.legacy.Compat;
import dev.blendemotes.legacy.LegacyEmotes;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;


/** Radial emote menu: hold the key, point at an emote, release to play it. */
public class EmoteWheelScreen extends GuiScreen {
    private static final int SLOTS = EmoteConfig.WHEEL_SLOTS;
    private final int keyCode;
    private int hovered = -1;
    private boolean done;

    public EmoteWheelScreen(int keyCode) {
        this.keyCode = keyCode;
    }

    private boolean keyHeld() {
        if (keyCode == 0) {
            return true;
        }
        return keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ClientEmotes client = LegacyEmotes.client();
        if (client == null) {
            mc.displayGuiScreen(null);
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
            double from = i * step - step / 2 + 0.02;
            double to = i * step + step / 2 - 0.02;
            int color = i == hovered ? 0xC04A90E2 : 0x90101014;
            Draw.ring(cx, cy, inner, outer, from, to, color);
        }
        int iconSize = (int) Math.max(16, (outer - inner) * 0.55);
        for (int i = 0; i < SLOTS; i++) {
            Emote e = client.wheelEmote(i);
            double[] c = EmoteWheel.slotCenter(i, SLOTS, (inner + outer) / 2);
            int x = (int) (cx + c[0]);
            int y = (int) (cy + c[1]);
            if (e == null) {
                drawCenteredString(Compat.font(), "-", x, y - 4, 0x777777);
                continue;
            }
            ResourceLocation icon = Icons.get(e);
            if (icon != null) {
                Draw.icon(icon, x - iconSize / 2, y - iconSize / 2 - 5, iconSize);
                drawCenteredString(Compat.font(), Compat.font().trimStringToWidth(e.info.name, (int) (outer - inner)), x, y + iconSize / 2 - 3, 0xFFFFFF);
            } else {
                drawCenteredString(Compat.font(), Compat.font().trimStringToWidth(e.info.name, (int) (outer - inner)), x, y - 4, 0xFFFFFF);
            }
        }
        Emote current = hovered >= 0 ? client.wheelEmote(hovered) : null;
        if (current != null) {
            drawCenteredString(Compat.font(), current.info.name, cx, cy - 10, 0xFFFFFF);
            if (!current.info.author.isEmpty()) {
                drawCenteredString(Compat.font(), current.info.author, cx, cy + 2, 0xAAAAAA);
            }
        } else {
            drawCenteredString(Compat.font(), I18n.format("blendemotes.wheel.title"), cx, cy - 10, 0xFFFFFF);
            drawCenteredString(Compat.font(), I18n.format("blendemotes.wheel.edit"), cx, cy + 2, 0xAAAAAA);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void select() {
        if (done) {
            return;
        }
        done = true;
        ClientEmotes client = LegacyEmotes.client();
        Emote e = hovered >= 0 && client != null ? client.wheelEmote(hovered) : null;
        mc.displayGuiScreen(null);
        if (e != null) {
            client.playLocal(e);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            select();
        } else if (button == 1) {
            done = true;
            mc.displayGuiScreen(new EmoteMenuScreen(null));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int key) {
        if (key == Keyboard.KEY_TAB) {
            done = true;
            mc.displayGuiScreen(new EmoteMenuScreen(null));
            return;
        }
        super.keyTyped(typedChar, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
