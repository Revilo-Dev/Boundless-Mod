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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final Map<String, Boolean> subOpen = new HashMap<>();

    private float scrollY = 0;
    private final int rowH = 27;
    private final int pad = 1;
    private final int subHeaderH = 12;

    private String category = "all";

    public QuestListWidget(int x, int y, int w, int h, Consumer<QuestData.Quest> onClick) {
        super(x, y, w, h, Component.empty());
        this.onClick = onClick;
    }

    public void setQuests(Iterable<QuestData.Quest> qs) {
        quests.clear();
        for (QuestData.Quest q : qs) quests.add(q);
        quests.sort(Comparator.comparing(QuestData.Quest::sourceSortKey));
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

    private static final class RowEntry {
        final QuestData.SubCategory subCategory;
        final QuestData.Quest quest;

        RowEntry(QuestData.SubCategory subCategory) {
            this.subCategory = subCategory;
            this.quest = null;
        }

        RowEntry(QuestData.Quest quest) {
            this.quest = quest;
            this.subCategory = null;
        }

        boolean isHeader() {
            return subCategory != null;
        }
    }

    private String subKey(String cat, String subId) {
        String c = cat == null ? "" : cat;
        String s = subId == null ? "" : subId;
        return c + "::" + s;
    }

    private boolean isSubOpen(QuestData.SubCategory sc) {
        if (sc == null) return true;
        String key = subKey(sc.category, sc.id);
        return subOpen.getOrDefault(key, sc.defaultOpen);
    }

    private void toggleSubOpen(QuestData.SubCategory sc) {
        if (sc == null) return;
        String key = subKey(sc.category, sc.id);
        boolean next = !isSubOpen(sc);
        subOpen.put(key, next);
    }

    private String prettifyId(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String clean = raw.replace('_', ' ').replace('-', ' ').trim();
        String[] parts = clean.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.toString();
    }

    private void drawScaledString(GuiGraphics gg, String text, float scale, int x, int y, int color) {
        if (text == null || text.isEmpty()) return;
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1f);
        float inv = 1f / scale;
        gg.drawString(mc.font, text, Mth.floor(x * inv), Mth.floor(y * inv), color, false);
        gg.pose().popPose();
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

    private List<RowEntry> buildRows() {
        List<RowEntry> rows = new ArrayList<>();
        if (mc.player == null) return rows;

        List<QuestData.Quest> ungrouped = new ArrayList<>();
        Map<String, List<QuestData.Quest>> grouped = new HashMap<>();

        for (QuestData.Quest q : quests) {
            if (!matchesCategory(q)) continue;
            if (!isActuallyVisible(q)) continue;
            if (!passesFilters(q)) continue;

            if (q.subCategory == null || q.subCategory.isBlank()) {
                ungrouped.add(q);
            } else {
                String key = subKey(q.category, q.subCategory);
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
            }
        }

        for (QuestData.Quest q : ungrouped) {
            rows.add(new RowEntry(q));
        }

        Set<String> seen = new HashSet<>();
        List<QuestData.SubCategory> subCats = "all".equalsIgnoreCase(category)
                ? QuestData.subCategoriesAllOrdered()
                : QuestData.subCategoriesForCategory(category);

        for (QuestData.SubCategory sc : subCats) {
            String key = subKey(sc.category, sc.id);
            List<QuestData.Quest> qs = grouped.get(key);
            if (qs == null || qs.isEmpty()) continue;

            rows.add(new RowEntry(sc));
            if (isSubOpen(sc)) {
                for (QuestData.Quest q : qs) rows.add(new RowEntry(q));
            }
            seen.add(key);
        }

        for (var entry : grouped.entrySet()) {
            if (seen.contains(entry.getKey())) continue;
            List<QuestData.Quest> qs = entry.getValue();
            if (qs == null || qs.isEmpty()) continue;

            QuestData.Quest first = qs.get(0);
            QuestData.SubCategory sc = new QuestData.SubCategory(
                    first.subCategory,
                    first.category,
                    first.icon,
                    prettifyId(first.subCategory),
                    0,
                    true,
                    List.of(),
                    ""
            );

            rows.add(new RowEntry(sc));
            if (isSubOpen(sc)) {
                for (QuestData.Quest q : qs) rows.add(new RowEntry(q));
            }
        }

        return rows;
    }

    private int rowHeight(RowEntry row) {
        if (row == null) return 0;
        return (row.isHeader() ? subHeaderH : rowH) + pad;
    }

    private int contentHeight() {
        int total = 0;
        for (RowEntry row : buildRows()) {
            total += rowHeight(row);
        }
        return total;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        if (!visible || mc.player == null) return;

        RenderSystem.enableBlend();
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);

        int yOff = getY() - Mth.floor(scrollY);
        int yCursor = yOff;

        List<RowEntry> rows = buildRows();
        for (RowEntry row : rows) {
            int h = row.isHeader() ? subHeaderH : rowH;
            int top = yCursor;

            if (top > getY() + height) break;
            if (top + h < getY()) {
                yCursor += rowHeight(row);
                continue;
            }

            if (row.isHeader()) {
                QuestData.SubCategory sc = row.subCategory;
                boolean open = isSubOpen(sc);

                float iconScale = 0.45f;
                int iconSize = (int) (16 * iconScale);
                int iconX = getX() + 2;
                int iconY = top + (h - iconSize) / 2;

                sc.iconItem().ifPresent(item -> {
                    gg.pose().pushPose();
                    gg.pose().translate(iconX, iconY, 0);
                    gg.pose().scale(iconScale, iconScale, 1f);
                    gg.renderItem(new ItemStack(item), 0, 0);
                    gg.pose().popPose();
                });

                float textScale = 0.66f;
                String name = sc.name;
                int textX = iconX + iconSize + 2;
                int textH = (int) (mc.font.lineHeight * textScale);
                int textY = top + (h - textH) / 2 + 1;

                String sym = open ? "-" : "+";
                int symW = (int) (mc.font.width(sym) * textScale);
                int symX = getX() + width - symW - 2;
                int maxW = symX - textX - 2;
                int maxWUnscaled = maxW > 0 ? (int) (maxW / textScale) : 0;
                if (mc.font.width(name) > maxWUnscaled) {
                    name = mc.font.plainSubstrByWidth(name, Math.max(0, maxWUnscaled - mc.font.width("..."))) + "...";
                }

                drawScaledString(gg, name, textScale, textX, textY, 0xFFFFFF);
                drawScaledString(gg, sym, textScale, symX, textY, 0xFFFFFF);
            } else {
                QuestData.Quest q = row.quest;
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

                int indent = ("all".equalsIgnoreCase(category) || q.subCategory == null || q.subCategory.isBlank()) ? 0 : 8;

                q.iconItem().ifPresent(item ->
                        gg.renderItem(new ItemStack(item), getX() + 6 + indent, top + 5)
                );

                String name = q.name;
                int maxW = width - 42 - indent;
                if (mc.font.width(name) > maxW) {
                    name = mc.font.plainSubstrByWidth(name, maxW - mc.font.width("...")) + "...";
                }

                gg.drawString(mc.font, name, getX() + 30 + indent, top + 9,
                        deps ? 0xFFFFFF : 0xA0A0A0, false);
            }

            yCursor += rowHeight(row);
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
        int yCursor = 0;

        List<RowEntry> rows = buildRows();
        for (RowEntry row : rows) {
            int h = rowHeight(row);
            if (localY >= yCursor && localY < yCursor + h) {
                if (row.isHeader()) {
                    toggleSubOpen(row.subCategory);
                    return true;
                }
                if (row.quest != null && onClick != null) {
                    onClick.accept(row.quest);
                    return true;
                }
                return false;
            }
            yCursor += h;
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
