package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.quest.QuestData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class CategoryTabsWidget extends AbstractWidget {
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");
    private static final ResourceLocation MOVE_DOWN =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/move_down.png");
    private static final ResourceLocation MOVE_UP =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/move_up.png");
    private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/move_down-highlighted.png");
    private static final ResourceLocation MOVE_UP_HIGHLIGHTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/move_up-highlighted.png");
    private static final int PAGE_SIZE = 5;
    private static final int CONTROL_ICON_BASE = 16;
    private static final int CONTROL_ICON = CONTROL_ICON_BASE;
    private static final int CONTROL_GAP = 2;

    private final Minecraft mc = Minecraft.getInstance();
    private final Consumer<String> onSelect;
    private final List<QuestData.Category> categories = new ArrayList<>();
    private String selected = "";
    private int pageIndex = 0;

    private int cellW = 26;
    private int cellH = 26;
    private int gap = 2;

    // Tooltip state (rendered later, on top)
    private Component pendingTooltip;
    private int pendingTooltipX;
    private int pendingTooltipY;
    private int tabRenderX() {
        return getX() + Math.max(0, width - cellW);
    }

    public CategoryTabsWidget(int x, int y, int w, int h, Consumer<String> onSelect) {
        super(x, y, w, h, Component.empty());
        this.onSelect = onSelect;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    public void setCategories(List<QuestData.Category> list) {
        categories.clear();
        for (QuestData.Category c : list) {
            if (c == null) continue;
            if ("all".equalsIgnoreCase(c.id)) continue;
            if (Config.disabledCategories().contains(c.id)) continue;
            categories.add(c);
        }

        if (!categories.isEmpty()) {
            boolean hasSelected = false;
            for (QuestData.Category c : categories) {
                if (c.id.equalsIgnoreCase(selected)) {
                    hasSelected = true;
                    break;
                }
            }
            if (!hasSelected) selected = categories.get(0).id;
        } else {
            selected = "";
            pageIndex = 0;
            return;
        }
        clampPage();
    }

    public void setSelected(String id) {
        this.selected = id == null ? "" : id;
        ensureSelectedVisible();
    }

    public String getSelectedId() {
        return selected;
    }

    public String getSelectedName() {
        if (selected == null || selected.isBlank()) return "";
        for (QuestData.Category c : categories) {
            if (c.id.equalsIgnoreCase(selected)) return c.name;
        }
        return "";
    }

    public String selectFirstCategory() {
        if (categories.isEmpty()) {
            selected = "";
            pageIndex = 0;
            return "";
        }
        selected = categories.get(0).id;
        ensureSelectedVisible();
        return selected;
    }

    /**
     * Call this AFTER the screen finishes rendering (i.e., at the end of Screen#render),
     * so it won't be clipped by any scissor/cutout used while rendering widgets.
     */
    public void renderHoverTooltipOnTop(GuiGraphics gg) {
        if (pendingTooltip == null) return;

        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 500.0F); // above normal GUI layers
        gg.renderTooltip(mc.font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        gg.pose().popPose();

        pendingTooltip = null;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        pendingTooltip = null;

        int visibleCount = visibleTabCount();
        if (visibleCount <= 0) return;
        clampPage();

        int x = tabRenderX();
        int y = getY();

        int start = pageIndex * PAGE_SIZE;
        int end = Math.min(categories.size(), start + visibleCount);
        for (int idx = start; idx < end; idx++) {
            QuestData.Category c = categories.get(idx);
            int i = idx - start;
            int top = y + i * (cellH + gap);

            boolean sel = !selected.isBlank() && c.id.equalsIgnoreCase(selected);
            ResourceLocation tex = sel ? TAB_SELECTED : TAB;

            gg.blit(tex, x, top, 0, 0, cellW, cellH, cellW, cellH);
            c.iconItem().ifPresent(it -> gg.renderItem(new ItemStack(it), x + 5, top + 5));

            boolean hover = mouseX >= x && mouseX < x + cellW && mouseY >= top && mouseY < top + cellH;
            if (hover) {
                pendingTooltip = Component.literal(c.name);
                pendingTooltipX = mouseX;
                pendingTooltipY = mouseY;
            }
        }
        renderPageControls(gg, mouseX, mouseY, getX(), y + visibleCount * (cellH + gap));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (button != 0) return false;

        int visibleCount = visibleTabCount();
        if (visibleCount <= 0) return false;
        clampPage();

        int x = tabRenderX();
        int y = getY();

        int start = pageIndex * PAGE_SIZE;
        int end = Math.min(categories.size(), start + visibleCount);
        for (int idx = start; idx < end; idx++) {
            int i = idx - start;
            int top = y + i * (cellH + gap);
            if (mouseX >= x && mouseX < x + cellW && mouseY >= top && mouseY < top + cellH) {
                String id = categories.get(idx).id;
                selected = id;
                if (onSelect != null) onSelect.accept(id);
                return true;
            }
        }

        if (categories.size() > PAGE_SIZE) {
            int controlsY = y + visibleCount * (cellH + gap);
            if (isOverUp(mouseX, mouseY, getX(), controlsY) && pageIndex > 0) {
                pageIndex--;
                return true;
            }
            if (isOverDown(mouseX, mouseY, getX(), controlsY) && pageIndex < maxPageIndex()) {
                pageIndex++;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    private int visibleTabCount() {
        return PAGE_SIZE;
    }

    private int maxPageIndex() {
        return Math.max(0, (categories.size() - 1) / PAGE_SIZE);
    }

    private void clampPage() {
        pageIndex = Math.max(0, Math.min(pageIndex, maxPageIndex()));
    }

    private void ensureSelectedVisible() {
        if (selected == null || selected.isBlank() || categories.isEmpty()) {
            clampPage();
            return;
        }
        int selectedIndex = -1;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id.equalsIgnoreCase(selected)) {
                selectedIndex = i;
                break;
            }
        }
        if (selectedIndex < 0) {
            clampPage();
            return;
        }
        pageIndex = selectedIndex / PAGE_SIZE;
        clampPage();
    }

    private void renderPageControls(GuiGraphics gg, int mouseX, int mouseY, int x, int y) {
        if (categories.size() <= PAGE_SIZE) return;
        int controlsY = y + 1;
        int iconX = tabRenderX() + (cellW - CONTROL_ICON) / 2;
        int downY = controlsY + CONTROL_ICON + CONTROL_GAP;
        boolean hoverUp = isOverUp(mouseX, mouseY, x, y);
        boolean hoverDown = isOverDown(mouseX, mouseY, x, y);
        gg.blit(hoverUp ? MOVE_UP_HIGHLIGHTED : MOVE_UP, iconX, controlsY, 0, 0, CONTROL_ICON, CONTROL_ICON, CONTROL_ICON_BASE, CONTROL_ICON_BASE);
        gg.blit(hoverDown ? MOVE_DOWN_HIGHLIGHTED : MOVE_DOWN, iconX, downY, 0, 0, CONTROL_ICON, CONTROL_ICON, CONTROL_ICON_BASE, CONTROL_ICON_BASE);

        String text = (pageIndex + 1) + "/" + (maxPageIndex() + 1);
        int textX = iconX + (CONTROL_ICON / 2) - (int) ((mc.font.width(text) * 0.8f) / 2f);
        int textY = downY + CONTROL_ICON + 2;
        drawScaledString(gg, text, 0.8f, textX, textY, 0xFFFFFFFF);

        if (hoverUp && pageIndex > 0) {
            pendingTooltip = Component.literal("Previous page");
            pendingTooltipX = mouseX;
            pendingTooltipY = mouseY;
        } else if (hoverDown && pageIndex < maxPageIndex()) {
            pendingTooltip = Component.literal("Next page");
            pendingTooltipX = mouseX;
            pendingTooltipY = mouseY;
        }
    }

    private boolean isOverUp(double mouseX, double mouseY, int x, int y) {
        int controlsY = y + 1;
        int iconX = tabRenderX() + (cellW - CONTROL_ICON) / 2;
        return mouseX >= iconX && mouseX < iconX + CONTROL_ICON
                && mouseY >= controlsY && mouseY < controlsY + CONTROL_ICON;
    }

    private boolean isOverDown(double mouseX, double mouseY, int x, int y) {
        int controlsY = y + 1;
        int iconX = tabRenderX() + (cellW - CONTROL_ICON) / 2;
        int downY = controlsY + CONTROL_ICON + CONTROL_GAP;
        return mouseX >= iconX && mouseX < iconX + CONTROL_ICON
                && mouseY >= downY && mouseY < downY + CONTROL_ICON;
    }

    private void drawScaledString(GuiGraphics gg, String text, float scale, int x, int y, int color) {
        if (text == null || text.isEmpty()) return;
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1f);
        float inv = 1f / scale;
        gg.drawString(mc.font, text, (int) (x * inv), (int) (y * inv), color, false);
        gg.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
