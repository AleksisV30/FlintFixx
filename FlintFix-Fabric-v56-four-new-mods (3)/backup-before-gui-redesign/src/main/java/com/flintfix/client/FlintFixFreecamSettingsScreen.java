package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** FlintFix card options for changing Freecam's on/off toggle key. */
public final class FlintFixFreecamSettingsScreen extends Screen {
    private final Screen parent;
    private int x, y, w, h;
    private int keyX, keyY, keyW;
    private int resetX, resetY;
    private int backX, backY, backW;
    private int speedX, speedY, speedW;
    private boolean listening;
    private boolean draggingSpeed;
    private boolean speedDirty;

    public FlintFixFreecamSettingsScreen(Screen parent) {
        super(Text.literal("Freecam Settings"));
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        context.fill(0, 0, width, height, 0xE5090B0E);
        FlintFixUi.fadeOutline(context, x - 2, y - 2, w + 4, h + 4, 4, 0x664A4D54);
        FlintFixUi.outlinedBox(context, x, y, w, h, 4, 0xFF4C5058, 0xFF17191D);
        FlintFixFont.draw(context, "FREECAM OPTIONS", x + 11, y + 9, 9, FlintFixClient.TEXT, true);
        FlintFixFont.draw(context, "Same key turns Freecam on and off", x + 11, y + 25, 5,
            FlintFixClient.MUTED, false);

        FlintFixUi.outlinedBox(context, x + 8, y + 39, w - 16, 27, 3, 0xFF343840, 0xFF202329);
        FlintFixFont.draw(context, "TOGGLE KEY", x + 13, y + 49, 5, FlintFixClient.TEXT, true);

        boolean hoverKey = FlintFixUi.inside(mouseX, mouseY, keyX, keyY, keyW, 20);
        FlintFixUi.compactButton(context, keyX, keyY, keyW, 20,
            listening ? "PRESS KEY" : keyLabel(), hoverKey || listening, listening);
        FlintFixUi.compactButton(context, resetX, resetY, 30, 14, "RESET",
            FlintFixUi.inside(mouseX, mouseY, resetX, resetY, 30, 14), false);

        FlintFixFont.draw(context, "MOVEMENT SPEED", x + 12, y + 76, 5,
            FlintFixClient.TEXT, true);
        String speedLabel = String.format(java.util.Locale.ROOT, "%.2f b/t", FlintFixClient.CONFIG.freecamSpeed);
        FlintFixFont.draw(context, speedLabel, x + w - 63, y + 76, 5,
            FlintFixClient.MUTED, false);
        FlintFixUi.rounded(context, speedX, speedY, speedW, 4, 2, 0xFF363B44);
        float speedT = (FlintFixClient.CONFIG.freecamSpeed - 0.10f) / 1.90f;
        int filled = Math.round((speedW - 6) * speedT);
        if (filled > 0) FlintFixUi.rounded(context, speedX + 3, speedY + 1, filled, 2, 1, 0xFF9DA3AD);
        int knobX = speedX + 3 + filled - 3;
        FlintFixUi.rounded(context, knobX, speedY - 3, 6, 10, 2, 0xFFE3E7EC);
        FlintFixFont.draw(context, "SPRINT KEY DOUBLES SPEED", x + 12, y + 106, 4,
            0xFF858B95, false);

        FlintFixUi.compactButton(context, backX, backY, backW, 14, "BACK",
            FlintFixUi.inside(mouseX, mouseY, backX, backY, backW, 14), false);
        FlintFixFont.draw(context, "Rebind this in Minecraft Controls too.", x + 71, backY + 4, 4,
            0xFF858B95, false);
    }

    private String keyLabel() {
        KeyBinding binding = FlintFixClient.getFreecamKeyBinding();
        if (binding == null || binding.isUnbound()) return "UNBOUND";
        return FlintFixFont.trim(binding.getBoundKeyLocalizedText().getString(), keyW - 8, 5, true);
    }

    private void bind(InputUtil.Key key) {
        KeyBinding binding = FlintFixClient.getFreecamKeyBinding();
        if (binding == null) return;
        binding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        client.options.write();
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
            KeyBinding binding = FlintFixClient.getFreecamKeyBinding();
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
            close();
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
            bind(InputUtil.UNKNOWN_KEY);
        } else {
            bind(InputUtil.fromKeyCode(keyCode, scanCode));
        }
        listening = false;
        return true;
    }

    @Override
    public void close() {
        saveSpeed();
        if (client != null) client.setScreen(parent);
    }
}
