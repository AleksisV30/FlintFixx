package com.flintfix.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    //? if >=1.21.2 {
    /*@Invoker("getHoveredSlot")
    *///?} else {
    @Invoker("findSlot")
    //?}
    @Nullable Slot flintfix$getSlotAt(double x, double y);
}
