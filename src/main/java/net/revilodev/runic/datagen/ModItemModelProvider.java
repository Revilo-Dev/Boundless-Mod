package net.revilodev.runic.datagen;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.revilodev.runic.RunicMod;
import net.revilodev.runic.item.ModItems;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, RunicMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // DeferredItem<Item> is ItemLike in NeoForge → no .get()
        basicItem(ModItems.RUNE);
        basicItem(ModItems.EXPANSION_RUNE);
    }
}
