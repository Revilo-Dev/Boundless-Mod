package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class QuestPanel {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_book.png");

    private boolean visible = false;
    private int x, y;

    public void toggle() {
        this.visible = !this.visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void init(int leftPos, int topPos) {
        this.x = leftPos - 120;
        this.y = topPos;
    }

    public void render(GuiGraphics graphics) {
        if (!visible) return;
        graphics.blit(TEXTURE, x, y, 0, 0, 120, 166, 256, 256);
    }
}
