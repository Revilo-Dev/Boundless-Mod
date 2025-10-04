package net.revilodev.boundless.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class QuestListWidget extends AbstractWidget {
    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");
    private static final ResourceLocation ROW_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_disabled.png");

    private final Minecraft mc;
    private final List<QuestData.Quest> quests = new ArrayList<>();
    private final Consumer<QuestData.Quest> onClick;

    private float scrollY = 0f;
    private final int rowHeight = 27;
    private final int rowPad = 2;

    private String category = "all";

    public QuestListWidget(int x, int y, int width, int height, Consumer<QuestData.Quest> onClick) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty());
        this.mc = Minecraft.getInstance();
        this.onClick = onClick;
    }

    public void setQuests(Iterable<QuestData.Quest> all) {
        quests.clear();
        for (QuestData.Quest q : all) quests.add(q);
        scrollY = 0f;
    }

    public void setCategory(String categoryId) {
        this.category = categoryId == null ? "all" : categoryId;
        scrollY = 0f;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    private boolean matchesCategory(QuestData.Quest q) {
        if (Config.disabledCategories().contains(q.category)) return false;
        if ("all".equalsIgnoreCase(category)) return true;
        return q.category != null && q.category.equalsIgnoreCase(category);
    }

    private int contentHeight() {
        if (mc.player == null) return 0;
        int visible = 0;
        for (QuestData.Quest q : quests) {
            if (!matchesCategory(q)) continue;
            if (QuestTracker.isVisible(q, mc.player)) visible++;
        }
        return visible * (rowHeight + rowPad);
    }

    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        RenderSystem.enableBlend();
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        int yOff = this.getY() - Mth.floor(scrollY);
        int drawn = 0;
        for (QuestData.Quest q : quests) {
            if (mc.player == null) continue;
            if (!matchesCategory(q)) continue;
            if (!QuestTracker.isVisible(q, mc.player)) continue;
            int top = yOff + drawn * (rowHeight + rowPad);
            drawn++;
            if (top > this.getY() + this.height) break;
            if (top + rowHeight < this.getY()) continue;
            boolean deps = QuestTracker.dependenciesMet(q, mc.player);
            ResourceLocation rowTex = deps ? ROW_TEX : ROW_TEX_DISABLED;
            gg.blit(rowTex, this.getX(), top, 0, 0, 127, 27, 127, 27);
            Item iconItem = q.iconItem().orElse(null);
            if (iconItem != null) {
                gg.renderItem(new ItemStack(iconItem), this.getX() + 6, top + 5);
            }
            String name = q.name;
            int maxWidth = this.width - 42;
            int nameWidth = mc.font.width(name);
            if (nameWidth > maxWidth) {
                String trimmed = mc.font.plainSubstrByWidth(name, maxWidth - mc.font.width("...")) + "...";
                gg.drawString(mc.font, trimmed, this.getX() + 30, top + 9, deps ? 0xFFFFFF : 0xA0A0A0, false);
            } else {
                gg.drawString(mc.font, name, this.getX() + 30, top + 9, deps ? 0xFFFFFF : 0xA0A0A0, false);
            }
        }
        gg.disableScissor();
        int content = contentHeight();
        if (content > this.height) {
            float ratio = (float) this.height / content;
            int barHeight = Math.max(10, (int) (this.height * ratio));
            int barY = getY() + (int) ((this.height - barHeight) * (scrollY / (content - this.height)));
            gg.fill(getX() + width + 4, barY, getX() + width + 6, barY + barHeight, 0xFF808080);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;
        int content = contentHeight();
        int view = this.height;
        if (content <= view) return false;
        float max = content - view;
        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, max);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!this.visible || !this.active) return false;
        int content = contentHeight();
        int view = this.height;
        if (content <= view) return false;
        float max = content - view;
        scrollY = Mth.clamp(scrollY - (float) (deltaY * 12), 0f, max);
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active || !this.isMouseOver(mouseX, mouseY)) return false;
        if (button != 0) return false;
        if (mc.player == null) return false;
        int localY = (int) (mouseY - this.getY() + scrollY);
        int idx = localY / (rowHeight + rowPad);
        int visibleIndex = 0;
        for (QuestData.Quest q : quests) {
            if (!matchesCategory(q)) continue;
            if (!QuestTracker.isVisible(q, mc.player)) continue;
            if (visibleIndex == idx) {
                if (onClick != null) onClick.accept(q);
                return true;
            }
            visibleIndex++;
        }
        return false;
    }

    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
