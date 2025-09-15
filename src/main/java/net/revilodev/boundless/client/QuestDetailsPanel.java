package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.ArrayList;
import java.util.List;

public final class QuestDetailsPanel extends AbstractWidget {
    private final Minecraft mc = Minecraft.getInstance();
    private QuestData.Quest quest;
    private final BackButton back;
    private final CompleteButton complete;
    private final RejectButton reject;
    private final Runnable onBack;

    public QuestDetailsPanel(int x, int y, int w, int h, Runnable onBack) {
        super(x, y, w, h, Component.empty());
        this.onBack = onBack;
        this.back = new BackButton(getX(), getY(), () -> {
            if (this.onBack != null) this.onBack.run();
        });
        this.back.visible = false;
        this.back.active = false;
        this.complete = new CompleteButton(getX(), getY(), () -> {
            if (quest != null && mc.player != null) {
                PacketDistributor.sendToServer(new BoundlessNetwork.RedeemPayload(quest.id));
                QuestTracker.clientSetStatus(quest.id, QuestTracker.Status.REDEEMED);
                if (this.onBack != null) this.onBack.run();
            }
        });
        this.complete.visible = false;
        this.complete.active = false;
        this.reject = new RejectButton(getX(), getY(), () -> {
            if (quest != null && mc.player != null && quest.optional) {
                PacketDistributor.sendToServer(new BoundlessNetwork.RejectPayload(quest.id));
                QuestTracker.clientSetStatus(quest.id, QuestTracker.Status.REJECTED);
                if (this.onBack != null) this.onBack.run();
            }
        });
        this.reject.visible = false;
        this.reject.active = false;
    }

    public AbstractButton backButton() { return back; }
    public AbstractButton completeButton() { return complete; }
    public AbstractButton rejectButton() { return reject; }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
        int cy = y + h - complete.getHeight() - 4;
        int cxCenter = x + (w - complete.getWidth()) / 2;
        back.setPosition(x + 2, cy);
        complete.setPosition(cxCenter, cy);
        reject.setPosition(x + w - reject.getWidth() - 2, cy);
    }

    public void setQuest(QuestData.Quest q) {
        this.quest = q;
    }

    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || quest == null) return;
        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int nameWidth = w - 32;
        int[] cursorY = {y + 8};

        quest.iconItem().ifPresent(item -> gg.renderItem(new ItemStack(item), x + 4, cursorY[0]));
        gg.drawWordWrap(mc.font, Component.literal(quest.name), x + 26, cursorY[0] + 2, nameWidth, 0xFFFFFF);
        cursorY[0] += mc.font.wordWrapHeight(quest.name, nameWidth) + 12;

        if (!quest.description.isBlank()) {
            gg.drawWordWrap(mc.font, Component.literal(quest.description), x + 4, cursorY[0], w - 8, 0xCFCFCF);
            cursorY[0] += mc.font.wordWrapHeight(quest.description, w - 8) + 8;
        }

        if (!quest.dependencies.isEmpty()) {
            gg.drawWordWrap(mc.font, Component.literal("Requires:"), x + 4, cursorY[0], w - 8, 0xFFD37F);
            cursorY[0] += mc.font.wordWrapHeight("Requires:", w - 8) + 2;
            for (String dep : quest.dependencies) {
                gg.drawWordWrap(mc.font, Component.literal("- " + dep), x + 10, cursorY[0], w - 16, 0xFFD37F);
                cursorY[0] += mc.font.wordWrapHeight("- " + dep, w - 16) + 2;
            }
            cursorY[0] += 2;
        }

        if (quest.completion != null && !quest.completion.targets.isEmpty() && mc.player != null) {
            for (QuestData.Target t : quest.completion.targets) {
                if (t.isItem()) {
                    String raw = t.id;
                    boolean isTagSyntax = raw.startsWith("#");
                    String key = isTagSyntax ? raw.substring(1) : raw;
                    ResourceLocation rl = ResourceLocation.parse(key);
                    Item direct = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                    boolean treatAsTag = isTagSyntax || direct == null;

                    int need = t.count;
                    int found = QuestTracker.getCountInInventory(t.id, mc.player);
                    boolean ready = found >= need;
                    int color = ready ? 0x55FF55 : 0xFF5555;
                    String prefix = "submission".equals(quest.type) ? "Submit: " : "Collect: ";

                    int lineY = cursorY[0];
                    gg.drawString(mc.font, prefix, x + 4, lineY + 4, color, false);
                    int px = x + 4 + mc.font.width(prefix) + 2;

                    Item iconItem;
                    if (treatAsTag) {
                        List<Item> tagItems = resolveTagItems(rl);
                        iconItem = tagItems.isEmpty() ? null : tagItems.get((int)((mc.level != null ? mc.level.getGameTime() : 0) / 20 % tagItems.size()));
                    } else {
                        iconItem = direct;
                    }
                    if (iconItem != null) {
                        gg.renderItem(new ItemStack(iconItem), px, lineY);
                        px += 18;
                    }

                    String progress = found + "/" + need;
                    gg.drawString(mc.font, progress, px, lineY + 4, color, false);
                    cursorY[0] += 22;
                } else if (t.isEntity()) {
                    ResourceLocation rl = ResourceLocation.parse(t.id);
                    EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
                    String eName = et == null ? rl.toString() : et.getDescription().getString();
                    int have = QuestTracker.getKillCount(mc.player, t.id);
                    int color = have >= t.count ? 0x55FF55 : 0xFF5555;
                    String p = "Kill: " + eName + " " + have + "/" + t.count;
                    gg.drawWordWrap(mc.font, Component.literal(p), x + 4, cursorY[0], w - 8, color);
                    cursorY[0] += mc.font.wordWrapHeight(p, w - 8) + 4;
                }
            }
            cursorY[0] += 2;
        }

        if (quest.rewards != null && quest.rewards.items != null && !quest.rewards.items.isEmpty()) {
            gg.drawWordWrap(mc.font, Component.literal("Reward:"), x + 4, cursorY[0], w - 8, 0xA8FFA8);
            cursorY[0] += mc.font.wordWrapHeight("Reward:", w - 8);

            int startX = x + 10;
            int cell = 18;
            int usable = Math.max(1, w - 20);
            int perRow = Math.max(1, usable / cell);
            int col = 0;
            int row = 0;

            for (QuestData.RewardEntry re : quest.rewards.items) {
                ResourceLocation rl = ResourceLocation.parse(re.item);
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null) {
                    int ix = startX + col * cell;
                    int iy = cursorY[0] + row * cell;
                    gg.renderItem(new ItemStack(item), ix, iy);
                    String countStr = "x" + Math.max(1, re.count);
                    gg.drawString(mc.font, countStr, ix + 10, iy + 10, 0xA8FFA8, false);
                    col++;
                    if (col >= perRow) {
                        col = 0;
                        row++;
                    }
                } else {
                    String fallback = "- " + re.item + " x" + Math.max(1, re.count);
                    gg.drawWordWrap(mc.font, Component.literal(fallback), x + 10, cursorY[0] + row * cell, w - 16, 0xA8FFA8);
                    row++;
                    col = 0;
                }
            }
            cursorY[0] += (row + (col > 0 ? 1 : 0)) * cell + 6;
        }

        boolean depsMet = QuestTracker.dependenciesMet(quest, mc.player);
        boolean red = QuestTracker.getStatus(quest, mc.player) == QuestTracker.Status.REDEEMED;
        boolean rej = QuestTracker.getStatus(quest, mc.player) == QuestTracker.Status.REJECTED;
        boolean done = red || rej;
        boolean ready = depsMet && !done && QuestTracker.isReady(quest, mc.player);
        complete.active = ready;
        complete.visible = !done;
        reject.setOptionalAllowed(quest.optional);
        reject.active = !done && quest.optional;
        reject.visible = !done;
    }

    private List<Item> resolveTagItems(ResourceLocation tagId) {
        List<Item> out = new ArrayList<>();
        TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagId);
        for (Item it : BuiltInRegistries.ITEM) {
            if (it.builtInRegistryHolder().is(itemTag)) out.add(it);
        }
        if (out.isEmpty()) {
            var blockTag = TagKey.create(Registries.BLOCK, tagId);
            for (Item it : BuiltInRegistries.ITEM) {
                if (it instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag)) out.add(it);
            }
        }
        return out;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private static final class BackButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_highlighted.png");
        private final Runnable onPress;
        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }
        public void onPress() { if (onPress != null) onPress.run(); }
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? TEX_HOVER : TEX_NORMAL;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class CompleteButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
        private static final ResourceLocation TEX_DISABLED =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");
        private final Runnable onPress;
        public CompleteButton(int x, int y, Runnable onPress) {
            super(x, y, 68, 20, Component.translatable("quest.boundless.complete"));
            this.onPress = onPress;
        }
        public void onPress() {
            if (onPress != null) onPress.run();
            this.active = false;
            this.visible = false;
        }
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_DISABLED : (hovered ? TEX_HOVER : TEX_NORMAL);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            var font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(font, getMessage(), textX, textY, color, false);
        }
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class RejectButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_highlighted.png");
        private static final ResourceLocation TEX_DISABLED =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_disabled.png");
        private final Runnable onPress;
        private boolean optionalAllowed;

        public RejectButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        public void setOptionalAllowed(boolean v) {
            this.optionalAllowed = v;
        }

        public void onPress() {
            if (this.active && onPress != null) onPress.run();
            this.active = false;
            this.visible = false;
        }

        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_DISABLED : (hovered ? TEX_HOVER : TEX_NORMAL);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            if (hovered && !this.active && !optionalAllowed) {
                gg.renderTooltip(Minecraft.getInstance().font, Component.literal("This quest is not optional"), mouseX, mouseY);
            }
        }

        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
