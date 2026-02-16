package net.revilodev.boundless.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.client.QuestListWidget;
import net.revilodev.boundless.quest.QuestData;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class QuestSettingsScreen extends Screen {
    private static final ResourceLocation PANEL_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_panel.png");
    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;

    private static final ResourceLocation ROW_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_widget.png");
    private static final ResourceLocation BTN_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
    private static final ResourceLocation BTN_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
    private static final ResourceLocation BTN_TEX_DISABLED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");
    private static final ResourceLocation BTN_BACK_TEX =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
    private static final ResourceLocation BTN_BACK_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_highlighted.png");

    private static final String MENU_ID_CONFIG = "01_settings_config";
    private static final String MENU_ID_EDITOR = "02_settings_editor";
    private static final List<String> HUD_POSITIONS = List.of(
            "bottom_left",
            "bottom_right",
            "top_left",
            "top_right"
    );

    private final Screen parent;
    private Page page = Page.MENU;

    private int leftX;
    private int topY;
    private int px;
    private int py;
    private int pw;
    private int ph;

    private QuestListWidget menuList;

    private ConfigRow pinnedRow;
    private ConfigRow spawnRow;
    private ConfigRow hideToggleRow;
    private EditBox disabledCategories;
    private ActionButton saveButton;
    private BackButton backButton;

    private String pinnedHudPos;
    private boolean spawnWithQuestBook;
    private boolean hideQuestBookToggle;

    public QuestSettingsScreen(Screen parent) {
        super(Component.literal("Quest Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasPermissions(2)) {
            mc.setScreen(parent);
            return;
        }

        leftX = (width - PANEL_W) / 2;
        topY = (height - PANEL_H) / 2;
        px = leftX + 10;
        py = topY + 10;
        pw = 127;
        ph = PANEL_H - 20;

        initMenu();
        initConfigWidgets();
        initNavButtons();

        setPage(Page.MENU);
    }

    private void initMenu() {
        menuList = new QuestListWidget(px, py, pw, ph, this::handleMenuClick);
        menuList.setQuests(buildMenuQuests());
        menuList.setCategory("all");
        menuList.setBypassFilters(true);
        addRenderableWidget(menuList);
    }

    private void initConfigWidgets() {
        int rowGap = 28;
        int rowY = py;

        pinnedRow = new ConfigRow(px, rowY, pw, "Pinned HUD Position",
                "Where the pinned quest HUD sits on screen.",
                () -> pinnedHudPos, this::cyclePinnedHudPosition);
        spawnRow = new ConfigRow(px, rowY + rowGap, pw, "Spawn With Quest Book",
                "Give players a quest book on first join.",
                () -> spawnWithQuestBook ? "On" : "Off",
                () -> spawnWithQuestBook = !spawnWithQuestBook);
        hideToggleRow = new ConfigRow(px, rowY + rowGap * 2, pw, "Hide Quest Book Toggle",
                "Hide the inventory quest book toggle button.",
                () -> hideQuestBookToggle ? "On" : "Off",
                () -> hideQuestBookToggle = !hideQuestBookToggle);

        int labelY = rowY + rowGap * 3 + 2;
        int boxY = labelY + font.lineHeight + 2;
        disabledCategories = new EditBox(font, px + 2, boxY, pw - 4, 18, Component.literal("Disabled categories"));
        disabledCategories.setMaxLength(512);

        addRenderableWidget(pinnedRow);
        addRenderableWidget(spawnRow);
        addRenderableWidget(hideToggleRow);
        addRenderableWidget(disabledCategories);
    }

    private void initNavButtons() {
        int btnY = py + ph - 20 - 2;
        backButton = new BackButton(px, btnY, () -> setPage(Page.MENU));
        saveButton = new ActionButton(px + pw - 68, btnY, 68, 20, Component.literal("Save"), this::saveConfig);

        addRenderableWidget(backButton);
        addRenderableWidget(saveButton);
    }

    private void handleMenuClick(QuestData.Quest q) {
        if (q == null) return;
        if (MENU_ID_CONFIG.equals(q.id)) {
            setPage(Page.CONFIG);
        } else if (MENU_ID_EDITOR.equals(q.id)) {
            Minecraft.getInstance().setScreen(new QuestEditorScreen(this));
        }
    }

    private void setPage(Page next) {
        page = next;

        boolean menu = page == Page.MENU;
        menuList.visible = menu;
        menuList.active = menu;

        boolean config = page == Page.CONFIG;
        pinnedRow.visible = config;
        pinnedRow.active = config;
        spawnRow.visible = config;
        spawnRow.active = config;
        hideToggleRow.visible = config;
        hideToggleRow.active = config;
        disabledCategories.visible = config;
        disabledCategories.active = config;
        saveButton.visible = config;
        saveButton.active = config;

        boolean showBack = page != Page.MENU;
        backButton.visible = showBack;
        backButton.active = showBack;

        if (config) refreshConfigFields();
    }

    private void refreshConfigFields() {
        pinnedHudPos = normalizeHudPos(Config.pinnedQuestHudPosition());
        spawnWithQuestBook = Config.spawnWithQuestBook();
        hideQuestBookToggle = Config.hideQuestBookToggle();
        disabledCategories.setValue(formatDisabledCategories(Config.disabledCategories()));
    }

    private void cyclePinnedHudPosition() {
        int idx = HUD_POSITIONS.indexOf(pinnedHudPos);
        int next = idx < 0 ? 0 : (idx + 1) % HUD_POSITIONS.size();
        pinnedHudPos = HUD_POSITIONS.get(next);
    }

    private void saveConfig() {
        List<String> disabled = parseDisabledCategories(disabledCategories.getValue());

        Config.DISABLED_CATEGORIES.set(new ArrayList<>(disabled));
        Config.PINNED_QUEST_HUD_POSITION.set(pinnedHudPos);
        Config.SPAWN_WITH_QUEST_BOOK.set(spawnWithQuestBook);
        Config.HIDE_QUEST_BOOK_TOGGLE.set(hideQuestBookToggle);
        Config.SPEC.save();
        QuestPanelClient.applyConfigChanges();
    }

    private List<QuestData.Quest> buildMenuQuests() {
        List<QuestData.Quest> out = new ArrayList<>();
        out.add(buildMenuQuest(MENU_ID_CONFIG, "Config", "minecraft:comparator"));
        out.add(buildMenuQuest(MENU_ID_EDITOR, "Quest Editor", "minecraft:writable_book"));
        return out;
    }

    private QuestData.Quest buildMenuQuest(String id, String name, String icon) {
        return new QuestData.Quest(
                id,
                name,
                icon,
                "",
                List.of(),
                false,
                null,
                null,
                null,
                "all",
                "",
                id
        );
    }

    private String normalizeHudPos(String raw) {
        if (raw == null) return HUD_POSITIONS.get(0);
        String lower = raw.trim().toLowerCase();
        return HUD_POSITIONS.contains(lower) ? lower : HUD_POSITIONS.get(0);
    }

    private String formatDisabledCategories(List<? extends String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(", ", list);
    }

    private List<String> parseDisabledCategories(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String[] parts = raw.split(",");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            if (part == null) continue;
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        gg.fill(0, 0, this.width, this.height, 0xA0000000);
        gg.blit(PANEL_TEX, leftX, topY, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        if (page == Page.CONFIG) {
            int labelY = py + 86;
            gg.drawString(font, "Disabled categories", px + 2, labelY, 0xFFFFFF, false);
        }

        super.render(gg, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Page {
        MENU,
        CONFIG
    }

    private static final class ConfigRow extends AbstractButton {
        private final String label;
        private final String subtitle;
        private final java.util.function.Supplier<String> value;
        private final Runnable onPress;

        public ConfigRow(int x, int y, int w, String label, String subtitle,
                         java.util.function.Supplier<String> value,
                         Runnable onPress) {
            super(x, y, w, 27, Component.empty());
            this.label = label == null ? "" : label;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.value = value;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            gg.blit(ROW_TEX, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            boolean hovered = this.isMouseOver(mouseX, mouseY);
            if (hovered) {
                gg.fill(getX() + 1, getY() + 1, getX() + this.width - 1, getY() + this.height - 1, 0x20FFFFFF);
            }

            var font = Minecraft.getInstance().font;
            String valueText = value == null ? "" : value.get();
            if (valueText == null) valueText = "";

            int labelX = getX() + 6;
            int labelY = getY() + 5;

            int valueW = font.width(valueText);
            int valueX = getX() + this.width - valueW - 6;

            String labelText = label;
            int maxLabelW = valueX - labelX - 4;
            if (maxLabelW > 0 && font.width(labelText) > maxLabelW) {
                int ellipsisW = font.width("...");
                int allowed = Math.max(0, maxLabelW - ellipsisW);
                labelText = font.plainSubstrByWidth(labelText, allowed) + "...";
            }

            gg.drawString(font, labelText, labelX, labelY, 0xFFFFFF, false);
            gg.drawString(font, valueText, valueX, labelY, 0xA0C8FF, false);

            float subScale = 0.65f;
            int subY = getY() + 15;
            int maxSubW = this.width - 12;
            int maxSubUnscaled = maxSubW > 0 ? (int) (maxSubW / subScale) : 0;
            String subText = subtitle;
            if (maxSubUnscaled > 0 && font.width(subText) > maxSubUnscaled) {
                int ellipsisW = font.width("...");
                int allowed = Math.max(0, maxSubUnscaled - ellipsisW);
                subText = font.plainSubstrByWidth(subText, allowed) + "...";
            }
            if (!subText.isBlank()) {
                drawScaledString(gg, subText, subScale, labelX, subY, 0xB0B0B0);
            }

            if (hovered) {
                gg.renderTooltip(Minecraft.getInstance().font,
                        Component.literal(label),
                        mouseX, mouseY);
            }
        }

        private void drawScaledString(GuiGraphics gg, String text, float scale, int x, int y, int color) {
            if (text == null || text.isEmpty()) return;
            gg.pose().pushPose();
            gg.pose().scale(scale, scale, 1f);
            float inv = 1f / scale;
            gg.drawString(Minecraft.getInstance().font, text, (int) (x * inv), (int) (y * inv), color, false);
            gg.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class ActionButton extends AbstractButton {
        private final Runnable onPress;

        public ActionButton(int x, int y, int w, int h, Component text, Runnable onPress) {
            super(x, y, w, h, text);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? BTN_TEX_DISABLED : (hovered ? BTN_TEX_HOVER : BTN_TEX);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            var font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(font, getMessage(), textX, textY, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class BackButton extends AbstractButton {
        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? BTN_BACK_TEX_HOVER : BTN_BACK_TEX;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
