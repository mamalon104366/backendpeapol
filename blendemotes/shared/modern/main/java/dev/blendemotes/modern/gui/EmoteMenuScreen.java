package dev.blendemotes.modern.gui;

import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.config.EmoteConfig;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.modern.ModernEmotes;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Emote list: click an emote to select it, then click a wheel slot to put it there
 * (right click a slot to empty it). Double click an emote to play it.
 */
public class EmoteMenuScreen extends Screen {
    private final Screen parent;
    private int page;
    private Emote selected;
    private Emote lastClicked;
    private long lastClick;
    private final List<Button> slotButtons = new ArrayList<>();

    public EmoteMenuScreen(Screen parent) {
        super(Component.translatable("blendemotes.menu.title"));
        this.parent = parent;
    }

    private ClientEmotes client() {
        return ModernEmotes.client();
    }

    @Override
    protected void init() {
        clearWidgets();
        slotButtons.clear();
        ClientEmotes client = client();
        if (client == null) {
            return;
        }
        int slotW = Math.min(90, (width - 20) / 4 - 4);
        for (int i = 0; i < EmoteConfig.WHEEL_SLOTS; i++) {
            final int slot = i;
            int x = width / 2 - 2 * (slotW + 4) + (i % 4) * (slotW + 4) + 2;
            int y = 24 + (i / 4) * 22;
            Emote e = client.wheelEmote(i);
            String label = font.plainSubstrByWidth((i + 1) + ": " + (e == null ? "-" : e.info.name), slotW - 6);
            Button b = Button.builder(Component.literal(label), btn -> {
                if (selected != null) {
                    client.setWheelEmote(slot, selected);
                    init();
                }
            }).bounds(x, y, slotW, 20).build();
            slotButtons.add(b);
            addRenderableWidget(b);
        }
        List<Emote> emotes = client.emotes();
        int top = 24 + 2 * 22 + 16;
        int bottom = height - 52;
        int entryW = 120;
        int columns = Math.max(1, (width - 20) / (entryW + 4));
        int rows = Math.max(1, (bottom - top) / 22);
        int perPage = columns * rows;
        int pages = Math.max(1, (emotes.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int startX = width / 2 - columns * (entryW + 4) / 2;
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= emotes.size()) {
                break;
            }
            final Emote e = emotes.get(idx);
            int x = startX + (i % columns) * (entryW + 4);
            int y = top + (i / columns) * 22;
            String label = font.plainSubstrByWidth((e == selected ? "> " : "") + e.info.name, entryW - 6);
            addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                long now = System.currentTimeMillis();
                if (e == lastClicked && now - lastClick < 400) {
                    play(e);
                    return;
                }
                lastClick = now;
                lastClicked = e;
                selected = e;
                init();
            }).bounds(x, y, entryW, 20).build());
        }
        int by = height - 28;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--;
            init();
        }).bounds(width / 2 - 206, by, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("blendemotes.menu.reload"), b -> {
            client.reload();
            selected = null;
            init();
        }).bounds(width / 2 - 162, by, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("blendemotes.menu.folder"), b -> {
            File folder = client.emotesFolder();
            if (!folder.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                folder.mkdirs();
            }
            Util.getPlatform().openFile(folder);
        }).bounds(width / 2 - 78, by, 80, 20).build());
        Button play = Button.builder(Component.translatable("blendemotes.menu.play"), b -> {
            if (selected != null) {
                play(selected);
            }
        }).bounds(width / 2 + 6, by, 80, 20).build();
        play.active = selected != null;
        addRenderableWidget(play);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 + 90, by, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            init();
        }).bounds(width / 2 + 166, by, 40, 20).build());
    }

    private void play(Emote e) {
        minecraft.setScreen(null);
        client().playLocal(e);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            for (int i = 0; i < slotButtons.size(); i++) {
                if (slotButtons.get(i).isMouseOver(mouseX, mouseY)) {
                    client().setWheelEmote(i, null);
                    init();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        page += delta > 0 ? -1 : 1;
        init();
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
        ClientEmotes client = client();
        if (client == null) {
            return;
        }
        Component hint = selected == null ? Component.translatable("blendemotes.menu.hint")
                : Component.translatable("blendemotes.menu.selected", selected.info.name);
        g.drawCenteredString(font, hint, width / 2, 24 + 2 * 22 + 4, 0xAAAAAA);
        int errors = client.library.errors().size();
        if (errors > 0) {
            g.drawCenteredString(font, Component.translatable("blendemotes.menu.errors", errors), width / 2, height - 42, 0xFF6060);
        }
        if (selected != null) {
            ResourceLocation icon = Icons.get(selected);
            if (icon != null) {
                g.blit(icon, 6, 6, 0, 0, 48, 48, 48, 48);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
