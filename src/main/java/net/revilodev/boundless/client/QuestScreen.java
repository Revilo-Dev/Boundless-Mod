package net.revilodev.boundless.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class QuestScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_book.png");

    private int leftPos;
    private int topPos;

    public QuestScreen() {
        super(Component.literal("Quests"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - 248) / 2;
        this.topPos = (this.height - 166) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // ✅ Correct method signature in 1.21.1 parchment
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // ✅ Draw quest panel (120x166 section from 256x256 texture)
        graphics.blit(TEXTURE,
                leftPos - 120, topPos,   // screen position
                120, 166,                // width, height to draw
                0, 0,                    // u, v in texture
                256, 256                 // texture size
        );

        // Title text
        graphics.drawCenteredString(this.font, this.title, this.width / 2, topPos - 12, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
