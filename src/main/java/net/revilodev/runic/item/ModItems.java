package net.revilodev.runic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.revilodev.runic.RunicMod;
import net.revilodev.runic.item.custom.RuneItem;

import java.util.List;

public class ModItems {
    // Create the item registry for this mod
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RunicMod.MOD_ID);

    public static final DeferredItem<Item> RUNE = ITEMS.register("rune",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.runic.blank_rune"));
                    super.appendHoverText(stack, ctx, tooltip, flag);
                }
            });

    public static final DeferredItem<Item> EXPANSION_RUNE = ITEMS.register("expansion_rune",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ENHANCED_RUNE = ITEMS.register("enhanced_rune",
            () -> new RuneItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
