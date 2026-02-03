package net.revilodev.boundless.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class QuestBookKeybinds {
    private static final String CATEGORY = "key.categories.boundless";
    private static final String KEY_OPEN = "key.boundless.open_quest_book";
    private static KeyMapping openQuestBook;
    private static boolean registered = false;

    private QuestBookKeybinds() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (registered) return;
        registered = true;
        if (openQuestBook == null) {
            openQuestBook = new KeyMapping(KEY_OPEN, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY);
        }
        event.register(openQuestBook);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (openQuestBook == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        while (openQuestBook.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new net.revilodev.boundless.client.screen.StandaloneQuestBookScreen());
            }
        }
    }
}
