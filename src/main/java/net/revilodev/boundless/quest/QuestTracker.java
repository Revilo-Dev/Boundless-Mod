package net.revilodev.boundless.quest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.boundless.network.BoundlessNetwork;

import java.util.HashMap;
import java.util.Map;

public final class QuestTracker {
    public enum Status { INCOMPLETE, COMPLETED, REDEEMED, REJECTED }

    private static final Map<String, Status> CLIENT_STATES = new HashMap<>();
    private static final Map<String, Integer> CLIENT_KILLS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_ADV_DONE = new HashMap<>();
    private static final Map<String, Integer> CLIENT_STATS = new HashMap<>();

    private static QuestWorldState state(Player player) {
        if (player == null) return null;
        if (player.level() instanceof ServerLevel server) return QuestWorldState.get(server);
        return null;
    }

    public static Status getStatus(QuestData.Quest q, Player player) {
        QuestWorldState st = state(player);
        if (st != null) return st.get(q.id);
        return CLIENT_STATES.getOrDefault(q.id, Status.INCOMPLETE);
    }

    public static Status getStatus(String questId, Player player) {
        QuestWorldState st = state(player);
        if (st != null) return st.get(questId);
        return CLIENT_STATES.getOrDefault(questId, Status.INCOMPLETE);
    }

    public static boolean isVisible(QuestData.Quest q, Player player) {
        Status s = getStatus(q, player);
        return s == Status.INCOMPLETE || s == Status.COMPLETED;
    }

    public static boolean dependenciesMet(QuestData.Quest q, Player player) {
        if (q == null || q.dependencies == null || q.dependencies.isEmpty()) return true;
        for (String depId : q.dependencies) {
            QuestData.Quest dep;
            if (player.level().isClientSide) dep = QuestData.byId(depId).orElse(null);
            else {
                ServerLevel sl = (ServerLevel) player.level();
                dep = QuestData.byIdServer(sl.getServer(), depId).orElse(null);
            }
            if (dep == null) return false;
            if (getStatus(dep, player) != Status.REDEEMED) return false;
        }
        return true;
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        if (!dependenciesMet(q, player)) return false;
        if (q.completion.targets == null || q.completion.targets.isEmpty()) return false;
        for (QuestData.Target t : q.completion.targets) {
            if (t.isItem() && getCountInInventory(t.id, player) < t.count) return false;
            if (t.isEntity() && getKillCount(player, t.id) < t.count) return false;
            if (t.isEffect() && !hasEffect(player, t.id)) return false;
            if (t.isAdvancement() && !hasAdvancement(player, t.id)) return false;
            if (t.isStat() && getStatCount(player, t.id) < t.count) return false;
        }
        return true;
    }

    public static int getStatCount(Player player, String statId) {
        if (player == null || statId == null || statId.isBlank()) return 0;
        int value = 0;

        try {
            if (player instanceof ServerPlayer sp) {
                String id = statId.trim();
                int first = id.indexOf(':');
                int second = id.indexOf(':', first + 1);

                boolean typed = second > first;
                String type = typed ? id.substring(0, first) : "custom";
                String name = typed ? id.substring(first + 1) : id;

                ResourceLocation rl = ResourceLocation.tryParse(name);
                if (rl == null) return 0; // ✨ prevent null crash

                switch (type) {
                    case "custom" -> {
                        if (!BuiltInRegistries.CUSTOM_STAT.containsKey(rl)) return 0;
                        value = sp.getStats().getValue(Stats.CUSTOM.get(rl));
                    }
                    case "mine_block" -> {
                        var block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
                        if (block == null) return 0;
                        value = sp.getStats().getValue(Stats.BLOCK_MINED.get(block));
                    }
                    case "use_item" -> {
                        var item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                        if (item == null) return 0;
                        value = sp.getStats().getValue(Stats.ITEM_USED.get(item));
                    }
                    case "kill_entity" -> {
                        var et = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
                        if (et == null) return 0;
                        value = sp.getStats().getValue(Stats.ENTITY_KILLED.get(et));
                    }
                    default -> {
                        // unknown stat type → ignore safely
                        return 0;
                    }
                }

                CLIENT_STATS.put(statId, value);

            } else if (player.level().isClientSide && player instanceof net.minecraft.client.player.LocalPlayer lp) {
                var stats = lp.getStats();
                if (stats != null) {
                    ResourceLocation rl = ResourceLocation.tryParse(statId);
                    if (rl != null && BuiltInRegistries.CUSTOM_STAT.containsKey(rl)) {
                        value = stats.getValue(Stats.CUSTOM.get(rl));
                    }
                }
            } else {
                value = CLIENT_STATS.getOrDefault(statId, 0);
            }
        } catch (Exception e) {
            // safeguard all stat failures — prevents tick crash
            return 0;
        }

        return value;
    }




    private static ResourceLocation safeParse(String s) {
        try { return ResourceLocation.parse(s); } catch (Throwable t) { return null; }
    }

    public static boolean hasAnyCompleted(Player player) {
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
        boolean explicitHash = id.startsWith("#");
        String key = explicitHash ? id.substring(1) : id;
        ResourceLocation rl = ResourceLocation.parse(key);
        Item direct = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        int found = 0;
        if (explicitHash || direct == null) {
            var itemTag = net.minecraft.tags.TagKey.create(Registries.ITEM, rl);
            for (ItemStack s : player.getInventory().items) if (!s.isEmpty() && s.is(itemTag)) found += s.getCount();
            if (found == 0) {
                var blockTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, rl);
                for (ItemStack s : player.getInventory().items)
                    if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag))
                        found += s.getCount();
            }
        } else for (ItemStack s : player.getInventory().items) if (!s.isEmpty() && s.is(direct)) found += s.getCount();
        return found;
    }

    public static int getKillCount(Player player, String entityId) {
        if (player == null || entityId == null || entityId.isBlank()) return 0;
        if (player.level() instanceof ServerLevel sl) {
            var map = KillCounterState.get(sl).snapshotFor(player.getUUID());
            Integer v = map.get(entityId);
            return v == null ? 0 : v;
        }
        return CLIENT_KILLS.getOrDefault(entityId, 0);
    }

    public static boolean hasEffect(Player player, String effectId) {
        if (player == null) return false;
        ResourceLocation rl = ResourceLocation.parse(effectId);
        var opt = BuiltInRegistries.MOB_EFFECT.getHolder(rl);
        if (opt.isEmpty()) return false;
        Holder<MobEffect> holder = opt.get();
        return player.hasEffect(holder);
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
        if (player.level().isClientSide) return CLIENT_ADV_DONE.getOrDefault(rl.toString(), false);
        return false;
    }

    public static boolean completeAndRedeem(QuestData.Quest q, ServerPlayer player) {
        QuestWorldState st = state(player);
        if (st == null) return false;
        if (st.get(q.id) == Status.REDEEMED) return false;
        if (q.rewards != null && q.rewards.items != null) {
            for (QuestData.RewardEntry r : q.rewards.items) {
                ResourceLocation rl = ResourceLocation.parse(r.item);
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null) player.getInventory().add(new ItemStack(item, Math.max(1, r.count)));
            }
        }
        if (q.rewards != null && q.rewards.command != null && !q.rewards.command.isBlank()) {
            String raw = q.rewards.command.startsWith("/") ? q.rewards.command.substring(1) : q.rewards.command;
            CommandSourceStack source = player.createCommandSourceStack().withPermission(4);
            player.server.getCommands().performPrefixedCommand(source, raw);
        }
        st.set(q.id, Status.REDEEMED);
        return true;
    }

    public static boolean reject(QuestData.Quest q, ServerPlayer player) {
        QuestWorldState st = state(player);
        if (st == null) return false;
        if (!q.optional) return false;
        if (st.get(q.id) == Status.REDEEMED) return false;
        st.set(q.id, Status.REJECTED);
        return true;
    }

    public static void reset(Player player) {
        QuestWorldState st = state(player);
        if (st != null) st.reset();
        CLIENT_STATES.clear();
        CLIENT_KILLS.clear();
        CLIENT_ADV_DONE.clear();
        CLIENT_STATS.clear();
    }

    public static void clientSetStatus(String questId, Status status) { CLIENT_STATES.put(questId, status); }
    public static void clientSetKill(String entityId, int count) { CLIENT_KILLS.put(entityId, Math.max(0, count)); }
    public static void clientClearAll() { CLIENT_STATES.clear(); CLIENT_KILLS.clear(); CLIENT_ADV_DONE.clear(); CLIENT_STATS.clear(); }

    public static void tickPlayer(Player player) {
        if (player == null) return;
        if (!player.level().isClientSide) return;
        QuestData.loadClient(false);
        for (QuestData.Quest q : QuestData.all()) {
            if (q == null) continue;
            if (!"all".equalsIgnoreCase(q.category)) {
                var cat = QuestData.categoryById(q.category).orElse(null);
                if (cat != null && !QuestData.isCategoryUnlocked(cat, player)) continue;
            }
            Status current = getStatus(q, player);
            if (current == Status.REDEEMED || current == Status.REJECTED) continue;
            boolean ready = dependenciesMet(q, player) && isReady(q, player);
            if (ready && current == Status.INCOMPLETE) clientSetStatus(q.id, Status.COMPLETED);
            else if (!ready && current == Status.COMPLETED) clientSetStatus(q.id, Status.INCOMPLETE);
        }
    }

    public static void serverCheckAndMarkComplete(ServerPlayer sp) {
        QuestWorldState st = state(sp);
        if (st == null) return;
        for (QuestData.Quest q : QuestData.allServer(sp.server)) {
            Status cur = st.get(q.id);
            if (cur == Status.REDEEMED || cur == Status.REJECTED) continue;
            boolean ready = dependenciesMet(q, sp) && isReady(q, sp);
            if (ready && cur == Status.INCOMPLETE) {
                st.set(q.id, Status.COMPLETED);
                PacketDistributor.sendToPlayer(sp, new BoundlessNetwork.SyncStatusPayload(q.id, Status.COMPLETED.name()));
                BoundlessNetwork.sendToastTo(sp, q.id);
            } else if (!ready && cur == Status.COMPLETED) {
                st.set(q.id, Status.INCOMPLETE);
                PacketDistributor.sendToPlayer(sp, new BoundlessNetwork.SyncStatusPayload(q.id, Status.INCOMPLETE.name()));
            }
        }
    }
}
