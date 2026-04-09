package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class CategoryHeaderWidget extends AbstractWidget {
    private static final ResourceLocation HEADER_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/9-slice-header.png");
    private static final int TEX_W = 72;
    private static final int TEX_H = 10;
    private static final int SLICE = 3;

    private final Supplier<String> titleSupplier;
    private int panelX;
    private int panelY;
    private int panelW;

    public CategoryHeaderWidget(int panelX, int panelY, int panelW, Supplier<String> titleSupplier) {
        super(panelX, panelY, panelW, TEX_H, Component.empty());
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelW = panelW;
        this.titleSupplier = titleSupplier;
    }

    public void setPanelBounds(int x, int y, int w) {
        this.panelX = x;
        this.panelY = y;
        this.panelW = w;
        this.setX(x);
        this.setY(y);
        this.width = w;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        String title = titleSupplier == null ? "" : titleSupplier.get();
        if (title == null || title.isBlank()) title = "Categories";

        var font = Minecraft.getInstance().font;
        int textW = font.width(title);
        int headerW = Math.max(22, textW + 10);
        int x = panelX + 5;
        int y = panelY - 7;

        int middleW = Math.max(0, headerW - SLICE * 2);
        gg.blit(HEADER_TEX, x, y, 0, 0, SLICE, TEX_H, TEX_W, TEX_H);
        for (int i = 0; i < middleW; i++) {
            gg.blit(HEADER_TEX, x + SLICE + i, y, SLICE, 0, 1, TEX_H, TEX_W, TEX_H);
        }
        gg.blit(HEADER_TEX, x + SLICE + middleW, y, TEX_W - SLICE, 0, SLICE, TEX_H, TEX_W, TEX_H);

        int textX = x + (headerW - textW) / 2;
        int textY = y + 4;
        gg.drawString(font, title, textX, textY, 0x404040, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
