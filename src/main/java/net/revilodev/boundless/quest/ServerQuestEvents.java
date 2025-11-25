package net.revilodev.boundless.quest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ServerQuestEvents {
    private ServerQuestEvents() {
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        QuestProgressState state = QuestProgressState.get(sp.serverLevel());
        state.setDirty();
        sp.server.overworld().getDataStorage().save();
    }
}
