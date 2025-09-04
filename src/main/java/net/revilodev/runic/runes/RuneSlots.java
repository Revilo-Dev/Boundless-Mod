package net.revilodev.runic.runes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.revilodev.runic.registry.ModDataComponents;

public final class RuneSlots {

    public static int capacity(ItemStack stack) {
        Item item = stack.getItem();
        return RuneSlotCapacityData.capacity(item);
    }

    public static int used(ItemStack stack) {
        Integer v = stack.get(ModDataComponents.RUNE_SLOTS_USED.get());
        return v == null ? 0 : Math.max(0, v);
    }

    public static int remaining(ItemStack stack) {
        int cap = capacity(stack);
        return Math.max(0, cap - used(stack));
    }

    public static boolean tryConsumeSlot(ItemStack stack) {
        int cap = capacity(stack);
        if (cap <= 0) return false;
        int u = used(stack);
        if (u >= cap) return false;
        stack.set(ModDataComponents.RUNE_SLOTS_USED.get(), u + 1);
        return true;
    }

    public static void refundOne(ItemStack stack) {
        int u = used(stack);
        if (u > 0) stack.set(ModDataComponents.RUNE_SLOTS_USED.get(), u - 1);
    }

    public static Component bar(ItemStack stack) {
        int cap = capacity(stack);
        int u = used(stack);
        if (cap <= 0) return Component.literal("No rune slots").withStyle(ChatFormatting.DARK_GRAY);

        int rem = Math.max(0, cap - u);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < u; i++) sb.append('⬤');
        for (int i = 0; i < rem; i++) sb.append('◯');
        return Component.literal(sb.toString()).withStyle(ChatFormatting.AQUA);
    }

    private RuneSlots() {}
}
