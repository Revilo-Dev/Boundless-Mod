package net.revilodev.runic.runes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.revilodev.runic.RunicMod;

@EventBusSubscriber(modid = RunicMod.MOD_ID) // GAME bus is default, no 'bus =' needed
public final class RuneSlotTooltipHandler {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();
        int cap = RuneSlots.capacity(stack);
        if (cap <= 0) return;

        int used = RuneSlots.used(stack);
        int remaining = Math.max(0, cap - used);

        e.getToolTip().add(Component.empty());
        e.getToolTip().add(Component.literal("Rune Slots:").withStyle(ChatFormatting.GRAY));
        e.getToolTip().add(
                RuneSlots.bar(stack).copy()
                        .withStyle(remaining > 0 ? ChatFormatting.GRAY : ChatFormatting.RED)
        );
    }
}
