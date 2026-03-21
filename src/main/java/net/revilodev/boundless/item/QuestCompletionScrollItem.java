package net.revilodev.boundless.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestProgressState;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.List;

public final class QuestCompletionScrollItem extends Item {
    private static final String QUEST_ID_KEY = "QuestId";

    public QuestCompletionScrollItem(Properties properties) {
        super(properties);
    }

    public static ItemStack withQuestId(ItemStack stack, String questId) {
        if (stack.isEmpty()) return stack;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(QUEST_ID_KEY, questId == null ? "" : questId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static String getQuestId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "";
        CompoundTag tag = customData.copyTag();
        return tag.getString(QUEST_ID_KEY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (!Config.enableQuestScrolls()) {
            sp.displayClientMessage(Component.literal("Quest scrolls are disabled.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        String questId = getQuestId(stack);
        if (questId.isBlank()) {
            sp.displayClientMessage(Component.literal("This scroll is not bound to a quest.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        QuestData.Quest quest = QuestData.byIdServer(sp.server, questId).orElse(null);
        if (quest == null) {
            sp.displayClientMessage(Component.literal("That quest no longer exists.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (QuestTracker.hasEverClaimed(quest, sp)) {
            sp.displayClientMessage(Component.literal("You have already completed this quest.").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }

        if (QuestTracker.hasRedeemedScroll(quest.id, sp)) {
            sp.displayClientMessage(Component.literal("You have already redeemed a scroll for this quest.").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!QuestTracker.dependenciesMet(quest, sp)) {
            sp.displayClientMessage(Component.literal("You have not unlocked this quest yet.").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!QuestTracker.serverRedeem(quest, sp)) {
            sp.displayClientMessage(Component.literal("That quest could not be redeemed from this scroll.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        QuestProgressState.get(sp.serverLevel()).setScrollRedeemed(sp.getUUID(), quest.id, true);
            BoundlessNetwork.sendStatus(sp, quest.id, QuestTracker.Status.REDEEMED.name());
        BoundlessNetwork.sendProgressMeta(sp, quest.id);
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String questId = getQuestId(stack);
        if (questId.isBlank()) {
            tooltipComponents.add(Component.literal("Empty quest scroll").withStyle(ChatFormatting.GRAY));
            return;
        }
        QuestData.Quest quest = QuestData.byId(questId).orElse(null);
        if (quest != null) {
            tooltipComponents.add(Component.literal(quest.name).withStyle(ChatFormatting.GOLD));
        } else {
            tooltipComponents.add(Component.literal("Missing quest").withStyle(ChatFormatting.RED));
        }
        tooltipComponents.add(Component.literal(questId).withStyle(ChatFormatting.DARK_GRAY));
    }
}
