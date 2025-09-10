package net.revilodev.boundless.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

public final class QuestPanelClient {
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_book_hovered.png");
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");

    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;

    private static final Map<Screen, State> STATES = new WeakHashMap<>();
    private static Field LEFT_FIELD;
    private static Field RECIPE_BUTTON_FIELD;

    public static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen inv)) return;

        State st = STATES.computeIfAbsent(s, k -> new State(inv));
        int btnX = inv.getGuiLeft() + 125;
        int btnY = inv.getGuiTop() + 61;

        QuestToggleButton btn = new QuestToggleButton(
                btnX, btnY,
                BTN_TEX,
                BTN_TEX_HOVER,
                () -> toggle(st)
        );
        st.btn = btn;
        e.addListener(btn);
        reposition(inv, st);
    }

    public static void onScreenClosing(ScreenEvent.Closing e) {
        State st = STATES.remove(e.getScreen());
        if (st == null) return;
        if (st.open && st.originalLeft != null) setLeft(st.inv, st.originalLeft);
    }

    public static void onScreenRenderPre(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;

        if (st.open) {
            int centered = computeCenteredLeft(inv);
            setLeft(inv, centered);
            repositionRecipeBook(inv, st);
        }
        reposition(inv, st);

        if (st.btn != null) {
            st.btn.active = !getRecipeBook(inv).isVisible();
        }
    }

    public static void onScreenRenderPost(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        State st = STATES.get(s);
        if (st == null || !(s instanceof InventoryScreen inv)) return;

        if (!st.open) {
            ImageButton rb = getRecipeButton(inv);
            if (rb != null) rb.active = true;
            return;
        }

        int panelX = inv.getGuiLeft() - PANEL_W - 2;
        int panelY = inv.getGuiTop();
        RenderSystem.enableBlend();
        GuiGraphics gg = e.getGuiGraphics();
        gg.blit(PANEL_TEX, panelX, panelY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
    }

    private static void toggle(State st) {
        st.open = !st.open;
        if (st.open) {
            if (st.originalLeft == null) st.originalLeft = getLeft(st.inv);
            int centered = computeCenteredLeft(st.inv);
            setLeft(st.inv, centered);
            repositionRecipeBook(st.inv, st);
            ImageButton rb = getRecipeButton(st.inv);
            if (rb != null) rb.active = false;
        } else {
            if (st.originalLeft != null) setLeft(st.inv, st.originalLeft);
            repositionRecipeBook(st.inv, st);
            ImageButton rb = getRecipeButton(st.inv);
            if (rb != null) rb.active = true;
        }
        reposition(st.inv, st);
    }

    private static int computeCenteredLeft(InventoryScreen inv) {
        int screenW = inv.width;
        int invW = inv.getXSize();
        int total = PANEL_W + 2 + invW;
        return (screenW - total) / 2 + PANEL_W + 2;
    }

    private static void reposition(InventoryScreen inv, State st) {
        if (st.btn == null) return;
        int x = inv.getGuiLeft() + 125;
        int y = inv.getGuiTop() + 61;
        st.btn.setPosition(x, y);
    }

    private static void repositionRecipeBook(InventoryScreen inv, State st) {
        try {
            int dx = getLeft(inv) - (st.originalLeft == null ? getLeft(inv) : st.originalLeft);
            ImageButton rb = getRecipeButton(inv);
            if (rb != null) {
                rb.setX(inv.getGuiLeft() + inv.getXSize() - 20 + dx);
                rb.setY(inv.getGuiTop() + 5);
            }
        } catch (Throwable ignored) {}
    }

    private static RecipeBookComponent getRecipeBook(InventoryScreen inv) {
        return inv.getRecipeBookComponent();
    }

    private static ImageButton getRecipeButton(InventoryScreen inv) {
        try {
            if (RECIPE_BUTTON_FIELD == null) {
                RECIPE_BUTTON_FIELD = RecipeBookComponent.class.getDeclaredField("recipeButton");
                RECIPE_BUTTON_FIELD.setAccessible(true);
            }
            return (ImageButton) RECIPE_BUTTON_FIELD.get(inv.getRecipeBookComponent());
        } catch (Throwable t) {
            return null;
        }
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

    private static final class State {
        final InventoryScreen inv;
        QuestToggleButton btn;
        boolean open;
        Integer originalLeft;
        State(InventoryScreen inv) { this.inv = inv; }
    }
}
