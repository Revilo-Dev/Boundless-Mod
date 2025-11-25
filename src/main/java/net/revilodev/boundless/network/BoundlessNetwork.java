package net.revilodev.boundless.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.boundless.client.toast.QuestUnlockedToast;
import net.revilodev.boundless.quest.KillCounterState;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestProgressState;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.ArrayList;
import java.util.List;

public final class BoundlessNetwork {

    private static final String CHANNEL = "boundless";
    private static final String VERSION = "1";
    private static boolean REGISTERED = false;

    private static final Gson GSON = new GsonBuilder().setLenient().create();

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

        r.playToClient(SyncStatus.TYPE, SyncStatus.CODEC, BoundlessNetwork::handleSyncStatus);
        r.playToClient(SyncKills.TYPE, SyncKills.CODEC, BoundlessNetwork::handleSyncKills);
        r.playToClient(SyncClear.TYPE, SyncClear.CODEC, BoundlessNetwork::handleSyncClear);
        r.playToClient(Toast.TYPE, Toast.CODEC, BoundlessNetwork::handleToast);
        r.playToClient(OpenQuestBook.TYPE, OpenQuestBook.CODEC, BoundlessNetwork::handleOpenQuestBook);
        r.playToClient(SyncQuests.TYPE, SyncQuests.CODEC, BoundlessNetwork::handleSyncQuests);
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

    public record SyncQuests(String json) implements CustomPacketPayload {
        public static final Type<SyncQuests> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "sync_quests"));
        public static final StreamCodec<FriendlyByteBuf, SyncQuests> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.json),
                buf -> new SyncQuests(buf.readUtf())
        );
        @Override public Type<SyncQuests> type() { return TYPE; }
    }

    public static void syncPlayer(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new SyncClear());
        sendQuestData(p);

        KillCounterState.get(p.serverLevel()).snapshotFor(p.getUUID())
                .forEach((id, ct) -> PacketDistributor.sendToPlayer(
                        p, new SyncKills(List.of(new KillEntry(id, ct)))
                ));

        QuestProgressState.get(p.serverLevel()).snapshotFor(p.getUUID())
                .forEach((questId, status) -> PacketDistributor.sendToPlayer(
                        p, new SyncStatus(questId, status)
                ));
    }

    private static void sendQuestData(ServerPlayer p) {
        var quests = QuestData.allServer(p.server);
        var categories = QuestData.categoriesOrderedServer(p.server);

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
                ro.addProperty("command", q.rewards.command);
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

            qs.add(o);
        }

        root.add("quests", qs);

        String json = GSON.toJson(root);
        PacketDistributor.sendToPlayer(p, new SyncQuests(json));
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
                if (QuestTracker.isReady(q, sp)) {
                    boolean ok = QuestTracker.serverRedeem(q, sp);
                    if (ok) {
                        if (q.rewards.hasExp()) {
                            if ("points".equals(q.rewards.expType)) sp.giveExperiencePoints(q.rewards.expAmount);
                            else if ("levels".equals(q.rewards.expType)) sp.giveExperienceLevels(q.rewards.expAmount);
                        }
                        sendStatus(sp, q.id, QuestTracker.Status.REDEEMED.name());
                    }
                }
            });
        });
    }

    private static void handleReject(Reject p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = (ServerPlayer) ctx.player();
            QuestData.byIdServer(sp.server, p.questId()).ifPresent(q -> {
                if (QuestTracker.serverReject(q, sp))
                    sendStatus(sp, q.id, QuestTracker.Status.REJECTED.name());
            });
        });
    }

    private static void handleSyncStatus(SyncStatus p, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                QuestTracker.clientSetStatus(p.questId(), QuestTracker.Status.valueOf(p.status()))
        );
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
            if (ctx.player().level().isClientSide()) {
                ClientOnly.openQuestBook();
            }
        });
    }

    private static void handleSyncQuests(SyncQuests p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> QuestData.applyNetworkJson(p.json()));
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private static void openQuestBook() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new net.revilodev.boundless.client.screen.StandaloneQuestBookScreen());
        }
    }

}
