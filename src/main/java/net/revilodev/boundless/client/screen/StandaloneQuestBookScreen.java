package net.revilodev.boundless.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.client.CategoryTabsWidget;
import net.revilodev.boundless.client.QuestDetailsPanel;
import net.revilodev.boundless.client.QuestListWidget;
import net.revilodev.boundless.quest.QuestData;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

@OnlyIn(Dist.CLIENT)
public final class StandaloneQuestBookScreen extends Screen {

    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");

    private int panelWidth = 147;
    private int panelHeight = 166;

    private int leftX;
    private int rightX;
    private int topY;

    private CategoryTabsWidget tabs;
    private QuestListWidget list;
    private QuestDetailsPanel details;

    private boolean showingDetails = false;

    public StandaloneQuestBookScreen() {
        super(Component.literal("Quests"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        leftX = cx - panelWidth - 2;
        rightX = cx + 2;
        topY = cy - panelHeight / 2;

        int pxLeft = leftX + 10;
        int pxRight = rightX + 10;
        int py = topY + 10;
        int pw = 127;
        int ph = panelHeight - 20;

        tabs = new CategoryTabsWidget(leftX - 23, topY + 4, 26, panelHeight - 8, id -> {
            list.setCategory(id);
            showingDetails = false;
            updateVisibility();
        });
        tabs.setCategories(QuestData.categoriesOrdered());
        tabs.setSelected("all");

        list = new QuestListWidget(pxLeft, py, pw, ph, q -> {
            details.setQuest(q);
            showingDetails = true;
            updateVisibility();
        });
        list.setQuests(QuestData.all());
        list.setCategory("all");

        details = new QuestDetailsPanel(pxRight, py, pw, ph, () -> {
            showingDetails = false;
            updateVisibility();
        });

        // REQUIRED: aligns the entire details panel content & scissor
        details.setBounds(pxRight, py, pw, ph);

        addRenderableWidget(tabs);
        addRenderableWidget(list);
        addRenderableWidget(details);
        addRenderableWidget(details.backButton());
        addRenderableWidget(details.completeButton());
        addRenderableWidget(details.rejectButton());

        updateVisibility();
    }

    private void updateVisibility() {

        // --- do not hide quest list on this screen ---
        list.visible = true;
        list.active = true;

        // details panel toggles normally
        details.visible = showingDetails;
        details.active = showingDetails;

        details.backButton().visible = showingDetails;
        details.backButton().active = showingDetails;

        details.completeButton().visible = showingDetails;
        details.completeButton().active = showingDetails;

        details.rejectButton().visible = showingDetails;
        details.rejectButton().active = showingDetails;

        tabs.visible = true;
        tabs.active = true;
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // disable vanilla background entirely
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {

        // --- inventory-style darkening ---
        gg.fill(0, 0, this.width, this.height, 0xA0000000);

        // --- panels ---
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);

        // --- widgets ---
        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

        if (list.visible && list.active) {
            if (mouseX >= list.getX() && mouseX <= list.getX() + list.getWidth()
                    && mouseY >= list.getY() && mouseY <= list.getY() + list.getHeight()) {

                if (list.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
        }

        if (details.visible && details.active) {
            if (mouseX >= details.getX() && mouseX <= details.getX() + details.getWidth()
                    && mouseY >= details.getY() && mouseY <= details.getY() + details.getHeight()) {

                if (details.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
