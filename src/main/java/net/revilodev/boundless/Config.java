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

    static {
        BUILDER.push("UI");
    }
    public static final ModConfigSpec.ConfigValue<String> PINNED_QUEST_HUD_POSITION =

            BUILDER.comment("Pin the Quest hud to the: bottom_left, bottom_right, top_left, top_right")
                    .define("pinnedQuestHudPosition", "bottom_left", o -> {
                if (!(o instanceof String s)) return false;
                s = s.trim().toLowerCase();
                return s.equals("top_left") || s.equals("top_right") || s.equals("bottom_left") || s.equals("bottom_right");
            });

    public static final ModConfigSpec.ConfigValue<Boolean> HIDE_QUEST_BOOK_IN_INVENTORY =
            BUILDER.comment("If true, hides the quest book button in the inventory screen.")
                    .define("hideQuestBookInInventory", false);
    public static final ModConfigSpec.ConfigValue<String> QUEST_BOOK_INVENTORY_BUTTON_POSITION =
            BUILDER.comment("Quest book button position in inventory: beside_recipe_book, above_offhand_slot")
                    .define("questBookInventoryButtonPosition", "beside_recipe_book", o -> {
                        if (!(o instanceof String s)) return false;
                        s = s.trim().toLowerCase();
                        return s.equals("beside_recipe_book") || s.equals("above_offhand_slot");
                    });
    public static final ModConfigSpec.ConfigValue<Boolean> CENTER_INVENTORY_WITH_QUEST_PANEL =
            BUILDER.comment("If true, centers inventory and quest panel together when the quest panel is open.")
                    .define("centerInventoryWithQuestPanel", true);
    public static final ModConfigSpec.ConfigValue<Boolean> HIDE_CATEGORY_HEADER =
            BUILDER.comment("If true, hides the category header bar.")
                    .define("hideCategoryHeader", false);
    public static final ModConfigSpec.ConfigValue<String> FILTER_DISPLAY_MODE =
            BUILDER.comment("How quest filters are displayed: tabs, buttons, hidden.")
                    .define("filterDisplayMode", "tabs", o -> {
                        if (!(o instanceof String s)) return false;
                        s = s.trim().toLowerCase();
                        return s.equals("tabs") || s.equals("buttons") || s.equals("hidden");
                    });
    public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_CATEGORIES =
            BUILDER.comment("If true, disables category tabs and category-based filtering.")
                    .define("disableCategories", false);
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_BUILTIN_QUEST_PACK =
            BUILDER.comment("If false, disables the built-in Boundless quest pack.")
                    .define("enableBuiltinQuestPack", true);
    public static final ModConfigSpec.ConfigValue<Boolean> HIDE_QUEST_WIDGET_ICONS =
            BUILDER.comment("If true, hides icons in quest list widgets.")
                    .define("hideQuestWidgetIcons", false);
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_QUEST_SEARCH_BOX =
            BUILDER.comment("If true, shows the quest search box above the quest list.")
                    .define("enableQuestSearchBox", false);
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_DESCRIPTION_COLORS =
            BUILDER.comment("If true, allows colored quest descriptions to render with Boundless color tokens.")
                    .define("enableDescriptionColors", false);
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_QUEST_TOASTS =
            BUILDER.comment("If true, shows quest unlocked toasts.")
                    .define("enableQuestToasts", true);
    static {
        BUILDER.pop();
        BUILDER.push("Functionality");
    }
    public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_QUEST_PINNING =
            BUILDER.comment("If true, quest pinning and pinned HUD are disabled.")
                    .define("disableQuestPinning", false);
    public static final ModConfigSpec.ConfigValue<Boolean> AUTO_CLAIM_QUEST_REWARDS =
            BUILDER.comment("If true, quest rewards are automatically claimed when a quest becomes complete.")
                    .define("autoClaimQuestRewards", false);
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_QUEST_SCROLLS =
            BUILDER.comment("If true, quest completion scrolls can be created and used.")
                    .define("enableQuestScrolls", true);
    static {
        BUILDER.pop();
        BUILDER.push("Gameplay");
    }
    public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_QUEST_BOOK =
            BUILDER.comment("If true, quest book opening is disabled.")
                    .define("disableQuestBook", false);
    public static final ModConfigSpec.ConfigValue<Boolean> SPAWN_WITH_QUEST_BOOK =
            BUILDER.comment("If true, players spawn with the quest book.")
                    .define("spawnWithQuestBook", false);
    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static List<? extends String> disabledCategories() {
        return DISABLED_CATEGORIES.get();
    }

    public static String pinnedQuestHudPosition() {
        String s = PINNED_QUEST_HUD_POSITION.get();
        if (s == null) return "bottom_left";
        s = s.trim().toLowerCase();
        return s.isBlank() ? "bottom_left" : s;
    }

    public static boolean spawnWithQuestBook() {
        return SPAWN_WITH_QUEST_BOOK.get();
    }

    public static boolean hideQuestBookInInventory() {
        return HIDE_QUEST_BOOK_IN_INVENTORY.get();
    }

    public static String questBookInventoryButtonPosition() {
        String s = QUEST_BOOK_INVENTORY_BUTTON_POSITION.get();
        if (s == null) return "beside_recipe_book";
        s = s.trim().toLowerCase();
        return (s.equals("beside_recipe_book") || s.equals("above_offhand_slot")) ? s : "beside_recipe_book";
    }

    public static boolean centerInventoryWithQuestPanel() {
        return CENTER_INVENTORY_WITH_QUEST_PANEL.get();
    }

    public static boolean hideCategoryHeader() {
        return HIDE_CATEGORY_HEADER.get();
    }

    public static String filterDisplayMode() {
        String s = FILTER_DISPLAY_MODE.get();
        if (s == null) return "tabs";
        s = s.trim().toLowerCase();
        return (s.equals("tabs") || s.equals("buttons") || s.equals("hidden")) ? s : "tabs";
    }

    public static boolean displayFiltersAsTabs() {
        return "tabs".equals(filterDisplayMode());
    }

    public static boolean displayFiltersAsButtons() {
        return "buttons".equals(filterDisplayMode());
    }

    public static boolean hideFilters() {
        return "hidden".equals(filterDisplayMode());
    }

    public static boolean disableCategories() {
        return DISABLE_CATEGORIES.get();
    }

    public static boolean hideQuestWidgetIcons() {
        return HIDE_QUEST_WIDGET_ICONS.get();
    }

    public static boolean enableBuiltinQuestPack() {
        return ENABLE_BUILTIN_QUEST_PACK.get();
    }

    public static boolean enableQuestSearchBox() {
        return ENABLE_QUEST_SEARCH_BOX.get();
    }

    public static boolean enableDescriptionColors() {
        return ENABLE_DESCRIPTION_COLORS.get();
    }

    public static boolean enableQuestToasts() {
        return ENABLE_QUEST_TOASTS.get();
    }

    public static boolean disableQuestPinning() {
        return DISABLE_QUEST_PINNING.get();
    }

    public static boolean autoClaimQuestRewards() {
        return AUTO_CLAIM_QUEST_REWARDS.get();
    }

    public static boolean enableQuestScrolls() {
        return ENABLE_QUEST_SCROLLS.get();
    }

    public static boolean disableQuestBook() {
        return DISABLE_QUEST_BOOK.get();
    }

    // Backward-compatible accessor used by existing callers.
    public static boolean hideQuestBookToggle() {
        return hideQuestBookInInventory();
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading e) {
        if (e.getConfig().getSpec() == SPEC)
            BoundlessMod.LOGGER.info("[Boundless] Config loaded: categories={}, pos={}, hideInvBtn={}, invBtnPos={}, centerInv={}, hideHeader={}, filterMode={}, disableCategories={}, builtinPack={}, hideWidgetIcons={}, searchBox={}, descColors={}, questToasts={}, disablePinning={}, autoClaim={}, questScrolls={}, disableBook={}, spawnBook={}",
                    disabledCategories(),
                    pinnedQuestHudPosition(),
                    hideQuestBookInInventory(),
                    questBookInventoryButtonPosition(),
                    centerInventoryWithQuestPanel(),
                    hideCategoryHeader(),
                    filterDisplayMode(),
                    disableCategories(),
                    enableBuiltinQuestPack(),
                    hideQuestWidgetIcons(),
                    enableQuestSearchBox(),
                    enableDescriptionColors(),
                    enableQuestToasts(),
                    disableQuestPinning(),
                    autoClaimQuestRewards(),
                    enableQuestScrolls(),
                    disableQuestBook(),
                    spawnWithQuestBook());
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() == SPEC)
            BoundlessMod.LOGGER.info("[Boundless] Config reloaded: categories={}, pos={}, hideInvBtn={}, invBtnPos={}, centerInv={}, hideHeader={}, filterMode={}, disableCategories={}, builtinPack={}, hideWidgetIcons={}, searchBox={}, descColors={}, questToasts={}, disablePinning={}, autoClaim={}, questScrolls={}, disableBook={}, spawnBook={}",
                    disabledCategories(),
                    pinnedQuestHudPosition(),
                    hideQuestBookInInventory(),
                    questBookInventoryButtonPosition(),
                    centerInventoryWithQuestPanel(),
                    hideCategoryHeader(),
                    filterDisplayMode(),
                    disableCategories(),
                    enableBuiltinQuestPack(),
                    hideQuestWidgetIcons(),
                    enableQuestSearchBox(),
                    enableDescriptionColors(),
                    enableQuestToasts(),
                    disableQuestPinning(),
                    autoClaimQuestRewards(),
                    enableQuestScrolls(),
                    disableQuestBook(),
                    spawnWithQuestBook());
    }
}
