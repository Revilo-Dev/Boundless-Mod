package net.revilodev.boundless.client;

import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.revilodev.boundless.quest.QuestTracker;

public final class ClientQuestEvents {
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        QuestTracker.forceSave();
    }

    public static void onClientLevelUnload(LevelEvent.Unload e) {
        if (!e.getLevel().isClientSide()) return;
        QuestTracker.forceSave();
    }
}
