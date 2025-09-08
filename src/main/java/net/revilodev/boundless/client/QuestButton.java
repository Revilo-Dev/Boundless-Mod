package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class QuestButton {
    private static final WidgetSprites QUEST_BUTTON_SPRITES =
            new WidgetSprites(
                    ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_button.png"),
                    ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/quest_button_highlighted.png")
            );

    private static final QuestPanel QUEST_PANEL = new QuestPanel();

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen invScreen) {
            int leftPos = (invScreen.width - 176) / 2;
            int topPos = (invScreen.height - 166) / 2;

            QUEST_PANEL.init(leftPos, topPos);

            int x = leftPos + 147;
            int y = topPos + 61;

            ImageButton button = new ImageButton(
                    x, y, 20, 18,
                    QUEST_BUTTON_SPRITES,
                    btn -> QUEST_PANEL.toggle()
            );

            event.addListener(button);
        }
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof InventoryScreen) {
            QUEST_PANEL.render(event.getGuiGraphics());
        }
    }
}
