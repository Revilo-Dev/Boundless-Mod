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
    private static final ResourceLocation TEX_COMPLETE_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/complete_filter_hovered.png");
    private static final ResourceLocation TEX_COMPLETE_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/complete_filter_disabled.png");

    private static final ResourceLocation TEX_REJECT =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter.png");
    private static final ResourceLocation TEX_REJECT_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter_hovered.png");
    private static final ResourceLocation TEX_REJECT_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/reject_filter_disabled.png");

    private static final ResourceLocation TEX_LOCKED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter.png");
    private static final ResourceLocation TEX_LOCKED_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter_hovered.png");
    private static final ResourceLocation TEX_LOCKED_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/locked_filter_disabled.png");
    private static final ResourceLocation TEX_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button.png");
    private static final ResourceLocation TEX_SETTINGS_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button_hovered.png");

    private static boolean showCompleted = false;
    private static boolean showRejected = false;
    private static boolean showLocked = true;

    private final Runnable onSettings;

    public QuestFilterBar(int x, int y) {
        this(x, y, null);
    }

    public QuestFilterBar(int x, int y, Runnable onSettings) {
        super(x, y, SIZE * 3 + GAP * 2, SIZE, Component.empty());
        this.onSettings = onSettings;
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

    public int getPreferredWidth() {
        int count = showSettingsButton() ? 4 : 3;
        return SIZE * count + GAP * (count - 1);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        int bx = getX();
        int by = getY();

        drawButton(gg, bx, by, showCompleted, TEX_COMPLETE, TEX_COMPLETE_HOVER, TEX_COMPLETE_DISABLED,
                "Show redeemed quests", mouseX, mouseY);
        bx += SIZE + GAP;

        drawButton(gg, bx, by, showRejected, TEX_REJECT, TEX_REJECT_HOVER, TEX_REJECT_DISABLED,
                "Show rejected quests", mouseX, mouseY);
        bx += SIZE + GAP;

        drawButton(gg, bx, by, showLocked, TEX_LOCKED, TEX_LOCKED_HOVER, TEX_LOCKED_DISABLED,
                "Show locked quests", mouseX, mouseY);
        bx += SIZE + GAP;

        if (showSettingsButton()) {
            drawHoverButton(gg, bx, by, TEX_SETTINGS, TEX_SETTINGS_HOVER,
                    "Settings", mouseX, mouseY);
        }
    }

    private void drawButton(GuiGraphics gg, int x, int y, boolean state,
                            ResourceLocation on, ResourceLocation onHover, ResourceLocation off, String tooltip,
                            int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;

        ResourceLocation tex = state ? (hover ? onHover : on) : off;
        gg.blit(tex, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);

        if (hover) {
            gg.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(tooltip),
                    mouseX, mouseY);
        }
    }

    private void drawHoverButton(GuiGraphics gg, int x, int y,
                                 ResourceLocation normal, ResourceLocation hoverTex,
                                 String tooltip, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
        gg.blit(hover ? hoverTex : normal, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);

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
        bx += SIZE + GAP;

        if (showSettingsButton() && hit(mx, my, bx, by)) {
            if (onSettings != null) onSettings.run();
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

    private boolean showSettingsButton() {
        if (onSettings == null) return false;
        var player = Minecraft.getInstance().player;
        return player != null && player.hasPermissions(2);
    }
}
