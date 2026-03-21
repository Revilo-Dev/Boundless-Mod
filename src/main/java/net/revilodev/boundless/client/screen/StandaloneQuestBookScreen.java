package net.revilodev.boundless.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.CategoryTabsWidget;
import net.revilodev.boundless.client.CategoryHeaderWidget;
import net.revilodev.boundless.client.QuestDetailsPanel;
import net.revilodev.boundless.client.QuestFilterBar;
import net.revilodev.boundless.client.QuestListWidget;
import net.revilodev.boundless.quest.QuestData;

@OnlyIn(Dist.CLIENT)
public final class StandaloneQuestBookScreen extends Screen {

    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    private static final ResourceLocation BTN_SETTINGS =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button.png");
    private static final ResourceLocation BTN_SETTINGS_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/settings_button_hovered.png");

    private int panelWidth = 147;
    private int panelHeight = 166;

    private int leftX;
    private int rightX;
    private int topY;

    private CategoryTabsWidget tabs;
    private CategoryHeaderWidget header;
    private QuestListWidget list;
    private QuestDetailsPanel details;
    private QuestFilterBar filter;
    private SettingsButton settingsButton;
    private EditBox searchBox;

    private boolean showingDetails = false;
    private String searchQuery = "";

    public StandaloneQuestBookScreen() {
        super(Component.literal("Quests"));
    }

    @Override
    protected void init() {
        if (Config.disableQuestBook()) {
            if (minecraft != null) minecraft.setScreen(null);
            return;
        }

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
            if (Config.disableCategories()) return;
            list.setCategory(id);
            showingDetails = false;
            updateVisibility();
        });
        String selectedCategory = "all";
        if (!Config.disableCategories()) {
            tabs.setCategories(QuestData.categoriesOrdered());
            selectedCategory = tabs.selectFirstCategory();
        }

        searchBox = new EditBox(font, pxLeft, py, pw, 16, Component.literal("Search quests"));
        searchBox.setHint(Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            searchQuery = value == null ? "" : value;
            if (list != null) list.setSearchQuery(searchQuery);
        });

        list = new QuestListWidget(pxLeft, py, pw, ph, q -> {
            details.setQuest(q);
            showingDetails = true;
            updateVisibility();
        });
        list.setTopInset(Config.enableQuestSearchBox() ? 18 : 0);
        list.setQuests(QuestData.all());
        list.setCategory(selectedCategory);
        list.setSearchQuery(searchQuery);

        details = new QuestDetailsPanel(pxRight, py, pw, ph, () -> {
            showingDetails = false;
            updateVisibility();
        });
        details.setBounds(pxRight, py, pw, ph);

        int filterX = pxLeft;
        filter = new QuestFilterBar(filterX, 0, () -> {
            if (minecraft == null || minecraft.player == null || !minecraft.player.hasPermissions(2)) return;
            minecraft.setScreen(new QuestSettingsScreen(this));
        });
        int filterY = topY + panelHeight - filter.getPreferredHeight() + 29;
        filter.setBounds(filterX, filterY, filter.getPreferredWidth(), filter.getPreferredHeight());

        header = new CategoryHeaderWidget(leftX, topY, panelWidth, () -> tabs == null ? "" : tabs.getSelectedName());
        header.setPanelBounds(leftX, topY, panelWidth);
        settingsButton = new SettingsButton(leftX - 22, topY + panelHeight - 20, () -> {
            if (minecraft == null || minecraft.player == null || !minecraft.player.hasPermissions(2)) return;
            minecraft.setScreen(new QuestSettingsScreen(this));
        });

        addRenderableWidget(tabs);
        addRenderableWidget(header);
        addRenderableWidget(list);
        addRenderableWidget(details);
        addRenderableWidget(details.backButton());
        addRenderableWidget(details.completeButton());
        addRenderableWidget(details.rejectButton());
        addRenderableWidget(details.scrollButton());
        addRenderableWidget(searchBox);
        addRenderableWidget(filter);
        addRenderableWidget(settingsButton);

        updateVisibility();
    }

    private void updateVisibility() {
        list.visible = true;
        list.active = true;

        if (searchBox != null) {
            boolean showSearch = !showingDetails && Config.enableQuestSearchBox();
            searchBox.visible = showSearch;
            searchBox.active = showSearch;
        }

        details.visible = showingDetails;
        details.active = showingDetails;

        details.backButton().visible = showingDetails;
        details.backButton().active = showingDetails;

        details.completeButton().visible = showingDetails;
        details.completeButton().active = showingDetails;

        details.rejectButton().visible = showingDetails;
        details.rejectButton().active = showingDetails;

        details.scrollButton().visible = showingDetails;
        details.scrollButton().active = showingDetails;

        boolean showTabs = !Config.disableCategories();
        tabs.visible = showTabs;
        tabs.active = showTabs;

        header.visible = true;
        header.active = false;

        if (header != null) {
            boolean showHeader = !Config.hideCategoryHeader() && !Config.disableCategories();
            header.visible = showHeader;
        }

        if (filter != null) {
            boolean showFilters = !Config.hideFilters();
            filter.visible = showFilters;
            filter.active = showFilters;
        }
        if (settingsButton != null) {
            boolean canAccessSettings = minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
            boolean showSettings = Config.hideFilters() && canAccessSettings;
            settingsButton.visible = showSettings;
            settingsButton.active = showSettings;
        }
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (Config.disableQuestBook()) {
            if (minecraft != null) minecraft.setScreen(null);
            return;
        }
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);
        gg.blit(PANEL_TEX, rightX, topY, 0, 0, panelWidth, panelHeight, panelWidth, panelHeight);
        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean overSearch = searchBox != null && searchBox.visible
                && mouseX >= searchBox.getX() && mouseX <= searchBox.getX() + searchBox.getWidth()
                && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight();
        if (list.visible && list.active) {
            if (!overSearch && mouseX >= list.getX() && mouseX <= list.getX() + list.getWidth()
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

    private static final class SettingsButton extends AbstractButton {
        private final Runnable onPress;

        SettingsButton(int x, int y, Runnable onPress) {
            super(x, y, 20, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            ResourceLocation tex = isMouseOver(mouseX, mouseY) ? BTN_SETTINGS_HOVER : BTN_SETTINGS;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }
}
