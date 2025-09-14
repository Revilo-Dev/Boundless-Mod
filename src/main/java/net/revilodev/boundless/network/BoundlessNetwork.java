package net.revilodev.boundless.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;
import net.revilodev.boundless.quest.QuestWorldState;

import java.util.Map;

@EventBusSubscriber(modid = "boundless")
public final class BoundlessNetwork {
    private static boolean REGISTERED = false;

    public record RedeemPayload(String questId) implements CustomPacketPayload {
        public static final Type<RedeemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "redeem"));
        public static final StreamCodec<ByteBuf, RedeemPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RedeemPayload::questId,
                RedeemPayload::new
        );
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RejectPayload(String questId) implements CustomPacketPayload {
        public static final Type<RejectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "reject"));
        public static final StreamCodec<ByteBuf, RejectPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RejectPayload::questId,
                RejectPayload::new
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

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        if (REGISTERED) return;
        REGISTERED = true;
        PayloadRegistrar reg = event.registrar("1").executesOn(HandlerThread.MAIN);
        reg.playToServer(RedeemPayload.TYPE, RedeemPayload.STREAM_CODEC, BoundlessNetwork::handleRedeem);
        reg.playToServer(RejectPayload.TYPE, RejectPayload.STREAM_CODEC, BoundlessNetwork::handleReject);
        reg.playToClient(SyncStatusPayload.TYPE, SyncStatusPayload.STREAM_CODEC, BoundlessNetwork::handleSync);
    }

    public static void syncPlayer(ServerPlayer player) {
        QuestWorldState st = QuestWorldState.get(player.serverLevel());
        for (Map.Entry<String, String> e : st.snapshot().entrySet()) {
            try {
                QuestTracker.Status status = QuestTracker.Status.valueOf(e.getValue());
                PacketDistributor.sendToPlayer(player, new SyncStatusPayload(e.getKey(), status.name()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static void handleRedeem(final RedeemPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            QuestData.byIdServer(sp.server, payload.questId()).ifPresent(q -> {
                if (!QuestTracker.isReady(q, sp)) return;
                if (!QuestTracker.dependenciesMet(q, sp)) return;
                boolean changed = QuestTracker.completeAndRedeem(q, sp);
                if (changed) {
                    PacketDistributor.sendToPlayer(sp, new SyncStatusPayload(q.id, QuestTracker.Status.REDEEMED.name()));
                }
            });
        });
    }

    private static void handleReject(final RejectPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            QuestData.byIdServer(sp.server, payload.questId()).ifPresent(q -> {
                boolean changed = QuestTracker.reject(q, sp);
                if (changed) {
                    PacketDistributor.sendToPlayer(sp, new SyncStatusPayload(q.id, QuestTracker.Status.REJECTED.name()));
                }
            });
        });
    }

    private static void handleSync(final SyncStatusPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                QuestTracker.clientSetStatus(payload.questId(), QuestTracker.Status.valueOf(payload.status()));
            } catch (IllegalArgumentException ignored) {}
        });
    }
}
