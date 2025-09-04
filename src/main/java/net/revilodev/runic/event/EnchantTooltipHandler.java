package net.revilodev.runic.event;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.revilodev.runic.RunicMod;
import net.revilodev.runic.loot.rarity.EnhancementRarities;
import net.revilodev.runic.loot.rarity.EnhancementRarity;

@EventBusSubscriber(modid = RunicMod.MOD_ID, value = Dist.CLIENT)
public class EnchantTooltipHandler {

    // Use a plain ResourceLocation for the rune ID (no RegistryObject / ResourceKey needed)
    private static final ResourceLocation ENHANCED_RUNE_ID =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "enhanced_rune");
    // If your mappings don’t have fromNamespaceAndPath(..), use:
    // private static final ResourceLocation ENHANCED_RUNE_ID =
    //         ResourceLocation.parse(RunicMod.MOD_ID + ":enhanced_rune");

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // Determine if this stack is the Enhanced Rune item by comparing registry ID
        boolean isRune = stack.getItemHolder()
                .unwrapKey()
                .map(k -> k.location().equals(ENHANCED_RUNE_ID))
                .orElse(false);

        ItemEnchantments live   = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

        boolean recolorGear = !live.isEmpty();
        boolean recolorRune = isRune && (!live.isEmpty() || !stored.isEmpty());
        if (!recolorGear && !recolorRune) return;

        var vanillaStrings = new java.util.ArrayList<Component>();
        var newLines       = new java.util.ArrayList<Component>();

        if (recolorGear) collectRebuilt(live, vanillaStrings, newLines, isRune);
        if (recolorRune && !stored.isEmpty()) collectRebuilt(stored, vanillaStrings, newLines, isRune);

        if (vanillaStrings.isEmpty()) return;

        var tooltip = event.getToolTip();

        // Remove vanilla enchant lines
        int firstIdx = -1;
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            String s = tooltip.get(i).getString();
            for (Component v : vanillaStrings) {
                if (s.equals(v.getString())) {
                    firstIdx = i;
                    tooltip.remove(i);
                    break;
                }
            }
        }

        // Insert our rebuilt lines
        int insertAt = firstIdx;
        if (insertAt != -1) {
            tooltip.addAll(insertAt, newLines);
            insertAt += newLines.size();
        }

        // Enhanced Rune: add Shift details section (descriptions from lang)
        if (isRune && (recolorGear || !stored.isEmpty())) {
            appendRuneShiftDescriptions(tooltip, insertAt, live, stored);
        }
    }

    private static void collectRebuilt(ItemEnchantments enchants,
                                       java.util.List<Component> vanillaOut,
                                       java.util.List<Component> rebuiltOut,
                                       boolean isRune) {
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchants.entrySet()) {
            Holder<Enchantment> holder = e.getKey();
            int level = e.getIntValue();

            Component vanilla = holder.value().getFullname(holder, level);
            vanillaOut.add(vanilla);

            EnhancementRarity rarity = EnhancementRarities.get(holder);

            if (isRune) {
                // Rune item → enchant name always neutral gray
                Component grayLine = Component.literal(vanilla.getString())
                        .withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false));
                rebuiltOut.add(grayLine);

                // Then add rarity line in color
                String key = rarity.key();
                String rarityName = key.substring(0, 1).toUpperCase() + key.substring(1);
                Component rarityLine = Component.literal(rarityName)
                        .withStyle(st -> st.withColor(rarity.color()).withItalic(false));
                rebuiltOut.add(rarityLine);
            } else {
                // Normal gear → enchant name colored by rarity
                Component colored = Component.literal(vanilla.getString())
                        .withStyle(st -> st.withColor(rarity.color()).withItalic(false));
                rebuiltOut.add(colored);
            }
        }
    }

    private static void appendRuneShiftDescriptions(java.util.List<Component> tooltip,
                                                    int insertAt,
                                                    ItemEnchantments... sets) {
        // Collect unique enchant keys across provided sets (live + stored)
        java.util.LinkedHashSet<ResourceKey<Enchantment>> keys = new java.util.LinkedHashSet<>();
        for (ItemEnchantments set : sets) {
            for (Object2IntMap.Entry<Holder<Enchantment>> e : set.entrySet()) {
                e.getKey().unwrapKey().ifPresent(keys::add);
            }
        }
        if (keys.isEmpty()) return;

        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();

        if (insertAt >= 0 && insertAt <= tooltip.size()) {
            tooltip.add(insertAt, Component.empty());
            insertAt++;
        } else {
            tooltip.add(Component.empty());
            insertAt = tooltip.size();
        }

        if (!shift) {
            tooltip.add(insertAt, Component.literal("Hold \u00A7eShift\u00A7r for details")
                    .withStyle(Style.EMPTY.withColor(0x777777).withItalic(false)));
            return;
        }

        tooltip.add(insertAt++, Component.literal("Effects")
                .withStyle(Style.EMPTY.withColor(0xBBBBBB).withItalic(false)));

        for (ResourceKey<Enchantment> key : keys) {
            ResourceLocation loc = key.location();
            String langKey = "tooltip.runic." + loc.getPath();

            Component line = Component.translatable(langKey)
                    .withStyle(Style.EMPTY.withItalic(false));
            tooltip.add(insertAt++, line);
        }
    }
}
