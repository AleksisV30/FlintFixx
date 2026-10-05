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
        // Glassy panel over the animated background; hover slides in a violet edge.
        float hoverT = FlintFixUi.hoverProgress("title-button:" + x + ":" + y, selected && this.active);
        int accent = 0xFF8A72FF;
        int shadow = withAlpha(0x40000000, widgetAlpha);
        int border = withAlpha(FlintFixUi.blendColors(0x40FFFFFF, accent, hoverT), widgetAlpha);
        int fill = withAlpha(!this.active ? 0x80101322
            : FlintFixUi.blendColors(0xB30E1226, 0xD91B1D3D, hoverT), widgetAlpha);
        int textColor = this.active
            ? withAlpha(FlintFixUi.blendColors(0xFFE6E9F5, 0xFFFFFFFF, hoverT), widgetAlpha)
            : withAlpha(0xFF6F7688, widgetAlpha);

        FlintFixUi.roundedRaw(context, x - 1, y + 2, w + 2, h + 1, 3, shadow);
        FlintFixUi.roundedRaw(context, x, y, w, h, 3, border);
        FlintFixUi.roundedRaw(context, x + 1, y + 1, w - 2, h - 2, 2, fill);
        context.fill(x + 3, y + 1, x + w - 3, y + 2, withAlpha(0x14FFFFFF, widgetAlpha));
        if (hoverT > 0.01f) {
            int barW = Math.round((w - 16) * hoverT);
            context.fill(x + w / 2 - barW / 2, y + h - 2, x + w / 2 + barW / 2, y + h - 1,
                withAlpha(FlintFixUi.opacity(accent, hoverT), widgetAlpha));
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
