package net.revilodev.boundless.quest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
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
        if (q == null || q.dependencies == null || q.dependencies.isEmpty()) return true;
        for (String depId : q.dependencies) {
            QuestData.Quest dep = QuestData.byId(depId).orElse(null);
            if (dep == null) return false;
            if (getStatus(dep, player) != Status.REDEEMED) return false;
        }
        return true;
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        if ("collection".equals(q.type) || "submission".equals(q.type)) {
            if (q.completion.targets == null || q.completion.targets.isEmpty()) return false;
            for (QuestData.Target t : q.completion.targets) {
                if (!t.isItem()) continue;
                if (getCountInInventory(t.id, player) < t.count) return false;
            }
            return true;
        }
        return false;
    }

    public static int getCollected(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null || q.completion.targets == null) return 0;
        for (QuestData.Target t : q.completion.targets) {
            if (t.isItem()) return getCountInInventory(t.id, player);
        }
        return 0;
    }

    public static int getCountInInventory(String id, Player player) {
        if (player == null || id == null || id.isBlank()) return 0;
        boolean explicitHash = id.startsWith("#");
        String key = explicitHash ? id.substring(1) : id;
        ResourceLocation rl = ResourceLocation.parse(key);
        Item direct = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        int found = 0;
        if (explicitHash || direct == null) {
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, rl);
            for (ItemStack s : player.getInventory().items) {
                if (!s.isEmpty() && s.is(itemTag)) found += s.getCount();
            }
            if (found == 0) {
                var blockTag = TagKey.create(Registries.BLOCK, rl);
                for (ItemStack s : player.getInventory().items) {
                    if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag)) {
                        found += s.getCount();
                    }
                }
            }
        } else {
            for (ItemStack s : player.getInventory().items) {
                if (!s.isEmpty() && s.is(direct)) found += s.getCount();
            }
        }
        return found;
    }

    public static boolean completeAndRedeem(QuestData.Quest q, ServerPlayer player) {
        QuestWorldState st = state(player);
        if (st == null) return false;
        if (st.get(q.id) == Status.REDEEMED) return false;
        if (q.rewards != null && q.rewards.items != null) {
            for (QuestData.RewardEntry r : q.rewards.items) {
                ResourceLocation rl = ResourceLocation.parse(r.item);
                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (item != null) {
                    player.getInventory().add(new ItemStack(item, Math.max(1, r.count)));
                }
            }
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

    public static int getKillCount(Player player, String entityId) {
        if (player == null || entityId == null || entityId.isBlank()) return 0;
        if (player.level() instanceof ServerLevel sl) {
            var map = KillCounterState.get(sl).snapshotFor(player.getUUID());
            Integer v = map.get(entityId);
            return v == null ? 0 : v;
        }
        return CLIENT_KILLS.getOrDefault(entityId, 0);
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

    public static void clientSetKill(String entityId, int count) {
        CLIENT_KILLS.put(entityId, Math.max(0, count));
    }

    public static void clientClearAll() {
        CLIENT_STATES.clear();
        CLIENT_KILLS.clear();
    }
}
