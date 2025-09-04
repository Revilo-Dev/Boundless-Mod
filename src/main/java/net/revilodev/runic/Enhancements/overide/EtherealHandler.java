package net.revilodev.runic.Enhancements.overide;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import net.revilodev.runic.RunicMod;

@EventBusSubscriber(modid = RunicMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class EtherealHandler {
    private EtherealHandler() {}

    public static final String TAG_INFINITE_SHOT = "runic_infinity_success";

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;

        if (player.getAbilities().instabuild) return;
        if (isShotFromCrossbow(arrow)) return;

        // Vanilla marks bow+Infinity shots as CREATIVE_ONLY pickup.
        if (arrow.pickup != AbstractArrow.Pickup.CREATIVE_ONLY) return;

        // 50/50 roll
        if (player.getRandom().nextBoolean()) {
            arrow.setGlowingTag(true);
            if (arrow instanceof Arrow normal) {
                // mark with tag, spawn a blue trail (your renderer can read this)
                CompoundTag tag = normal.getPersistentData();
                tag.putBoolean(TAG_INFINITE_SHOT, true);
            }
        } else {
            // Failure: consume one arrow and allow normal pickup
            consumeOneArrow(player);
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        }
    }

    private static boolean isShotFromCrossbow(AbstractArrow arrow) {
        try {
            // Public in modern versions; reflection keeps it resilient if mapped differently
            return (boolean) AbstractArrow.class.getMethod("isShotFromCrossbow").invoke(arrow);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Consume one arrow from inventory: regular -> tipped -> spectral -> any ArrowItem (modded). */
    private static void consumeOneArrow(Player player) {
        Item[] pref = new Item[] { Items.ARROW, Items.TIPPED_ARROW, Items.SPECTRAL_ARROW };
        for (Item it : pref) {
            int idx = findFirst(player, it);
            if (idx >= 0) {
                player.getInventory().getItem(idx).shrink(1);
                return;
            }
        }
        // Fallback: any ArrowItem from other mods
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof ArrowItem) {
                s.shrink(1);
                return;
            }
        }
    }

    private static int findFirst(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(item)) return i;
        }
        return -1;
    }
}
