package net.revilodev.boundless.client.toast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class QuestUnlockedToast implements Toast {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_toast.png");
    private final Component title;
    private final Component subtitle;
    private final Item icon;

    public QuestUnlockedToast(Component title, Component subtitle, Item icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }

    public static void show(String questName, Item icon) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.getToasts().addToast(new QuestUnlockedToast(
                Component.translatable("toast.boundless.quest_unlocked"),
                Component.literal(questName),
                icon
        ));
    }

    public Visibility render(GuiGraphics gg, ToastComponent component, long time) {
        gg.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height(), this.width(), this.height());
        if (icon != null) gg.renderItem(new ItemStack(icon), 6, 6);
        gg.drawString(Minecraft.getInstance().font, title, 30, 7, 0xFFFFFF, false);
        gg.drawString(Minecraft.getInstance().font, subtitle, 30, 18, 0xD0D0D0, false);
        return time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

    public int width() { return 160; }
    public int height() { return 32; }
}
