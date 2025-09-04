package net.revilodev.runic.Enhancements.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record LightningAspect() implements EnchantmentEntityEffect {
    public static final MapCodec<LightningAspect> CODEC = MapCodec.unit(LightningAspect::new);

    @Override
    public void apply(ServerLevel pLevel, int pEnchantmentLevel, EnchantedItemInUse pItem, Entity pEntity, Vec3 pOrigin) {
        RandomSource random = pLevel.getRandom();

        if (pEnchantmentLevel == 1) {
            if (random.nextFloat() < 0.25f) {
                EntityType.LIGHTNING_BOLT.spawn(pLevel, pEntity.getOnPos(), MobSpawnType.TRIGGERED);
            }
        }

        if (pEnchantmentLevel == 2) {
            if (random.nextFloat() < 0.50f) {
                EntityType.LIGHTNING_BOLT.spawn(pLevel, pEntity.getOnPos(), MobSpawnType.TRIGGERED);
                EntityType.LIGHTNING_BOLT.spawn(pLevel, pEntity.getOnPos(), MobSpawnType.TRIGGERED);
            }
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}