package net.revilodev.boundless.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestTracker {
    public enum Status { INCOMPLETE, REDEEMED, REJECTED }

    private static final Map<String, Status> CLIENT_STATES = new HashMap<>();
    private static final Map<String, Integer> CLIENT_KILLS = new HashMap<>();

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

    public static boolean isVisible(QuestData.Quest q, Player player) {
        return getStatus(q, player) == Status.INCOMPLETE;
    }

    public static boolean dependenciesMet(QuestData.Quest q, Player player) {
        if (player == null) return false;
        List<String> deps = q.dependencies;
        if (deps == null || deps.isEmpty()) return true;
        for (String dep : deps) {
            QuestData.Quest d = QuestData.byId(dep).orElse(null);
            if (d == null) return false;
            if (getStatus(d, player) != Status.REDEEMED) return false;
        }
        return true;
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null) return false;
        if (!dependenciesMet(q, player)) return false;
        if (q.completion == null || q.completion.targets.isEmpty()) return false;
        if ("kill".equals(q.type)) {
            for (QuestData.Target t : q.completion.targets) {
                if (!t.isEntity()) continue;
                if (getKillCount(player, t.id) < t.count) return false;
            }
            return true;
        }
        if ("collection".equals(q.type) || "submission".equals(q.type)) {
            for (QuestData.Target t : q.completion.targets) {
                if (!t.isItem()) continue;
                if (getCountInInventory(t.id, player) < t.count) return false;
            }
            return true;
        }
        return false;
    }

    public static int getCountInInventory(String itemId, Player player) {
        if (player == null) return 0;
        ResourceLocation rl = ResourceLocation.parse(itemId);
        Item target = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (target == null) return 0;
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(target)) found += stack.getCount();
        }
        return found;
    }

    public static int getKillCount(Player player, String entityId) {
        if (player instanceof ServerPlayer sp && player.level() instanceof ServerLevel server) {
            return KillCounterState.get(server).get(sp.getUUID(), entityId);
        }
        return CLIENT_KILLS.getOrDefault(entityId, 0);
    }

    public static boolean completeAndRedeem(QuestData.Quest q, ServerPlayer player) {
        QuestWorldState st = state(player);
        if (st == null) return false;
        if (st.get(q.id) != Status.INCOMPLETE) return false;
        if (!isReady(q, player)) return false;
        if ("submission".equals(q.type) && q.completion != null) {
            for (QuestData.Target t : q.completion.targets) {
                if (!t.isItem()) continue;
                consumeFromInventory(player, t.id, t.count);
            }
        }
        if (q.rewards != null && q.rewards.items != null) {
            for (QuestData.RewardEntry re : q.rewards.items) {
                ResourceLocation rl = ResourceLocation.parse(re.item);
                Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null) {
                    player.getInventory().add(new ItemStack(item, Math.max(1, re.count)));
                }
            }
        }
        st.set(q.id, Status.REDEEMED);
        return true;
    }

    private static void consumeFromInventory(ServerPlayer player, String itemId, int count) {
        ResourceLocation rl = ResourceLocation.parse(itemId);
        Item target = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (target == null) return;
        int remaining = count;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack s = player.getInventory().items.get(i);
            if (!s.isEmpty() && s.is(target)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
    }

    public static boolean reject(QuestData.Quest q, ServerPlayer player) {
        QuestWorldState st = state(player);
        if (st == null) return false;
        if (st.get(q.id) == Status.REJECTED) return false;
        st.set(q.id, Status.REJECTED);
        return true;
    }

    public static void reset(Player player) {
        QuestWorldState st = state(player);
        if (st != null) st.reset();
        CLIENT_STATES.clear();
        CLIENT_KILLS.clear();
    }

    public static void clientSetStatus(String questId, Status status) {
        CLIENT_STATES.put(questId, status);
    }

    public static void clientSetKills(Map<String, Integer> all) {
        CLIENT_KILLS.clear();
        CLIENT_KILLS.putAll(all);
    }

    public static void clientSetKill(String entityId, int count) {
        CLIENT_KILLS.put(entityId, count);
    }

    public static void clientClearAll() {
        CLIENT_STATES.clear();
        CLIENT_KILLS.clear();
    }
}
