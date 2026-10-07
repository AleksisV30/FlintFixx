package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import com.flintfix.client.FlintFixZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void flintfix$recordMouseClick(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) {
            FlintFixClient.recordClick(button);
        }
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void flintfix$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null || !FlintFixZoom.isActive()) return;
        FlintFixZoom.scroll(vertical);
        ci.cancel();
    }
}
