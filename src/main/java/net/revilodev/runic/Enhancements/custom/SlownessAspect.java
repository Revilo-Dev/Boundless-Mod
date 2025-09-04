package net.revilodev.runic.Enhancements.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record SlownessAspect() implements EnchantmentEntityEffect {
    public static final MapCodec<SlownessAspect> CODEC = MapCodec.unit(SlownessAspect::new);

    @Override
    public void apply(ServerLevel level, int enchantLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        if (!(entity instanceof LivingEntity living)) return;
        RandomSource rng = level.getRandom();

        float chance = enchantLevel >= 2 ? 0.50f : 0.25f;
        if (rng.nextFloat() >= chance) return;

        int amplifier = enchantLevel >= 2 ? 1 : 0; // Slowness I/II
        int duration = 100; // 5s
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
