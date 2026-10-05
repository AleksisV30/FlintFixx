package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Shift-opened FlintFix landing menu. */
public final class FlintFixHomeScreen extends Screen {
    private static final ItemStack FLINT = new ItemStack(Items.FLINT);
    private static final long TRANSITION_MS = 180L;

    private final Screen parent;
    private long transitionStartedAt = System.currentTimeMillis();
    private boolean closing;
    private int panelX, panelY, panelW, panelH;
    private int modsX, modsY, modsW;

    public FlintFixHomeScreen(Screen parent) {
        super(Text.literal("FlintFix Client"));
        this.parent = parent;
    }

    private void layout() {
        panelW = Math.min(260, Math.max(1, width - 24));
        panelH = Math.min(174, Math.max(1, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        modsW = Math.max(1, Math.min(148, panelW - 32));
        modsX = panelX + (panelW - modsW) / 2;
        modsY = panelY + panelH - 49;
    }

    private float visibility() {
        float t = Math.max(0.0f, Math.min(1.0f,
            (System.currentTimeMillis() - transitionStartedAt) / (float) TRANSITION_MS));
        float eased = t * t * (3.0f - 2.0f * t);
        return closing ? 1.0f - eased : eased;
    }

    private static int alpha(int color, float amount) {
        int original = color >>> 24;
        int adjusted = Math.round(original * Math.max(0.0f, Math.min(1.0f, amount)));
        return (color & 0x00FFFFFF) | (adjusted << 24);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        float visible = visibility();
        applyBlur(delta);

        float scale = 0.96f + 0.04f * visible;
        float cx = panelX + panelW / 2.0f;
        float cy = panelY + panelH / 2.0f;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy + (1.0f - visible) * 6.0f, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-cx, -cy, 0);

        int avatarSize = 48;
        int avatarX = panelX + (panelW - avatarSize) / 2;
        int avatarY = panelY + 18;
        FlintFixUi.rounded(context, avatarX, avatarY, avatarSize, avatarSize, 16,
            alpha(0xFF3D3D3D, visible));
        context.getMatrices().push();
        context.getMatrices().translate(avatarX + 8, avatarY + 8, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);
        context.drawItem(FLINT, 0, 0);
        context.getMatrices().pop();

        FlintFixFont.drawCentered(context, "FlintFix Client", panelX + panelW / 2,
            panelY + 75, 13, alpha(0xFFF5F7FA, visible), true);
        FlintFixFont.drawCentered(context, "YOUR MINECRAFT CLIENT", panelX + panelW / 2,
            panelY + 96, 6, alpha(0xFF9DA9B8, visible), false);

        boolean hover = FlintFixUi.inside(mouseX, mouseY, modsX, modsY, modsW, 34);
        FlintFixUi.rounded(context, modsX, modsY, modsW, 34, 9,
            alpha(hover ? 0xFF696969 : 0xFF4D4D4D, visible));
        FlintFixUi.rounded(context, modsX + 8, modsY + 6, 22, 22, 7,
            alpha(hover ? 0xFF7A7A7A : 0xFF626262, visible));
        FlintFixIcons.draw(context, "modules", modsX + 14, modsY + 12, 10, alpha(0xFFEAF1F9, visible));
        FlintFixFont.drawCentered(context, "MODS", modsX + modsW / 2 + 5,
            modsY + 12, 10, alpha(0xFFFFFFFF, visible), true);
        FlintFixFont.drawCentered(context, ">", modsX + modsW - 15,
            modsY + 13, 7, alpha(0xFFD5DFEB, visible), true);

        if (panelH >= 166) {
            String closeHint = FlintFixFont.trim(FlintFixClient.getSettingsKeyLabel() + " TO CLOSE",
                panelW - 24, 6, false);
            FlintFixFont.drawCentered(context, closeHint,
                panelX + panelW / 2, panelY + panelH - 10, 6, alpha(0xFF7F8B9A, visible), false);
        }
        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (closing || System.currentTimeMillis() - transitionStartedAt < TRANSITION_MS) return true;
        if (button == 0 && FlintFixUi.inside(mouseX, mouseY, modsX, modsY, modsW, 34)) {
            if (client != null) client.setScreen(new FlintFixSettingsScreen(this));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && System.currentTimeMillis() - transitionStartedAt >= TRANSITION_MS) {
            closing = false;
            if (client != null) client.setScreen(parent);
        }
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        transitionStartedAt = System.currentTimeMillis();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
