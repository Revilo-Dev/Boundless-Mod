package net.revilodev.runic.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.revilodev.runic.RunicMod;

@EventBusSubscriber(modid = RunicMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT) // ok to keep; deprecation is harmless for now
public final class RunicClientModels {

    private RunicClientModels() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, path);
    }
    private static ResourceLocation rl(String s) { return ResourceLocation.parse(s); } // <- ensure this exists

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional e) {
        final String[] models = new String[]{
                // ARMOR
                "item/rune/minecraft/protection","item/rune/minecraft/fire_protection","item/rune/minecraft/blast_protection",
                "item/rune/minecraft/projectile_protection","item/rune/minecraft/thorns","item/rune/minecraft/respiration",
                "item/rune/minecraft/aqua_affinity","item/rune/minecraft/depth_strider","item/rune/minecraft/frost_walker",
                "item/rune/minecraft/soul_speed","item/rune/minecraft/feather_falling",
                // WEAPON
                "item/rune/minecraft/sharpness","item/rune/minecraft/smite","item/rune/minecraft/bane_of_arthropods",
                "item/rune/minecraft/knockback","item/rune/minecraft/fire_aspect","item/rune/minecraft/looting",
                "item/rune/minecraft/sweeping_edge",
                // CUSTOM
                "item/rune/runic/lightning_aspect","item/rune/runic/poison_aspect","item/rune/runic/slowness_aspect",
                "item/rune/runic/weakness_aspect","item/rune/runic/swift_strike","item/rune/runic/swift",
                // TOOL
                "item/rune/minecraft/efficiency","item/rune/minecraft/silk_touch","item/rune/minecraft/fortune",
                "item/rune/minecraft/unbreaking","item/rune/minecraft/mending","item/rune/minecraft/vanishing_curse",
                "item/rune/minecraft/binding_curse",
                // BOW/CROSSBOW
                "item/rune/minecraft/power","item/rune/minecraft/punch","item/rune/minecraft/flame","item/rune/minecraft/infinity",
                "item/rune/minecraft/multishot","item/rune/minecraft/piercing","item/rune/minecraft/quick_charge",
                // TRIDENT
                "item/rune/minecraft/impaling","item/rune/minecraft/riptide","item/rune/minecraft/loyalty","item/rune/minecraft/channeling",
                // FISHING
                "item/rune/minecraft/luck_of_the_sea","item/rune/minecraft/lure",
                // 1.21+
                "item/rune/minecraft/breach","item/rune/minecraft/density","item/rune/minecraft/wind_burst","item/rune/minecraft/swift_sneak",
                // CUSTOM utility
                "item/rune/runic/smelting"
        };
        for (String path : models) {
            e.register(ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath("runic", path)));
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            // Lookup the item directly from the frozen registry — no .get() / .value() needed
            ResourceLocation ENHANCED_RUNE_ID = rl("runic:enhanced_rune");
            Item enhancedRune = BuiltInRegistries.ITEM.get(ENHANCED_RUNE_ID);
            ItemProperties.register(enhancedRune, id("rune_model"), RunicClientModels::runePredicate);
        });
    }

    private static float runePredicate(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        if (level == null) return 0.0F;

        Registry<Enchantment> reg = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

        int idx = 1;

        // ----- ARMOR -----
        if (has(reg.getHolderOrThrow(Enchantments.PROTECTION), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.FIRE_PROTECTION), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.BLAST_PROTECTION), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.PROJECTILE_PROTECTION), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.THORNS), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.RESPIRATION), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.AQUA_AFFINITY), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.DEPTH_STRIDER), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.FROST_WALKER), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.SOUL_SPEED), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.FEATHER_FALLING), stack)) return idx; idx++;

        // ----- WEAPON -----
        if (has(reg.getHolderOrThrow(Enchantments.SHARPNESS), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.SMITE), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.BANE_OF_ARTHROPODS), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.KNOCKBACK), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.FIRE_ASPECT), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.LOOTING), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.SWEEPING_EDGE), stack)) return idx; idx++;

        // ----- CUSTOM “ASPECTS” -----
        if (has(reg, rl("runic:lightning_aspect"), stack)) return idx; idx++;
        if (has(reg, rl("runic:poison_aspect"), stack)) return idx; idx++;
        if (has(reg, rl("runic:slowness_aspect"), stack)) return idx; idx++;
        if (has(reg, rl("runic:weakness_aspect"), stack)) return idx; idx++;
        if (has(reg, rl("runic:swiftstrike"), stack)) return idx; idx++;

        // ----- TOOLS -----
        if (has(reg.getHolderOrThrow(Enchantments.EFFICIENCY), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.SILK_TOUCH), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.FORTUNE), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.UNBREAKING), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.MENDING), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.VANISHING_CURSE), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.BINDING_CURSE), stack)) return idx; idx++;

        // ----- BOW / CROSSBOW -----
        if (has(reg.getHolderOrThrow(Enchantments.POWER), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.PUNCH), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.FLAME), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.INFINITY), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.MULTISHOT), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.PIERCING), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.QUICK_CHARGE), stack)) return idx; idx++;

        // ----- TRIDENT -----
        if (has(reg.getHolderOrThrow(Enchantments.IMPALING), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.RIPTIDE), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.LOYALTY), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.CHANNELING), stack)) return idx; idx++;

        // ----- FISHING -----
        if (has(reg.getHolderOrThrow(Enchantments.LUCK_OF_THE_SEA), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.LURE), stack)) return idx; idx++;

        // ----- 1.21+ VANILLA -----
        if (has(reg.getHolderOrThrow(Enchantments.BREACH), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.DENSITY), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.WIND_BURST), stack)) return idx; idx++;
        if (has(reg.getHolderOrThrow(Enchantments.SWIFT_SNEAK), stack)) return idx; idx++;
        if (has(reg, rl("runic:swift"), stack)) return idx; idx++;

        // ----- CUSTOM UTILITY -----
        if (has(reg, rl("runic:smelting"), stack)) return idx;

        return 0.0F;
    }

    private static boolean has(Holder<Enchantment> ench, ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ench, stack) > 0;
    }

    private static boolean has(Registry<Enchantment> reg, ResourceLocation id, ItemStack stack) {
        ResourceKey<Enchantment> key = ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, id);
        return reg.getHolder(key).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack) > 0).orElse(false);
    }
}
