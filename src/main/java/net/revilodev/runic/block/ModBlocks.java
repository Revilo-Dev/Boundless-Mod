package net.revilodev.runic.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.block.custom.EtchingTable;

public final class ModBlocks {
    private ModBlocks() {}

    // Register for all blocks
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, RunicMod.MOD_ID);

    // Register for all items (block items, misc items go here if you want)
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, RunicMod.MOD_ID);

    // ---------- Blocks ----------

    public static final DeferredHolder<Block, EtchingTable> ETCHING_TABLE =
            BLOCKS.register("etching_table",
                    () -> new EtchingTable(BlockBehaviour.Properties.of()
                            .strength(5.0F)
                            .sound(SoundType.STONE)));

    // ---------- Block Items ----------

    public static final DeferredHolder<Item, BlockItem> ETCHING_TABLE_ITEM =
            ITEMS.register("etching_table",
                    () -> new BlockItem(ETCHING_TABLE.get(), new Item.Properties()));

    // ---------- Registry hook ----------

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
