package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class QuestEvents {

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() == null) return;

        // Client-side: run the lightweight readiness sweep (no downgrades)
        if (e.getEntity().level().isClientSide) {
            QuestTracker.tickPlayer(e.getEntity());
            return;
        }

        // Server-side: upgrade to COMPLETED every 20 ticks (~1s), never downgrade
        if (e.getEntity() instanceof ServerPlayer sp) {
            if ((sp.tickCount % 20) == 0) {
                QuestTracker.serverCheckAndMarkComplete(sp);
            }
        }
    }
}
