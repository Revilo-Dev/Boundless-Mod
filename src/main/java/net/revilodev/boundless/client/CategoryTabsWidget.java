package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.quest.QuestData;

import java.util.List;
import java.util.function.Consumer;

public final class CategoryTabsWidget extends AbstractWidget {
    private static final ResourceLocation TAB_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");

    private final Consumer<String> onSelect;
    private List<QuestData.Category> cats = List.of();
    private String selected = "all";
    private final int tabW;
    private final int tabH = 26;
    private final int gap = 2;

    public CategoryTabsWidget(int x, int y, int w, int h, Consumer<String> onSelect) {
        super(x, y, w, h, Component.empty());
        this.onSelect = onSelect;
        this.tabW = w;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        cats = QuestData.categoriesOrdered();
        int ty = getY();
        for (int i = 0; i < cats.size(); i++) {
            int y = ty + i * (tabH + gap);
            boolean sel = cats.get(i).id.equals(selected);
            ResourceLocation tex = sel ? TAB_SELECTED_TEX : TAB_TEX;
            gg.blit(tex, getX(), y, 0, 0, tabW, tabH, tabW, tabH);
            Item icon = cats.get(i).iconItem().orElse(null);
            if (icon != null) gg.renderItem(new ItemStack(icon), getX() + 5, y + 5);
            if (mouseX >= getX() && mouseX < getX() + tabW && mouseY >= y && mouseY < y + tabH) {
                gg.renderTooltip(Minecraft.getInstance().font, Component.literal(cats.get(i).name), mouseX, mouseY);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;
        if (button != 0) return false;
        cats = QuestData.categoriesOrdered();
        for (int i = 0; i < cats.size(); i++) {
            int y = getY() + i * (tabH + gap);
            if (mouseX >= getX() && mouseX < getX() + tabW && mouseY >= y && mouseY < y + tabH) {
                selected = cats.get(i).id;
                if (onSelect != null) onSelect.accept(selected);
                return true;
            }
        }
        return false;
    }

    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
