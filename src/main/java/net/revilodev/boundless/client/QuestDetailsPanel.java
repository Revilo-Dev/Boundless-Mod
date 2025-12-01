package net.revilodev.boundless.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.ArrayList;
import java.util.List;


@OnlyIn(Dist.CLIENT)
public final class QuestDetailsPanel extends AbstractWidget {
    private static final int LINE_ITEM_ROW = 22;
    private static final int BOTTOM_PADDING = 28;

    private final Minecraft mc = Minecraft.getInstance();
    private QuestData.Quest quest;

    private final BackButton back;
    private final CompleteButton complete;
    private final RejectButton reject;
    private final Runnable onBack;

    private float scrollY = 0f;
    private int measuredContentHeight = 0;

    private final List<DepClickRegion> depRegions = new ArrayList<>();

    private static final class DepClickRegion {
        final int x, y, w, h;
        final String questId;
        DepClickRegion(int x, int y, int w, int h, String questId) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.questId = questId;
        }
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public QuestDetailsPanel(int x, int y, int w, int h, Runnable onBack) {
        super(x, y, w, h, Component.empty());
        this.onBack = onBack;

        this.back = new BackButton(getX(), getY(), () -> { if (this.onBack != null) this.onBack.run(); });
        this.back.visible = false;
        this.back.active = false;

        this.complete = new CompleteButton(getX(), getY(), () -> {
            if (quest != null && mc.player != null) {
                PacketDistributor.sendToServer(new BoundlessNetwork.Redeem(quest.id));
                QuestTracker.clientSetStatus(quest.id, QuestTracker.Status.REDEEMED);
                if (this.onBack != null) this.onBack.run();
            }
        });
        this.complete.visible = false;
        this.complete.active = false;

        this.reject = new RejectButton(getX(), getY(), () -> {
            if (quest != null && mc.player != null && quest.optional) {
                PacketDistributor.sendToServer(new BoundlessNetwork.Reject(quest.id));
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
        this.scrollY = 0f;
    }

    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || quest == null) return;

        depRegions.clear();
        List<Component> hoveredTooltips = new ArrayList<>();

        int x = this.getX();
        int y = this.getY();
        int w = this.width;

        int contentTop = y + 4;
        int contentBottom = complete.getY() - 6;
        int viewportH = Math.max(0, contentBottom - contentTop);

        measuredContentHeight = measureContentHeight(w);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        scrollY = Mth.clamp(scrollY, 0f, maxScroll);

        gg.enableScissor(x, contentTop, x + w, contentBottom);

        int[] curY = {contentTop + 4 - Mth.floor(scrollY)};

        int nameWidth = w - 32;
        quest.iconItem().ifPresent(item -> gg.renderItem(new ItemStack(item), x + 4, curY[0]));
        gg.drawWordWrap(mc.font, Component.literal(quest.name), x + 26, curY[0] + 2, nameWidth, 0xFFFFFF);
        curY[0] += mc.font.wordWrapHeight(quest.name, nameWidth) + 12;

        if (!quest.description.isBlank()) {
            gg.drawWordWrap(mc.font, Component.literal(quest.description), x + 4, curY[0], w - 8, 0xCFCFCF);
            curY[0] += mc.font.wordWrapHeight(quest.description, w - 8) + 8;
        }

        if (!quest.dependencies.isEmpty()) {
            gg.drawWordWrap(mc.font, Component.literal("Requires:"), x + 4, curY[0], w - 8, 0xff9f0f);
            curY[0] += mc.font.wordWrapHeight("Requires:", w - 8) + 2;

            for (String depId : quest.dependencies) {
                QuestData.Quest depQuest = QuestData.byId(depId).orElse(null);
                String depName = depQuest != null ? depQuest.name : depId;

                int lineY = curY[0];
                int textX = x + 24;
                int textW = mc.font.width(depName);
                int color = 0xFF5555;

                if (depQuest != null && mc.player != null) {
                    var status = QuestTracker.getStatus(depQuest, mc.player);
                    if (status == QuestTracker.Status.REDEEMED) color = 0x55FF55;
                }

                if (depQuest != null) depQuest.iconItem().ifPresent(icon -> gg.renderItem(new ItemStack(icon), x + 4, lineY));

                gg.drawString(mc.font, depName, textX, lineY + 4, color, false);
                gg.fill(textX, lineY + 14, textX + textW, lineY + 15, color);

                DepClickRegion region = new DepClickRegion(textX, lineY, textW, mc.font.lineHeight + 2, depId);
                depRegions.add(region);
                if (region.contains(mouseX, mouseY)) hoveredTooltips.add(Component.literal("View"));

                curY[0] += LINE_ITEM_ROW;
            }
            curY[0] += 2;
        }

        if (quest.completion != null && !quest.completion.targets.isEmpty() && mc.player != null) {
            for (QuestData.Target t : quest.completion.targets) {
                if (t.isItem()) {
                    String prefix = "submission".equals(quest.type) ? "Submit:" : "Collect:";
                    gg.drawString(mc.font, prefix, x + 4, curY[0], 0x1d9633, false);
                    curY[0] += mc.font.lineHeight + 2;

                    String raw = t.id;
                    boolean isTagSyntax = raw.startsWith("#");
                    String key = isTagSyntax ? raw.substring(1) : raw;
                    ResourceLocation rl = ResourceLocation.parse(key);
                    Item direct = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                    boolean treatAsTag = isTagSyntax || direct == null;

                    int need = t.count;
                    int found = QuestTracker.getCountInInventory(t.id, mc.player);

                    String progressKey = quest.id + ":" + t.id;
                    int permFound = QuestTracker.getPermanentItemProgress(progressKey, found, need);

                    int shown = Math.min(permFound, need);
                    boolean ready = shown >= need;
                    int color = ready ? 0x55FF55 : 0xFF5555;

                    int px = x + 4;

                    Item iconItem;
                    if (treatAsTag) {
                        List<Item> tagItems = resolveTagItems(rl);
                        iconItem = tagItems.isEmpty() ? null :
                                tagItems.get((int)((mc.level != null ? mc.level.getGameTime() : 0) / 20 % tagItems.size()));
                    } else {
                        iconItem = direct;
                    }

                    if (iconItem != null) {
                        ItemStack st = new ItemStack(iconItem);
                        gg.renderItem(st, px, curY[0]);
                        if (mouseX >= px && mouseX <= px + 16 && mouseY >= curY[0] && mouseY <= curY[0] + 16)
                            hoveredTooltips.add(st.getHoverName());
                        px += 20;
                    }

                    gg.drawString(mc.font, shown + "/" + need, px, curY[0] + 4, color, false);


                    curY[0] += LINE_ITEM_ROW;

                } else if (t.isEntity()) {
                    gg.drawString(mc.font, "Kill:", x + 4, curY[0], 0x1d9633, false);
                    curY[0] += mc.font.lineHeight + 2;

                    ResourceLocation rl = ResourceLocation.parse(t.id);
                    EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
                    String eName = et == null ? rl.toString() : et.getDescription().getString();

                    int have = QuestTracker.getKillCount(mc.player, t.id);
                    int color = have >= t.count ? 0x55FF55 : 0xFF5555;

                    Item iconItem = null;
                    if (et != null) {
                        ResourceLocation eggRl = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath() + "_spawn_egg");
                        iconItem = BuiltInRegistries.ITEM.getOptional(eggRl).orElse(Items.DIAMOND_SWORD);
                    } else iconItem = Items.DIAMOND_SWORD;

                    ItemStack icon = new ItemStack(iconItem);
                    gg.renderItem(icon, x + 4, curY[0]);

                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18)
                        hoveredTooltips.add(Component.literal(eName));

                    gg.drawString(mc.font, have + "/" + t.count, x + 24, curY[0] + 4, color, false);
                    curY[0] += LINE_ITEM_ROW;

                } else if (t.isEffect()) {
                    gg.drawString(mc.font, "Have effect:", x + 4, curY[0], 0x55FFFF, false);
                    curY[0] += mc.font.lineHeight + 2;

                    ResourceLocation rl = ResourceLocation.parse(t.id);
                    MobEffect eff = BuiltInRegistries.MOB_EFFECT.getOptional(rl).orElse(null);
                    String eName = eff == null ? rl.toString() : Component.translatable(eff.getDescriptionId()).getString();
                    boolean has = QuestTracker.hasEffect(mc.player, t.id);
                    int color = has ? 0x55FF55 : 0xFF5555;

                    ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/effects/" + rl.getPath() + ".png");
                    gg.blit(tex, x + 4, curY[0], 0, 0, 16, 16, 16, 16);
                    gg.drawString(mc.font, eName, x + 26, curY[0] + 6, color, false);

                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18)
                        hoveredTooltips.add(Component.literal(eName));

                    curY[0] += LINE_ITEM_ROW;

                } else if (t.isAdvancement()) {
                    gg.drawString(mc.font, "Achieve:", x + 4, curY[0], 0x55FFFF, false);
                    curY[0] += mc.font.lineHeight + 2;

                    ResourceLocation rl = ResourceLocation.parse(t.id);
                    ItemStack icon = new ItemStack(Items.MOJANG_BANNER_PATTERN);
                    String advName = rl.toString();

                    AdvancementHolder holder = null;

                    if (mc.getConnection() != null) {
                        holder = mc.getConnection().getAdvancements().get(rl);
                    }

                    if (holder == null && mc.hasSingleplayerServer()) {
                        var server = mc.getSingleplayerServer();
                        if (server != null) {
                            holder = server.getAdvancements().get(rl);
                        }
                    }

                    if (holder != null) {
                        var displayOpt = holder.value().display();
                        if (displayOpt.isPresent()) {
                            DisplayInfo di = displayOpt.get();
                            advName = di.getTitle().getString();
                            icon = di.getIcon();
                        }
                    }

                    boolean done = QuestTracker.hasAdvancement(mc.player, t.id);
                    int color = done ? 0x55FF55 : 0xFF5555;

                    gg.renderItem(icon, x + 4, curY[0]);
                    gg.drawString(mc.font, advName, x + 26, curY[0] + 6, color, false);

                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18)
                        hoveredTooltips.add(Component.literal(advName));

                    curY[0] += LINE_ITEM_ROW;

                } else if (t.isStat()) {
                    gg.drawString(mc.font, "Stat:", x + 4, curY[0], 0x1d9633, false);
                    curY[0] += mc.font.lineHeight + 2;

                    int have = QuestTracker.getStatCount(mc.player, t.id);
                    int color = have >= t.count ? 0x55FF55 : 0xFF5555;

                    ItemStack icon = new ItemStack(Items.PAPER);
                    gg.renderItem(icon, x + 4, curY[0]);
                    gg.drawString(mc.font, have + "/" + t.count, x + 24, curY[0] + 4, color, false);
                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18)
                        hoveredTooltips.add(Component.literal(t.id));
                    curY[0] += LINE_ITEM_ROW;
                }
            }
            curY[0] += 2;
        }

        boolean hasItemRewards = quest.rewards != null && quest.rewards.items != null && !quest.rewards.items.isEmpty();
        boolean hasCommandReward = quest.rewards != null && quest.rewards.command != null && !quest.rewards.command.isBlank();
        boolean hasExpReward = quest.rewards != null && quest.rewards.hasExp();

        boolean hasAnyReward = hasItemRewards || hasCommandReward || hasExpReward;
        if (!hasAnyReward) {
            // skip entire reward section
            gg.disableScissor();
            for (Component tip : hoveredTooltips)
                gg.renderTooltip(mc.font, tip, mouseX, mouseY);
            return;
        }




        if (hasItemRewards || hasCommandReward || hasExpReward) {
            gg.drawWordWrap(mc.font, Component.literal("Reward:"), x + 4, curY[0], w - 8, 0xA8FFA8);
            curY[0] += mc.font.wordWrapHeight("Reward:", w - 8) + 4;

            if (hasItemRewards) {
                for (QuestData.RewardEntry re : quest.rewards.items) {
                    ResourceLocation rl = ResourceLocation.parse(re.item);
                    Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                    int lineY = curY[0];
                    if (item != null) {
                        ItemStack st = new ItemStack(item, Math.max(1, re.count));
                        gg.renderItem(st, x + 4, lineY);
                        gg.drawString(mc.font, "x" + st.getCount(), x + 24, lineY + 6, 0xA8FFA8, false);
                        if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16)
                            hoveredTooltips.add(st.getHoverName());
                    } else {
                        gg.drawWordWrap(mc.font, Component.literal("- " + re.item + " x" + Math.max(1, re.count)), x + 4, lineY, w - 8, 0xA8FFA8);
                    }
                    curY[0] += LINE_ITEM_ROW;
                }
            }

            if (hasCommandReward) {
                int lineY = curY[0];
                gg.renderItem(new ItemStack(Items.COMMAND_BLOCK), x + 4, lineY);
                gg.drawWordWrap(mc.font, Component.literal(quest.rewards.command), x + 24, lineY + 4, w - 30, 0xA8FFA8);
                if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16)
                    hoveredTooltips.add(Component.literal("Command Reward"));
                curY[0] += LINE_ITEM_ROW;
            }

            if (hasExpReward) {
                int lineY = curY[0];
                gg.renderItem(new ItemStack(Items.EXPERIENCE_BOTTLE), x + 4, lineY);
                String txt = quest.rewards.expType.equals("levels") ? ("Levels: " + quest.rewards.expAmount)
                        : ("XP: " + quest.rewards.expAmount);
                gg.drawString(mc.font, txt, x + 24, lineY + 6, 0xA8FFA8, false);
                if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16)
                    hoveredTooltips.add(Component.literal(txt));
                curY[0] += LINE_ITEM_ROW;
            }


            curY[0] += 2;
        }

        gg.disableScissor();
        for (Component tip : hoveredTooltips) gg.renderTooltip(mc.font, tip, mouseX, mouseY);

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

    private int measureContentHeight(int panelWidth) {
        if (quest == null) return 0;
        int w = panelWidth;
        int y = 4;
        y += mc.font.wordWrapHeight(quest.name, w - 32) + 12;
        if (!quest.description.isBlank())
            y += mc.font.wordWrapHeight(quest.description, w - 8) + 8;
        if (!quest.dependencies.isEmpty()) {
            y += mc.font.wordWrapHeight("Requires:", w - 8) + 2;
            y += quest.dependencies.size() * LINE_ITEM_ROW + 2;
        }
        if (quest.completion != null && !quest.completion.targets.isEmpty())
            y += quest.completion.targets.size() * LINE_ITEM_ROW + 2;

        boolean hasItemRewards = quest.rewards != null && quest.rewards.items != null && !quest.rewards.items.isEmpty();
        boolean hasCommandReward = quest.rewards != null && quest.rewards.command != null && !quest.rewards.command.isBlank();
        boolean hasExpReward = quest.rewards != null && quest.rewards.hasExp();


        if (hasItemRewards || hasCommandReward || hasExpReward) {
            y += mc.font.wordWrapHeight("Reward:", w - 8) + 4;
            if (hasItemRewards) y += quest.rewards.items.size() * LINE_ITEM_ROW;
            if (hasCommandReward) y += LINE_ITEM_ROW;
            if (hasExpReward) y += LINE_ITEM_ROW;
            y += 2;
        }
        return y;
    }

    private List<Item> resolveTagItems(ResourceLocation tagId) {
        List<Item> out = new ArrayList<>();
        var itemTag = net.minecraft.tags.TagKey.create(Registries.ITEM, tagId);
        for (Item it : BuiltInRegistries.ITEM)
            if (it.builtInRegistryHolder().is(itemTag)) out.add(it);
        if (out.isEmpty()) {
            var blockTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, tagId);
            for (Item it : BuiltInRegistries.ITEM)
                if (it instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag)) out.add(it);
        }
        return out;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;
        int contentTop = this.getY() + 4;
        int contentBottom = complete.getY() - 6;
        if (mouseX < this.getX() || mouseX > this.getX() + this.width) return false;
        if (mouseY < contentTop || mouseY > contentBottom) return false;
        int viewportH = Math.max(0, contentBottom - contentTop);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        if (maxScroll <= 0) return false;
        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) { return mouseScrolled(mouseX, mouseY, deltaY); }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;
        for (DepClickRegion r : depRegions)
            if (r.contains(mouseX, mouseY)) {
                QuestData.Quest depQuest = QuestData.byId(r.questId).orElse(null);
                if (depQuest != null) { setQuest(depQuest); return true; }
            }
        return false;
    }

    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private static final class BackButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
        private static final ResourceLocation TEX_HOVER = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_highlighted.png");
        private final Runnable onPress;
        public BackButton(int x, int y, Runnable onPress) { super(x, y, 24, 20, Component.empty()); this.onPress = onPress; }
        public void onPress() { if (onPress != null) onPress.run(); }
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? TEX_HOVER : TEX_NORMAL;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class CompleteButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
        private static final ResourceLocation TEX_HOVER = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
        private static final ResourceLocation TEX_DISABLED = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");
        private final Runnable onPress;
        public CompleteButton(int x, int y, Runnable onPress) { super(x, y, 68, 20, Component.translatable("quest.boundless.complete")); this.onPress = onPress; }
        public void onPress() { if (onPress != null) onPress.run(); this.active = false; this.visible = false; }
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
        private static final ResourceLocation TEX_NORMAL = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject.png");
        private static final ResourceLocation TEX_HOVER = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_highlighted.png");
        private static final ResourceLocation TEX_DISABLED = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_disabled.png");
        private final Runnable onPress; private boolean optionalAllowed;
        public RejectButton(int x, int y, Runnable onPress) { super(x, y, 24, 20, Component.empty()); this.onPress = onPress; }
        public void setOptionalAllowed(boolean v) { this.optionalAllowed = v; }
        public void onPress() { if (this.active && onPress != null) onPress.run(); this.active = false; this.visible = false; }
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_DISABLED : (hovered ? TEX_HOVER : TEX_NORMAL);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
            if (hovered && !this.active && !optionalAllowed)
                gg.renderTooltip(Minecraft.getInstance().font, Component.literal("This quest is not optional"), mouseX, mouseY);
        }
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
