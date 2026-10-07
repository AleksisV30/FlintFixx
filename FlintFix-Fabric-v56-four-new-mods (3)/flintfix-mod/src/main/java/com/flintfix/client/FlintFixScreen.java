package com.flintfix.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base for FlintFix screens: hides the Screen API differences between
 * Minecraft versions so the screens themselves stay version-independent.
 */
public abstract class FlintFixScreen extends Screen {
    protected FlintFixScreen(Component title) {
        super(title);
    }

    /** Blurs the game behind the screen where the game supports it (1.20.5+); a no-op before that. */
    protected void blurBehind(float delta) {
        //? if >=1.21.2 {
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
}
