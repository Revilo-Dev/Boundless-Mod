// src/main/java/net/revilodev/boundless/quest/QuestTracker.java
package net.revilodev.boundless.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    private static boolean SERVER_TOASTS_DISABLED = false;
    private static String ACTIVE_KEY = null;

    private QuestTracker() {}

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

    private static boolean getPermanentEffectProgress(String key, boolean hasEffect) {
        boolean prev = CLIENT_EFFECT_PROGRESS.getOrDefault(key, false);
        boolean now = prev || hasEffect;
        if (now) CLIENT_EFFECT_PROGRESS.put(key, true);
        return now;
    }

    private static String sanitize(String s) {
        return s == null ? "default" : s.replaceAll("[^a-zA-Z0-9._-]", "_");
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
        if (st == null || st == Status.INCOMPLETE || st == Status.COMPLETED) data.set(player.getUUID(), questId, null);
        else data.set(player.getUUID(), questId, st.name());
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

    public static boolean dependenciesMet(QuestData.Quest q, Player player) {
        if (q == null || q.dependencies.isEmpty()) return true;
        for (String depId : q.dependencies) {
            QuestData.Quest dep = QuestData.byId(depId).orElse(null);
            if (dep == null) return false;
            if (getStatus(dep, player) != Status.REDEEMED) return false;
        }
        return true;
    }

    public static boolean isVisible(QuestData.Quest q, Player player) {
        if (q == null || player == null) return false;
        if (Config.disabledCategories().contains(q.category)) return false;
        Status st = getStatus(q, player);
        return st != Status.REDEEMED && st != Status.REJECTED;
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
        return getStatus(questId, player) == Status.REDEEMED;
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

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;

        if (q.completion.targets != null && !q.completion.targets.isEmpty()) {
            boolean hasItemTargets = false;
            for (QuestData.Target t : q.completion.targets) {
                if (t != null && (t.isItem() || t.isSubmit())) {
                    hasItemTargets = true;
                    break;
                }
            }
            if (hasItemTargets && getStatus(q, player) == Status.COMPLETED) return true;
        }

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
                int prog = getPermanentItemProgress(key, cur, t.count);
                if (prog < t.count) return false;
                continue;
            }

            if (t.isEntity() && getKillCount(player, t.id) < t.count) return false;

            if (t.isEffect()) {
                String key = q.id + ":effect:" + t.id;
                boolean hasNow = hasEffect(player, t.id);
                boolean done = getPermanentEffectProgress(key, hasNow);
                if (!done) return false;
                continue;
            }

            if (t.isAdvancement() && !hasAdvancement(player, t.id)) return false;
            if (t.isStat() && getStatCount(player, t.id) < t.count) return false;
        }

        return true;
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

        ResourceLocation rl;
        try { rl = ResourceLocation.parse(key); } catch (Exception e) { return 0; }

        Item direct = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
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
        ResourceLocation rl;
        try { rl = ResourceLocation.parse(effectId); } catch (Exception e) { return false; }
        Holder<MobEffect> opt = BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
        return opt != null && player.hasEffect(opt);
    }

    public static boolean hasAdvancement(Player player, String advId) {
        if (player == null || advId == null || advId.isBlank()) return false;

        final ResourceLocation rl;
        try { rl = ResourceLocation.parse(advId); } catch (Exception e) { return false; }

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

    public static boolean serverRedeem(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;

        Status current = getServerStatus(player, q.id);
        if (current == Status.REDEEMED || current == Status.REJECTED) return false;

        if (q.rewards != null && q.rewards.items != null) {
            for (QuestData.RewardEntry r : q.rewards.items) {
                if (r == null || r.item == null || r.item.isBlank()) continue;
                ResourceLocation rl = ResourceLocation.parse(r.item);
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null) player.getInventory().add(new ItemStack(item, Math.max(1, r.count)));
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

        setServerStatus(player, q.id, Status.COMPLETED);
        setServerStatus(player, q.id, Status.REDEEMED);
        return true;
    }

    public static void forceCompleteWithoutRewards(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return;
        setServerStatus(player, q.id, Status.COMPLETED);
        BoundlessNetwork.sendStatus(player, q.id, Status.COMPLETED.name());
        setServerStatus(player, q.id, Status.REDEEMED);
        BoundlessNetwork.sendStatus(player, q.id, Status.REDEEMED.name());
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
        activeStateMap().put(questId, st);
        if (FMLEnvironment.dist == Dist.CLIENT && ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try { ensureClientStateLoaded(null); } catch (Throwable ignored) {}
            activeStateMap().clear();
            if (ACTIVE_KEY != null) ClientOnly.saveClientState(ACTIVE_KEY);
        }
    }

    public static void tickPlayer(Player player) {
        if (player == null || !player.level().isClientSide) return;

        ensureClientStateLoaded(player);
        QuestData.loadClient(false);

        for (QuestData.Quest q : QuestData.all()) {
            if (q == null) continue;

            Status cur = getStatus(q, player);
            if (cur == Status.REDEEMED || cur == Status.REJECTED) continue;

            boolean ready = dependenciesMet(q, player) && isReady(q, player);

            boolean hasItemTargets = false;
            if (q.completion != null && q.completion.targets != null) {
                for (QuestData.Target t : q.completion.targets) {
                    if (t != null && (t.isItem() || t.isSubmit())) {
                        hasItemTargets = true;
                        break;
                    }
                }
            }

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

            boolean hasItemTargets = false;
            if (q.completion != null && q.completion.targets != null) {
                for (QuestData.Target t : q.completion.targets) {
                    if (t != null && (t.isItem() || t.isSubmit())) {
                        hasItemTargets = true;
                        break;
                    }
                }
            }

            if (ready && cur == Status.INCOMPLETE) {
                setServerStatus(sp, q.id, Status.COMPLETED);
                BoundlessNetwork.sendStatus(sp, q.id, Status.COMPLETED.name());
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
