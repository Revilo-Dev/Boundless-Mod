package net.revilodev.boundless.quest;

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
import net.revilodev.boundless.Config;
import net.revilodev.boundless.network.BoundlessNetwork;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestTracker {

    public enum Status { INCOMPLETE, COMPLETED, REDEEMED, REJECTED }

    private static final Map<String, Integer> CLIENT_KILLS = new HashMap<>();
    private static final Map<String, Boolean> CLIENT_ADV_DONE = new HashMap<>();
    private static final Map<String, Integer> CLIENT_STATS = new HashMap<>();
    private static final Map<String, Status> CLIENT_STATUS = new LinkedHashMap<>();

    private QuestTracker() {}

    public static void forceSave() {
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

    private static void setServerStatus(ServerPlayer player, String questId, Status st) {
        QuestProgressState data = QuestProgressState.get(player.serverLevel());
        if (st == null || st == Status.INCOMPLETE) {
            data.set(player.getUUID(), questId, null);
        } else {
            data.set(player.getUUID(), questId, st.name());
        }
    }

    public static Status getStatus(QuestData.Quest q, Player player) {
        if (q == null) return Status.INCOMPLETE;
        return getStatus(q.id, player);
    }

    public static Status getStatus(String questId, Player player) {
        if (player instanceof ServerPlayer sp) return getServerStatus(sp, questId);
        return CLIENT_STATUS.getOrDefault(questId, Status.INCOMPLETE);
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
        if (st == Status.REDEEMED || st == Status.REJECTED) return false;
        return true;
    }

    public static boolean isReady(QuestData.Quest q, Player player) {
        if (player == null || q == null || q.completion == null) return false;
        if (!dependenciesMet(q, player)) return false;

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
            return prog.isDone();
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

        setServerStatus(player, q.id, Status.REDEEMED);
        BoundlessNetwork.sendStatus(player, q.id, Status.REDEEMED.name());
        return true;
    }

    public static boolean serverReject(QuestData.Quest q, ServerPlayer player) {
        if (q == null || player == null) return false;
        if (!q.optional) return false;
        setServerStatus(player, q.id, Status.REJECTED);
        BoundlessNetwork.sendStatus(player, q.id, Status.REJECTED.name());
        return true;
    }

    public static void reset(Player player) {
        CLIENT_KILLS.clear();
        CLIENT_ADV_DONE.clear();
        CLIENT_STATS.clear();
        CLIENT_STATUS.clear();

        if (player instanceof ServerPlayer sp) {
            QuestProgressState.get(sp.serverLevel()).clear(sp.getUUID());
            BoundlessNetwork.syncPlayer(sp);
        }
    }

    public static void clientSetStatus(String questId, Status st) {
        if (questId == null || st == null) return;
        if (st == Status.INCOMPLETE) CLIENT_STATUS.remove(questId);
        else CLIENT_STATUS.put(questId, st);
    }

    public static void clientSetKill(String entityId, int count) {
        CLIENT_KILLS.put(entityId, Math.max(0, count));
    }

    public static void clientClearAll() {
        CLIENT_KILLS.clear();
        CLIENT_ADV_DONE.clear();
        CLIENT_STATS.clear();
        CLIENT_STATUS.clear();
    }

    public static void tickPlayer(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        QuestData.loadServer(sp.server, false);
        QuestProgressState state = QuestProgressState.get(sp.serverLevel());
        Map<String, String> snapshot = state.snapshotFor(sp.getUUID());

        for (QuestData.Quest q : QuestData.allServer(sp.server)) {
            Status cur = decodeStatus(snapshot.get(q.id));
            if (cur == Status.REDEEMED || cur == Status.REJECTED || cur == Status.COMPLETED) continue;

            boolean ready = dependenciesMet(q, sp) && isReady(q, sp);
            if (ready) {
                setServerStatus(sp, q.id, Status.COMPLETED);
                BoundlessNetwork.sendStatus(sp, q.id, Status.COMPLETED.name());
                BoundlessNetwork.sendToast(sp, q.id);
            }
        }
    }
}
