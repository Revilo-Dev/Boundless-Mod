package net.revilodev.runic.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.revilodev.runic.RunicMod;

@EventBusSubscriber(modid = RunicMod.MOD_ID)
public final class BlacklistStrippingEvents {
    private BlacklistStrippingEvents() {}

    /** Strip blacklisted enchants when a dropped item is picked up (before inventory add). */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if (EnchantBlacklist.strip(stack)) {
            itemEntity.setItem(stack);
        }
    }

    /** Strip blacklisted enchants from the player's inventory every tick (server only). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) { // run at end of tick
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                EnchantBlacklist.strip(stack);
            }
        }
    }
}
