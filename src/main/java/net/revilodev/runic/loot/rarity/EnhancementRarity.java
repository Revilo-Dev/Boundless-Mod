package net.revilodev.runic.loot.rarity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public enum EnhancementRarity {
    COMMON("common",     ChatFormatting.GRAY),
    UNCOMMON("uncommon", ChatFormatting.GREEN),
    RARE("rare",         ChatFormatting.DARK_AQUA),
    EPIC("epic",         ChatFormatting.GOLD),
    LEGENDARY("legendary", ChatFormatting.LIGHT_PURPLE),
    CURSED("cursed",     ChatFormatting.DARK_RED);

    private final String key;
    private final ChatFormatting color;

    EnhancementRarity(String key, ChatFormatting color) {
        this.key = key;
        this.color = color;
    }

    public String key() { return key; }
    public ChatFormatting color() { return color; }

    public Style style() {
        return Style.EMPTY.withColor(color);
    }

    public MutableComponent applyTo(Component base) {
        return base.copy().withStyle(style());
    }

    public static EnhancementRarity fromKey(String s, EnhancementRarity fallback) {
        if (s == null) return fallback;
        String n = s.trim().toLowerCase();
        for (EnhancementRarity r : values()) {
            if (r.key.equals(n)) return r;
        }
        return fallback;
    }
}
