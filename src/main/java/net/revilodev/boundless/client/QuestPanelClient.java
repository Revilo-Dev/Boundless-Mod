package net.revilodev.boundless.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.revilodev.boundless.quest.QuestData;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

public final class QuestPanelClient {
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_button_hovered.png");
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");

    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;

    private static final Map<Screen, State> STATES = new WeakHashMap<>();
    private static Field LEFT_FIELD;

    private static boolean lastQuestOpen = false;

    public static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen inv)) return;
        QuestData.loadClient(false);
        State st = STATES.computeIfAbsent(s, k -> new State(inv));
        int btnX = inv.getGuiLeft() + 125;
        int btnY = inv.getGuiTop() + 61;
        QuestToggleButton btn = new QuestToggleButton(btnX, btnY, BTN_TEX, BTN_TEX_HOVER, () -> toggle(st));
        st.btn = btn;
        if (st.bg == null) {
            st.bg = new PanelBackground(0, 0, PANEL_W, PANEL_H);
            e.addListener(st.bg);
        }
        if (st.tabs == null) {
            st.tabs = new CategoryTabsWidget(0, 0, 28, PANEL_H, id -> {
                if (st.list != null) st.list.setSelectedCategory(id);
            });
            e.addListener(st.tabs);
        }
        e.addListener(btn);
        createOrUpdateWidgets(e, inv, st);
        reposition(inv, st);
        if (lastQuestOpen) {
            st.open = true;
            if (st.originalLeft == null) st.originalLeft = getLeft(inv);
            int centered = computeCenteredLeft(inv);
            setLeft(inv, centered);
            updateVisibility(st);
        }
    }

    public static void onScreenClosing(ScreenEvent.Closing e) {
        State st = STATES.remove(e.getScreen());
        if (st == null) return;
        if (st.open && st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
    }

    public static void onScreenRenderPre(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (st.open) {
            int centered = computeCenteredLeft(inv);
            setLeft(inv, centered);
        }
        reposition(inv, st);
        updateVisibility(st);
        handleRecipeButtonRules(inv, st);
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post e) {}

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;
        if (!st.open || st.list == null || !st.list.visible) return;
        int px = computePanelX(inv) + 10;
        int py = inv.getGuiTop() + 10;
        int pw = 127;
        int ph = PANEL_H - 20;
        double mx = e.getMouseX();
        double my = e.getMouseY();
        if (mx < px || mx > px + pw || my < py || my > py + ph) return;
        double dY = e.getScrollDeltaY();
        boolean used = st.list.mouseScrolled(mx, my, dY);
        if (!used) used = st.list.mouseScrolled(mx, my, 0.0, dY);
        if (used) e.setCanceled(true);
    }

    private static void createOrUpdateWidgets(ScreenEvent.Init.Post e, InventoryScreen inv, State st) {
        if (st.list == null) {
            st.list = new QuestListWidget(0, 0, 127, PANEL_H - 20, q -> openDetails(st, q));
            st.list.setQuests(QuestData.all());
            e.addListener(st.list);
        }
        if (st.details == null) {
            st.details = new QuestDetailsPanel(0, 0, 127, PANEL_H - 20, () -> closeDetails(st));
            e.addListener(st.details);
            e.addListener(st.details.backButton());
            e.addListener(st.details.completeButton());
            e.addListener(st.details.rejectButton());
        }
        setPanelChildBounds(inv, st);
        updateVisibility(st);
    }

    private static void toggle(State st) {
        st.open = !st.open;
        lastQuestOpen = st.open;
        if (st.open) {
            if (st.originalLeft == null) st.originalLeft = getLeft(st.inv);
            int centered = computeCenteredLeft(st.inv);
            setLeft(st.inv, centered);
        } else if (st.originalLeft != null) {
            setLeft(st.inv, st.originalLeft);
        }
        reposition(st.inv, st);
        updateVisibility(st);
    }

    private static int computeCenteredLeft(InventoryScreen inv) {
        int screenW = inv.width;
        int invW = inv.getXSize();
        int total = PANEL_W + 2 + invW;
        return (screenW - total) / 2 + PANEL_W + 2;
    }

    private static int computePanelX(InventoryScreen inv) {
        return inv.getGuiLeft() - PANEL_W - 2;
    }

    private static void setPanelChildBounds(InventoryScreen inv, State st) {
        int bgx = computePanelX(inv);
        int bgy = inv.getGuiTop();
        int tx = bgx - st.tabs.getWidth() + 3;
        int ty = bgy + 10;
        int px = bgx + 10;
        int py = bgy + 10;
        int pw = 127;
        int ph = PANEL_H - 20;
        if (st.bg != null) st.bg.setBounds(bgx, bgy, PANEL_W, PANEL_H);
        if (st.tabs != null) st.tabs.setBounds(tx, ty, st.tabs.getWidth(), PANEL_H - 20);
        if (st.list != null) st.list.setBounds(px, py, pw, ph);
        if (st.details != null) {
            st.details.setBounds(px, py, pw, ph);
            st.details.backButton().setPosition(px + 2, py + ph - st.details.backButton().getHeight() - 4);
            st.details.completeButton().setPosition(px + (pw - st.details.completeButton().getWidth()) / 2, py + ph - st.details.completeButton().getHeight() - 4);
            st.details.rejectButton().setPosition(px + pw - st.details.rejectButton().getWidth() - 2, py + ph - st.details.rejectButton().getHeight() - 4);
        }
    }

    private static void reposition(InventoryScreen inv, State st) {
        if (st.btn != null) {
            int x = inv.getGuiLeft() + 125;
            int y = inv.getGuiTop() + 61;
            st.btn.setPosition(x, y);
        }
        setPanelChildBounds(inv, st);
    }

    private static void handleRecipeButtonRules(InventoryScreen inv, State st) {
        if (st.open) {
            if (st.btn != null) st.btn.visible = true;
            toggleRecipeButtonVisibility(inv, false);
        } else if (isRecipePanelOpen(inv)) {
            if (st.btn != null) st.btn.visible = false;
            toggleRecipeButtonVisibility(inv, true);
        } else {
            if (st.btn != null) st.btn.visible = true;
            toggleRecipeButtonVisibility(inv, true);
        }
    }

    private static void toggleRecipeButtonVisibility(InventoryScreen inv, boolean visible) {
        for (var child : inv.children()) {
            if (child instanceof ImageButton btn) {
                if (btn.getWidth() == 20 && btn.getHeight() == 18) {
                    btn.visible = visible;
                }
            }
        }
    }

    private static boolean isRecipePanelOpen(InventoryScreen inv) {
        int centeredLeft = (inv.width - inv.getXSize()) / 2;
        return inv.getGuiLeft() > centeredLeft + 10;
    }

    private static Integer getLeft(InventoryScreen inv) {
        try {
            if (LEFT_FIELD == null) LEFT_FIELD = findLeftField(inv.getClass());
            return (Integer) LEFT_FIELD.get(inv);
        } catch (Throwable t) {
            return inv.getGuiLeft();
        }
    }

    private static void setLeft(InventoryScreen inv, int v) {
        try {
            if (LEFT_FIELD == null) LEFT_FIELD = findLeftField(inv.getClass());
            LEFT_FIELD.setInt(inv, v);
        } catch (Throwable ignored) {}
    }

    private static Field findLeftField(Class<?> c) throws NoSuchFieldException {
        Class<?> cur = c;
        while (cur != null) {
            try {
                Field f = cur.getDeclaredField("leftPos");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException("leftPos");
    }

    private static void openDetails(State st, net.revilodev.boundless.quest.QuestData.Quest quest) {
        if (st.details == null) return;
        st.details.setQuest(quest);
        st.showingDetails = true;
        updateVisibility(st);
    }

    private static void closeDetails(State st) {
        st.showingDetails = false;
        updateVisibility(st);
    }

    private static void updateVisibility(State st) {
        boolean listVisible = st.open && !st.showingDetails;
        boolean detailsVisible = st.open && st.showingDetails;
        if (st.bg != null) st.bg.visible = st.open;
        if (st.tabs != null) { st.tabs.visible = st.open; st.tabs.active = st.open; }
        if (st.list != null) { st.list.visible = listVisible; st.list.active = listVisible; }
        if (st.details != null) {
            st.details.visible = detailsVisible;
            st.details.active = detailsVisible;
            st.details.backButton().visible = detailsVisible;
            st.details.backButton().active = detailsVisible;
            st.details.completeButton().visible = detailsVisible;
            st.details.completeButton().active = detailsVisible;
            st.details.rejectButton().visible = detailsVisible;
            st.details.rejectButton().active = detailsVisible;
        }
    }

    private static final class PanelBackground extends AbstractWidget {
        public PanelBackground(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
        }
        public void setBounds(int x, int y, int w, int h) {
            this.setX(x); this.setY(y); this.width = w; this.height = h;
        }
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.disableBlend();
            gg.blit(PANEL_TEX, getX(), getY(), 0, 0, width, height, width, height);
        }
        public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput n) {}
    }

    private static final class State {
        final InventoryScreen inv;
        QuestToggleButton btn;
        PanelBackground bg;
        CategoryTabsWidget tabs;
        QuestListWidget list;
        QuestDetailsPanel details;
        boolean showingDetails;
        boolean open;
        Integer originalLeft;
        State(InventoryScreen inv) { this.inv = inv; }
    }
}
