package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ServerQuestEvents {

    private ServerQuestEvents() {}

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        ServerLevel level = sp.serverLevel();
        QuestProgressState state = QuestProgressState.get(level);
        state.setDirty();
        level.getServer().overworld().getDataStorage().save();
    }
}
