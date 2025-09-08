package net.revilodev.boundless;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.revilodev.boundless.client.QuestButton;
import org.slf4j.Logger;

@Mod(BoundlessMod.MOD_ID)
public class BoundlessMod {
    public static final String MOD_ID = "boundless";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BoundlessMod(ModContainer modContainer, IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuestButton::onScreenInit);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuestButton::onScreenRender);

        // global events
        NeoForge.EVENT_BUS.register(this);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {}



    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // server start logic
    }

    public static class ClientModEvents {

    }
}
