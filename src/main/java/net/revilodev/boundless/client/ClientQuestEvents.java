package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

public final class ClientQuestEvents {

    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn e) {
        QuestData.loadClient(true);
        QuestTracker.setClientMultiplayer(!Minecraft.getInstance().hasSingleplayerServer());
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        QuestTracker.forceSave();
        QuestTracker.setClientMultiplayer(false);
    }

    public static void onClientLevelUnload(LevelEvent.Unload e) {
        if (!e.getLevel().isClientSide()) return;
        QuestTracker.forceSave();
        QuestTracker.setClientMultiplayer(false);
    }
}
