package net.revilodev.runic.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.screen.custom.EtchingTableMenu;


public final class ModMenuTypes {
    private ModMenuTypes() {}

    // Deferred register for all menu types
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, RunicMod.MOD_ID);

    // Etching Table menu registration
    public static final DeferredHolder<MenuType<?>, MenuType<EtchingTableMenu>> ETCHING_TABLE =
            MENUS.register("etching_table",
                    () -> new MenuType<>(
                            (int id, Inventory inv) -> EtchingTableMenu.client(id, inv, inv.player.blockPosition()),
                            FeatureFlags.VANILLA_SET
                    )
            );

    // Hook into mod event bus
    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
