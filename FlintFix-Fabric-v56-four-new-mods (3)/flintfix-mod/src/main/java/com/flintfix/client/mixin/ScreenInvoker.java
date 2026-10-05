package com.flintfix.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets FlintFix rebuild a vanilla screen's widgets after changing the options they show. */
@Mixin(Screen.class)
public interface ScreenInvoker {
    @Invoker("clearAndInit")
    void flintfix$clearAndInit();
}
