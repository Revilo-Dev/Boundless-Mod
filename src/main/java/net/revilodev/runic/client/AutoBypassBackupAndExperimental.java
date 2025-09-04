package net.revilodev.runic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.revilodev.runic.RunicMod;

import java.util.Locale;


@EventBusSubscriber(modid = RunicMod.MOD_ID, value = Dist.CLIENT)
public final class AutoBypassBackupAndExperimental {
    private AutoBypassBackupAndExperimental() {}

    @SubscribeEvent
    public static void onInitPost(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        String name = s.getClass().getName();
        RunicMod.LOGGER.info("[Runic] Init.Post: {}", name);

        if (s instanceof ConfirmExperimentalFeaturesScreen
                || s instanceof BackupConfirmScreen
                || s instanceof GenericMessageScreen) {
            clickProceedButton(s);
        }
    }

    private static void clickProceedButton(Screen screen) {
        AbstractWidget candidate = null;
        int rightMost = Integer.MIN_VALUE;
        int widest = -1;

        for (Object o : screen.children()) {
            if (!(o instanceof Button w)) continue;

            String label = w.getMessage() == null ? "" : w.getMessage().getString();
            String low = label.toLowerCase(Locale.ROOT);

            boolean looksProceed =
                    low.contains("proceed") || low.contains("continue") || low.contains("ok") ||
                            low.contains("load")    || low.contains("join")     || low.contains("yes") ||
                            low.contains("skip");

            boolean looksCancel =
                    low.contains("back") || low.contains("cancel") || low.contains("abort")
                            || low.contains("no") || low.contains("previous");

            if (looksProceed && !looksCancel) {
                candidate = w;
                break;
            }

            int right = w.getX() + w.getWidth();
            if (right > rightMost || (right == rightMost && w.getWidth() > widest)) {
                rightMost = right;
                widest = w.getWidth();
                candidate = w;
            }
        }

        if (candidate != null) {
            AbstractWidget btn = candidate;
            Minecraft.getInstance().execute(() -> {
                double mx = btn.getX() + btn.getWidth() / 2.0;
                double my = btn.getY() + btn.getHeight() / 2.0;
                btn.mouseClicked(mx, my, 0);
                RunicMod.LOGGER.info("[Runic] Auto-clicked: '{}'",
                        btn.getMessage() == null ? "" : btn.getMessage().getString());
            });
        } else {
            RunicMod.LOGGER.warn("[Runic] No suitable button found to auto-click.");
        }
    }
}
