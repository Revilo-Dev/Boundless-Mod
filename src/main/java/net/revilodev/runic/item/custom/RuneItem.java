package net.revilodev.runic.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.revilodev.runic.item.ModItems;

import java.util.Set;

public class RuneItem extends Item {
    private static final Set<ResourceLocation> BLACKLIST = Set.of(
            ResourceLocation.parse("minecraft:mending"),
            ResourceLocation.parse("minecraft:protection")
    );

    public RuneItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    private static boolean isBlacklisted(Holder<Enchantment> enchantHolder) {
        return enchantHolder.unwrapKey()
                .map(ResourceKey::location)
                .map(BLACKLIST::contains)
                .orElse(false);
    }

    public static ItemStack createForEnchantment(EnchantmentInstance inst) {
        if (isBlacklisted(inst.enchantment)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(ModItems.ENHANCED_RUNE.value());
        stack.enchant(inst.enchantment, inst.level);
        return stack;
    }
}
