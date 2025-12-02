package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class QuestFilterBar extends AbstractWidget {

    private static final int SIZE = 20;
    private static final int GAP = 4;

    private static final ResourceLocation TEX_COMPLETE =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/complete_filter.png");
    private static final ResourceLocation TEX_COMPLETE_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/complete_filter_disabled.png");

    private static final ResourceLocation TEX_REJECT =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter.png");
    private static final ResourceLocation TEX_REJECT_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter_disabled.png");

    private static final ResourceLocation TEX_LOCKED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter.png");
    private static final ResourceLocation TEX_LOCKED_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter_disabled.png");

    private static boolean showCompleted = false;
    private static boolean showRejected = false;
    private static boolean showLocked = true;

    public QuestFilterBar(int x, int y) {
        super(x, y, SIZE * 3 + GAP * 2, SIZE, Component.empty());
    }

    public static boolean allowCompleted() {
        return showCompleted;
    }

    public static boolean allowRejected() {
        return showRejected;
    }

    public static boolean allowLocked() {
        return showLocked;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        int bx = getX();
        int by = getY();

        drawButton(gg, bx, by, showCompleted, TEX_COMPLETE, TEX_COMPLETE_DISABLED,
                "Show redeemed quests", mouseX, mouseY);
        bx += SIZE + GAP;

        drawButton(gg, bx, by, showRejected, TEX_REJECT, TEX_REJECT_DISABLED,
                "Show rejected quests", mouseX, mouseY);
        bx += SIZE + GAP;

        drawButton(gg, bx, by, showLocked, TEX_LOCKED, TEX_LOCKED_DISABLED,
                "Show locked quests (dependencies not met)", mouseX, mouseY);
    }

    private void drawButton(GuiGraphics gg, int x, int y, boolean state,
                            ResourceLocation on, ResourceLocation off, String tooltip,
                            int mouseX, int mouseY) {

        gg.blit(state ? on : off, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);

        boolean hover = mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;

        if (hover) {
            gg.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(tooltip),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || !active) return false;
        if (button != 0) return false;

        int bx = getX();
        int by = getY();

        if (hit(mx, my, bx, by)) {
            showCompleted = !showCompleted;
            return true;
        }
        bx += SIZE + GAP;

        if (hit(mx, my, bx, by)) {
            showRejected = !showRejected;
            return true;
        }
        bx += SIZE + GAP;

        if (hit(mx, my, bx, by)) {
            showLocked = !showLocked;
            return true;
        }

        return false;
    }

    private boolean hit(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SIZE && my >= y && my < y + SIZE;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
