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

    private static final int TAB_W = 32;
    private static final int TAB_H = 32;
    private static final int TAB_SELECTED_H = 35;
    private static final int GAP = -2;
    private static final int BAR_H = TAB_SELECTED_H;

    private static final ResourceLocation TEX_COMPLETE =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-completed.png");
    private static final ResourceLocation TEX_COMPLETE_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-completed-pulled.png");
    private static final ResourceLocation TEX_COMPLETE_SELECTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-completed-selected.png");

    private static final ResourceLocation TEX_REJECT =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-trash.png");
    private static final ResourceLocation TEX_REJECT_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-trash-pulled.png");
    private static final ResourceLocation TEX_REJECT_SELECTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-trash-select.png");

    private static final ResourceLocation TEX_LOCKED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-locked.png");
    private static final ResourceLocation TEX_LOCKED_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-locked-pulled.png");
    private static final ResourceLocation TEX_LOCKED_SELECTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-locked-selected.png");
    private static final ResourceLocation TEX_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-settings.png");
    private static final ResourceLocation TEX_SETTINGS_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pull-tab-settings-pulled.png");

    private static boolean showCompleted = false;
    private static boolean showRejected = false;
    private static boolean showLocked = true;

    private final Runnable onSettings;

    public QuestFilterBar(int x, int y) {
        this(x, y, null);
    }

    public QuestFilterBar(int x, int y, Runnable onSettings) {
        super(x, y, TAB_W * 3 + GAP * 2, BAR_H, Component.empty());
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
        return TAB_W * count + GAP * (count - 1);
    }

    public int getPreferredHeight() {
        return BAR_H;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        int bx = getX();
        int by = getY();

        drawButton(gg, bx, by, showCompleted, TEX_COMPLETE, TEX_COMPLETE_HOVER, TEX_COMPLETE_SELECTED,
                "Show redeemed quests", mouseX, mouseY);
        bx += TAB_W + GAP;

        drawButton(gg, bx, by, showRejected, TEX_REJECT, TEX_REJECT_HOVER, TEX_REJECT_SELECTED,
                "Show rejected quests", mouseX, mouseY);
        bx += TAB_W + GAP;

        drawButton(gg, bx, by, showLocked, TEX_LOCKED, TEX_LOCKED_HOVER, TEX_LOCKED_SELECTED,
                "Show locked quests", mouseX, mouseY);
        bx += TAB_W + GAP;

        if (showSettingsButton()) {
            drawHoverButton(gg, bx, by, TEX_SETTINGS, TEX_SETTINGS_HOVER,
                    "Settings", mouseX, mouseY);
        }
    }

    private void drawButton(GuiGraphics gg, int x, int y, boolean state,
                            ResourceLocation normal, ResourceLocation pulled, ResourceLocation selected, String tooltip,
                            int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + BAR_H;

        ResourceLocation tex = state ? selected : (hover ? pulled : normal);
        int h = state ? TAB_SELECTED_H : TAB_H;
        int drawY = y + BAR_H - h;
        gg.blit(tex, x, drawY, 0, 0, TAB_W, h, TAB_W, h);

        if (hover) {
            gg.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(tooltip),
                    mouseX, mouseY);
        }
    }

    private void drawHoverButton(GuiGraphics gg, int x, int y,
                                 ResourceLocation normal, ResourceLocation hoverTex,
                                 String tooltip, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + BAR_H;
        ResourceLocation tex = hover ? hoverTex : normal;
        int drawY = y + BAR_H - TAB_H;
        gg.blit(tex, x, drawY, 0, 0, TAB_W, TAB_H, TAB_W, TAB_H);

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
        bx += TAB_W + GAP;

        if (hit(mx, my, bx, by)) {
            showRejected = !showRejected;
            return true;
        }
        bx += TAB_W + GAP;

        if (hit(mx, my, bx, by)) {
            showLocked = !showLocked;
            return true;
        }
        bx += TAB_W + GAP;

        if (showSettingsButton() && hit(mx, my, bx, by)) {
            if (onSettings != null) onSettings.run();
            return true;
        }

        return false;
    }

    private boolean hit(double mx, double my, int x, int y) {
        return mx >= x && mx < x + TAB_W && my >= y && my < y + BAR_H;
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
