package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class QuestToggleButton extends AbstractButton {
    private ResourceLocation texNormal;
    private ResourceLocation texHover;
    private final Runnable onPress;

    public QuestToggleButton(int x, int y, ResourceLocation normal, ResourceLocation hover, Runnable onPress) {
        super(x, y, 20, 18, Component.empty());
        this.texNormal = normal;
        this.texHover = hover;
        this.onPress = onPress;
    }

    public void setTextures(ResourceLocation normal, ResourceLocation hover) {
        this.texNormal = normal;
        this.texHover = hover;
    }

    public void onPress() {
        if (onPress != null) onPress.run();
    }

    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isMouseOver(mouseX, mouseY);
        ResourceLocation tex = hovered ? texHover : texNormal;
        gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
    }

    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {}
}
