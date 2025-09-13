package net.revilodev.boundless.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

public final class BoundlessNetwork {
    public record RedeemPayload(String questId) implements CustomPacketPayload {
        public static final Type<RedeemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("boundless", "redeem"));
        public static final StreamCodec<ByteBuf, RedeemPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RedeemPayload::questId,
                RedeemPayload::new
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

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1").executesOn(HandlerThread.MAIN);
        reg.playToServer(RedeemPayload.TYPE, RedeemPayload.STREAM_CODEC, BoundlessNetwork::handleRedeem);
        reg.playToClient(SyncStatusPayload.TYPE, SyncStatusPayload.STREAM_CODEC, BoundlessNetwork::handleSync);
    }

    private static void handleRedeem(final RedeemPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            QuestData.byIdServer(sp.server, payload.questId()).ifPresent(q -> {
                if (!QuestTracker.isReady(q, sp)) return;
                boolean changed = QuestTracker.completeAndRedeem(q, sp);
                if (changed) {
                    for (var p : sp.server.getPlayerList().getPlayers()) {
                        PacketDistributor.sendToPlayer(p, new SyncStatusPayload(q.id, QuestTracker.Status.REDEEMED.name()));
                    }
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
