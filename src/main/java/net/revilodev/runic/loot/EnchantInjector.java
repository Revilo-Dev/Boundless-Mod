package net.revilodev.runic.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.revilodev.runic.RunicMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnchantInjector extends LootModifier {
    public static final MapCodec<EnchantInjector> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(
                    ResourceLocation.CODEC.listOf().fieldOf("enchants").forGetter(m -> m.enchantIds)
            ).and(
                    Codec.FLOAT.fieldOf("chance").orElse(0.25f).forGetter(m -> m.chance)
            ).and(
                    Codec.INT.fieldOf("min_level").orElse(1).forGetter(m -> m.minLevel)
            ).and(
                    Codec.INT.fieldOf("max_level").orElse(2).forGetter(m -> m.maxLevel)
            ).and(
                    Codec.BOOL.fieldOf("only_vanilla").orElse(true).forGetter(m -> m.onlyVanilla)
            ).apply(inst, EnchantInjector::new)
    );

    private final List<ResourceLocation> enchantIds;
    private final float chance;
    private final int minLevel;
    private final int maxLevel;
    private final boolean onlyVanilla;

    public EnchantInjector(LootItemCondition[] conditions,
                           List<ResourceLocation> enchantIds,
                           float chance, int minLevel, int maxLevel, boolean onlyVanilla) {
        super(conditions);
        this.enchantIds = new ArrayList<>(enchantIds);
        this.chance = chance;
        this.minLevel = Math.max(1, minLevel);
        this.maxLevel = Math.max(this.minLevel, maxLevel);
        this.onlyVanilla = onlyVanilla;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generated, LootContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return generated;
        RandomSource rng = level.getRandom();

        var enchRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        ObjectArrayList<ItemStack> out = new ObjectArrayList<>(generated.size());
        for (ItemStack stack : generated) {
            // Skip enchanted books
            if (stack.is(Items.ENCHANTED_BOOK)) {
                out.add(stack);
                continue;
            }

            // Only vanilla items
            if (onlyVanilla && !"minecraft".equals(stack.getItem().builtInRegistryHolder().key().location().getNamespace())) {
                out.add(stack);
                continue;
            }

            // Read current enchants once
            ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(current);
            boolean changed = false;

            for (ResourceLocation id : enchantIds) {
                Optional<Holder.Reference<Enchantment>> opt = enchRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, id));
                if (opt.isEmpty()) continue;

                Holder<Enchantment> ench = opt.get();
                // If this enchantment can go on this item
                if (!ench.value().canEnchant(stack)) continue;

                if (rng.nextFloat() < this.chance) {
                    int rolled = rng.nextIntBetweenInclusive(this.minLevel, this.maxLevel);
                    int levelClamped = Mth.clamp(rolled, 1, ench.value().getMaxLevel());

                    if (mut.getLevel(ench) < levelClamped) {
                        mut.set(ench, levelClamped);
                        changed = true;
                        RunicMod.LOGGER.debug("[EnchantInjector] Applied {} lvl {} to {}",
                                id, levelClamped, stack.getHoverName().getString());
                    }
                }
            }

            if (changed) {
                stack.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
            }
            out.add(stack);
        }
        return out;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
