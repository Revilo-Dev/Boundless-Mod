package net.revilodev.boundless.quest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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
        }
        return true;
    }

    public static boolean hasAnyCompleted(Player player) {
        for (QuestData.Quest q : QuestData.all()) {
            if (getStatus(q, player) == Status.COMPLETED) return true;
        }
        return false;
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
                    if (!s.isEmpty() && s.getItem() instanceof BlockItem bi &&
                            bi.getBlock().builtInRegistryHolder().is(blockTag)) {
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

    public static int getKillCount(Player player, String entityId) {
        if (player == null || entityId == null || entityId.isBlank()) return 0;
        if (player.level() instanceof ServerLevel sl) {
            var map = KillCounterState.get(sl).snapshotFor(player.getUUID());
            Integer v = map.get(entityId);
            return v == null ? 0 : v;
        }
        return CLIENT_KILLS.getOrDefault(entityId, 0);
    }

    // --- fixed to use Holder<MobEffect> ---
    public static boolean hasEffect(Player player, String effectId) {
        if (player == null) return false;
        ResourceLocation rl = ResourceLocation.parse(effectId);
        var opt = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(rl);
        if (opt.isEmpty()) return false;
        Holder<MobEffect> holder = opt.get();
        return player.hasEffect(holder);
    }

    public static boolean hasAdvancement(Player player, String advId) {
        if (!(player instanceof ServerPlayer sp)) return false;
        ResourceLocation rl = ResourceLocation.parse(advId);
        AdvancementHolder holder = sp.server.getAdvancements().get(rl);
        if (holder == null) return false;
        AdvancementProgress prog = sp.getAdvancements().getOrStartProgress(holder);
        return prog.isDone();
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

    public static void tickPlayer(Player player) {
        if (player == null) return;
        if (!player.level().isClientSide) return;
        QuestData.loadClient(false);
        for (QuestData.Quest q : QuestData.all()) {
            Status current = getStatus(q, player);
            if (current == Status.REDEEMED || current == Status.REJECTED) continue;
            boolean ready = dependenciesMet(q, player) && isReady(q, player);
            if (ready && current == Status.INCOMPLETE) {
                clientSetStatus(q.id, Status.COMPLETED);
            } else if (!ready && current == Status.COMPLETED) {
                clientSetStatus(q.id, Status.INCOMPLETE);
            }
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
