package net.revilodev.runic;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.revilodev.runic.Enhancements.ModEnhancementEffects;
import net.revilodev.runic.block.ModBlocks;
import net.revilodev.runic.client.RunicClientModels;
import net.revilodev.runic.item.ModCreativeModeTabs;
import net.revilodev.runic.item.ModItems;
import net.revilodev.runic.loot.ModLootModifiers;
import net.revilodev.runic.registry.ModDataComponents;
import net.revilodev.runic.screen.ModMenuTypes;
import net.revilodev.runic.screen.custom.EtchingTableScreen;
import org.slf4j.Logger;

@Mod(RunicMod.MOD_ID)
public class RunicMod {
    public static final String MOD_ID = "runic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RunicMod(ModContainer modContainer, IEventBus modEventBus) {
        // lifecycle listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        // client-only listeners
        modEventBus.addListener(ClientModEvents::onRegisterScreens);
        modEventBus.addListener(ClientModEvents::onClientSetup);

        // deferred registers
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        ModEnhancementEffects.register(modEventBus);

        // global events
        NeoForge.EVENT_BUS.register(this);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {}



    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RUNE.get());
            event.accept(ModItems.EXPANSION_RUNE.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ETCHING_TABLE.get());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // server start logic
    }

    public static class ClientModEvents {
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.ETCHING_TABLE.get(), EtchingTableScreen::new);
        }
        public static void onClientSetup(FMLClientSetupEvent event) {
            RunicClientModels.init();
        }

    }
}
