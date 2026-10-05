package com.flintfix.client;

import com.flintfix.client.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

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
                if (!FlintFixClient.CONFIG.shulkerPreviewEnabled || !(renderedScreen instanceof HandledScreen<?> handled)) return;
                Slot slot = ((HandledScreenAccessor) handled).flintfix$getSlotAt(mouseX, mouseY);
                if (slot != null) render(context, renderedScreen, mouseX, mouseY, slot.getStack());
            })
        );
    }

    private static void render(DrawContext context, Screen screen, int mouseX, int mouseY, ItemStack shulker) {
        if (!(shulker.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;
        ContainerComponent contents = shulker.get(DataComponentTypes.CONTAINER);
        if (contents == null) return;

        int panelX = mouseX + 14;
        if (panelX + PANEL_WIDTH > screen.width - 4) panelX = mouseX - PANEL_WIDTH - 14;
        panelX = Math.max(4, Math.min(panelX, screen.width - PANEL_WIDTH - 4));
        int panelY = mouseY + 12;
        if (panelY + PANEL_HEIGHT > screen.height - 4) panelY = mouseY - PANEL_HEIGHT - 12;
        panelY = Math.max(4, Math.min(panelY, screen.height - PANEL_HEIGHT - 4));

        MinecraftClient client = MinecraftClient.getInstance();
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0.0, 0.0, 500.0);
        try {
            FlintFixUi.rounded(context, panelX + 2, panelY + 3, PANEL_WIDTH, PANEL_HEIGHT, 5, 0x88000000);
            FlintFixUi.rounded(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 5, 0xFF515A65);
            FlintFixUi.rounded(context, panelX + 1, panelY + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, 4, 0xF2161B22);
            context.drawText(client.textRenderer, Text.literal("SHULKER CONTENTS"), panelX + 7, panelY + 6, 0xFFF1F2F4, false);
            context.fill(panelX + 7, panelY + 18, panelX + PANEL_WIDTH - 7, panelY + 19, 0xFF343B44);

            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(COLUMNS * ROWS, ItemStack.EMPTY);
            contents.copyTo(stacks);
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
                    context.drawItem(stack, x + 1, y + 1);
                    context.drawItemInSlot(client.textRenderer, stack, x + 1, y + 1);
                }
            }

            String count = nonEmpty + "/27 slots";
            context.drawText(client.textRenderer, Text.literal(count), panelX + PANEL_WIDTH - 7 - client.textRenderer.getWidth(count),
                panelY + PANEL_HEIGHT - 9, 0xFF9BA4B0, false);
        } finally {
            matrices.pop();
        }
    }
}
