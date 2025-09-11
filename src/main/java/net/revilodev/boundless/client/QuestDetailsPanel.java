package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.quest.QuestData;

public final class QuestDetailsPanel extends AbstractWidget {
    private final Minecraft mc = Minecraft.getInstance();
    private QuestData.Quest quest;
    private final BackButton back;
    private final Runnable onBack;

    public QuestDetailsPanel(int x, int y, int w, int h, Runnable onBack) {
        super(x, y, w, h, Component.empty());
        this.onBack = onBack;
        this.back = new BackButton(getX(), getY(), () -> {
            if (this.onBack != null) this.onBack.run();
        });
        this.back.visible = false;
        this.back.active = false;
    }

    public AbstractButton backButton() { return back; }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x); this.setY(y); this.width = w; this.height = h;
        back.setPosition(x + 4, y + h - back.getHeight() - 4);
    }

    public void setQuest(QuestData.Quest q) {
        this.quest = q;
    }

    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || quest == null) return;

        int x = this.getX();
        int y = this.getY();
        int w = this.width;

        int[] cursorY = {y + 8};

        quest.iconItem().ifPresent(item -> gg.renderItem(new ItemStack(item), x + 4, cursorY[0]));
        gg.drawString(mc.font, quest.name, x + 26, cursorY[0] + 6, 0xFFFFFF, false);
        cursorY[0] += 24;

        if (!quest.description.isBlank()) {
            gg.drawWordWrap(mc.font, Component.literal(quest.description), x + 4, cursorY[0], w - 8, 0xCFCFCF);
            cursorY[0] += mc.font.wordWrapHeight(quest.description, w - 8) + 8;
        }

        if (!quest.dependencies.isEmpty()) {
            gg.drawString(mc.font, "Requires:", x + 4, cursorY[0], 0xFFD37F, false);
            cursorY[0] += 12;
            for (String dep : quest.dependencies) {
                gg.drawString(mc.font, "- " + dep, x + 10, cursorY[0], 0xFFD37F, false);
                cursorY[0] += 10;
            }
            cursorY[0] += 2;
        }

        if (quest.reward != null && quest.reward.item != null && !quest.reward.item.isBlank()) {
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(quest.reward.item)).orElse(null);
            if (item != null) {
                gg.drawString(mc.font, "Reward:", x + 4, cursorY[0], 0xA8FFA8, false);
                gg.renderItem(new ItemStack(item, Math.max(1, quest.reward.count)), x + 56, cursorY[0] - 2);
                gg.drawString(mc.font, "x" + quest.reward.count, x + 74, cursorY[0] + 2, 0xA8FFA8, false);
                cursorY[0] += 18;
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {}

    private static final class BackButton extends AbstractButton {
        private static final ResourceLocation TEX =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 20, 18, Component.empty());
            this.onPress = onPress;
        }

        public void onPress() { if (onPress != null) onPress.run(); }

        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            gg.blit(TEX, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
