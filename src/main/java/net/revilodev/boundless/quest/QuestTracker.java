package net.revilodev.boundless.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestTracker {
    public enum Status { INCOMPLETE, REDEEMED, REJECTED }

    private static final Map<String, Status> CLIENT_STATES = new HashMap<>();

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
        QuestWorldState st = state(player);
        if (st != null) {
            for (String dep : deps) {
                if (st.get(dep) != Status.REDEEMED) return false;
            }
            return true;
        } else {
            for (String dep : deps) {
                if (CLIENT_STATES.getOrDefault(dep, Status.INCOMPLETE) != Status.REDEEMED) return false;
            }
            return true;
        }
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null) return false;
        if ("collection".equals(q.type) && q.completion != null) {
            return getCollected(q, player) >= q.completion.count;
        }
        if ("submission".equals(q.type) && q.completion != null) {
            return getCollected(q, player) >= q.completion.count;
        }
        if ("kill".equals(q.type) && q.completion != null) {
            return getKills(q, player) >= q.completion.count;
        }
        return false;
    }

    public static int getCollected(QuestData.Quest q, Player player) {
        if (player == null) return 0;
        if ((("collection".equals(q.type) || "submission".equals(q.type)) && q.completion != null)) {
            ResourceLocation rl = ResourceLocation.parse(q.completion.item);
            Item target = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (target == null) return 0;
            int found = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.is(target)) found += stack.getCount();
            }
            return found;
        }
        return 0;
    }

    public static int getKills(QuestData.Quest q, Player player) {
        if (player == null) return 0;
        if ("kill".equals(q.type) && q.completion != null && q.completion.entity != null && !q.completion.entity.isBlank()) {
            ResourceLocation rl = ResourceLocation.parse(q.completion.entity);
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
            if (type == null) return 0;
            if (player instanceof ServerPlayer sp) {
                return sp.getStats().getValue(Stats.ENTITY_KILLED.get(type));
            } else {
                return 0;
            }
        }
        return 0;
    }

    public static boolean completeAndRedeem(QuestData.Quest q, ServerPlayer player) {
        QuestWorldState st = state(player);
        if (st == null) return false;
        Status cur = st.get(q.id);
        if (cur != Status.INCOMPLETE) return false;
        if ("submission".equals(q.type) && q.completion != null) {
            ResourceLocation rl = ResourceLocation.parse(q.completion.item);
            Item target = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (target == null) return false;
            int need = Math.max(1, q.completion.count);
            if (getCollected(q, player) < need) return false;
            int remaining = need;
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (!stack.isEmpty() && stack.is(target)) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
            player.getInventory().setChanged();
        }
        if (q.reward != null && q.reward.item != null && !q.reward.item.isBlank()) {
            ResourceLocation rl = ResourceLocation.parse(q.reward.item);
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item != null) {
                player.getInventory().add(new ItemStack(item, Math.max(1, q.reward.count)));
            }
        }
        st.set(q.id, Status.REDEEMED);
        return true;
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
    }

    public static void clientSetStatus(String questId, Status status) {
        CLIENT_STATES.put(questId, status);
    }

    public static void clientClear() {
        CLIENT_STATES.clear();
    }
}
