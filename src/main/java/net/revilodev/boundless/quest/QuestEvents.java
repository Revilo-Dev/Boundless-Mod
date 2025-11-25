package net.revilodev.boundless.quest;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class QuestEvents {

    private QuestEvents() {}

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        Player player = e.getEntity();
        if (player == null || !player.level().isClientSide) return;
        QuestTracker.tickPlayer(player);
    }
}
