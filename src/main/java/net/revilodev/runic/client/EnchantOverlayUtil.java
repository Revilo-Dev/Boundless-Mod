package net.revilodev.runic.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.revilodev.runic.RunicMod;

public final class EnchantOverlayUtil {
    private EnchantOverlayUtil() {}

    public static final ResourceLocation ICON_WEAPON   =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/weapon.png");
    public static final ResourceLocation ICON_ARMOR    =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/armor.png");
    public static final ResourceLocation ICON_BOOTS    =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/boots.png");
    public static final ResourceLocation ICON_TOOL     =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/tool.png");
    public static final ResourceLocation ICON_BOW      =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/bow.png");
    public static final ResourceLocation ICON_CROSSBOW =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/crossbow.png");
    public static final ResourceLocation ICON_TRIDENT  =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/trident.png");
    public static final ResourceLocation ICON_ROD      =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/rod.png");
    public static final ResourceLocation ICON_SHIELD     =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/shield.png");
    public static final ResourceLocation ICON_STAR     =
            ResourceLocation.fromNamespaceAndPath(RunicMod.MOD_ID, "textures/gui/enchant/star.png");

    private static final int SLOT_W = 16, SLOT_H = 16;
    private static final int ICON_W = 6,  ICON_H = 6;

    /** Draw a 6x6 icon centered on a 16x16 item slot. */
    public static void drawCenteredIcon(GuiGraphics gg, ResourceLocation tex, int slotX, int slotY) {
        int x = slotX + (SLOT_W - ICON_W) / 2;
        int y = slotY + (SLOT_H - ICON_H) / 2;
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 200); // draw above the item
        gg.blit(tex, x, y, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
        gg.pose().popPose();
    }

    /** Returns the first icon that matches enchantments (item or stored) or null. */
    public static ResourceLocation pickIcon(ItemStack stack) {
        ResourceLocation icon = pickIconFrom(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        if (icon != null) return icon;
        return pickIconFrom(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
    }

    private static ResourceLocation pickIconFrom(ItemEnchantments ench) {
        if (ench.isEmpty()) return null;

        for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
            var key = e.getKey().unwrapKey().orElse(null);
            if (key == null) continue;
            String id = key.location().getPath();

            switch (id) {
                // Weapons
                case "sharpness", "smite", "bane_of_arthropods",
                     "knockback", "fire_aspect", "looting", "sweeping_edge", "breach":
                    return ICON_WEAPON;

                // Mace (1.21+)
                case "density", "wind_burst":
                    return ICON_SHIELD;

                // Tools
                case "efficiency", "silk_touch", "fortune":
                    return ICON_TOOL;

                // Armor (generic)
                case "protection", "blast_protection", "fire_protection",
                     "projectile_protection", "thorns":
                    return ICON_ARMOR;

                // Boots
                case "feather_falling", "depth_strider", "frost_walker",
                     "soul_speed", "swift_sneak":
                    return ICON_BOOTS;

                // Helmet (use dedicated helmet icon if you add one)
                case "aqua_affinity", "respiration":
                    return ICON_ARMOR;

                // Bow
                case "power", "flame", "infinity", "punch":
                    return ICON_BOW;

                // Crossbow
                case "multishot", "piercing", "quick_charge":
                    return ICON_CROSSBOW;

                // Trident
                case "channeling", "loyalty", "riptide", "impaling":
                    return ICON_TRIDENT;

                // Fishing Rod
                case "luck_of_the_sea", "lure":
                    return ICON_ROD;

                // Broad utility / curses
                case "unbreaking", "mending", "binding_curse", "vanishing_curse":
                    return ICON_STAR;
            }
        }
        return null;
    }
}
