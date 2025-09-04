package net.revilodev.runic.Enhancements;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.revilodev.runic.Enhancements.custom.LightningAspect;
import net.revilodev.runic.Enhancements.custom.PoisonAspect;
import net.revilodev.runic.Enhancements.custom.SlownessAspect;
import net.revilodev.runic.Enhancements.custom.WeaknessAspect;
import net.revilodev.runic.RunicMod;

public final class ModEnhancementEffects {

    // Registry type is MapCodec<? extends EnchantmentEntityEffect>
    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> ENTITY_ENCHANTMENT_EFFECTS =
            DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, RunicMod.MOD_ID);

    // NeoForge uses DeferredHolder for generic registries (no .get() needed unless you want the value)
    public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<? extends EnchantmentEntityEffect>> LIGHTNING_ASPECT =
            ENTITY_ENCHANTMENT_EFFECTS.register("lightning_aspect", () -> LightningAspect.CODEC);

    public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<? extends EnchantmentEntityEffect>> POISON_ASPECT =
            ENTITY_ENCHANTMENT_EFFECTS.register("poison_aspect", () -> PoisonAspect.CODEC);

    public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<? extends EnchantmentEntityEffect>> SLOWNESS_ASPECT =
            ENTITY_ENCHANTMENT_EFFECTS.register("slowness_aspect", () -> SlownessAspect.CODEC);

    public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<? extends EnchantmentEntityEffect>> WEAKNESS_ASPECT =
            ENTITY_ENCHANTMENT_EFFECTS.register("weakness_aspect", () -> WeaknessAspect.CODEC);

    public static void register(IEventBus modBus) {
        ENTITY_ENCHANTMENT_EFFECTS.register(modBus);
    }
}
