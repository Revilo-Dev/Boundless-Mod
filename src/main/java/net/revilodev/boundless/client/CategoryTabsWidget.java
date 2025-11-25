package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.quest.QuestData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CategoryTabsWidget extends AbstractWidget {
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");

    private final Minecraft mc = Minecraft.getInstance();
    private final Consumer<String> onSelect;
    private final List<QuestData.Category> categories = new ArrayList<>();
    private String selected = "all";

    private int cellW = 26;
    private int cellH = 26;
    private int gap = 2;
    private static final int MAX_TABS = 5;

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
        int count = 0;
        for (QuestData.Category c : list) {
            if (Config.disabledCategories().contains(c.id)) continue;
            categories.add(c);
            count++;
            if (count >= MAX_TABS) break;
        }
    }

    public void setSelected(String id) {
        this.selected = id == null ? "all" : id;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        int x = getX();
        int y = getY();
        int i = 0;
        for (QuestData.Category c : categories) {
            int top = y + i * (cellH + gap);
            boolean sel = c.id.equalsIgnoreCase(selected);
            ResourceLocation tex = sel ? TAB_SELECTED : TAB;
            gg.blit(tex, x, top, 0, 0, cellW, cellH, cellW, cellH);
            c.iconItem().ifPresent(it -> gg.renderItem(new ItemStack(it), x + 5, top + 5));
            boolean hover = mouseX >= x && mouseX < x + cellW && mouseY >= top && mouseY < top + cellH;
            if (hover) gg.renderTooltip(mc.font, Component.literal(c.name), mouseX, mouseY);
            i++;
            if (i >= MAX_TABS) break;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (button != 0) return false;
        int x = getX();
        int y = getY();
        for (int i = 0; i < Math.min(categories.size(), MAX_TABS); i++) {
            int top = y + i * (cellH + gap);
            if (mouseX >= x && mouseX < x + cellW && mouseY >= top && mouseY < top + cellH) {
                String id = categories.get(i).id;
                selected = id;
                if (onSelect != null) onSelect.accept(id);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
