package dev.blendemotes.modern.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//#if MC >= 12109
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//#endif
//#if MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#else
import com.mojang.blaze3d.vertex.PoseStack;
//#endif

/** Screen with the version specific overrides done once, for the emote screens. */
abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    /** Drawn on top of the widgets. */
    protected abstract void draw(Ui ui, int mouseX, int mouseY, float partialTick);

    /** False: no darkened (or blurred) background, the world stays visible. */
    protected boolean background() {
        return true;
    }

    protected boolean click(double mouseX, double mouseY, int button) {
        return false;
    }

    /** @param amount positive when scrolling up */
    protected boolean scroll(double amount) {
        return false;
    }

    protected boolean key(int keyCode) {
        return false;
    }

    protected Button button(int x, int y, int w, int h, Component text, Runnable action) {
        //#if MC >= 11903
        Button b = Button.builder(text, btn -> action.run()).bounds(x, y, w, h).build();
        //#else
        Button b = new Button(x, y, w, h, text, btn -> action.run());
        //#endif
        //#if MC >= 11700
        addRenderableWidget(b);
        //#else
        addButton(b);
        //#endif
        return b;
    }

    protected void clearAll() {
        //#if MC >= 11700
        clearWidgets();
        //#else
        buttons.clear();
        children.clear();
        //#endif
    }

    //#if MC >= 12000
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        //#if MC < 12002
        if (background()) {
            renderBackground(g);
        }
        //#endif
        super.render(g, mouseX, mouseY, partialTick);
        draw(new Ui(g), mouseX, mouseY, partialTick);
    }

    //#if MC >= 12002
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (background()) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        }
    }
    //#endif
    //#else
    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        if (background()) {
            renderBackground(pose);
        }
        super.render(pose, mouseX, mouseY, partialTick);
        draw(new Ui(pose), mouseX, mouseY, partialTick);
    }
    //#endif

    //#if MC >= 12109
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return click(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }
    //#else
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return click(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }
    //#endif

    //#if MC >= 12002
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return scroll(vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    //#else
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return scroll(amount) || super.mouseScrolled(mouseX, mouseY, amount);
    }
    //#endif

    //#if MC >= 12109
    @Override
    public boolean keyPressed(KeyEvent event) {
        return key(event.key()) || super.keyPressed(event);
    }
    //#else
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return key(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }
    //#endif

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
