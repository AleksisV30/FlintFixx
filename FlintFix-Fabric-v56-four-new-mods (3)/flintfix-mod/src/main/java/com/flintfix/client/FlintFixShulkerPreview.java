package com.flintfix.client;

import com.flintfix.client.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
*///?}
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Renders a small, real 27-slot inventory beside a hovered shulker box. */
public final class FlintFixShulkerPreview {
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int CELL = 18;
    private static final int PANEL_WIDTH = COLUMNS * CELL + 14;
    private static final int PANEL_HEIGHT = 22 + ROWS * CELL + 10;

    private FlintFixShulkerPreview() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
            ScreenEvents.afterRender(screen).register((renderedScreen, context, mouseX, mouseY, delta) -> {
                if (!FlintFixClient.CONFIG.shulkerPreviewEnabled || !(renderedScreen instanceof AbstractContainerScreen<?> handled)) return;
                Slot slot = ((HandledScreenAccessor) handled).flintfix$getSlotAt(mouseX, mouseY);
                if (slot != null) render(context, renderedScreen, mouseX, mouseY, slot.getItem());
            })
        );
    }

    private static void render(GuiGraphics context, Screen screen, int mouseX, int mouseY, ItemStack shulker) {
        if (!(shulker.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;
        NonNullList<ItemStack> stacks = NonNullList.withSize(COLUMNS * ROWS, ItemStack.EMPTY);
        if (!readContents(shulker, stacks)) return;

        int panelX = mouseX + 14;
        if (panelX + PANEL_WIDTH > screen.width - 4) panelX = mouseX - PANEL_WIDTH - 14;
        panelX = Math.max(4, Math.min(panelX, screen.width - PANEL_WIDTH - 4));
        int panelY = mouseY + 12;
        if (panelY + PANEL_HEIGHT > screen.height - 4) panelY = mouseY - PANEL_HEIGHT - 12;
        panelY = Math.max(4, Math.min(panelY, screen.height - PANEL_HEIGHT - 4));

        Minecraft client = Minecraft.getInstance();
        var matrices = context.pose();
        matrices.pushPose();
        matrices.translate(0.0, 0.0, 500.0);
        try {
            FlintFixUi.rounded(context, panelX + 2, panelY + 3, PANEL_WIDTH, PANEL_HEIGHT, 5, 0x88000000);
            FlintFixUi.rounded(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 5, 0xFF515A65);
            FlintFixUi.rounded(context, panelX + 1, panelY + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, 4, 0xF2161B22);
            context.drawString(client.font, Component.literal("SHULKER CONTENTS"), panelX + 7, panelY + 6, 0xFFF1F2F4, false);
            context.fill(panelX + 7, panelY + 18, panelX + PANEL_WIDTH - 7, panelY + 19, 0xFF343B44);

            int nonEmpty = 0;
            for (ItemStack stack : stacks) if (!stack.isEmpty()) nonEmpty++;

            int startX = panelX + 7;
            int startY = panelY + 22;
            for (int index = 0; index < stacks.size(); index++) {
                int x = startX + (index % COLUMNS) * CELL;
                int y = startY + (index / COLUMNS) * CELL;
                context.fill(x, y, x + CELL - 1, y + CELL - 1, 0xFF303740);
                ItemStack stack = stacks.get(index);
                if (!stack.isEmpty()) {
                    context.renderItem(stack, x + 1, y + 1);
                    context.renderItemDecorations(client.font, stack, x + 1, y + 1);
                }
            }

            String count = nonEmpty + "/27 slots";
            context.drawString(client.font, Component.literal(count), panelX + PANEL_WIDTH - 7 - client.font.width(count),
                panelY + PANEL_HEIGHT - 9, 0xFF9BA4B0, false);
        } finally {
            matrices.popPose();
        }
    }

    /** Copies a shulker box item's stored items into stacks; false when the item carries no contents. */
    private static boolean readContents(ItemStack shulker, NonNullList<ItemStack> stacks) {
        //? if >=1.20.5 {
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) return false;
        contents.copyInto(stacks);
        return true;
        //?} else {
        /*CompoundTag tag = BlockItem.getBlockEntityData(shulker);
        if (tag == null || !tag.contains("Items", 9)) return false;
        ContainerHelper.loadAllItems(tag, stacks);
        return true;
        *///?}
    }
}
