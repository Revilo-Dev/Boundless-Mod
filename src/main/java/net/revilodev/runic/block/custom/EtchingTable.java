package net.revilodev.runic.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.revilodev.runic.screen.custom.EtchingTableMenu;

public class EtchingTable extends Block {
    public EtchingTable(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, ply) -> EtchingTableMenu.server(id, inv, level, pos),
                    Component.translatable("block.runic.etching_table")
            );
            sp.openMenu(provider, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
