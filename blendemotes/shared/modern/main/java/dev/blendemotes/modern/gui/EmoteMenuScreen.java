package dev.blendemotes.modern.gui;

import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.config.EmoteConfig;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.modern.Compat;
import dev.blendemotes.modern.ModernEmotes;
import net.minecraft.Util;
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
public class EmoteMenuScreen extends BaseScreen {
    private final Screen parent;
    private int page;
    private Emote selected;
    private Emote lastClicked;
    private long lastClick;
    private final List<Button> slotButtons = new ArrayList<>();

    public EmoteMenuScreen(Screen parent) {
        super(Compat.translatable("blendemotes.menu.title"));
        this.parent = parent;
    }

    private ClientEmotes client() {
        return ModernEmotes.client();
    }

    @Override
    protected void init() {
        clearAll();
        slotButtons.clear();
        final ClientEmotes client = client();
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
            slotButtons.add(button(x, y, slotW, 20, Compat.literal(label), () -> {
                if (selected != null) {
                    client.setWheelEmote(slot, selected);
                    init();
                }
            }));
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
            button(x, y, entryW, 20, Compat.literal(label), () -> {
                long now = System.currentTimeMillis();
                if (e == lastClicked && now - lastClick < 400) {
                    play(e);
                    return;
                }
                lastClick = now;
                lastClicked = e;
                selected = e;
                init();
            });
        }
        int by = height - 28;
        button(width / 2 - 206, by, 40, 20, Compat.literal("<"), () -> {
            page--;
            init();
        });
        button(width / 2 - 162, by, 80, 20, Compat.translatable("blendemotes.menu.reload"), () -> {
            client.reload();
            selected = null;
            init();
        });
        button(width / 2 - 78, by, 80, 20, Compat.translatable("blendemotes.menu.folder"), () -> {
            File folder = client.emotesFolder();
            if (!folder.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                folder.mkdirs();
            }
            Util.getPlatform().openFile(folder);
        });
        Button play = button(width / 2 + 6, by, 80, 20, Compat.translatable("blendemotes.menu.play"), () -> {
            if (selected != null) {
                play(selected);
            }
        });
        play.active = selected != null;
        button(width / 2 + 90, by, 72, 20, Compat.translatable("gui.done"), this::onClose);
        button(width / 2 + 166, by, 40, 20, Compat.literal(">"), () -> {
            page++;
            init();
        });
    }

    private void play(Emote e) {
        minecraft.setScreen(null);
        client().playLocal(e);
    }

    @Override
    protected boolean click(double mouseX, double mouseY, int button) {
        if (button == 1) {
            for (int i = 0; i < slotButtons.size(); i++) {
                if (slotButtons.get(i).isMouseOver(mouseX, mouseY)) {
                    client().setWheelEmote(i, null);
                    init();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(double amount) {
        page += amount > 0 ? -1 : 1;
        init();
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    protected void draw(Ui ui, int mouseX, int mouseY, float partialTick) {
        ui.centered(font, title, width / 2, 8, 0xFFFFFFFF);
        ClientEmotes client = client();
        if (client == null) {
            return;
        }
        Component hint = selected == null ? Compat.translatable("blendemotes.menu.hint")
                : Compat.translatable("blendemotes.menu.selected", selected.info.name);
        ui.centered(font, hint, width / 2, 24 + 2 * 22 + 4, 0xFFAAAAAA);
        int errors = client.library.errors().size();
        if (errors > 0) {
            ui.centered(font, Compat.translatable("blendemotes.menu.errors", errors), width / 2, height - 42, 0xFFFF6060);
        }
        if (selected != null) {
            ResourceLocation icon = Icons.get(selected);
            if (icon != null) {
                ui.image(icon, 6, 6, 48, 48);
            }
        }
    }
}
