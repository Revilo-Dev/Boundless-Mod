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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.revilodev.boundless.network.BoundlessNetwork.sendToastLocal;

public final class QuestTracker {

    public enum Status { INCOMPLETE, COMPLETED, REDEEMED, REJECTED }

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final Map<String, Map<String, Status>> WORLD_STATES = new HashMap<>();
    private static final Map<String, Integer> CLIENT_KILLS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_ADV_DONE = new HashMap<>();
    private static final Map<String, Integer> CLIENT_STATS = new HashMap<>();
    private static final Map<String, Integer> CLIENT_ITEM_PROGRESS = new HashMap<>();
    private static boolean SERVER_TOASTS_DISABLED = false;



    private static String ACTIVE_KEY = null;

    private QuestTracker() {}

    public static int getPermanentItemProgress(String key, int current, int required) {
        int prev = CLIENT_ITEM_PROGRESS.getOrDefault(key, 0);
        int now = Math.max(prev, Math.min(current, required));

        if (FMLEnvironment.dist == Dist.CLIENT && prev > 0) {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    int idx = key.indexOf(':');
                    if (idx > 0) {
                        String questId = key.substring(0, idx);
                        if (getStatus(questId, mc.player) == Status.INCOMPLETE) {
                            now = Math.min(current, required);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (now <= 0) {
            CLIENT_ITEM_PROGRESS.remove(key);
            return 0;
        }

        CLIENT_ITEM_PROGRESS.put(key, now);
        return now;
    }



    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String computeClientKey() {
        try {
            if (FMLEnvironment.dist != Dist.CLIENT) return "server";
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

    private static Map<String, Status> activeStateMap() {
        String key = ACTIVE_KEY;
        if (key == null) key = "default";
        return WORLD_STATES.computeIfAbsent(key, k -> new LinkedHashMap<>());
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

    public static void forceSave() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            if (ACTIVE_KEY == null) {
                ensureClientStateLoaded(null);
            }
        } catch (Throwable ignored) {}
        if (ACTIVE_KEY != null) saveClientState(ACTIVE_KEY);
    }

    private static void ensureClientStateLoaded(Player player) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        String key = computeClientKey();
        if (!key.equals(ACTIVE_KEY)) {
            if (ACTIVE_KEY != null) saveClientState(ACTIVE_KEY);
            ACTIVE_KEY = key;
            loadClientState(key);
        }
    }

    private static Status decodeStatus(String raw) {
        if (raw == null || raw.isBlank()) return Status.INCOMPLETE;
        try {
            return Status.valueOf(raw);
        } catch (Exception ignored) {
            return Status.INCOMPLETE;
        }
    }

    private static Status getServerStatus(ServerPlayer player, String questId) {
        String raw = QuestProgressState.get(player.serverLevel()).get(player.getUUID(), questId);
        return decodeStatus(raw);
    }

    public static void setServerStatus(ServerPlayer player, String questId, Status st) {
        QuestProgressState data = QuestProgressState.get(player.serverLevel());
        if (st == null || st == Status.INCOMPLETE || st == Status.COMPLETED) {
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

    public static boolean dependenciesMet(QuestData.Quest q, Player player) {
        if (q == null || q.dependencies.isEmpty()) return true;
        for (String depId : q.dependencies) {
            var dep = QuestData.byId(depId).orElse(null);
            if (dep == null) return false;
            if (getStatus(dep, player) != Status.REDEEMED) return false;
        }
        return true;
    }

    public static boolean isVisible(QuestData.Quest q, Player player) {
        if (q == null || player == null) return false;
        if (Config.disabledCategories().contains(q.category)) return false;
        Status st = getStatus(q, player);
        if (st == Status.REDEEMED || st == Status.REJECTED) return false;
        return true;
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;

        // Collection quests: once they hit COMPLETED, they stay "ready" forever,
        // regardless of whether the items are still in the inventory.
        if (q.completion.targets != null && !q.completion.targets.isEmpty()) {
            boolean hasItemTargets = false;
            for (QuestData.Target t : q.completion.targets) {
                if (t.isItem()) {
                    hasItemTargets = true;
                    break;
                }
            }
            if (hasItemTargets && getStatus(q, player) == Status.COMPLETED) {
                return true;
            }
        }

        if (!dependenciesMet(q, player)) return false;

        for (QuestData.Target t : q.completion.targets) {
            if (t.isItem()) {
                String key = q.id + ":" + t.id;
                int cur = getCountInInventory(t.id, player);
                int prog = getPermanentItemProgress(key, cur, t.count);
                if (prog < t.count) return false;
                continue;
            }
            if (t.isEntity() && getKillCount(player, t.id) < t.count) return false;
            if (t.isEffect() && !hasEffect(player, t.id)) return false;
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
            if (player.level().isClientSide)
                return CLIENT_STATS.getOrDefault(statId, 0);
        } catch (Exception ignored) {}
        return 0;
    }

    public static boolean hasAnyCompleted(Player player) {
        if (player != null && player.level().isClientSide) ensureClientStateLoaded(player);
        for (QuestData.Quest q : QuestData.all()) {
            if (getStatus(q, player) == Status.COMPLETED) return true;
        }
        return false;
    }

    public static boolean hasCompleted(Player player, String questId) {
        return getStatus(questId, player) == Status.REDEEMED;
    }

    public static int getCountInInventory(String id, Player player) {
        if (player == null || id == null || id.isBlank()) return 0;
        boolean isTagSyntax = id.startsWith("#");
        String key = isTagSyntax ? id.substring(1) : id;
        ResourceLocation rl = ResourceLocation.parse(key);
        Item direct = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        int found = 0;
        if (isTagSyntax || direct == null) {
            var itemTag = net.minecraft.tags.TagKey.create(Registries.ITEM, rl);
            for (ItemStack s : player.getInventory().items)
                if (!s.isEmpty() && s.is(itemTag)) found += s.getCount();
            if (found == 0) {
                var blockTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, rl);
                for (ItemStack s : player.getInventory().items)
                    if (!s.isEmpty() && s.getItem() instanceof BlockItem bi
                            && bi.getBlock().builtInRegistryHolder().is(blockTag))
                        found += s.getCount();
            }
        } else {
            for (ItemStack s : player.getInventory().items)
                if (!s.isEmpty() && s.is(direct)) found += s.getCount();
        }
        return found;
    }

    public static int getKillCount(Player player, String entityId) {
        if (player == null || entityId == null || entityId.isBlank()) return 0;
        if (player instanceof ServerPlayer sp) {
            return KillCounterState.get(sp.serverLevel())
                    .snapshotFor(player.getUUID())
                    .getOrDefault(entityId, 0);
        }
        return CLIENT_KILLS.getOrDefault(entityId, 0);
    }

    public static boolean hasEffect(Player player, String effectId) {
        ResourceLocation rl = ResourceLocation.parse(effectId);
        Holder<MobEffect> opt = BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
        return opt != null && player.hasEffect(opt);
    }

    public static boolean hasAdvancement(Player player, String advId) {
        ResourceLocation rl = ResourceLocation.parse(advId);
        if (player instanceof ServerPlayer sp) {
            AdvancementHolder holder = sp.server.getAdvancements().get(rl);
            if (holder == null) return false;
            AdvancementProgress prog = sp.getAdvancements().getOrStartProgress(holder);
            boolean done = prog.isDone();
            CLIENT_ADV_DONE.put(rl.toString(), done);
            return done;
        }
        if (player.level().isClientSide)
            return CLIENT_ADV_DONE.getOrDefault(rl.toString(), false);
        return false;
    }

    public static boolean serverRedeem(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;
        if (q.rewards != null && q.rewards.items != null)
            for (QuestData.RewardEntry r : q.rewards.items) {
                ResourceLocation rl = ResourceLocation.parse(r.item);
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null)
                    player.getInventory().add(new ItemStack(item, Math.max(1, r.count)));
            }
        if (q.rewards != null && q.rewards.command != null && !q.rewards.command.isBlank()) {
            String cmd = q.rewards.command.startsWith("/") ? q.rewards.command.substring(1) : q.rewards.command;
            CommandSourceStack css = player.createCommandSourceStack().withPermission(4);
            player.server.getCommands().performPrefixedCommand(css, cmd);
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
            if (FMLEnvironment.dist == Dist.CLIENT) {
                clientClearAll();
            }
            return;
        }
        if (player != null && player.level().isClientSide) {
            clientClearAll();
        }
    }


    public static void clientSetStatus(String questId, Status st) {
        if (questId == null || st == null) return;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                ensureClientStateLoaded(null);
            } catch (Throwable ignored) {}
        }
        activeStateMap().put(questId, st);
        if (FMLEnvironment.dist == Dist.CLIENT && ACTIVE_KEY != null)
            saveClientState(ACTIVE_KEY);
    }

    public static void clientSetKill(String entityId, int count) {
        CLIENT_KILLS.put(entityId, Math.max(0, count));
    }

    public static void clientClearAll() {
        CLIENT_KILLS.clear();
        CLIENT_ADV_DONE.clear();
        CLIENT_STATS.clear();
        CLIENT_ITEM_PROGRESS.clear();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                ensureClientStateLoaded(null);
            } catch (Throwable ignored) {}
            activeStateMap().clear();
            if (ACTIVE_KEY != null) saveClientState(ACTIVE_KEY);
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

// Detect if this quest uses item collection targets.
            boolean hasItemTargets = false;
            if (q.completion != null && q.completion.targets != null) {
                for (QuestData.Target t : q.completion.targets) {
                    if (t.isItem()) {
                        hasItemTargets = true;
                        break;
                    }
                }
            }

            if (ready && cur == Status.INCOMPLETE) {
                clientSetStatus(q.id, Status.COMPLETED);
                if (player instanceof ServerPlayer sp) {
                    setServerStatus(sp, q.id, Status.COMPLETED);
                }
                if (!QuestTracker.serverToastsDisabled()) sendToastLocal(q.id);
                continue;
            }


            if (hasItemTargets && cur == Status.COMPLETED) {
                continue;
            }



// Non-item quests revert normally.
            if (!ready && cur == Status.COMPLETED) {
                clientSetStatus(q.id, Status.INCOMPLETE);
            }

        }
    }

    public static boolean serverToastsDisabled() {
        return SERVER_TOASTS_DISABLED;
    }

    public static void setServerToastsDisabled(boolean v) {
        SERVER_TOASTS_DISABLED = v;
    }

}
