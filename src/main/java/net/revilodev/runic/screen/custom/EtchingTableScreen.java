package net.revilodev.runic.screen.custom;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.client.EtchingPreviewRenderer;

public class EtchingTableScreen extends AbstractContainerScreen<EtchingTableMenu> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/container/etching_table.png");

    // preview box rect inside the 176x166 texture
    private static final int PREVIEW_X = 116;
    private static final int PREVIEW_Y = 16;
    private static final int PREVIEW_W = 52;
    private static final int PREVIEW_H = 52;

    public EtchingTableScreen(EtchingTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);

        // Vanilla GUI size
        this.imageWidth = 176;
        this.imageHeight = 166;

        // title
        this.titleLabelX = 40;
        this.titleLabelY = 20;

        // Inventory label
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Explicitly pass the actual PNG size (176x166) so it doesn’t get cut off
        gg.blit(TEX, x, y, 0, 0, this.imageWidth, this.imageHeight, 176, 166);

        // 3D item preview in the black box
        ItemStack toPreview = this.menu.getSlot(2).getItem(); // slot 2 = result slot
        if (!toPreview.isEmpty()) {
            EtchingPreviewRenderer.render(
                    gg,
                    toPreview,
                    x + PREVIEW_X,
                    y + PREVIEW_Y,
                    PREVIEW_W,
                    PREVIEW_H,
                    partialTick
            );
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg, mouseX, mouseY, partialTick); // vanilla gradient background
        super.render(gg, mouseX, mouseY, partialTick);
        this.renderTooltip(gg, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        gg.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
