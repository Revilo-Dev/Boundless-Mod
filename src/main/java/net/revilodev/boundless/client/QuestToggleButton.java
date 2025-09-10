package net.revilodev.boundless.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class QuestToggleButton extends AbstractButton {
    private final ResourceLocation normalTex;
    private final ResourceLocation hoverTex;
    private final Runnable onPressRunnable;

    public QuestToggleButton(int x, int y, ResourceLocation normal, ResourceLocation hovered, Runnable onPress) {
        super(x, y, 20, 18, Component.empty());
        this.normalTex = normal;
        this.hoverTex = hovered;
        this.onPressRunnable = onPress;
    }

    @Override
    public void onPress() {
        if (onPressRunnable != null) onPressRunnable.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ResourceLocation tex = this.isHoveredOrFocused() ? hoverTex : normalTex;
        g.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput n) {}
}
