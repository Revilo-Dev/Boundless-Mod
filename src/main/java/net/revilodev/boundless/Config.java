package net.revilodev.boundless;

import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_CATEGORIES =
            BUILDER.comment("A list of quest category IDs to completely disable.")
                    .defineListAllowEmpty(List.of("disabledQuestCategories"), List::of, o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    static {
        // 1.20.1 requires explicit registration to the event bus
        NeoForge.EVENT_BUS.register(Config.class);
    }

    public static List<? extends String> disabledCategories() {
        return DISABLED_CATEGORIES.get();
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            BoundlessMod.LOGGER.info("[Boundless] Config loaded: {}", disabledCategories());
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            BoundlessMod.LOGGER.info("[Boundless] Config reloaded: {}", disabledCategories());
        }
    }
}
