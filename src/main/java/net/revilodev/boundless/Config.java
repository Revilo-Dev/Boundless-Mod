package net.revilodev.boundless;

import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_CATEGORIES =
            BUILDER.comment("A list of quest category IDs to completely disable.")
                    .defineListAllowEmpty(List.of("disabledQuestCategories"), List::of, o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static List<? extends String> disabledCategories() {
        return DISABLED_CATEGORIES.get();
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading e) {
        if (e.getConfig().getSpec() == SPEC)
            BoundlessMod.LOGGER.info("[Boundless] Config loaded: {}", disabledCategories());
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() == SPEC)
            BoundlessMod.LOGGER.info("[Boundless] Config reloaded: {}", disabledCategories());
    }
}
