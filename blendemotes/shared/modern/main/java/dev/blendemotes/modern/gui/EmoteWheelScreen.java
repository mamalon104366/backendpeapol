package dev.blendemotes.modern.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.client.EmoteWheel;
import dev.blendemotes.core.client.WheelImage;
import dev.blendemotes.core.config.EmoteConfig;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.modern.Compat;
import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/** Radial emote menu: hold the key, point at an emote, release to play it. */
public class EmoteWheelScreen extends BaseScreen {
    private static final int SLOTS = EmoteConfig.WHEEL_SLOTS;
    private final KeyMapping key;
    private int hovered = -1;
    private boolean done;

    public EmoteWheelScreen(KeyMapping key) {
        super(Compat.translatable("blendemotes.wheel.title"));
        this.key = key;
    }

    private boolean keyHeld() {
        InputConstants.Key k = ((KeyMappingAccessor) key).blendemotes$getKey();
        //#if MC >= 12109
        long window = minecraft.getWindow().handle();
        //#else
        long window = minecraft.getWindow().getWindow();
        //#endif
        if (k.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
        }
        if (k.getValue() == InputConstants.UNKNOWN.getValue()) {
            return true;
        }
        return GLFW.glfwGetKey(window, k.getValue()) == GLFW.GLFW_PRESS;
    }

    @Override
    protected boolean background() {
        return false;
    }

    @Override
    protected void draw(Ui ui, int mouseX, int mouseY, float partialTick) {
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
        int outer = (int) (Math.min(width, height) * 0.40);
        double inner = outer * WheelImage.INNER;
        hovered = EmoteWheel.slotAt(mouseX - cx, mouseY - cy, inner, SLOTS);
        ui.image(Textures.wheel(SLOTS, hovered), cx - outer, cy - outer, outer * 2, outer * 2);
        int iconSize = (int) Math.max(16, (outer - inner) * 0.55);
        int labelWidth = (int) (outer - inner);
        for (int i = 0; i < SLOTS; i++) {
            Emote e = client.wheelEmote(i);
            double[] c = EmoteWheel.slotCenter(i, SLOTS, (inner + outer) / 2);
            int x = (int) (cx + c[0]);
            int y = (int) (cy + c[1]);
            if (e == null) {
                ui.centered(font, "-", x, y - 4, 0xFF777777);
                continue;
            }
            String name = font.plainSubstrByWidth(e.info.name, labelWidth);
            ResourceLocation icon = Icons.get(e);
            if (icon != null) {
                ui.image(icon, x - iconSize / 2, y - iconSize / 2 - 5, iconSize, iconSize);
                ui.centered(font, name, x, y + iconSize / 2 - 3, 0xFFFFFFFF);
            } else {
                ui.centered(font, name, x, y - 4, 0xFFFFFFFF);
            }
        }
        Emote current = hovered >= 0 ? client.wheelEmote(hovered) : null;
        if (current != null) {
            ui.centered(font, current.info.name, cx, cy - 10, 0xFFFFFFFF);
            if (!current.info.author.isEmpty()) {
                ui.centered(font, current.info.author, cx, cy + 2, 0xFFAAAAAA);
            }
        } else {
            ui.centered(font, Compat.translatable("blendemotes.wheel.title"), cx, cy - 10, 0xFFFFFFFF);
            ui.centered(font, Compat.translatable("blendemotes.wheel.edit"), cx, cy + 2, 0xFFAAAAAA);
        }
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
    protected boolean click(double mouseX, double mouseY, int button) {
        if (button == 0) {
            select();
            return true;
        }
        if (button == 1) {
            done = true;
            minecraft.setScreen(new EmoteMenuScreen(null));
            return true;
        }
        return false;
    }

    @Override
    protected boolean key(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            done = true;
            minecraft.setScreen(new EmoteMenuScreen(null));
            return true;
        }
        return false;
    }
}
