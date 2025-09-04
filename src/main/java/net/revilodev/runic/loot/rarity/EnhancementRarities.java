package net.revilodev.runic.loot.rarity;

import com.google.common.collect.Maps;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

public final class EnhancementRarities {
    private static final Map<ResourceLocation, EnhancementRarity> MAP = Maps.newHashMap();
    private static EnhancementRarity DEFAULT = EnhancementRarity.COMMON;

    private EnhancementRarities() {}

    public static void clear() {
        MAP.clear();
        DEFAULT = EnhancementRarity.COMMON;
    }

    public static void setDefault(EnhancementRarity rarity) {
        DEFAULT = rarity == null ? EnhancementRarity.COMMON : rarity;
    }

    public static void put(ResourceLocation id, EnhancementRarity rarity) {
        if (id != null && rarity != null) MAP.put(id, rarity);
    }

    public static EnhancementRarity get(Holder<Enchantment> holder) {
        if (holder == null) return DEFAULT;
        return holder.unwrapKey()
                .map(k -> MAP.getOrDefault(k.location(), DEFAULT))
                .orElse(DEFAULT);
    }

    public static EnhancementRarity get(ResourceLocation id) {
        return MAP.getOrDefault(id, DEFAULT);
    }

    public static void replaceAllClient(Map<ResourceLocation, EnhancementRarity> incoming,
                                        EnhancementRarity def) {
        MAP.clear();
        MAP.putAll(incoming);
        setDefault(def);
    }
    public static Map<ResourceLocation, EnhancementRarity> rawMap() { return MAP; }
    public static EnhancementRarity defaultRarity() { return DEFAULT; }

}
