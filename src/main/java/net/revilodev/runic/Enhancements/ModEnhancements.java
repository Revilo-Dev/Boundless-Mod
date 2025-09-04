package net.revilodev.runic.Enhancements;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.revilodev.runic.Enhancements.custom.LightningAspect;
import net.revilodev.runic.Enhancements.custom.PoisonAspect;
import net.revilodev.runic.Enhancements.custom.SlownessAspect;
import net.revilodev.runic.Enhancements.custom.WeaknessAspect;
import net.revilodev.runic.RunicMod;

public class ModEnhancements {
    public static final ResourceKey<Enchantment> LIGHTNING_ASPECT = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "lightning_aspect"));
    public static final ResourceKey<Enchantment> POISON_ASPECT = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "poison_aspect"));
    public static final ResourceKey<Enchantment> SLOWNESS_ASPECT = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "slowness_aspect"));
    public static final ResourceKey<Enchantment> WEAKNESS_ASPECT = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "weakness_aspect"));
    public static final ResourceKey<Enchantment> SWIFTSTRIKE = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "swiftstrike"));
    public static final ResourceKey<Enchantment> SWIFT = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "swift"));


    public static void bootstrap(BootstrapContext<Enchantment> context) {
        var enchantments = context.lookup(Registries.ENCHANTMENT);
        var items = context.lookup(Registries.ITEM);

        // Lightning
        register(context, LIGHTNING_ASPECT, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                        items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5, 2,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(25, 8),
                        2,
                        EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER,
                        EnchantmentTarget.VICTIM, new LightningAspect()));

        // Poison
        register(context, POISON_ASPECT, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                        items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5, 2,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(25, 8),
                        2,
                        EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER,
                        EnchantmentTarget.VICTIM, new PoisonAspect()));

        // Slowness
        register(context, SLOWNESS_ASPECT, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                        items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5, 2,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(25, 8),
                        2,
                        EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER,
                        EnchantmentTarget.VICTIM, new SlownessAspect()));

        // Weakness
        register(context, WEAKNESS_ASPECT, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                        items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5, 2,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(25, 8),
                        2,
                        EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER,
                        EnchantmentTarget.VICTIM, new WeaknessAspect()));

        // Swiftstrike (I–IV): +5% attack speed per level
        var swiftstrikeDef = Enchantment.definition(
                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                5, 4,
                Enchantment.dynamicCost(5, 8),
                Enchantment.dynamicCost(25, 8),
                2,
                EquipmentSlotGroup.MAINHAND
        );

        register(context, SWIFTSTRIKE, Enchantment.enchantment(swiftstrikeDef)
                .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.ATTRIBUTES,
                        new EnchantmentAttributeEffect(
                                ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "swiftstrike"),
                                Attributes.ATTACK_SPEED,
                                // 5% base, +5% per extra level → I:0.05, II:0.10, III:0.15, IV:0.20
                                LevelBasedValue.perLevel(0.05F, 0.05F),
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        )));
        var swiftDef = Enchantment.definition(
                items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                5,
                2,
                Enchantment.dynamicCost(10, 8),
                Enchantment.dynamicCost(30, 8),
                2,
                EquipmentSlotGroup.FEET
        );

        register(context, SWIFT,
                Enchantment.enchantment(swiftDef)
                        .withEffect(EnchantmentEffectComponents.ATTRIBUTES,
                                new EnchantmentAttributeEffect(
                                        ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "swift_speed"),
                                        Attributes.MOVEMENT_SPEED,
                                        LevelBasedValue.perLevel(0.15F, 0.10F),
                                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                )
                        )
        );


    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }
}
