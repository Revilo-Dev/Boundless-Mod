package net.revilodev.boundless.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.revilodev.boundless.network.BoundlessNetwork;

public class QuestBookItem extends Item {

    public QuestBookItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        // SERVER side → tell client to open screen
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BoundlessNetwork.sendOpenQuestBook(sp);
        }

        // return normally
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
