package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.revilodev.boundless.network.BoundlessNetwork;

public final class ServerQuestTicker {
    private ServerQuestTicker() {}

    // check once per second per player
    private static final int CHECK_INTERVAL_TICKS = 20;

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if ((sp.tickCount % CHECK_INTERVAL_TICKS) != 0) return;

        // Update statuses server-side and notify client on changes
        QuestTracker.serverTickPlayer(sp);

        // If you want to also keep kills/status snapshots fresh on reconnect only, do nothing else here.
        // Do NOT spam full syncPlayer every tick.
    }
}
