package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixTitleBackground;
import com.flintfix.client.FlintFixTitleBranding;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
//? if <1.20.5 {
/*import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.util.Identifier;
*///?}
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow @Nullable private SplashTextRenderer splashText;

    @Unique private int flintfix$headerX;
    @Unique private int flintfix$headerY;
    @Unique private int flintfix$headerW;
    @Unique private int flintfix$headerH;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void flintfix$layoutTitleScreen(CallbackInfo ci) {
        // No vanilla yellow splash text.
        this.splashText = null;

        List<ButtonWidget> fullWidth = new ArrayList<>();
        List<ButtonWidget> splitWidth = new ArrayList<>();
        List<ButtonWidget> iconButtons = new ArrayList<>();

        for (var child : this.children()) {
            if (!(child instanceof ButtonWidget button)) continue;
            if (button.getHeight() < 18) continue;

            int width = button.getWidth();
            int center = button.getX() + width / 2;

            // Vanilla title menu: 200px main rows, ~98px bottom-row buttons,
            // and 20px language/accessibility buttons.
            if (width >= 180 && Math.abs(center - this.width / 2) <= 12) {
                fullWidth.add(button);
            } else if (width >= 80 && width < 180) {
                splitWidth.add(button);
            } else if (width < 80) {
                iconButtons.add(button);
            }
        }

        fullWidth.sort(Comparator.comparingInt(ButtonWidget::getY));
        splitWidth.sort(Comparator.comparingInt(ButtonWidget::getX));
        iconButtons.sort(Comparator.comparingInt(ButtonWidget::getX));

        // Smaller header so the complete vanilla menu comfortably fits.
        this.flintfix$headerW = Math.min(430, Math.max(320, this.width - 250));
        this.flintfix$headerH = 84;
        this.flintfix$headerX = (this.width - this.flintfix$headerW) / 2;
        this.flintfix$headerY = Math.max(18, Math.min(32, this.height / 14));

        int rowHeight = fullWidth.isEmpty() ? 20 : fullWidth.get(0).getHeight();
        int rowGap = 10;
        int bottomGap = 12;
        int menuHeight = rowHeight * 4 + rowGap * 2 + bottomGap;
        int footerReserve = 34;
        int desiredMenuY = this.flintfix$headerY + this.flintfix$headerH + 14;
        int maxMenuY = Math.max(this.flintfix$headerY + this.flintfix$headerH + 8,
            this.height - footerReserve - menuHeight);
        int menuY = Math.min(desiredMenuY, maxMenuY);

        int centerX = this.width / 2;
        int mainWidth = fullWidth.isEmpty() ? 200 : fullWidth.get(0).getWidth();
        int mainX = centerX - mainWidth / 2;

        // Singleplayer / Multiplayer / Realms
        for (int i = 0; i < Math.min(3, fullWidth.size()); i++) {
            ButtonWidget button = fullWidth.get(i);
            button.setPosition(mainX, menuY + i * (rowHeight + rowGap));
        }

        int bottomY = menuY + (rowHeight + rowGap) * 3 + bottomGap - rowGap;

        // Options + Quit Game
        if (!splitWidth.isEmpty()) {
            int splitGap = 4;
            int splitW = splitWidth.get(0).getWidth();
            int total = splitW * Math.min(2, splitWidth.size()) + splitGap;
            int startX = centerX - total / 2;
            for (int i = 0; i < Math.min(2, splitWidth.size()); i++) {
                splitWidth.get(i).setPosition(startX + i * (splitW + splitGap), bottomY);
            }
        }

        // Language + Accessibility icon buttons, kept aligned with bottom row.
        if (!iconButtons.isEmpty()) {
            int iconGap = 8;
            if (splitWidth.size() >= 2) {
                ButtonWidget left = splitWidth.get(0);
                ButtonWidget right = splitWidth.get(1);
                if (iconButtons.size() >= 1) {
                    ButtonWidget icon = iconButtons.get(0);
                    icon.setPosition(left.getX() - icon.getWidth() - iconGap, bottomY);
                }
                if (iconButtons.size() >= 2) {
                    ButtonWidget icon = iconButtons.get(iconButtons.size() - 1);
                    icon.setPosition(right.getX() + right.getWidth() + iconGap, bottomY);
                }
            }
        }
    }

    //? if >=1.20.5 {
    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void flintfix$replacePanorama(DrawContext context, float delta, CallbackInfo ci) {
        FlintFixTitleBackground.render(context, this.width, this.height);
        ci.cancel();
    }
    //?} else {
    /*/^* Before 1.20.5 the title screen draws the panorama cube map itself... ^/
    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/RotatingCubeMapRenderer;render(FF)V"))
    private void flintfix$replacePanorama(RotatingCubeMapRenderer panorama, float delta, float alpha,
                                          DrawContext context, int mouseX, int mouseY, float tickDelta) {
        FlintFixTitleBackground.render(context, this.width, this.height);
    }

    /^* ...followed by its vignette overlay, which the FlintFix background replaces too. ^/
    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V"))
    private void flintfix$hidePanoramaOverlay(DrawContext context, Identifier texture, int x, int y, int width,
                                              int height, float u, float v, int regionWidth, int regionHeight,
                                              int textureWidth, int textureHeight) {
        if (texture.getPath().contains("panorama_overlay")) return;
        context.drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }
    *///?}

    /** FlintFix branding replaces the vanilla Minecraft logo. */
    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"))
    private void flintfix$hideVanillaLogo(LogoDrawer logoDrawer, DrawContext context, int screenWidth, float alpha) {
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void flintfix$renderBranding(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        FlintFixTitleBranding.render(
            context,
            this.flintfix$headerX,
            this.flintfix$headerY,
            this.flintfix$headerW,
            this.flintfix$headerH
        );
    }
}
