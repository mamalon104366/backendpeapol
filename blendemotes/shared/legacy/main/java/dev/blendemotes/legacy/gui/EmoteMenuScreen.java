package dev.blendemotes.legacy.gui;

import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.config.EmoteConfig;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.legacy.Compat;
import dev.blendemotes.legacy.LegacyEmotes;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

/**
 * Emote list: click an emote to select it, then click a wheel slot to put it there
 * (right click a slot to empty it). Double click an emote to play it.
 */
public class EmoteMenuScreen extends GuiScreen {
    private static final int SLOT_BASE = 100;
    private static final int ENTRY_BASE = 1000;
    private static final int RELOAD = 1;
    private static final int FOLDER = 2;
    private static final int PREV = 3;
    private static final int NEXT = 4;
    private static final int DONE = 5;
    private static final int PLAY = 6;

    private final GuiScreen parent;
    private int page;
    private Emote selected;
    private long lastClick;
    private Emote lastClicked;
    private int columns;
    private int rows;

    public EmoteMenuScreen(GuiScreen parent) {
        this.parent = parent;
    }

    private ClientEmotes client() {
        return LegacyEmotes.client();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        ClientEmotes client = client();
        if (client == null) {
            return;
        }
        int slotW = Math.min(90, (width - 20) / 4 - 4);
        for (int i = 0; i < EmoteConfig.WHEEL_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            int x = width / 2 - 2 * (slotW + 4) + col * (slotW + 4) + 2;
            int y = 24 + row * 22;
            Emote e = client.wheelEmote(i);
            String label = (i + 1) + ": " + (e == null ? "-" : e.info.name);
            buttonList.add(new GuiButton(SLOT_BASE + i, x, y, slotW, 20, Compat.font().trimStringToWidth(label, slotW - 6)));
        }
        List<Emote> emotes = client.emotes();
        int top = 24 + 2 * 22 + 16;
        int bottom = height - 52;
        int entryW = 120;
        columns = Math.max(1, (width - 20) / (entryW + 4));
        rows = Math.max(1, (bottom - top) / 22);
        int perPage = columns * rows;
        int pages = Math.max(1, (emotes.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int startX = width / 2 - columns * (entryW + 4) / 2;
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= emotes.size()) {
                break;
            }
            Emote e = emotes.get(idx);
            int x = startX + (i % columns) * (entryW + 4);
            int y = top + (i / columns) * 22;
            String label = (e == selected ? "> " : "") + e.info.name;
            buttonList.add(new GuiButton(ENTRY_BASE + idx, x, y, entryW, 20, Compat.font().trimStringToWidth(label, entryW - 6)));
        }
        int by = height - 28;
        buttonList.add(new GuiButton(PREV, width / 2 - 206, by, 40, 20, "<"));
        buttonList.add(new GuiButton(RELOAD, width / 2 - 162, by, 80, 20, I18n.format("blendemotes.menu.reload")));
        buttonList.add(new GuiButton(FOLDER, width / 2 - 78, by, 80, 20, I18n.format("blendemotes.menu.folder")));
        GuiButton play = new GuiButton(PLAY, width / 2 + 6, by, 80, 20, I18n.format("blendemotes.menu.play"));
        play.enabled = selected != null;
        buttonList.add(play);
        buttonList.add(new GuiButton(DONE, width / 2 + 90, by, 72, 20, I18n.format("gui.done")));
        buttonList.add(new GuiButton(NEXT, width / 2 + 166, by, 40, 20, ">"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ClientEmotes client = client();
        if (client == null) {
            return;
        }
        int id = button.id;
        if (id >= ENTRY_BASE) {
            Emote e = client.emotes().get(id - ENTRY_BASE);
            long now = System.currentTimeMillis();
            if (e == lastClicked && now - lastClick < 400) {
                play(e);
                return;
            }
            lastClick = now;
            lastClicked = e;
            selected = e;
            initGui();
        } else if (id >= SLOT_BASE) {
            if (selected != null) {
                client.setWheelEmote(id - SLOT_BASE, selected);
                initGui();
            }
        } else if (id == RELOAD) {
            client.reload();
            selected = null;
            initGui();
        } else if (id == FOLDER) {
            openFolder(client.emotesFolder());
        } else if (id == PREV) {
            page--;
            initGui();
        } else if (id == NEXT) {
            page++;
            initGui();
        } else if (id == PLAY) {
            if (selected != null) {
                play(selected);
            }
        } else if (id == DONE) {
            mc.displayGuiScreen(parent);
        }
    }

    private void play(Emote e) {
        mc.displayGuiScreen(null);
        client().playLocal(e);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 1) {
            for (GuiButton b : buttonList) {
                if (b.id >= SLOT_BASE && b.id < SLOT_BASE + EmoteConfig.WHEEL_SLOTS && b.mousePressed(mc, mouseX, mouseY)) {
                    client().setWheelEmote(b.id - SLOT_BASE, null);
                    initGui();
                    return;
                }
            }
        }
        //#if MC >= 11202
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (java.io.IOException ignored) {
            // not thrown by the vanilla screen code
        }
        //#else
        super.mouseClicked(mouseX, mouseY, mouseButton);
        //#endif
    }

    @Override
    public void handleMouseInput() {
        //#if MC >= 11202
        try {
            super.handleMouseInput();
        } catch (java.io.IOException ignored) {
            // not thrown by the vanilla screen code
        }
        //#else
        super.handleMouseInput();
        //#endif
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            page += wheel > 0 ? -1 : 1;
            initGui();
        }
    }

    private static void openFolder(File folder) {
        if (!folder.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (Throwable t) {
            Sys.openURL("file://" + folder.getAbsolutePath());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(Compat.font(), I18n.format("blendemotes.menu.title"), width / 2, 8, 0xFFFFFF);
        ClientEmotes client = client();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (client == null) {
            return;
        }
        int info = 24 + 2 * 22 + 4;
        String hint = selected == null ? I18n.format("blendemotes.menu.hint")
                : I18n.format("blendemotes.menu.selected", selected.info.name);
        drawCenteredString(Compat.font(), hint, width / 2, info, 0xAAAAAA);
        int errors = client.library.errors().size();
        if (errors > 0) {
            drawCenteredString(Compat.font(), I18n.format("blendemotes.menu.errors", errors), width / 2, height - 42, 0xFF6060);
        }
        if (selected != null) {
            ResourceLocation icon = Icons.get(selected);
            if (icon != null) {
                Draw.icon(icon, 6, 6, 48);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
