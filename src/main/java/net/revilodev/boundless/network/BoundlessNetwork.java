package net.revilodev.boundless.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.boundless.client.toast.QuestUnlockedToast;
import net.revilodev.boundless.quest.KillCounterState;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;
import net.revilodev.boundless.quest.QuestWorldState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BoundlessNetwork {
    private BoundlessNetwork() {}

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener(BoundlessNetwork::register);
    }

    // --------------------- PAYLOADS ---------------------

    public record RedeemPayload(String questId) implements CustomPacketPayload {
        public static final Type<RedeemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "redeem"));
        public static final StreamCodec<ByteBuf, RedeemPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RedeemPayload::questId, RedeemPayload::new
        );
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RejectPayload(String questId) implements CustomPacketPayload {
        public static final Type<RejectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "reject"));
        public static final StreamCodec<ByteBuf, RejectPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RejectPayload::questId, RejectPayload::new
        );
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncStatusPayload(String questId, String status) implements CustomPacketPayload {
        public static final Type<SyncStatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "quest_sync"));
        public static final StreamCodec<ByteBuf, SyncStatusPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SyncStatusPayload::questId,
                ByteBufCodecs.STRING_UTF8, SyncStatusPayload::status,
                SyncStatusPayload::new
        );
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record KillEntry(String entityId, int count) {
        public static final StreamCodec<ByteBuf, KillEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, KillEntry::entityId,
                ByteBufCodecs.INT, KillEntry::count,
                KillEntry::new
        );
    }

    public record SyncKillsPayload(List<KillEntry> entries) implements CustomPacketPayload {
        public static final Type<SyncKillsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "kills_sync"));
        public static final StreamCodec<ByteBuf, SyncKillsPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.collection(ArrayList::new, KillEntry.STREAM_CODEC), SyncKillsPayload::entries,
                SyncKillsPayload::new
        );
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncClearPayload() implements CustomPacketPayload {
        public static final Type<SyncClearPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "quest_clear"));
        public static final StreamCodec<ByteBuf, SyncClearPayload> STREAM_CODEC = StreamCodec.unit(new SyncClearPayload());
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ToastPayload(String questId) implements CustomPacketPayload {
        public static final Type<ToastPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "toast"));
        public static final StreamCodec<ByteBuf, ToastPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ToastPayload::questId, ToastPayload::new
        );
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // --------------------- REGISTRATION ---------------------

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1").executesOn(HandlerThread.MAIN);
        reg.playToServer(RedeemPayload.TYPE, RedeemPayload.STREAM_CODEC, BoundlessNetwork::handleRedeem);
        reg.playToServer(RejectPayload.TYPE, RejectPayload.STREAM_CODEC, BoundlessNetwork::handleReject);
        reg.playToClient(SyncStatusPayload.TYPE, SyncStatusPayload.STREAM_CODEC, BoundlessNetwork::handleSyncStatus);
        reg.playToClient(SyncKillsPayload.TYPE, SyncKillsPayload.STREAM_CODEC, BoundlessNetwork::handleSyncKills);
        reg.playToClient(SyncClearPayload.TYPE, SyncClearPayload.STREAM_CODEC, BoundlessNetwork::handleSyncClear);
        reg.playToClient(ToastPayload.TYPE, ToastPayload.STREAM_CODEC, BoundlessNetwork::handleToast);
    }

    // --------------------- SYNC LOGIC ---------------------

    public static void syncPlayer(ServerPlayer player) {

        syncClear(player);

        QuestWorldState qs = QuestWorldState.get(player.serverLevel());
        for (Map.Entry<String, String> e : qs.snapshot().entrySet()) {
            PacketDistributor.sendToPlayer(player, new SyncStatusPayload(e.getKey(), e.getValue()));
        }

        Map<String, Integer> kills = KillCounterState.get(player.serverLevel()).snapshotFor(player.getUUID());
        List<KillEntry> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : kills.entrySet()) list.add(new KillEntry(e.getKey(), e.getValue()));
        if (!list.isEmpty()) PacketDistributor.sendToPlayer(player, new SyncKillsPayload(list));
    }

    public static void syncClear(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncClearPayload());
    }

    public static void sendToastTo(ServerPlayer player, String questId) {
        PacketDistributor.sendToPlayer(player, new ToastPayload(questId));
    }

    // --------------------- HANDLERS ---------------------

    private static void handleRedeem(final RedeemPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            QuestData.byIdServer(sp.server, payload.questId()).ifPresent(q -> {
                if (!QuestTracker.isReady(q, sp)) return;
                boolean changed = QuestTracker.completeAndRedeem(q, sp);
                if (changed) {
                    PacketDistributor.sendToPlayer(sp,
                            new SyncStatusPayload(q.id, QuestTracker.Status.REDEEMED.name()));
                }
            });
        });
    }

    private static void handleReject(final RejectPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            QuestData.byIdServer(sp.server, payload.questId()).ifPresent(q -> {
                if (QuestTracker.reject(q, sp)) {
                    PacketDistributor.sendToPlayer(sp, new SyncStatusPayload(q.id, QuestTracker.Status.REJECTED.name()));
                }
            });
        });
    }

    private static void handleSyncStatus(final SyncStatusPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try { QuestTracker.clientSetStatus(payload.questId(), QuestTracker.Status.valueOf(payload.status())); }
            catch (IllegalArgumentException ignored) {}
        });
    }

    private static void handleSyncKills(final SyncKillsPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            for (KillEntry e : payload.entries()) QuestTracker.clientSetKill(e.entityId(), e.count());
        });
    }

    private static void handleSyncClear(final SyncClearPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(QuestTracker::clientClearAll);
    }

    private static void handleToast(final ToastPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            QuestData.byId(payload.questId()).ifPresent(q -> {
                QuestUnlockedToast.show(q.name, q.iconItem().orElse(null));
            });
        });
    }
}
