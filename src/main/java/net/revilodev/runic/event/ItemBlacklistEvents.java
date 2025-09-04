package net.revilodev.runic.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import net.revilodev.runic.RunicMod;

import java.util.List;

@EventBusSubscriber(modid = RunicMod.MOD_ID)
public final class ItemBlacklistEvents {
    private ItemBlacklistEvents() {}

    /** Block picking up enchanted books/tables by removing the item entity before vanilla handles it. */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();

        if (stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.ENCHANTING_TABLE)) {
            // No cancel/result calls needed; removing the entity prevents pickup.
            itemEntity.discard();
        }
    }

    /** Disable librarian book trades by clearing Master (level 5) offers. */
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.LIBRARIAN) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            List<VillagerTrades.ItemListing> master = trades.get(5); // Master tier
            if (master != null) {
                master.clear();
            }
        }
    }
}
