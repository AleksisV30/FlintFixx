package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Displays a one-time risk notice before the first Freecam activation. */
final class FlintFixFreecamWarningScreen extends Screen {
    private final Screen parent;
    private int panelX, panelY, panelW, panelH;

    FlintFixFreecamWarningScreen(Screen parent) {
        super(Text.literal("Freecam warning"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA9000000);
        panelW = Math.min(220, Math.max(150, width - 20));
        panelH = 88;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        FlintFixUi.fadeOutline(context, panelX - 2, panelY - 2, panelW + 4, panelH + 4, 4, 0x663E4148);
        FlintFixUi.outlinedBox(context, panelX, panelY, panelW, panelH, 4, 0xFF51545B, 0xFF191B20);
        FlintFixFont.drawCentered(context, "FREECAM", width / 2, panelY + 10, 10, FlintFixClient.TEXT, true);
        FlintFixFont.drawCentered(context, "Use at your own risk!", width / 2, panelY + 31, 8,
            0xFFE7C27A, true);
        FlintFixFont.drawCentered(context, "Some servers may prohibit this feature.", width / 2,
            panelY + 45, 5, FlintFixClient.MUTED, false);

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
            close();
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, continueX, buttonY, buttonW, 14)) {
            FlintFixClient.CONFIG.freecamRiskAccepted = true;
            FlintFixClient.CONFIG.save();
            client.setScreen(null);
            FlintFixFreecam.enable(client);
            return true;
        }
        return true;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
