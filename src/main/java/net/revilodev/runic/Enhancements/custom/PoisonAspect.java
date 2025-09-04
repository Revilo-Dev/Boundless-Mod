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

public record PoisonAspect() implements EnchantmentEntityEffect {
    public static final MapCodec<PoisonAspect> CODEC = MapCodec.unit(PoisonAspect::new);

    @Override
    public void apply(ServerLevel level, int enchantLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        RandomSource random = level.getRandom();

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (enchantLevel == 1) {
            if (random.nextFloat() < 0.25f) { // 25%
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); // 5s, Poison I
            }
        } else if (enchantLevel == 2) {
            if (random.nextFloat() < 0.50f) { // 50%
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)); // 5s, Poison II
            }
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}