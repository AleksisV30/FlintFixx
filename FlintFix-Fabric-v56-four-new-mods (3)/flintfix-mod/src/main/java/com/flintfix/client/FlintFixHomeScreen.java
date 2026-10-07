package com.flintfix.client;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shift-opened FlintFix landing menu. */
public final class FlintFixHomeScreen extends FlintFixScreen {
    private static final ItemStack FLINT = new ItemStack(Items.FLINT);
    private static final long TRANSITION_MS = 180L;
    private static final String[][] TILES = {
        {"layout", "HUD Layout"},
        {"themes", "Themes"},
        {"profiles", "Profiles"}
    };

    private final Screen parent;
    private long transitionStartedAt = System.currentTimeMillis();
    private boolean closing;
    private int panelX, panelY, panelW, panelH;
    private int modsX, modsY, modsW;
    private int tilesY, tileW;
    private static final int MODS_H = 24;
    private static final int TILE_H = 36;
    private static final int TILE_GAP = 6;

    public FlintFixHomeScreen(Screen parent) {
        super(Component.literal("FlintFix Client"));
        this.parent = parent;
    }

    private void layout() {
        panelW = Math.min(244, Math.max(1, width - 24));
        panelH = Math.min(196, Math.max(1, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        modsW = Math.max(1, panelW - 32);
        modsX = panelX + 16;
        modsY = panelY + 98;
        tilesY = modsY + MODS_H + 8;
        tileW = Math.max(1, (modsW - TILE_GAP * 2) / 3);
    }

    private float visibility() {
        float t = Math.max(0.0f, Math.min(1.0f,
            (System.currentTimeMillis() - transitionStartedAt) / (float) TRANSITION_MS));
        float eased = t * t * (3.0f - 2.0f * t);
        return closing ? 1.0f - eased : eased;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        layout();
        float visible = visibility();
        blurBehind(context, delta);
        FlintFixUi.backdrop(context, width, height, visible);

        float scale = 0.96f + 0.04f * visible;
        float cx = panelX + panelW / 2.0f;
        float cy = panelY + panelH / 2.0f;
        FlintFixCompat.pushGui(context);
        FlintFixCompat.translateGui(context, cx, cy + (1.0f - visible) * 6.0f);
        FlintFixCompat.scaleGui(context, scale, scale);
        FlintFixCompat.translateGui(context, -cx, -cy);

        FlintFixUi.panelFrame(context, panelX, panelY, panelW, panelH);

        int centerX = panelX + panelW / 2;
        int avatarSize = 40;
        int avatarX = centerX - avatarSize / 2;
        int avatarY = panelY + 14;
        FlintFixUi.shadow(context, avatarX, avatarY, avatarSize, avatarSize, 0.6f);
        FlintFixUi.surface(context, avatarX, avatarY, avatarSize, avatarSize, FlintFixUi.raised(),
            FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accent(), 0.5f));
        FlintFixCompat.pushGui(context);
        FlintFixCompat.translateGui(context, avatarX + 4, avatarY + 4);
        FlintFixCompat.scaleGui(context, 2.0f, 2.0f);
        context.renderItem(FLINT, 0, 0);
        FlintFixCompat.popGui(context);

        FlintFixFont.drawCenteredExact(context, "FlintFix Client", centerX, panelY + 62, 12, FlintFixUi.text(), true);
        String profile = "Profile  ·  " + FlintFixProfileStore.selectedName();
        FlintFixFont.drawCenteredExact(context, FlintFixFont.trim(profile, panelW - 24, 6, false), centerX,
            panelY + 79, 6, FlintFixUi.muted(), false);

        renderModsButton(context, mouseX, mouseY);
        for (int i = 0; i < TILES.length; i++) {
            int tileX = modsX + i * (tileW + TILE_GAP);
            renderTile(context, i, tileX, mouseX, mouseY);
        }

        if (tilesY + TILE_H + 18 <= panelY + panelH) {
            String key = FlintFixClient.getSettingsKeyLabel().toUpperCase(Locale.ROOT);
            String hint = FlintFixFont.trim(key + " or ESC to close", panelW - 24, 6, false);
            FlintFixFont.drawCenteredExact(context, hint, centerX, panelY + panelH - 13, 6,
                FlintFixUi.subtle(), false);
        }

        context.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1,
            FlintFixUi.opacity(FlintFixUi.bg(), 1.0f - visible));
        FlintFixCompat.popGui(context);
    }

    private void renderModsButton(GuiGraphics c, int mouseX, int mouseY) {
        boolean hover = FlintFixUi.inside(mouseX, mouseY, modsX, modsY, modsW, MODS_H);
        float t = FlintFixUi.hoverProgress("home-mods", hover);
        int fill = FlintFixUi.blendColors(FlintFixUi.accent(), FlintFixUi.accentBright(), t);
        FlintFixUi.surface(c, modsX, modsY, modsW, MODS_H, fill, fill);
        int ink = FlintFixUi.onAccent();
        FlintFixIcons.drawExact(c, "modules", modsX + 9, modsY + (MODS_H - 10) / 2, 10, ink);
        FlintFixFont.drawExact(c, "Open Modules", modsX + 25, FlintFixFont.centeredY(modsY, MODS_H, 8), 8, ink, true);
        int arrowX = modsX + modsW - 15 + Math.round(2 * t);
        FlintFixIcons.drawExact(c, "next", arrowX, modsY + (MODS_H - 8) / 2, 8, ink);
    }

    private void renderTile(GuiGraphics c, int index, int tileX, int mouseX, int mouseY) {
        boolean hover = FlintFixUi.inside(mouseX, mouseY, tileX, tilesY, tileW, TILE_H);
        float t = FlintFixUi.hoverProgress("home-tile:" + index, hover);
        FlintFixUi.surface(c, tileX, tilesY, tileW, TILE_H,
            FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), t),
            FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accentBright(), t * 0.6f));
        FlintFixIcons.drawExact(c, TILES[index][0], tileX + (tileW - 11) / 2, tilesY + 7, 11,
            FlintFixUi.blendColors(FlintFixUi.muted(), FlintFixUi.accentBright(), t));
        FlintFixFont.drawCenteredExact(c, FlintFixFont.trim(TILES[index][1], tileW - 6, 6, true),
            tileX + tileW / 2, tilesY + 23, 6, FlintFixUi.text(), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (closing || System.currentTimeMillis() - transitionStartedAt < TRANSITION_MS) return true;
        if (button != 0 || minecraft == null) return super.mouseClicked(mouseX, mouseY, button);
        if (FlintFixUi.inside(mouseX, mouseY, modsX, modsY, modsW, MODS_H)) {
            minecraft.setScreen(new FlintFixSettingsScreen(this));
            return true;
        }
        for (int i = 0; i < TILES.length; i++) {
            int tileX = modsX + i * (tileW + TILE_GAP);
            if (!FlintFixUi.inside(mouseX, mouseY, tileX, tilesY, tileW, TILE_H)) continue;
            switch (i) {
                case 0 -> minecraft.setScreen(new FlintFixHudEditorScreen(this));
                case 1 -> minecraft.setScreen(new FlintFixThemeScreen(this));
                default -> minecraft.setScreen(new FlintFixProfileScreen(this));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || FlintFixClient.isSettingsKey(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && System.currentTimeMillis() - transitionStartedAt >= TRANSITION_MS) {
            closing = false;
            if (minecraft != null) minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        transitionStartedAt = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
