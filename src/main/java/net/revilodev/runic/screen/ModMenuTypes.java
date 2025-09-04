package net.revilodev.runic.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.screen.custom.EtchingTableMenu;

public final class ModMenuTypes {
    private ModMenuTypes() {}

    // All menu types are registered here
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RunicMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EtchingTableMenu>> ETCHING_TABLE =
            MENUS.register("etching_table",
                    () -> new MenuType<>((id, inv, buf) -> {

                        BlockPos pos = FriendlyByteBuf.readBlockPos(buf);
                        return EtchingTableMenu.client(id, inv, pos);
                    })
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
