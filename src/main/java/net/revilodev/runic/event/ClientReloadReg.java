package net.revilodev.runic.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import net.revilodev.runic.RunicMod;
import net.revilodev.runic.loot.rarity.EnhancementRarityClientReloadListener;

@EventBusSubscriber(modid = RunicMod.MOD_ID, value = Dist.CLIENT)
public class ClientReloadReg {
    @SubscribeEvent
    public static void onRegisterClientReloaders(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener(new EnhancementRarityClientReloadListener());
    }
}
