package net.revilodev.runic.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.item.custom.RuneItem;
import net.revilodev.runic.loot.rarity.EnhancementRarities;
import net.revilodev.runic.loot.rarity.EnhancementRarity;

import java.io.BufferedReader;
import java.util.*;
import java.util.regex.Pattern;

public class RuneRarityInjector extends LootModifier {

    public static final MapCodec<RuneRarityInjector> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, RuneRarityInjector::new));

    public RuneRarityInjector(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext ctx) {
        final ResourceLocation tableId = ctx.getQueriedLootTableId();
        final Entity entity = ctx.getParamOrNull(LootContextParams.THIS_ENTITY);

        RoutesConfig routes = RoutesConfig.get();

        RoutesConfig.Group match = null;
        if (tableId != null) {
            match = routes.matchTable(tableId);
            if (match != null) RunicMod.LOGGER.info("[Runic] Routes: matched group '{}' for table {}", match.name(), tableId);
        }
        if (match == null && entity != null) {
            match = routes.matchEntity(entity);
            if (match != null) RunicMod.LOGGER.info("[Runic] Routes: matched group '{}' for entity {}", match.name(),
                    entity.getType().builtInRegistryHolder().key().location());
        }
        if (match == null) return generatedLoot;

        RandomSource rand = ctx.getRandom();
        if (rand.nextFloat() > match.baseChance) return generatedLoot;

        EnhancementRarity rarity = match.pickRarity(rand);
        if (rarity == null) return generatedLoot;

        Holder<Enchantment> chosen = pickRandomEnchantOf(rarity, rand, ctx.getLevel());
        if (chosen == null) return generatedLoot;

        // --- Centralized creation with blacklist & EMPTY guard ---
        ItemStack rune = RuneItem.createForEnchantment(new EnchantmentInstance(chosen, 1));
        if (!rune.isEmpty()) {
            generatedLoot.add(rune);
            RunicMod.LOGGER.info("[Runic] Injected rune with {} (rarity {}) into loot from {}",
                    chosen.unwrapKey().map(k -> k.location().toString()).orElse("unknown"),
                    rarity.key(),
                    tableId != null ? tableId : entity != null ? entity.getType().builtInRegistryHolder().key().location() : "unknown");
        } else {
            RunicMod.LOGGER.info("[Runic] Skipped blacklisted enchant {} for rarity {} in {}",
                    chosen.unwrapKey().map(k -> k.location().toString()).orElse("unknown"),
                    rarity.key(),
                    tableId != null ? tableId : entity != null ? entity.getType().builtInRegistryHolder().key().location() : "unknown");
        }

        return generatedLoot;
    }

    private Holder<Enchantment> pickRandomEnchantOf(EnhancementRarity target, RandomSource rand, Level level) {
        Registry<Enchantment> reg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        List<Holder<Enchantment>> pool = new ArrayList<>();
        for (Map.Entry<ResourceLocation, EnhancementRarity> e : EnhancementRarities.rawMap().entrySet()) {
            if (e.getValue() != target) continue;
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, e.getKey());
            reg.getHolder(key).ifPresent(pool::add);
        }

        if (pool.isEmpty()) return null;
        return pool.get(rand.nextInt(pool.size()));
    }

    // ===== Wildcard-aware routes loader =====
    public static final class RoutesConfig {
        private static RoutesConfig CACHED = null;

        public static RoutesConfig get() {
            if (CACHED == null) CACHED = load();
            return CACHED;
        }

        record Group(String name,
                     List<Pattern> tablePatterns,
                     List<Pattern> entityPatterns,
                     Map<EnhancementRarity, Integer> weights,
                     float baseChance) {
            EnhancementRarity pickRarity(RandomSource rand) {
                int total = weights.values().stream().mapToInt(Integer::intValue).sum();
                if (total <= 0) return null;
                int r = rand.nextInt(total);
                for (var e : weights.entrySet()) {
                    r -= e.getValue();
                    if (r < 0) return e.getKey();
                }
                return null;
            }
        }

        private final List<Group> groups;

        private RoutesConfig(List<Group> groups) {
            this.groups = groups;
        }

        Group matchTable(ResourceLocation tableId) {
            String path = tableId.toString();
            for (Group g : groups) {
                for (Pattern p : g.tablePatterns) {
                    if (p.matcher(path).matches()) return g;
                }
            }
            return null;
        }

        Group matchEntity(Entity entity) {
            String id = entity.getType().builtInRegistryHolder().key().location().toString();
            for (Group g : groups) {
                for (Pattern p : g.entityPatterns) {
                    if (p.matcher(id).matches()) return g;
                }
            }
            return null;
        }

        private static RoutesConfig load() {
            try {
                var server = ServerLifecycleHooks.getCurrentServer();
                var rm = server.getServerResources().resourceManager();
                var rl = ResourceLocation.fromNamespaceAndPath("runic", "loot/routes.json");
                var opt = rm.getResource(rl);
                if (opt.isEmpty()) return new RoutesConfig(List.of());

                var res = opt.get();
                try (BufferedReader reader = res.openAsReader()) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                    return parse(sb.toString());
                }
            } catch (Exception e) {
                RunicMod.LOGGER.warn("[Runic] Failed to load routes.json", e);
            }
            return new RoutesConfig(List.of());
        }

        private static RoutesConfig parse(String json) {
            var root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            var arr = root.getAsJsonArray("groups");
            List<Group> groups = new ArrayList<>();
            if (arr != null) {
                for (var el : arr) {
                    var g = el.getAsJsonObject();
                    String name = g.get("name").getAsString();

                    List<Pattern> tablePatterns = new ArrayList<>();
                    if (g.has("tables")) {
                        for (var t : g.getAsJsonArray("tables")) {
                            String pat = t.getAsString()
                                    .replace(".", "\\.")
                                    .replace("*", ".*");
                            tablePatterns.add(Pattern.compile(pat));
                        }
                    }

                    List<Pattern> entityPatterns = new ArrayList<>();
                    if (g.has("entities")) {
                        for (var e : g.getAsJsonArray("entities")) {
                            String pat = e.getAsString()
                                    .replace(".", "\\.")
                                    .replace("*", ".*");
                            entityPatterns.add(Pattern.compile(pat));
                        }
                    }

                    Map<EnhancementRarity, Integer> weights = new EnumMap<>(EnhancementRarity.class);
                    if (g.has("rarity_weights")) {
                        var wobj = g.getAsJsonObject("rarity_weights");
                        for (var e : wobj.entrySet()) {
                            EnhancementRarity r = EnhancementRarity.fromKey(e.getKey(), null);
                            if (r != null) weights.put(r, e.getValue().getAsInt());
                        }
                    }

                    float baseChance = g.has("base_chance") ? g.get("base_chance").getAsFloat() : 0.0f;

                    groups.add(new Group(name, tablePatterns, entityPatterns, weights, baseChance));
                }
            }
            return new RoutesConfig(groups);
        }
    }
}
