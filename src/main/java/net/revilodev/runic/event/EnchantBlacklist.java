// file: net/revilodev/runic/enchant/EnchantBlacklist.java
package net.revilodev.runic.event;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Set;

public final class EnchantBlacklist {
    private EnchantBlacklist() {}


    private static final Set<ResourceLocation> IDS = Set.of(
            ResourceLocation.parse("minecraft:mending"),
            ResourceLocation.parse("minecraft:protection"),
            ResourceLocation.parse("minecraft:bane_of_arthropods")
    );

    public static boolean isBlacklisted(Holder<Enchantment> h) {
        return h.unwrapKey().map(ResourceKey::location).map(IDS::contains).orElse(false);
    }

    /** Remove blacklisted enchants (normal + stored). @return true if changed */
    public static boolean strip(ItemStack stack) {
        boolean changed = false;

        // 1) normal enchants on gear
        ItemEnchantments cur = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!cur.isEmpty()) {
            ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(cur);
            cur.entrySet().forEach(e -> { if (isBlacklisted(e.getKey())) mut.set(e.getKey(), 0); });
            ItemEnchantments cleaned = mut.toImmutable();
            if (!cleaned.equals(cur)) { stack.set(DataComponents.ENCHANTMENTS, cleaned); changed = true; }
        }

        // 2) stored enchants (enchanted books)
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!stored.isEmpty()) {
            ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(stored);
            stored.entrySet().forEach(e -> { if (isBlacklisted(e.getKey())) mut.set(e.getKey(), 0); });
            ItemEnchantments cleaned = mut.toImmutable();
            if (!cleaned.equals(stored)) { stack.set(DataComponents.STORED_ENCHANTMENTS, cleaned); changed = true; }
        }

        return changed;
    }
}
