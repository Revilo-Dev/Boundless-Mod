package net.revilodev.boundless.quest;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class QuestEvents {
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() == null) return;
        if (!e.getEntity().level().isClientSide) return;
        QuestTracker.tickPlayer(e.getEntity());
    }
}
