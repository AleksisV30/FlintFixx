package com.flintfix.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=1.21.9 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
*///?}

/**
 * Base for FlintFix screens: hides the Screen API differences between
 * Minecraft versions so the screens themselves stay version-independent.
 */
public abstract class FlintFixScreen extends Screen {
    protected FlintFixScreen(Component title) {
        super(title);
    }

    /** Blurs the game behind the screen where the game supports it (1.20.5+); a no-op before that. */
    protected void blurBehind(GuiGraphics context, float delta) {
        //? if >=1.21.6 {
        /*renderBlurredBackground(context);
        *///?} else if >=1.21.2 {
        /*renderBlurredBackground();
        *///?} else if >=1.20.5 {
        renderBlurredBackground(delta);
        //?}
    }

    //? if <1.20.2 {
    /*// 1.20.1 only has the vertical scroll amount; route it to the newer four-argument form the screens override.
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return mouseScrolled(mouseX, mouseY, 0.0, amount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, verticalAmount);
    }
    *///?}

    //? if >=1.21.9 {
    /*// 1.21.9 passes input as event records. Unpack them into the classic methods the
    // FlintFix screens override; the classic fallbacks repack them for vanilla.
    private boolean flintfix$doubleClick;
    private int flintfix$mouseModifiers;

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        flintfix$doubleClick = doubleClick;
        flintfix$mouseModifiers = event.modifiers();
        return mouseClicked(event.x(), event.y(), event.button());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseEvent(mouseX, mouseY, button), flintfix$doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        flintfix$mouseModifiers = event.modifiers();
        return mouseReleased(event.x(), event.y(), event.button());
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseEvent(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        flintfix$mouseModifiers = event.modifiers();
        return mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseEvent(mouseX, mouseY, button), deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // Characters outside the Basic Multilingual Plane do not fit a char; FlintFix fields only need typed text.
        return event.codepoint() <= Character.MAX_VALUE && charTyped((char) event.codepoint(), event.modifiers());
    }

    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(new CharacterEvent(chr, modifiers));
    }

    private MouseButtonEvent mouseEvent(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, flintfix$mouseModifiers));
    }
    *///?}
}
