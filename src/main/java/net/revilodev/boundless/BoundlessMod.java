package net.revilodev.boundless;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.revilodev.boundless.client.QuestPanelClient;
import net.revilodev.boundless.command.BoundlessCommands;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.KillCounterState;
import org.slf4j.Logger;

import java.util.List;

@Mod(BoundlessMod.MOD_ID)
public class BoundlessMod {
    public static final String MOD_ID = "boundless";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BoundlessMod(ModContainer modContainer, IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(QuestPanelClient::onMouseScrolled);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {}

    private void addCreative(BuildCreativeModeTabContentsEvent event) {}

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BoundlessCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            BoundlessNetwork.syncPlayer(sp);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel server)) return;
        ResourceLocation rl = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (rl == null) return;
        KillCounterState.get(server).inc(sp.getUUID(), rl.toString());
        var count = KillCounterState.get(server).get(sp.getUUID(), rl.toString());
        var entry = new BoundlessNetwork.KillEntry(rl.toString(), count);
        BoundlessNetwork.SyncKillsPayload payload = new BoundlessNetwork.SyncKillsPayload(List.of(entry));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, payload);
    }
}
