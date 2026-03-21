package net.revilodev.boundless.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.network.BoundlessNetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.revilodev.boundless.network.BoundlessNetwork.sendToastLocal;

public final class QuestTracker {

    public enum Status { INCOMPLETE, COMPLETED, REDEEMED, REJECTED }

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final Map<String, Map<String, Status>> WORLD_STATES = new HashMap<>();
    private static final Map<String, Integer> CLIENT_KILLS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_ADV_DONE = new HashMap<>();
    private static final Map<String, Integer> CLIENT_STATS = new HashMap<>();
    private static final Map<String, Integer> CLIENT_ITEM_PROGRESS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_EFFECT_PROGRESS = new HashMap<>();
    private static final Map<String, Integer> CLIENT_CLAIM_COUNTS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_SCROLL_REDEEMED = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_SCROLL_CREATED = new HashMap<>();
    private static final Map<String, ResourceLocation> RL_CACHE = new HashMap<>();
    private static final Map<String, Optional<Item>> ITEM_BY_ID_CACHE = new HashMap<>();
    private static final Map<String, Holder<MobEffect>> EFFECT_BY_ID_CACHE = new HashMap<>();

    private static boolean SERVER_TOASTS_DISABLED = false;
    private static String ACTIVE_KEY = null;
    private static volatile boolean CLIENT_IN_MULTIPLAYER = false;

    private QuestTracker() {}

    public static void setClientMultiplayer(boolean v) {
        CLIENT_IN_MULTIPLAYER = v;
    }

    private static boolean isClientMultiplayer() {
        if (FMLEnvironment.dist != Dist.CLIENT) return false;
        return ClientOnly.isClientMultiplayerFlag(CLIENT_IN_MULTIPLAYER);
    }

    public static int getPermanentItemProgress(String key, int current, int required) {
        int req = Math.max(0, required);
        int cur = Math.max(0, current);

        if (req <= 0) return 0;

        int prev = CLIENT_ITEM_PROGRESS.getOrDefault(key, 0);
        int now = Math.max(prev, Math.min(cur, req));

        if (FMLEnvironment.dist == Dist.CLIENT && prev > 0) {
            now = ClientOnly.adjustItemProgress(key, cur, req, prev, now);
        }

        if (now <= 0) {
            CLIENT_ITEM_PROGRESS.remove(key);
            return 0;
        }

        CLIENT_ITEM_PROGRESS.put(key, now);
        return now;
    }

    private static int getPermanentItemProgress(Player player, String key, int current, int required) {
        if (player instanceof ServerPlayer sp) {
            return QuestObjectiveState.get(sp.serverLevel())
                    .updateItemProgress(sp.getUUID(), key, current, required);
        }
        return getPermanentItemProgress(key, current, required);
    }

    private static boolean getPermanentEffectProgress(String key, boolean hasEffect) {
        boolean prev = CLIENT_EFFECT_PROGRESS.getOrDefault(key, false);
        boolean now = prev || hasEffect;
        if (now) CLIENT_EFFECT_PROGRESS.put(key, true);
        return now;
    }

    private static boolean getPermanentEffectProgress(Player player, String key, boolean hasEffect) {
        if (player instanceof ServerPlayer sp) {
            return QuestObjectiveState.get(sp.serverLevel())
                    .updateEffectDone(sp.getUUID(), key, hasEffect);
        }
        return getPermanentEffectProgress(key, hasEffect);
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "default";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '.'
                    || ch == '_'
                    || ch == '-') {
                out.append(ch);
            } else {
                out.append('_');
            }
        }
        return out.isEmpty() ? "default" : out.toString();
    }

    private static ResourceLocation tryParseCached(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (RL_CACHE.containsKey(raw)) return RL_CACHE.get(raw);
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        RL_CACHE.put(raw, rl);
        return rl;
    }

    private static Item resolveItemById(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        Optional<Item> cached = ITEM_BY_ID_CACHE.get(itemId);
        if (cached != null) return cached.orElse(null);
        ResourceLocation rl = tryParseCached(itemId);
        Optional<Item> resolved = rl == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(rl);
        ITEM_BY_ID_CACHE.put(itemId, resolved);
        return resolved.orElse(null);
    }

    private static String computeClientKey() {
        return ClientOnly.computeClientKey();
    }

    private static Map<String, Status> activeStateMap() {
        String key = ACTIVE_KEY;
        if (key == null) key = "default";
        return WORLD_STATES.computeIfAbsent(key, k -> new LinkedHashMap<>());
    }

    public static void forceSave() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            if (ACTIVE_KEY == null) ensureClientStateLoaded(null);
        } catch (Throwable ignored) {}
        if (ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
    }

    private static void ensureClientStateLoaded(Player player) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        String key = computeClientKey();
        if (!key.equals(ACTIVE_KEY)) {
            if (ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
            ACTIVE_KEY = key;
            ClientOnly.loadClientState(key);
        }
    }

    private static Status decodeStatus(String raw) {
        if (raw == null || raw.isBlank()) return Status.INCOMPLETE;
        try { return Status.valueOf(raw); } catch (Exception ignored) { return Status.INCOMPLETE; }
    }

    private static Status getServerStatus(ServerPlayer player, String questId) {
        String raw = QuestProgressState.get(player.serverLevel()).get(player.getUUID(), questId);
        return decodeStatus(raw);
    }

    public static void setServerStatus(ServerPlayer player, String questId, Status st) {
        QuestProgressState data = QuestProgressState.get(player.serverLevel());
        if (st == null || st == Status.INCOMPLETE) {
            data.set(player.getUUID(), questId, null);
        } else {
            data.set(player.getUUID(), questId, st.name());
        }
    }

    public static Status getStatus(QuestData.Quest q, Player player) {
        if (q == null) return Status.INCOMPLETE;
        if (player instanceof ServerPlayer sp) return getServerStatus(sp, q.id);
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return activeStateMap().getOrDefault(q.id, Status.INCOMPLETE);
    }

    public static Status getStatus(String questId, Player player) {
        if (player instanceof ServerPlayer sp) return getServerStatus(sp, questId);
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return activeStateMap().getOrDefault(questId, Status.INCOMPLETE);
    }

    public static int getClaimCount(String questId, Player player) {
        if (questId == null || questId.isBlank()) return 0;
        if (player instanceof ServerPlayer sp) {
            return QuestProgressState.get(sp.serverLevel()).getClaimCount(sp.getUUID(), questId);
        }
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return Math.max(0, CLIENT_CLAIM_COUNTS.getOrDefault(questId, 0));
    }

    public static boolean hasEverClaimed(QuestData.Quest q, Player player) {
        return q != null && hasEverClaimed(q.id, player);
    }

    public static boolean hasEverClaimed(String questId, Player player) {
        return getClaimCount(questId, player) > 0 || getStatus(questId, player) == Status.REDEEMED;
    }

    public static boolean hasRedeemedScroll(String questId, Player player) {
        if (questId == null || questId.isBlank()) return false;
        if (player instanceof ServerPlayer sp) {
            return QuestProgressState.get(sp.serverLevel()).hasRedeemedScroll(sp.getUUID(), questId);
        }
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return Boolean.TRUE.equals(CLIENT_SCROLL_REDEEMED.get(questId));
    }

    public static boolean hasCreatedScroll(String questId, Player player) {
        if (questId == null || questId.isBlank()) return false;
        if (player instanceof ServerPlayer sp) {
            return QuestProgressState.get(sp.serverLevel()).hasCreatedScroll(sp.getUUID(), questId);
        }
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        return Boolean.TRUE.equals(CLIENT_SCROLL_CREATED.get(questId));
    }

    public static boolean dependenciesMet(QuestData.Quest q, Player player) {
        if (q == null || q.dependencies.isEmpty()) return true;
        for (String depId : q.dependencies) {
            QuestData.Quest dep = QuestData.byId(depId).orElse(null);
            if (dep == null) return false;
            if (!hasEverClaimed(dep, player)) return false;
        }
        return true;
    }

    public static boolean isVisible(QuestData.Quest q, Player player) {
        if (q == null || player == null) return false;
        if (Config.disabledCategories().contains(q.category)) return false;
        if (q.hiddenUnderDependency && !q.dependencies.isEmpty() && !dependenciesMet(q, player)) return false;
        Status st = getStatus(q, player);
        if (st == Status.REJECTED) return false;
        if (st == Status.REDEEMED && !q.repeatable) return false;
        return true;
    }

    public static boolean hasAnyCompleted(Player player) {
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        QuestData.loadClient(false);
        for (QuestData.Quest q : QuestData.all()) {
            if (q == null) continue;
            if (getStatus(q, player) == Status.COMPLETED) return true;
        }
        return false;
    }

    public static boolean hasCompleted(Player player, String questId) {
        return hasEverClaimed(questId, player);
    }

    private static boolean isSubmissionQuestType(QuestData.Quest q) {
        if (q == null || q.type == null) return false;
        return "submission".equalsIgnoreCase(q.type) || "submit".equalsIgnoreCase(q.type);
    }

    private static boolean isSubmitTarget(QuestData.Quest q, QuestData.Target t) {
        if (t == null) return false;
        if (t.isSubmit()) return true;
        return isSubmissionQuestType(q) && t.isItem();
    }

    private static boolean hasItemOrSubmitTargets(QuestData.Quest q) {
        if (q == null || q.completion == null || q.completion.targets == null) return false;
        for (QuestData.Target t : q.completion.targets) {
            if (t != null && (t.isItem() || t.isSubmit() || t.isXp())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        if (getStatus(q, player) == Status.COMPLETED) return true;

        if (!dependenciesMet(q, player)) return false;

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;

            if (isSubmitTarget(q, t)) {
                int cur = getCountInInventory(t.id, player);
                if (cur < t.count) return false;
                continue;
            }

            if (t.isItem()) {
                String key = q.id + ":" + t.id;
                int cur = getCountInInventory(t.id, player);
                int prog = getPermanentItemProgress(player, key, cur, t.count);
                if (prog < t.count) return false;
                continue;
            }

            if (t.isEntity() && getKillCount(player, t.id) < t.count) return false;

            if (t.isEffect()) {
                String key = q.id + ":effect:" + t.id;
                boolean hasNow = hasEffect(player, t.id);
                boolean done = getPermanentEffectProgress(player, key, hasNow);
                if (!done) return false;
                continue;
            }

            if (t.isAdvancement() && !hasAdvancement(player, t.id)) return false;
            if (t.isStat() && getStatCount(player, t.id) < t.count) return false;
            if (t.isXp() && getXpAmount(player, t.id) < t.count) return false;
        }

        return true;
    }

    public static int getXpAmount(Player player, String xpType) {
        if (player == null) return 0;
        String mode = normalizeXpType(xpType);
        if ("levels".equals(mode)) {
            return Math.max(0, player.experienceLevel);
        }
        return Math.max(0, player.totalExperience);
    }

    public static String normalizeXpType(String xpType) {
        String mode = xpType == null ? "" : xpType.trim().toLowerCase(Locale.ROOT);
        return "levels".equals(mode) ? "levels" : "points";
    }

    public static int getStatCount(Player player, String statId) {
        if (player == null || statId == null || statId.isBlank()) return 0;
        try {
            if (player instanceof ServerPlayer sp) {
                int first = statId.indexOf(':');
                int second = statId.indexOf(':', first + 1);
                boolean typed = second > first;
                String type = typed ? statId.substring(0, first) : "custom";
                String name = typed ? statId.substring(first + 1) : statId;
                ResourceLocation rl = ResourceLocation.tryParse(name);
                if (rl == null) return 0;
                return switch (type) {
                    case "custom" -> sp.getStats().getValue(Stats.CUSTOM.get(rl));
                    case "mine_block" -> {
                        var block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
                        yield block == null ? 0 : sp.getStats().getValue(Stats.BLOCK_MINED.get(block));
                    }
                    case "use_item" -> {
                        var item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                        yield item == null ? 0 : sp.getStats().getValue(Stats.ITEM_USED.get(item));
                    }
                    case "kill_entity" -> {
                        var et = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
                        yield et == null ? 0 : sp.getStats().getValue(Stats.ENTITY_KILLED.get(et));
                    }
                    default -> 0;
                };
            }
            if (player.level().isClientSide) return CLIENT_STATS.getOrDefault(statId, 0);
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getCountInInventory(String id, Player player) {
        if (player == null || id == null || id.isBlank()) return 0;
        boolean isTagSyntax = id.startsWith("#");
        String key = isTagSyntax ? id.substring(1) : id;

        ResourceLocation rl = tryParseCached(key);
        if (rl == null) return 0;

        Item direct = resolveItemById(key);
        int found = 0;

        if (isTagSyntax || direct == null) {
            var itemTag = net.minecraft.tags.TagKey.create(Registries.ITEM, rl);
            for (ItemStack s : player.getInventory().items) if (!s.isEmpty() && s.is(itemTag)) found += s.getCount();
            if (found == 0) {
                var blockTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, rl);
                for (ItemStack s : player.getInventory().items) {
                    if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag)) {
                        found += s.getCount();
                    }
                }
            }
        } else {
            for (ItemStack s : player.getInventory().items) if (!s.isEmpty() && s.is(direct)) found += s.getCount();
        }

        return found;
    }

    public static int getKillCount(Player player, String entityId) {
        if (player == null || entityId == null || entityId.isBlank()) return 0;
        if (player instanceof ServerPlayer sp) {
            return KillCounterState.get(sp.serverLevel()).snapshotFor(player.getUUID()).getOrDefault(entityId, 0);
        }
        return CLIENT_KILLS.getOrDefault(entityId, 0);
    }

    public static boolean hasEffect(Player player, String effectId) {
        if (player == null || effectId == null || effectId.isBlank()) return false;
        Holder<MobEffect> holder = EFFECT_BY_ID_CACHE.get(effectId);
        if (!EFFECT_BY_ID_CACHE.containsKey(effectId)) {
            ResourceLocation rl = tryParseCached(effectId);
            holder = rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
            EFFECT_BY_ID_CACHE.put(effectId, holder);
        }
        return holder != null && player.hasEffect(holder);
    }

    public static boolean hasAdvancement(Player player, String advId) {
        if (player == null || advId == null || advId.isBlank()) return false;

        final ResourceLocation rl;
        rl = tryParseCached(advId);
        if (rl == null) return false;

        if (player instanceof ServerPlayer sp) return hasAdvancementServer(sp, rl);

        if (FMLEnvironment.dist == Dist.CLIENT && player.level().isClientSide) {
            try {
                Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
                Object mc = mcClass.getMethod("getInstance").invoke(null);
                Object srvObj = mcClass.getMethod("getSingleplayerServer").invoke(mc);

                if (srvObj instanceof net.minecraft.server.MinecraftServer srv) {
                    ServerPlayer sp = srv.getPlayerList().getPlayer(player.getUUID());
                    if (sp != null) return hasAdvancementServer(sp, rl);
                }

            } catch (Throwable ignored) {}

            return CLIENT_ADV_DONE.getOrDefault(rl.toString(), false);
        }

        return false;
    }

    private static boolean hasAdvancementServer(ServerPlayer sp, ResourceLocation rl) {
        AdvancementHolder holder = sp.server.getAdvancements().get(rl);
        if (holder == null) return false;

        AdvancementProgress prog = sp.getAdvancements().getOrStartProgress(holder);
        boolean done = prog.isDone();

        CLIENT_ADV_DONE.put(rl.toString(), done);
        return done;
    }

    public static boolean canCreateScroll(QuestData.Quest q, Player player) {
        return q != null && player != null && hasEverClaimed(q, player) && !hasCreatedScroll(q.id, player);
    }

    public static boolean canRestartRepeatable(QuestData.Quest q, Player player) {
        return q != null && q.repeatable && player != null && getStatus(q, player) == Status.REDEEMED;
    }

    public static boolean consumeXpTarget(ServerPlayer player, QuestData.Target target) {
        if (player == null || target == null || !target.isXp()) return false;
        int amount = Math.max(0, target.count);
        if (amount <= 0) return true;
        String mode = normalizeXpType(target.id);
        if ("levels".equals(mode)) {
            if (player.experienceLevel < amount) return false;
            player.giveExperienceLevels(-amount);
            return true;
        }

        int current = Math.max(0, player.totalExperience);
        if (current < amount) return false;
        setTotalExperience(player, current - amount);
        return true;
    }

    private static void setTotalExperience(ServerPlayer player, int totalExperience) {
        int clamped = Math.max(0, totalExperience);
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        if (clamped > 0) {
            player.giveExperiencePoints(clamped);
        }
    }

    private static void giveExpReward(ServerPlayer player, QuestData.Rewards rewards) {
        if (player == null || rewards == null || !rewards.hasExp()) return;
        int amount = Math.max(0, rewards.expAmount);
        if (amount <= 0) return;
        if ("levels".equalsIgnoreCase(rewards.expType)) {
            player.giveExperienceLevels(amount);
        } else {
            player.giveExperiencePoints(amount);
        }
    }

    private static void giveLootRewards(ServerPlayer player, QuestData.Rewards rewards) {
        if (player == null || rewards == null || !rewards.hasLootTables()) return;
        for (QuestData.LootTableReward reward : rewards.lootTables) {
            if (reward == null || reward.lootTable == null || reward.lootTable.isBlank()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(reward.lootTable);
            if (rl == null) continue;
            LootTable table;
            try {
                table = player.server.reloadableRegistries()
                        .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, rl));
            } catch (Throwable ignored) {
                table = null;
            }
            if (table == null || table == LootTable.EMPTY) continue;

            LootParams params = new LootParams.Builder(player.serverLevel())
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.GIFT);
            ObjectArrayList<ItemStack> generated = table.getRandomItems(params, player.getRandom());
            for (ItemStack stack : generated) {
                if (stack == null || stack.isEmpty()) continue;
                ItemStack copy = stack.copy();
                if (!player.getInventory().add(copy) && !copy.isEmpty()) {
                    player.drop(copy, false);
                }
            }
        }
    }

    private static void clearQuestCycle(ServerPlayer player, QuestData.Quest q) {
        if (player == null || q == null) return;
        QuestObjectiveState.get(player.serverLevel()).clearQuest(player.getUUID(), q.id);
    }

    public static void markQuestClaimed(ServerPlayer player, QuestData.Quest q) {
        if (player == null || q == null) return;
        QuestProgressState.get(player.serverLevel()).incrementClaimCount(player.getUUID(), q.id);
    }

    public static boolean serverRedeem(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;

        Status current = getServerStatus(player, q.id);
        if (current == Status.REDEEMED || current == Status.REJECTED) return false;

        if (q.rewards != null && q.rewards.items != null) {
            for (QuestData.RewardEntry r : q.rewards.items) {
                if (r == null || r.item == null || r.item.isBlank()) continue;
                ResourceLocation rl = ResourceLocation.parse(r.item);
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null) {
                    ItemStack stack = new ItemStack(item, Math.max(1, r.count));
                    if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                        player.drop(stack, false);
                    }
                }
            }
        }

        if (q.rewards != null) {
            CommandSourceStack css = player.createCommandSourceStack().withPermission(4);

            LinkedHashSet<String> toRun = new LinkedHashSet<>();

            if (q.rewards.commands != null) {
                for (QuestData.CommandReward cr : q.rewards.commands) {
                    if (cr == null || cr.command == null) continue;
                    String cmd = cr.command.trim();
                    if (cmd.isBlank()) continue;
                    if (cmd.startsWith("/")) cmd = cmd.substring(1).trim();
                    toRun.add(cmd);
                }
            }

            if (q.rewards.functions != null) {
                for (QuestData.FunctionReward fr : q.rewards.functions) {
                    if (fr == null || fr.function == null) continue;
                    String fn = fr.function.trim();
                    if (fn.isBlank()) continue;
                    if (fn.startsWith("/")) fn = fn.substring(1).trim();
                    if (fn.regionMatches(true, 0, "function", 0, "function".length())) {
                        fn = fn.substring("function".length()).trim();
                    }
                    toRun.add("function " + fn);
                }
            }

            for (String cmd : toRun) {
                try { player.server.getCommands().performPrefixedCommand(css, cmd); } catch (Throwable ignored) {}
            }
        }

        giveLootRewards(player, q.rewards);
        giveExpReward(player, q.rewards);
        markQuestClaimed(player, q);
        clearQuestCycle(player, q);
        setServerStatus(player, q.id, Status.REDEEMED);
        return true;
    }

    public static void forceCompleteWithoutRewards(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return;
        markQuestClaimed(player, q);
        clearQuestCycle(player, q);
        setServerStatus(player, q.id, Status.REDEEMED);
        BoundlessNetwork.sendStatus(player, q.id, Status.REDEEMED.name());
    }

    public static boolean restartRepeatable(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null || !q.repeatable) return false;
        if (getServerStatus(player, q.id) != Status.REDEEMED) return false;
        clearQuestCycle(player, q);
        setServerStatus(player, q.id, Status.INCOMPLETE);
        return true;
    }

    public static boolean serverReject(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;
        if (!q.optional) return false;
        setServerStatus(player, q.id, Status.REJECTED);
        return true;
    }

    public static void reset(Player player) {
        if (player instanceof ServerPlayer sp) {
            QuestProgressState.get(sp.serverLevel()).clear(sp.getUUID());
            QuestObjectiveState.get(sp.serverLevel()).clearPlayer(sp.getUUID());
            BoundlessNetwork.syncPlayer(sp);
            CLIENT_EFFECT_PROGRESS.clear();
            if (FMLEnvironment.dist == Dist.CLIENT) clientClearAll();
            return;
        }
        if (player != null && player.level().isClientSide) clientClearAll();
    }

    public static void clientSetStatus(String questId, Status st) {
        if (questId == null || st == null) return;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try { ensureClientStateLoaded(null); } catch (Throwable ignored) {}
        }
        if (st == Status.INCOMPLETE) {
            activeStateMap().remove(questId);
        } else {
            activeStateMap().put(questId, st);
        }
        if (FMLEnvironment.dist == Dist.CLIENT && ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
    }

    public static void clientSetClaimCount(String questId, int count) {
        if (questId == null || questId.isBlank()) return;
        int sanitized = Math.max(0, count);
        if (sanitized <= 0) CLIENT_CLAIM_COUNTS.remove(questId);
        else CLIENT_CLAIM_COUNTS.put(questId, sanitized);
    }

    public static void clientSetScrollRedeemed(String questId, boolean redeemed) {
        if (questId == null || questId.isBlank()) return;
        if (redeemed) CLIENT_SCROLL_REDEEMED.put(questId, true);
        else CLIENT_SCROLL_REDEEMED.remove(questId);
    }

    public static void clientSetScrollCreated(String questId, boolean created) {
        if (questId == null || questId.isBlank()) return;
        if (created) CLIENT_SCROLL_CREATED.put(questId, true);
        else CLIENT_SCROLL_CREATED.remove(questId);
    }

    public static void clientSetKill(String entityId, int count) {
        CLIENT_KILLS.put(entityId, Math.max(0, count));
    }

    public static void clientClearAll() {
        CLIENT_KILLS.clear();
        CLIENT_ADV_DONE.clear();
        CLIENT_STATS.clear();
        CLIENT_ITEM_PROGRESS.clear();
        CLIENT_EFFECT_PROGRESS.clear();
        CLIENT_CLAIM_COUNTS.clear();
        CLIENT_SCROLL_REDEEMED.clear();
        CLIENT_SCROLL_CREATED.clear();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try { ensureClientStateLoaded(null); } catch (Throwable ignored) {}
            activeStateMap().clear();
            if (ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
        }
    }

    public static void tickPlayer(Player player) {
        if (player == null || !player.level().isClientSide) return;
        if (isClientMultiplayer()) return;

        ensureClientStateLoaded(player);
        QuestData.loadClient(false);

        for (QuestData.Quest q : QuestData.all()) {
            if (q == null) continue;

            Status cur = getStatus(q, player);
            if (cur == Status.REDEEMED || cur == Status.REJECTED) continue;

            boolean ready = dependenciesMet(q, player) && isReady(q, player);
            boolean hasItemTargets = hasItemOrSubmitTargets(q);

            if (ready && cur == Status.INCOMPLETE) {
                clientSetStatus(q.id, Status.COMPLETED);
                if (!QuestTracker.serverToastsDisabled()) sendToastLocal(q.id);
                continue;
            }

            if (hasItemTargets && cur == Status.COMPLETED) continue;

            if (!ready && cur == Status.COMPLETED) clientSetStatus(q.id, Status.INCOMPLETE);
        }
    }

    public static void serverTickPlayer(ServerPlayer sp) {
        if (sp == null) return;

        QuestData.allServer(sp.server);

        for (QuestData.Quest q : QuestData.allServer(sp.server)) {
            if (q == null) continue;
            if (Config.disabledCategories().contains(q.category)) continue;

            Status cur = getServerStatus(sp, q.id);
            if (cur == Status.REDEEMED || cur == Status.REJECTED) continue;

            boolean ready = dependenciesMet(q, sp) && isReady(q, sp);
            boolean hasItemTargets = hasItemOrSubmitTargets(q);

            if (ready && cur == Status.INCOMPLETE) {
                if (Config.autoClaimQuestRewards()) {
                    BoundlessNetwork.claimQuest(sp, q);
                } else {
                    setServerStatus(sp, q.id, Status.COMPLETED);
                    BoundlessNetwork.sendStatus(sp, q.id, Status.COMPLETED.name());
                }
                continue;
            }

            if (hasItemTargets && cur == Status.COMPLETED) continue;

            if (!ready && cur == Status.COMPLETED) {
                setServerStatus(sp, q.id, Status.INCOMPLETE);
                BoundlessNetwork.sendStatus(sp, q.id, Status.INCOMPLETE.name());
            }
        }
    }

    public static boolean serverToastsDisabled() {
        return SERVER_TOASTS_DISABLED;
    }

    public static void setServerToastsDisabled(boolean v) {
        SERVER_TOASTS_DISABLED = v;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {

        private static boolean isClientMultiplayerFlag(boolean fallback) {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc == null) return fallback;
                if (mc.getConnection() == null) return false;
                return !mc.hasSingleplayerServer();
            } catch (Throwable ignored) {}
            return fallback;
        }

        private static int adjustItemProgress(String key, int current, int required, int prev, int now) {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    int idx = key.indexOf(':');
                    if (idx > 0) {
                        String questId = key.substring(0, idx);
                        if (getStatus(questId, mc.player) == Status.INCOMPLETE) return Math.min(current, required);
                    }
                }
            } catch (Throwable ignored) {}
            return now;
        }

        private static String computeClientKey() {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc == null) return "default";
                if (mc.getSingleplayerServer() != null) {
                    String name = mc.getSingleplayerServer().getWorldData().getLevelName();
                    if (name == null || name.isBlank()) name = "world";
                    return "sp_" + sanitize(name);
                }
                if (mc.getCurrentServer() != null) {
                    String ip = mc.getCurrentServer().ip;
                    if (ip == null || ip.isBlank()) ip = "multiplayer";
                    return "mp_" + sanitize(ip);
                }
            } catch (Throwable ignored) {}
            return "default";
        }

        private static Path clientSavePath(String key) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            File dir = new File(mc.gameDirectory, "config/boundless/quest_state");
            return new File(dir, key + ".json").toPath();
        }

        private static void loadClientState(String key) {
            Map<String, Status> map = WORLD_STATES.computeIfAbsent(key, k -> new LinkedHashMap<>());
            map.clear();
            try {
                Path p = clientSavePath(key);
                if (!Files.exists(p)) return;
                try (BufferedReader r = new BufferedReader(new FileReader(p.toFile()))) {
                    JsonObject obj = GSON.fromJson(r, JsonObject.class);
                    if (obj == null) return;
                    for (String qid : obj.keySet()) {
                        try {
                            Status st = Status.valueOf(obj.get(qid).getAsString());
                            map.put(qid, st);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        private static void saveClientState(String key) {
            try {
                Path p = clientSavePath(key);
                Files.createDirectories(p.getParent());
                JsonObject obj = new JsonObject();
                Map<String, Status> map = WORLD_STATES.get(key);
                if (map != null) map.forEach((qid, st) -> obj.addProperty(qid, st.name()));
                try (BufferedWriter w = new BufferedWriter(new FileWriter(p.toFile()))) {
                    GSON.toJson(obj, w);
                }
            } catch (Throwable ignored) {}
        }
    }
}
