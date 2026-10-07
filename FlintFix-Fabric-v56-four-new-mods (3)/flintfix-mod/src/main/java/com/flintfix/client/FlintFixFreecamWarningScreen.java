package com.flintfix.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Displays a one-time risk notice before the first Freecam activation. */
final class FlintFixFreecamWarningScreen extends FlintFixScreen {
    private final Screen parent;
    private int panelX, panelY, panelW, panelH;

    FlintFixFreecamWarningScreen(Screen parent) {
        super(Component.literal("Freecam warning"));
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA9000000);
        panelW = Math.min(220, Math.max(150, width - 20));
        panelH = 88;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int warning = 0xFFE7C27A;
        FlintFixUi.shadow(context, panelX, panelY, panelW, panelH, 1.0f);
        FlintFixUi.surface(context, panelX, panelY, panelW, panelH, FlintFixUi.bg(),
            FlintFixUi.blendColors(FlintFixUi.border(), warning, 0.45f));
        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 3, warning);
        FlintFixFont.drawCenteredExact(context, "Freecam", width / 2, panelY + 11, 10, FlintFixUi.text(), true);
        FlintFixFont.drawCenteredExact(context, "Use at your own risk!", width / 2, panelY + 30, 8, warning, true);
        FlintFixFont.drawCenteredExact(context, FlintFixFont.trim("Some servers may prohibit this feature.",
            panelW - 16, 6, false), width / 2, panelY + 44, 6, FlintFixUi.muted(), false);

        int buttonY = panelY + 66;
        int buttonW = 70;
        int cancelX = width / 2 - buttonW - 5;
        int continueX = width / 2 + 5;
        FlintFixUi.compactButton(context, cancelX, buttonY, buttonW, 14, "CANCEL",
            FlintFixUi.inside(mouseX, mouseY, cancelX, buttonY, buttonW, 14), false);
        FlintFixUi.compactButton(context, continueX, buttonY, buttonW, 14, "CONTINUE",
            FlintFixUi.inside(mouseX, mouseY, continueX, buttonY, buttonW, 14), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);
        int buttonY = panelY + 66;
        int buttonW = 70;
        int cancelX = width / 2 - buttonW - 5;
        int continueX = width / 2 + 5;
        if (FlintFixUi.inside(mouseX, mouseY, cancelX, buttonY, buttonW, 14)) {
            onClose();
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, continueX, buttonY, buttonW, 14)) {
            FlintFixClient.CONFIG.freecamRiskAccepted = true;
            FlintFixClient.CONFIG.save();
            minecraft.setScreen(null);
            FlintFixFreecam.enable(minecraft);
            return true;
        }
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
