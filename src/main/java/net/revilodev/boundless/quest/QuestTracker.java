package net.revilodev.boundless.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class QuestTracker {
    public enum Status { INCOMPLETE, REDEEMED }

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

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null) return false;
        if ("collection".equals(q.type) && q.completion != null) {
            return getCollected(q, player) >= q.completion.count;
        }
        return false;
    }

    public static int getCollected(QuestData.Quest q, Player player) {
        if (player == null) return 0;
        if ("collection".equals(q.type) && q.completion != null) {
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

    public static boolean completeAndRedeem(QuestData.Quest q, ServerPlayer player) {
        QuestWorldState st = state(player);
        if (st == null) return false;
        if (st.get(q.id) == Status.REDEEMED) return false;
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

    public static void reset(Player player) {
        QuestWorldState st = state(player);
        if (st != null) st.reset();
        CLIENT_STATES.clear();
    }

    public static void clientSetStatus(String questId, Status status) {
        CLIENT_STATES.put(questId, status);
    }
}
