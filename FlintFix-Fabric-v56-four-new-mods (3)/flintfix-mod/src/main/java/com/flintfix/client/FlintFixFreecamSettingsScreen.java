package com.flintfix.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** FlintFix card options for changing Freecam's on/off toggle key. */
public final class FlintFixFreecamSettingsScreen extends FlintFixScreen {
    private final Screen parent;
    private int x, y, w, h;
    private int keyX, keyY, keyW;
    private int resetX, resetY;
    private int backX, backY, backW;
    private int speedX, speedY, speedW;
    private boolean listening;
    private boolean draggingSpeed;
    private boolean speedDirty;
    private final long openedAt = System.currentTimeMillis();

    public FlintFixFreecamSettingsScreen(Screen parent) {
        super(Component.literal("Freecam Settings"));
        this.parent = parent;
    }

    private void layout() {
        w = Math.min(236, Math.max(1, width - 20));
        h = Math.min(150, Math.max(1, height - 20));
        x = (width - w) / 2;
        y = (height - h) / 2;
        keyY = y + 44;
        resetY = keyY + 3;
        resetX = x + w - 39;
        keyW = Math.max(36, Math.min(76, w - 133));
        keyX = resetX - keyW - 5;
        speedX = x + 13;
        speedY = y + 96;
        speedW = w - 26;
        backY = y + h - 22;
        backX = x + 9;
        backW = 52;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        layout();
        float intro = FlintFixUi.openProgress(openedAt);
        blurBehind(context, delta);
        FlintFixUi.backdrop(context, width, height, intro);
        FlintFixUi.pushPanelIntro(context, x, y, w, h, intro);
        FlintFixUi.panelFrame(context, x, y, w, h);
        FlintFixUi.header(context, x + 10, y + 9, w - 20, "freecam", "Freecam",
            "Same key turns Freecam on and off");

        FlintFixUi.surface(context, x + 8, y + 39, w - 16, 27, FlintFixUi.card(), FlintFixUi.border());
        FlintFixFont.drawExact(context, "Toggle key", x + 14, FlintFixFont.centeredY(y + 39, 27, 7), 7,
            FlintFixUi.text(), true);

        boolean hoverKey = FlintFixUi.inside(mouseX, mouseY, keyX, keyY, keyW, 20);
        FlintFixUi.compactButton(context, keyX, keyY, keyW, 20,
            listening ? "PRESS KEY" : keyLabel(), hoverKey || listening, listening);
        FlintFixUi.compactButton(context, resetX, resetY, 30, 14, "RESET",
            FlintFixUi.inside(mouseX, mouseY, resetX, resetY, 30, 14), false);

        FlintFixFont.drawExact(context, "Movement speed", x + 12, y + 76, 7, FlintFixUi.text(), true);
        String speedLabel = String.format(java.util.Locale.ROOT, "%.2f b/t", FlintFixClient.CONFIG.freecamSpeed);
        FlintFixFont.drawExact(context, speedLabel, x + w - 12 - FlintFixFont.width(speedLabel, 7, true), y + 76, 7,
            FlintFixUi.accentBright(), true);
        float speedT = (FlintFixClient.CONFIG.freecamSpeed - 0.10f) / 1.90f;
        FlintFixUi.slider(context, speedX + 3, speedY, speedW - 6, speedT, draggingSpeed);
        FlintFixFont.drawExact(context, "Hold sprint to double the speed", x + 12, y + 106, 6,
            FlintFixUi.subtle(), false);

        FlintFixUi.compactButton(context, backX, backY, backW, 14, "BACK",
            FlintFixUi.inside(mouseX, mouseY, backX, backY, backW, 14), false);
        FlintFixUi.drawTrimmedExact(context, "Also rebindable in Minecraft Controls", x + 69,
            FlintFixFont.centeredY(backY, 14, 6), x + w - 8 - (x + 69), 6, FlintFixUi.subtle(), false);
        FlintFixUi.finishPanelIntro(context, x, y, w, h, intro);
    }

    private String keyLabel() {
        KeyMapping binding = FlintFixClient.getFreecamKeyBinding();
        if (binding == null || binding.isUnbound()) return "UNBOUND";
        return FlintFixFont.trim(binding.getTranslatedKeyMessage().getString(), keyW - 8, 5, true);
    }

    private void bind(InputConstants.Key key) {
        KeyMapping binding = FlintFixClient.getFreecamKeyBinding();
        if (binding == null) return;
        binding.setKey(key);
        KeyMapping.resetMapping();
        minecraft.options.save();
    }

    private void updateSpeed(double mouseX) {
        if (speedW <= 0) return;
        double progress = Math.max(0.0, Math.min(1.0, (mouseX - speedX) / speedW));
        FlintFixClient.CONFIG.freecamSpeed = (float)(0.10 + progress * 1.90);
        speedDirty = true;
    }

    private void saveSpeed() {
        if (!speedDirty) return;
        FlintFixClient.CONFIG.save();
        speedDirty = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);
        layout();
        if (FlintFixUi.inside(mouseX, mouseY, keyX, keyY, keyW, 20)) {
            listening = true;
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, resetX, resetY, 30, 14)) {
            KeyMapping binding = FlintFixClient.getFreecamKeyBinding();
            if (binding != null) bind(binding.getDefaultKey());
            listening = false;
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, speedX, speedY - 4, speedW, 12)) {
            draggingSpeed = true;
            updateSpeed(mouseX);
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, backX, backY, backW, 14)) {
            saveSpeed();
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSpeed && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateSpeed(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSpeed && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingSpeed = false;
            saveSpeed();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            listening = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            bind(InputConstants.UNKNOWN);
        } else {
            bind(FlintFixCompat.inputKey(keyCode, scanCode));
        }
        listening = false;
        return true;
    }

    @Override
    public void onClose() {
        saveSpeed();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
