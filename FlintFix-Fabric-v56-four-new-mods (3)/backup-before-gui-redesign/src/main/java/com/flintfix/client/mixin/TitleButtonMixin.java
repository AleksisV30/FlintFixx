package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixUi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class TitleButtonMixin extends ClickableWidget {
    protected TitleButtonMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void flintfix$renderTitleButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(MinecraftClient.getInstance().currentScreen instanceof TitleScreen)) return;
        if (this.getWidth() < 90 || this.getHeight() < 18) return;

        ci.cancel();

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        boolean selected = this.isHovered() || this.isFocused();

        float widgetAlpha = MathHelper.clamp(this.alpha, 0.0F, 1.0F);
        int shadow = withAlpha(selected ? 0x222B63B6 : 0x16000000, widgetAlpha);
        int border = withAlpha(selected ? 0xFF5A72B6 : 0xFF31405D, widgetAlpha);
        int fill = withAlpha(!this.active ? 0xFF1A1D26 : (selected ? 0xFF1B2538 : 0xFF131C2D), widgetAlpha);
        int inset = withAlpha(!this.active ? 0xFF11151D : (selected ? 0xFF162031 : 0xFF101726), widgetAlpha);
        int highlight = withAlpha(selected ? 0xFFAE7BFF : 0xFF506284, widgetAlpha);
        int textColor = this.active
            ? (selected ? withAlpha(0xFFFFFFFF, widgetAlpha) : withAlpha(0xFFF3F6FD, widgetAlpha))
            : withAlpha(0xFF7F8898, widgetAlpha);

        FlintFixUi.rounded(context, x - 2, y + 2, w + 4, h + 2, 10, shadow);
        FlintFixUi.rounded(context, x - 1, y - 1, w + 2, h + 2, 10, border);
        FlintFixUi.rounded(context, x, y, w, h, 9, fill);
        FlintFixUi.rounded(context, x + 2, y + 2, w - 4, h - 4, 8, inset);
        FlintFixUi.rounded(context, x + 10, y + h - 5, w - 20, 2, 1, withAlpha(0x2FFFFFFF, widgetAlpha));
        if (selected && this.active) {
            FlintFixUi.rounded(context, x + 8, y + h - 4, w - 16, 2, 1, highlight);
        }

        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            this.getMessage(),
            x + w / 2,
            y + (h - 8) / 2,
            textColor
        );
    }

    private static int withAlpha(int argb, float multiplier) {
        int alpha = (argb >>> 24) & 0xFF;
        alpha = MathHelper.clamp(Math.round(alpha * multiplier), 0, 255);
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }
}
