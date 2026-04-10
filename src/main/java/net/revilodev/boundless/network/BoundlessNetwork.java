package net.revilodev.boundless.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.toast.QuestUnlockedToast;
import net.revilodev.boundless.item.ModItems;
import net.revilodev.boundless.quest.KillCounterState;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestProgressState;
import net.revilodev.boundless.quest.QuestTracker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class BoundlessNetwork {

    private static final String CHANNEL = "boundless";
    private static final String VERSION = "2";
    private static boolean REGISTERED = false;

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Set<String> REDEEM_IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private static final AtomicInteger SYNC_ID_GEN = new AtomicInteger();
    private static final int QUEST_CHUNK_BYTES = 60000;

    private BoundlessNetwork() {}

    public static void bootstrap(IEventBus bus) {
        bus.addListener(BoundlessNetwork::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        if (REGISTERED) return;
        REGISTERED = true;

        PayloadRegistrar r = event.registrar(CHANNEL).versioned(VERSION);

        r.playToServer(Redeem.TYPE, Redeem.CODEC, BoundlessNetwork::handleRedeem);
        r.playToServer(Reject.TYPE, Reject.CODEC, BoundlessNetwork::handleReject);
        r.playToServer(CreateScroll.TYPE, CreateScroll.CODEC, BoundlessNetwork::handleCreateScroll);
        r.playToServer(RestartRepeatable.TYPE, RestartRepeatable.CODEC, BoundlessNetwork::handleRestartRepeatable);

        r.playToClient(SyncStatus.TYPE, SyncStatus.CODEC, BoundlessNetwork::handleSyncStatus);
        r.playToClient(SyncStatuses.TYPE, SyncStatuses.CODEC, BoundlessNetwork::handleSyncStatuses);
        r.playToClient(SyncProgressMeta.TYPE, SyncProgressMeta.CODEC, BoundlessNetwork::handleSyncProgressMeta);
        r.playToClient(SyncKills.TYPE, SyncKills.CODEC, BoundlessNetwork::handleSyncKills);
        r.playToClient(SyncClear.TYPE, SyncClear.CODEC, BoundlessNetwork::handleSyncClear);
        r.playToClient(Toast.TYPE, Toast.CODEC, BoundlessNetwork::handleToast);
        r.playToClient(OpenQuestBook.TYPE, OpenQuestBook.CODEC, BoundlessNetwork::handleOpenQuestBook);
        r.playToClient(SyncQuestsChunk.TYPE, SyncQuestsChunk.CODEC, BoundlessNetwork::handleSyncQuestsChunk);
    }

    public record Redeem(String questId) implements CustomPacketPayload {
        public static final Type<Redeem> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "redeem"));
        public static final StreamCodec<FriendlyByteBuf, Redeem> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new Redeem(buf.readUtf())
        );
        @Override public Type<Redeem> type() { return TYPE; }
    }

    public record Reject(String questId) implements CustomPacketPayload {
        public static final Type<Reject> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "reject"));
        public static final StreamCodec<FriendlyByteBuf, Reject> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new Reject(buf.readUtf())
        );
        @Override public Type<Reject> type() { return TYPE; }
    }

    public record CreateScroll(String questId) implements CustomPacketPayload {
        public static final Type<CreateScroll> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "create_scroll"));
        public static final StreamCodec<FriendlyByteBuf, CreateScroll> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new CreateScroll(buf.readUtf())
        );
        @Override public Type<CreateScroll> type() { return TYPE; }
    }

    public record RestartRepeatable(String questId) implements CustomPacketPayload {
        public static final Type<RestartRepeatable> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "restart_repeatable"));
        public static final StreamCodec<FriendlyByteBuf, RestartRepeatable> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new RestartRepeatable(buf.readUtf())
        );
        @Override public Type<RestartRepeatable> type() { return TYPE; }
    }

    public record SyncStatus(String questId, String status) implements CustomPacketPayload {
        public static final Type<SyncStatus> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_status"));
        public static final StreamCodec<FriendlyByteBuf, SyncStatus> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.questId);
                    buf.writeUtf(p.status);
                },
                buf -> new SyncStatus(buf.readUtf(), buf.readUtf())
        );
        @Override public Type<SyncStatus> type() { return TYPE; }
    }

    public record StatusEntry(String questId, String status) {
        public static final StreamCodec<FriendlyByteBuf, StatusEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.questId);
                    buf.writeUtf(e.status);
                },
                buf -> new StatusEntry(buf.readUtf(), buf.readUtf())
        );
    }

    public record SyncStatuses(List<StatusEntry> entries) implements CustomPacketPayload {
        public static final Type<SyncStatuses> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_statuses"));
        public static final StreamCodec<FriendlyByteBuf, SyncStatuses> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entries.size());
                    for (StatusEntry e : p.entries) StatusEntry.CODEC.encode(buf, e);
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<StatusEntry> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) list.add(StatusEntry.CODEC.decode(buf));
                    return new SyncStatuses(list);
                }
        );
        @Override public Type<SyncStatuses> type() { return TYPE; }
    }

    public record ProgressMetaEntry(String questId, int claimCount, boolean scrollRedeemed, boolean scrollCreated) {
        public static final StreamCodec<FriendlyByteBuf, ProgressMetaEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.questId);
                    buf.writeVarInt(e.claimCount);
                    buf.writeBoolean(e.scrollRedeemed);
                    buf.writeBoolean(e.scrollCreated);
                },
                buf -> new ProgressMetaEntry(buf.readUtf(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean())
        );
    }

    public record SyncProgressMeta(List<ProgressMetaEntry> entries) implements CustomPacketPayload {
        public static final Type<SyncProgressMeta> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_progress_meta"));
        public static final StreamCodec<FriendlyByteBuf, SyncProgressMeta> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entries.size());
                    for (ProgressMetaEntry e : p.entries) ProgressMetaEntry.CODEC.encode(buf, e);
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<ProgressMetaEntry> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) list.add(ProgressMetaEntry.CODEC.decode(buf));
                    return new SyncProgressMeta(list);
                }
        );
        @Override public Type<SyncProgressMeta> type() { return TYPE; }
    }

    public record KillEntry(String entityId, int count) {
        public static final StreamCodec<FriendlyByteBuf, KillEntry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.entityId);
                    buf.writeVarInt(e.count);
                },
                buf -> new KillEntry(buf.readUtf(), buf.readVarInt())
        );
    }



    public record SyncKills(List<KillEntry> entries) implements CustomPacketPayload {
        public static final Type<SyncKills> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_kills"));
        public static final StreamCodec<FriendlyByteBuf, SyncKills> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entries.size());
                    for (KillEntry e : p.entries) KillEntry.CODEC.encode(buf, e);
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<KillEntry> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) list.add(KillEntry.CODEC.decode(buf));
                    return new SyncKills(list);
                }
        );
        @Override public Type<SyncKills> type() { return TYPE; }
    }

    public record SyncClear() implements CustomPacketPayload {
        public static final Type<SyncClear> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_clear"));
        public static final StreamCodec<FriendlyByteBuf, SyncClear> CODEC =
                StreamCodec.of((b, p) -> {}, b -> new SyncClear());
        @Override public Type<SyncClear> type() { return TYPE; }
    }

    public record Toast(String questId) implements CustomPacketPayload {
        public static final Type<Toast> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "toast"));
        public static final StreamCodec<FriendlyByteBuf, Toast> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.questId),
                buf -> new Toast(buf.readUtf())
        );
        @Override public Type<Toast> type() { return TYPE; }
    }

    public record OpenQuestBook() implements CustomPacketPayload {
        public static final Type<OpenQuestBook> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "open_quest_book"));
        public static final StreamCodec<FriendlyByteBuf, OpenQuestBook> CODEC =
                StreamCodec.of((buf, p) -> {}, buf -> new OpenQuestBook());
        @Override public Type<OpenQuestBook> type() { return TYPE; }
    }

    public record SyncQuestsChunk(int syncId, int totalParts, int index, byte[] part) implements CustomPacketPayload {
        public static final Type<SyncQuestsChunk> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_quests_chunk"));
        public static final StreamCodec<FriendlyByteBuf, SyncQuestsChunk> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.syncId);
                    buf.writeVarInt(p.totalParts);
                    buf.writeVarInt(p.index);
                    buf.writeVarInt(p.part.length);
                    buf.writeBytes(p.part);
                },
                buf -> {
                    int syncId = buf.readVarInt();
                    int total = buf.readVarInt();
                    int idx = buf.readVarInt();
                    int len = buf.readVarInt();
                    if (len < 0 || len > 1_200_000) throw new IllegalArgumentException("chunk len " + len);
                    byte[] bytes = new byte[len];
                    buf.readBytes(bytes);
                    return new SyncQuestsChunk(syncId, total, idx, bytes);
                }
        );
        @Override public Type<SyncQuestsChunk> type() { return TYPE; }
    }

    public static void syncPlayer(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new SyncClear());
        sendQuestData(p);

        List<KillEntry> killEntries = new ArrayList<>();
        KillCounterState.get(p.serverLevel()).snapshotFor(p.getUUID())
                .forEach((id, ct) -> killEntries.add(new KillEntry(id, ct)));
        if (!killEntries.isEmpty()) {
            PacketDistributor.sendToPlayer(p, new SyncKills(killEntries));
        }

        List<StatusEntry> statuses = new ArrayList<>();
        QuestProgressState.get(p.serverLevel()).snapshotFor(p.getUUID())
                .forEach((questId, status) -> statuses.add(new StatusEntry(questId, status)));
        if (!statuses.isEmpty()) {
            PacketDistributor.sendToPlayer(p, new SyncStatuses(statuses));
        }

        List<ProgressMetaEntry> metaEntries = new ArrayList<>();
        QuestProgressState.get(p.serverLevel()).progressSnapshotFor(p.getUUID())
                .forEach((questId, progress) -> metaEntries.add(new ProgressMetaEntry(
                        questId,
                        progress == null ? 0 : progress.claimCount(),
                        progress != null && progress.scrollRedeemed(),
                        progress != null && progress.scrollCreated()
                )));
        if (!metaEntries.isEmpty()) {
            PacketDistributor.sendToPlayer(p, new SyncProgressMeta(metaEntries));
        }

        syncComputedCompletion(p);
    }

    public static void sendProgressMeta(ServerPlayer player, String questId) {
        if (player == null || questId == null || questId.isBlank()) return;
        var progress = QuestProgressState.get(player.serverLevel()).progress(player.getUUID(), questId);
        PacketDistributor.sendToPlayer(player, new SyncProgressMeta(List.of(
                new ProgressMetaEntry(questId, progress.claimCount(), progress.scrollRedeemed(), progress.scrollCreated())
        )));
    }

    private static void syncComputedCompletion(ServerPlayer p) {
        for (QuestData.Quest q : QuestData.allServer(p.server)) {
            if (q == null) continue;
            QuestTracker.Status st = QuestTracker.getStatus(q, p);
            if (st == QuestTracker.Status.REDEEMED || st == QuestTracker.Status.REJECTED) continue;
            if (QuestTracker.isReady(q, p) && st == QuestTracker.Status.INCOMPLETE) {
                if (Config.autoClaimQuestRewards()) {
                    claimQuest(p, q);
                } else {
                    QuestTracker.setServerStatus(p, q.id, QuestTracker.Status.COMPLETED);
                    sendStatus(p, q.id, QuestTracker.Status.COMPLETED.name());
                }
            }
        }
    }

    // inside BoundlessNetwork.java
    private static void sendQuestData(ServerPlayer p) {
        var quests = QuestData.allServer(p.server);
        var categories = QuestData.categoriesOrderedServer(p.server);
        var subCats = QuestData.subCategoriesAllOrderedServer(p.server);

        JsonObject root = new JsonObject();

        JsonArray cats = new JsonArray();
        for (QuestData.Category c : categories) {
            JsonObject o = new JsonObject();
            o.addProperty("id", c.id);
            o.addProperty("icon", c.icon);
            o.addProperty("name", c.name);
            o.addProperty("order", c.order);
            o.addProperty("excludeFromAll", c.excludeFromAll);
            o.addProperty("dependency", c.dependency);
            cats.add(o);
        }
        root.add("categories", cats);

        JsonArray scs = new JsonArray();
        for (QuestData.SubCategory sc : subCats) {
            JsonObject o = new JsonObject();
            o.addProperty("id", sc.id);
            o.addProperty("category", sc.category);
            o.addProperty("icon", sc.icon);
            o.addProperty("name", sc.name);
            o.addProperty("order", sc.order);
            o.addProperty("defaultOpen", sc.defaultOpen);
            if (sc.sourcePath != null && !sc.sourcePath.isBlank()) {
                o.addProperty("sourcePath", sc.sourcePath);
            }

            JsonArray qids = new JsonArray();
            for (String qid : sc.quests) qids.add(qid);
            o.add("quests", qids);

            scs.add(o);
        }
        root.add("subCategories", scs);

        JsonArray qs = new JsonArray();
        for (QuestData.Quest q : quests) {
            JsonObject o = new JsonObject();
            o.addProperty("id", q.id);
            o.addProperty("name", q.name);
            o.addProperty("icon", q.icon);
            o.addProperty("description", q.description);

            JsonArray deps = new JsonArray();
            for (String d : q.dependencies) deps.add(d);
            o.add("dependencies", deps);

            o.addProperty("optional", q.optional);
            o.addProperty("repeatable", q.repeatable);
            o.addProperty("hiddenUnderDependency", q.hiddenUnderDependency);

            if (q.rewards != null) {
                JsonObject ro = new JsonObject();

                JsonArray items = new JsonArray();
                for (QuestData.RewardEntry r : q.rewards.items) {
                    JsonObject io = new JsonObject();
                    io.addProperty("item", r.item);
                    io.addProperty("count", r.count);
                    items.add(io);
                }
                ro.add("items", items);

                JsonArray cmds = new JsonArray();
                for (QuestData.CommandReward cr : q.rewards.commands) {
                    JsonObject co = new JsonObject();
                    co.addProperty("command", cr.command);
                    co.addProperty("icon", cr.icon);
                    co.addProperty("title", cr.title);
                    cmds.add(co);
                }
                ro.add("commands", cmds);

                JsonArray fns = new JsonArray();
                for (QuestData.FunctionReward fr : q.rewards.functions) {
                    JsonObject fo = new JsonObject();
                    fo.addProperty("function", fr.function);
                    fo.addProperty("icon", fr.icon);
                    fo.addProperty("title", fr.title);
                    fns.add(fo);
                }
                ro.add("functions", fns);

                JsonArray lootTables = new JsonArray();
                for (QuestData.LootTableReward lr : q.rewards.lootTables) {
                    JsonObject lo = new JsonObject();
                    lo.addProperty("lootTable", lr.lootTable);
                    lo.addProperty("icon", lr.icon);
                    lo.addProperty("title", lr.title);
                    lootTables.add(lo);
                }
                ro.add("lootTables", lootTables);

                ro.addProperty("expType", q.rewards.expType);
                ro.addProperty("expAmount", q.rewards.expAmount);

                o.add("rewards", ro);
            }

            o.addProperty("type", q.type);

            if (q.completion != null) {
                JsonObject co = new JsonObject();
                JsonArray targets = new JsonArray();
                for (QuestData.Target t : q.completion.targets) {
                    JsonObject to = new JsonObject();
                    to.addProperty("kind", t.kind);
                    to.addProperty("id", t.id);
                    to.addProperty("count", t.count);
                    targets.add(to);
                }
                co.add("targets", targets);
                o.add("completion", co);
            }

            o.addProperty("category", q.category);

            if (q.subCategory != null && !q.subCategory.isBlank()) {
                o.addProperty("subCategory", q.subCategory);
            }
            if (q.sourcePath != null && !q.sourcePath.isBlank()) {
                o.addProperty("sourcePath", q.sourcePath);
            }

            qs.add(o);
        }

        root.add("quests", qs);

        String json = GSON.toJson(root);
        sendQuestJsonChunked(p, json);
    }


    private static void sendQuestJsonChunked(ServerPlayer p, String json) {
        if (json == null) json = "";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        int syncId = SYNC_ID_GEN.incrementAndGet();

        int total = (bytes.length + QUEST_CHUNK_BYTES - 1) / QUEST_CHUNK_BYTES;
        if (total <= 0) total = 1;

        for (int i = 0; i < total; i++) {
            int start = i * QUEST_CHUNK_BYTES;
            int end = Math.min(bytes.length, start + QUEST_CHUNK_BYTES);
            byte[] part = start >= end ? new byte[0] : java.util.Arrays.copyOfRange(bytes, start, end);
            PacketDistributor.sendToPlayer(p, new SyncQuestsChunk(syncId, total, i, part));
        }
    }

    public static void sendStatus(ServerPlayer p, String questId, String status) {
        PacketDistributor.sendToPlayer(p, new SyncStatus(questId, status));
    }

    public static void sendToast(ServerPlayer p, String questId) {
        PacketDistributor.sendToPlayer(p, new Toast(questId));
    }

    public static void sendOpenQuestBook(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new OpenQuestBook());
    }

    public static void sendToastLocal(String questId) {
        QuestData.byId(questId).ifPresent(q ->
                QuestUnlockedToast.show(q.name, q.iconItem().orElse(null))
        );
    }

    private static void handleRedeem(Redeem p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (!QuestTracker.isReady(q, sp)) return;
                claimQuest(sp, q);
            });
        });
    }

    private static void handleReject(Reject p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (QuestTracker.serverReject(q, sp)) {
                    QuestTracker.setServerStatus(sp, q.id, QuestTracker.Status.REJECTED);
                    sendStatus(sp, q.id, QuestTracker.Status.REJECTED.name());
                }
            });
        });
    }

    private static void handleCreateScroll(CreateScroll p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            if (!Config.enableQuestScrolls()) return;
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (!QuestTracker.canCreateScroll(q, sp)) return;
                QuestProgressState.get(sp.serverLevel()).setScrollCreated(sp.getUUID(), q.id, true);
                ItemStack stack = ModItems.createQuestScroll(q.id);
                if (!sp.getInventory().add(stack) && !stack.isEmpty()) {
                    sp.drop(stack, false);
                }
                sendProgressMeta(sp, q.id);
            });
        });
    }

    private static void handleRestartRepeatable(RestartRepeatable p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (QuestTracker.restartRepeatable(q, sp)) {
                    sendStatus(sp, q.id, QuestTracker.Status.INCOMPLETE.name());
                }
            });
        });
    }

    private static void handleSyncStatus(SyncStatus p, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                QuestTracker.clientSetStatus(p.questId(), QuestTracker.decodeStatus(p.status()))
        );
    }

    private static void handleSyncStatuses(SyncStatuses p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            for (StatusEntry e : p.entries()) {
                QuestTracker.clientSetStatus(e.questId(), QuestTracker.decodeStatus(e.status()));
            }
        });
    }

    private static void handleSyncProgressMeta(SyncProgressMeta p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            for (ProgressMetaEntry e : p.entries()) {
                QuestTracker.clientSetClaimCount(e.questId(), e.claimCount());
                QuestTracker.clientSetScrollRedeemed(e.questId(), e.scrollRedeemed());
                QuestTracker.clientSetScrollCreated(e.questId(), e.scrollCreated());
            }
        });
    }

    private static void handleSyncKills(SyncKills p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            for (KillEntry e : p.entries())
                QuestTracker.clientSetKill(e.entityId(), e.count());
        });
    }

    private static void handleSyncClear(SyncClear p, IPayloadContext ctx) {
        ctx.enqueueWork(QuestTracker::clientClearAll);
    }

    private static void handleToast(Toast p, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                QuestData.byId(p.questId()).ifPresent(q ->
                        QuestUnlockedToast.show(q.name, q.iconItem().orElse(null))
                )
        );
    }

    private static void handleOpenQuestBook(OpenQuestBook p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().isClientSide() && !Config.disableQuestBook()) {
                ClientOnly.openQuestBook();
            }
        });
    }

    private static void handleSyncQuestsChunk(SyncQuestsChunk p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientQuestSync.accept(p));
    }

    private static boolean questHasSubmit(QuestData.Quest q) {
        if (q == null || q.completion == null) return false;

        if ("submission".equalsIgnoreCase(q.type) || "submit".equalsIgnoreCase(q.type)) return true;

        for (QuestData.Target t : q.completion.targets) {
            if (isSubmitTarget(q, t)) return true;
        }
        return false;
    }

    private static boolean isSubmitTarget(QuestData.Quest q, QuestData.Target t) {
        if (t == null) return false;
        return "submit".equalsIgnoreCase(t.kind)
                || "xp".equalsIgnoreCase(t.kind)
                || (("submission".equalsIgnoreCase(q.type) || "submit".equalsIgnoreCase(q.type)) && t.isItem());
    }

    public static boolean claimQuest(ServerPlayer sp, QuestData.Quest q) {
        if (sp == null || q == null) return false;
        String lockKey = sp.getUUID() + ":" + q.id;
        if (!REDEEM_IN_FLIGHT.add(lockKey)) return false;
        try {
            QuestTracker.Status status = QuestTracker.getStatus(q, sp);
            if (status == QuestTracker.Status.REDEEMED || status == QuestTracker.Status.REJECTED) return false;
            if (!QuestTracker.isReady(q, sp)) return false;
            if (questHasSubmit(q) && !consumeSubmitTargets(sp, q)) return false;
            boolean ok;
            try {
                ok = QuestTracker.serverRedeem(q, sp);
            } catch (Throwable ignored) {
                ok = false;
            }
            if (!ok) return false;
            sendStatus(sp, q.id, QuestTracker.Status.REDEEMED.name());
            sendProgressMeta(sp, q.id);
            return true;
        } finally {
            REDEEM_IN_FLIGHT.remove(lockKey);
        }
    }

    private static boolean consumeSubmitTargets(ServerPlayer sp, QuestData.Quest q) {
        if (sp == null || q == null || q.completion == null) return false;

        Inventory inv = sp.getInventory();
        int size = inv.getContainerSize();
        ItemStack[] sim = new ItemStack[size];
        for (int i = 0; i < size; i++) sim[i] = inv.getItem(i).copy();
        QuestTracker.ExperienceSnapshot simulatedXp =
                new QuestTracker.ExperienceSnapshot(sp.experienceLevel, sp.experienceProgress);
        boolean hasXpSubmitTarget = false;

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;
            boolean submitTarget = isSubmitTarget(q, t);
            if (!submitTarget) continue;

            if (t.isXp()) {
                hasXpSubmitTarget = true;
                simulatedXp = QuestTracker.consumeExperience(simulatedXp, t.id, t.count);
                if (simulatedXp == null) return false;
                continue;
            }

            String raw = t.id;
            int need = Math.max(1, t.count);

            if (raw == null || raw.isBlank()) return false;

            if (raw.startsWith("#")) {
                ResourceLocation tagRl;
                try { tagRl = ResourceLocation.parse(raw.substring(1)); }
                catch (Exception ignored) { return false; }

                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagRl);

                if (!canTakeTag(sim, tag, need)) return false;
                if (!takeTag(sim, tag, need)) return false;

            } else {
                Item item;
                try { item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw)); }
                catch (Exception ignored) { item = null; }
                if (item == null) return false;

                if (!canTakeItem(sim, item, need)) return false;
                if (!takeItem(sim, item, need)) return false;
            }
        }

        for (QuestData.Target t : q.completion.targets) {
            if (t == null) continue;
            boolean submitTarget = isSubmitTarget(q, t);
            if (!submitTarget) continue;

            if (t.isXp()) continue;

            String raw = t.id;
            int need = Math.max(1, t.count);

            if (raw == null || raw.isBlank()) return false;

            boolean ok;
            if (raw.startsWith("#")) {
                ResourceLocation tagRl;
                try { tagRl = ResourceLocation.parse(raw.substring(1)); }
                catch (Exception ignored) { return false; }

                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagRl);
                ok = takeTag(inv, tag, need);
            } else {
                Item item;
                try { item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw)); }
                catch (Exception ignored) { item = null; }
                if (item == null) return false;
                ok = takeItem(inv, item, need);
            }

            if (!ok) return false;
        }

        if (hasXpSubmitTarget) {
            QuestTracker.setExperienceSnapshot(sp, simulatedXp);
        }
        inv.setChanged();
        sp.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean canTakeItem(ItemStack[] stacks, Item item, int needed) {
        int have = 0;
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty()) continue;
            if (!s.is(item)) continue;
            have += s.getCount();
            if (have >= needed) return true;
        }
        return have >= needed;
    }

    private static boolean canTakeTag(ItemStack[] stacks, TagKey<Item> tag, int needed) {
        int have = 0;
        for (ItemStack s : stacks) {
            if (s == null || s.isEmpty()) continue;
            if (!s.is(tag)) continue;
            have += s.getCount();
            if (have >= needed) return true;
        }
        return have >= needed;
    }

    private static boolean takeItem(ItemStack[] stacks, Item item, int toTake) {
        int remaining = toTake;
        for (int i = 0; i < stacks.length && remaining > 0; i++) {
            ItemStack s = stacks[i];
            if (s == null || s.isEmpty()) continue;
            if (!s.is(item)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) stacks[i] = ItemStack.EMPTY;
        }
        return remaining <= 0;
    }

    private static boolean takeTag(ItemStack[] stacks, TagKey<Item> tag, int toTake) {
        int remaining = toTake;
        for (int i = 0; i < stacks.length && remaining > 0; i++) {
            ItemStack s = stacks[i];
            if (s == null || s.isEmpty()) continue;
            if (!s.is(tag)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) stacks[i] = ItemStack.EMPTY;
        }
        return remaining <= 0;
    }

    private static boolean takeItem(Inventory inv, Item item, int toTake) {
        int remaining = toTake;
        int size = inv.getContainerSize();

        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!s.is(item)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
        }
        return remaining <= 0;
    }

    private static boolean takeTag(Inventory inv, TagKey<Item> tag, int toTake) {
        int remaining = toTake;
        int size = inv.getContainerSize();

        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!s.is(tag)) continue;

            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;

            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
        }
        return remaining <= 0;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void openQuestBook() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new net.revilodev.boundless.client.screen.StandaloneQuestBookScreen());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientQuestSync {
        private static int activeSyncId = -1;
        private static int expected = -1;
        private static byte[][] parts = null;
        private static int received = 0;

        private static void reset() {
            activeSyncId = -1;
            expected = -1;
            parts = null;
            received = 0;
        }

        private static void accept(SyncQuestsChunk p) {
            if (p == null) return;

            int sid = p.syncId();
            int total = p.totalParts();
            int idx = p.index();

            if (total <= 0 || total > 65536) { reset(); return; }
            if (idx < 0 || idx >= total) { reset(); return; }

            if (activeSyncId != sid || expected != total || parts == null) {
                activeSyncId = sid;
                expected = total;
                parts = new byte[total][];
                received = 0;
            }

            if (parts[idx] == null) {
                parts[idx] = p.part() == null ? new byte[0] : p.part();
                received++;
            }

            if (received >= expected) {
                int totalLen = 0;
                for (int i = 0; i < expected; i++) {
                    if (parts[i] == null) { reset(); return; }
                    totalLen += parts[i].length;
                }

                byte[] all = new byte[totalLen];
                int off = 0;
                for (int i = 0; i < expected; i++) {
                    byte[] b = parts[i];
                    System.arraycopy(b, 0, all, off, b.length);
                    off += b.length;
                }

                String json = new String(all, StandardCharsets.UTF_8);
                reset();
                QuestData.applyNetworkJson(json);
            }
        }
    }
}
