package net.revilodev.runic.runes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.registry.ModDataComponents;

@EventBusSubscriber(modid = RunicMod.MOD_ID)
public final class RuneSlotResetHandler {

    @SubscribeEvent
    public static void onPlace(GrindstoneEvent.OnPlaceItem e) {
        ItemStack top = e.getTopItem();
        ItemStack bottom = e.getBottomItem();

        boolean topHasEnchants =
                !top.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty() ||
                        !top.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();

        boolean bottomHasEnchants =
                !bottom.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty() ||
                        !bottom.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();

        if (topHasEnchants && bottom.isEmpty()) {
            ItemStack out = top.copy();
            out.remove(DataComponents.ENCHANTMENTS);
            out.remove(DataComponents.STORED_ENCHANTMENTS);
            out.set(ModDataComponents.RUNE_SLOTS_USED.get(), 0);
            e.setOutput(out);
        } else if (bottomHasEnchants && top.isEmpty()) {
            ItemStack out = bottom.copy();
            out.remove(DataComponents.ENCHANTMENTS);
            out.remove(DataComponents.STORED_ENCHANTMENTS);
            out.set(ModDataComponents.RUNE_SLOTS_USED.get(), 0);
            e.setOutput(out);
        }
    }
}
