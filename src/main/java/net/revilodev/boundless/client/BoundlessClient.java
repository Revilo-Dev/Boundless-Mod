package net.revilodev.boundless.client;

import net.neoforged.neoforge.common.NeoForge;

public final class BoundlessClient {
    private BoundlessClient() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onMouseScrolled);
    }
}
