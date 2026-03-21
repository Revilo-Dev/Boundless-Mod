package net.revilodev.boundless.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.revilodev.boundless.BoundlessMod;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BoundlessMod.MOD_ID);

    public static final DeferredItem<Item> QUEST_BOOK =
            ITEMS.registerItem("quest_book", QuestBookItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> QUEST_COMPLETION_SCROLL =
            ITEMS.registerItem("quest_completion_scroll", QuestCompletionScrollItem::new, new Item.Properties().stacksTo(1));

    public static ItemStack createQuestScroll(String questId) {
        return QuestCompletionScrollItem.withQuestId(new ItemStack(QUEST_COMPLETION_SCROLL.get()), questId);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
