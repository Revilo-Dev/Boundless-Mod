package net.revilodev.runic.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.Holder;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.item.ModItems;

public final class RunicClientModels {
    private RunicClientModels() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, path);
    }

    /** Called from RunicMod.ClientModEvents#onClientSetup */
    public static void init() {
        RunicMod.LOGGER.info("[Runic] Registering rune_model predicate for {}", ModItems.ENHANCED_RUNE.getId());
        ItemProperties.register(ModItems.ENHANCED_RUNE.get(), id("rune_model"), RunicClientModels::runePredicate);
    }

    private static float runePredicate(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        if (stack.isEmpty()) return 0.0F;

        // Get stored or direct enchantments
        ItemEnchantments enchants = stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) {
            enchants = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        if (enchants.isEmpty()) return 0.0F;

        // Take the first enchantment
        for (var entry : enchants.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            String path = ench.unwrapKey().map(k -> k.location().getPath()).orElse("");
            return mapEnchantToIndex(path);
        }

        return 0.0F;
    }

    private static float mapEnchantToIndex(String path) {
        return switch (path) {
            case "protection" -> 1.0F;
            case "fire_protection" -> 2.0F;
            case "blast_protection" -> 3.0F;
            case "projectile_protection" -> 4.0F;
            case "thorns" -> 5.0F;
            case "respiration" -> 6.0F;
            case "aqua_affinity" -> 7.0F;
            case "depth_strider" -> 8.0F;
            case "frost_walker" -> 9.0F;
            case "soul_speed" -> 10.0F;
            case "feather_falling" -> 11.0F;

            case "sharpness" -> 12.0F;
            case "smite" -> 13.0F;
            case "bane_of_arthropods" -> 14.0F;
            case "knockback" -> 15.0F;
            case "fire_aspect" -> 16.0F;
            case "looting" -> 17.0F;
            case "sweeping_edge" -> 18.0F;

            case "lightning_aspect" -> 19.0F;
            case "poison_aspect" -> 20.0F;
            case "slowness_aspect" -> 21.0F;
            case "weakness_aspect" -> 22.0F;
            case "swiftstrike" -> 23.0F;

            case "efficiency" -> 24.0F;
            case "silk_touch" -> 25.0F;
            case "fortune" -> 26.0F;
            case "unbreaking" -> 27.0F;
            case "mending" -> 28.0F;
            case "vanishing" -> 29.0F;
            case "binding" -> 30.0F;

            case "power" -> 31.0F;
            case "punch" -> 32.0F;
            case "flame" -> 33.0F;
            case "infinity" -> 34.0F;
            case "multishot" -> 35.0F;
            case "piercing" -> 36.0F;
            case "quick_charge" -> 37.0F;

            case "impaling" -> 38.0F;
            case "riptide" -> 39.0F;
            case "loyalty" -> 40.0F;
            case "channeling" -> 41.0F;

            case "luck_of_the_sea" -> 42.0F;
            case "lure" -> 43.0F;

            case "breach" -> 44.0F;
            case "density" -> 45.0F;
            case "wind_burst" -> 46.0F;
            case "swift_sneak" -> 47.0F;

            case "swift" -> 48.0F;
            case "smelting" -> 49.0F;

            default -> 0.0F;
        };
    }
}
