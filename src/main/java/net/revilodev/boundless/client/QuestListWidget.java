package net.revilodev.boundless.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class QuestListWidget extends AbstractWidget {

    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");
    private static final ResourceLocation ROW_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_disabled.png");
    private static final ResourceLocation ROW_TEX_REDEEMABLE =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_redeemable.png");
    private static final ResourceLocation ROW_TEX_COMPLETED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_completed.png");
    private static final ResourceLocation ROW_TEX_DISCARDED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget_discarded.png");

    private final Minecraft mc = Minecraft.getInstance();
    private final List<QuestData.Quest> quests = new ArrayList<>();
    private final Consumer<QuestData.Quest> onClick;

    private float scrollY = 0;
    private final int rowH = 27;
    private final int pad = 2;

    private String category = "all";

    public QuestListWidget(int x, int y, int w, int h, Consumer<QuestData.Quest> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
    }

    public void setQuests(Iterable<QuestData.Quest> qs) {
        quests.clear();
        for (QuestData.Quest q : qs) quests.add(q);
        scrollY = 0;
    }

    public void setCategory(String cat) {
        category = cat == null ? "all" : cat;
        scrollY = 0;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    private boolean categoryUnlocked(String catId) {
        var c = QuestData.categoryById(catId).orElse(null);
        if (mc.player == null) return true;
        return QuestData.isCategoryUnlocked(c, mc.player);
    }

    private boolean includeInAll(QuestData.Quest q) {
        if (mc.player == null) return true;
        return QuestData.includeQuestInAll(q, mc.player);
    }

    private boolean matchesCategory(QuestData.Quest q) {
        if (Config.disabledCategories().contains(q.category)) return false;
        if ("all".equalsIgnoreCase(category)) return includeInAll(q);
        if (!q.category.equalsIgnoreCase(category)) return false;
        return categoryUnlocked(q.category);
    }

    private boolean isActuallyVisible(QuestData.Quest q) {
        if (mc.player == null) return true;
        QuestTracker.Status st = QuestTracker.getStatus(q, mc.player);
        if (st == QuestTracker.Status.INCOMPLETE) {
            return QuestTracker.isVisible(q, mc.player);
        }
        return true;
    }

    private boolean passesFilters(QuestData.Quest q) {
        if (mc.player == null) return true;

        QuestTracker.Status st = QuestTracker.getStatus(q, mc.player);
        boolean deps = QuestTracker.dependenciesMet(q, mc.player);

        if (!QuestFilterBar.allowCompleted() && st == QuestTracker.Status.REDEEMED) {
            return false;
        }

        if (!QuestFilterBar.allowRejected() && st == QuestTracker.Status.REJECTED) {
            return false;
        }

        if (!QuestFilterBar.allowLocked() && !deps) {
            return false;
        }

        return true;
    }

    private int contentHeight() {
        if (mc.player == null) return 0;
        int visible = 0;

        for (QuestData.Quest q : quests) {
            if (!matchesCategory(q)) continue;
            if (!isActuallyVisible(q)) continue;
            if (!passesFilters(q)) continue;
            visible++;
        }

        return visible * (rowH + pad);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        if (!visible || mc.player == null) return;

        RenderSystem.enableBlend();
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);

        int yOff = getY() - Mth.floor(scrollY);
        int drawn = 0;

        for (QuestData.Quest q : quests) {
            if (!matchesCategory(q)) continue;
            if (!isActuallyVisible(q)) continue;
            if (!passesFilters(q)) continue;

            int top = yOff + drawn * (rowH + pad);
            drawn++;

            if (top > getY() + height) break;
            if (top + rowH < getY()) continue;

            QuestTracker.Status st = QuestTracker.getStatus(q, mc.player);
            boolean deps = QuestTracker.dependenciesMet(q, mc.player);
            boolean ready = deps && QuestTracker.isReady(q, mc.player);

            ResourceLocation tex;
            if (!deps) {
                tex = ROW_TEX_DISABLED;
            } else if (st == QuestTracker.Status.REJECTED) {
                tex = ROW_TEX_DISCARDED;
            } else if (st == QuestTracker.Status.REDEEMED) {
                tex = ROW_TEX_COMPLETED;
            } else if (st == QuestTracker.Status.COMPLETED || ready) {
                tex = ROW_TEX_REDEEMABLE;
            } else {
                tex = ROW_TEX;
            }

            gg.blit(tex, getX(), top, 0, 0, 127, 27, 127, 27);

            q.iconItem().ifPresent(item ->
                    gg.renderItem(new ItemStack(item), getX() + 6, top + 5)
            );

            String name = q.name;
            int maxW = width - 42;
            if (mc.font.width(name) > maxW) {
                name = mc.font.plainSubstrByWidth(name, maxW - mc.font.width("...")) + "...";
            }

            gg.drawString(mc.font, name, getX() + 30, top + 9,
                    deps ? 0xFFFFFF : 0xA0A0A0, false);
        }

        gg.disableScissor();

        int content = contentHeight();
        if (content > height) {
            float maxScroll = content - height;
            float ratio = (float) height / (float) content;
            int barH = Math.max(12, (int) (height * ratio));
            float scrollRatio = maxScroll <= 0 ? 0f : scrollY / maxScroll;
            int barY = getY() + (int) ((height - barH) * scrollRatio);
            gg.fill(getX() + width + 4, barY, getX() + width + 6, barY + barH, 0xFF808080);
        }
    }

    @Override
    public boolean mouseClicked(double mxD, double myD, int button) {
        if (!visible || !active || button != 0) return false;

        int mx = (int) mxD;
        int my = (int) myD;

        if (!isMouseOver(mx, my)) return false;
        if (mc.player == null) return false;

        int localY = (int) (my - getY() + scrollY);
        int idx = localY / (rowH + pad);
        int visibleIndex = 0;

        for (QuestData.Quest q : quests) {
            if (!matchesCategory(q)) continue;
            if (!isActuallyVisible(q)) continue;
            if (!passesFilters(q)) continue;

            if (visibleIndex == idx) {
                if (onClick != null) onClick.accept(q);
                return true;
            }
            visibleIndex++;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !active) return false;
        int content = contentHeight();
        if (content <= height) return false;

        float max = content - height;
        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, max);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput n) {
    }
}
