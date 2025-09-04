// ModCreativeModeTabs.java
package net.revilodev.runic.item;

import net.revilodev.runic.RunicMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.revilodev.runic.block.ModBlocks;
import net.revilodev.runic.item.custom.RuneItem;

public class ModCreativeModeTabs {
    // ✅ NeoForge creative tab registry
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RunicMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RUNIC_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("runic_items_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.ETCHING_TABLE.get()))
                    .title(Component.translatable("creativetab.runicmod.runic_items"))
                    .displayItems((params, output) -> {
                        // Add base items/blocks
                        output.accept(ModBlocks.ETCHING_TABLE.get());
                        output.accept(ModItems.RUNE.get());
                        output.accept(ModItems.EXPANSION_RUNE.get());

                        // Add runes for every enchantment and level
                        params.holders()
                                .lookup(Registries.ENCHANTMENT)
                                .ifPresent((HolderLookup.RegistryLookup<Enchantment> enchants) -> {
                                    enchants.listElements().forEach(holder -> {
                                        Enchantment ench = holder.value();
                                        int min = Math.max(1, ench.getMinLevel());
                                        int max = Math.max(min, ench.getMaxLevel());
                                        for (int lvl = min; lvl <= max; ++lvl) {
                                            ItemStack s = RuneItem.createForEnchantment(new EnchantmentInstance(holder, lvl));
                                            if (!s.isEmpty()) {
                                                output.accept(s);
                                            }
                                        }
                                    });
                                });
                    })
                    .build()
            );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
