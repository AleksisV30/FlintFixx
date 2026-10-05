package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import com.flintfix.client.FlintFixZoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void flintfix$recordMouseClick(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            FlintFixClient.recordClick(button);
        }
    }

    @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void flintfix$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || !FlintFixZoom.isActive()) return;
        FlintFixZoom.scroll(vertical);
        ci.cancel();
    }
}
