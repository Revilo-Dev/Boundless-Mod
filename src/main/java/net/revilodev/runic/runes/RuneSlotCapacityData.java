package net.revilodev.runic.runes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.revilodev.runic.RunicMod;

import java.util.HashMap;
import java.util.Map;

public final class RuneSlotCapacityData extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String FOLDER = "rune_slots";

    private static Map<Item, Integer> CAPACITIES = new HashMap<>();
    private static Map<String, Integer> DEFAULTS = new HashMap<>();

    public RuneSlotCapacityData() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<Item, Integer> fresh = new HashMap<>();
        Map<String, Integer> defaults = new HashMap<>();

        objects.forEach((rl, element) -> {
            if (!element.isJsonObject()) {
                RunicMod.LOGGER.warn("RuneSlots: {} is not a JSON object", rl);
                return;
            }
            JsonObject json = element.getAsJsonObject();

            // --- Fallback defaults: { "defaults": { "helmet": 3, ... } }
            if (json.has("defaults") && json.get("defaults").isJsonObject()) {
                JsonObject defs = json.getAsJsonObject("defaults");
                for (String keyStr : defs.keySet()) {
                    int slots = GsonHelper.getAsInt(defs, keyStr, 0);
                    defaults.put(keyStr, Math.max(0, slots));
                }
                return;
            }

            // --- Bulk { "items": { "<id>": slots, ... } }
            if (json.has("items") && json.get("items").isJsonObject()) {
                JsonObject items = json.getAsJsonObject("items");
                for (String keyStr : items.keySet()) {
                    ResourceLocation key = ResourceLocation.tryParse(keyStr);
                    if (key == null) {
                        RunicMod.LOGGER.warn("RuneSlots: invalid item id '{}' in {}", keyStr, rl);
                        continue;
                    }
                    int slots = GsonHelper.getAsInt(items, keyStr, 0);
                    putSafe(fresh, key, slots, rl);
                }
                return;
            }

            // --- List format { "list": [ { "item": "...", "slots": n } ] }
            if (json.has("list") && json.get("list").isJsonArray()) {
                json.getAsJsonArray("list").forEach(el -> {
                    if (!el.isJsonObject()) return;
                    JsonObject entry = el.getAsJsonObject();
                    String itemId = GsonHelper.getAsString(entry, "item", "");
                    int slots = GsonHelper.getAsInt(entry, "slots", 0);
                    ResourceLocation key = ResourceLocation.tryParse(itemId);
                    if (key == null) {
                        RunicMod.LOGGER.warn("RuneSlots: invalid item id '{}' in {}", itemId, rl);
                        return;
                    }
                    putSafe(fresh, key, slots, rl);
                });
                return;
            }

            // --- Single entry { "item": "...", "slots": n }
            String itemId = GsonHelper.getAsString(json, "item", "");
            int slots = GsonHelper.getAsInt(json, "slots", 0);
            ResourceLocation key = ResourceLocation.tryParse(itemId);
            if (key == null) {
                RunicMod.LOGGER.warn("RuneSlots: invalid item id '{}' in {}", itemId, rl);
                return;
            }
            putSafe(fresh, key, slots, rl);
        });

        CAPACITIES = fresh;
        DEFAULTS = defaults;
        RunicMod.LOGGER.info("Loaded {} rune slot capacity entries and {} defaults.",
                CAPACITIES.size(), DEFAULTS.size());
    }

    private static void putSafe(Map<Item, Integer> map, ResourceLocation itemId, int slots, ResourceLocation source) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            RunicMod.LOGGER.warn("RuneSlots: unknown item '{}' in {}", itemId, source);
            return;
        }
        map.put(item, Math.max(0, slots));
    }

    /** Get rune slot capacity for a given item */
    public static int capacity(Item item) {
        // 1. Direct lookup
        Integer direct = CAPACITIES.get(item);
        if (direct != null) return direct;

        // 2. Fallback by type
        String type = classify(item);
        if (type != null && DEFAULTS.containsKey(type)) {
            return DEFAULTS.get(type);
        }

        // 3. None
        return 0;
    }

    /** Classify an item into a fallback type string (helmet, sword, pickaxe, etc.) */
    private static String classify(Item item) {
        if (item instanceof ArmorItem armor) {
            return switch (armor.getType()) {
                case HELMET -> "helmet";
                case CHESTPLATE -> "chestplate";
                case LEGGINGS -> "leggings";
                case BOOTS -> "boots";
                default -> null; //fallback
            };
        }
        if (item instanceof SwordItem) return "sword";
        if (item instanceof PickaxeItem) return "pickaxe";
        if (item instanceof AxeItem) return "axe";
        if (item instanceof ShovelItem) return "shovel";
        if (item instanceof HoeItem) return "hoe";
        if (item instanceof BowItem) return "bow";
        if (item instanceof CrossbowItem) return "crossbow";
        if (item instanceof ShieldItem) return "shield";
        if (item instanceof TridentItem) return "trident";
        if (item instanceof ElytraItem) return "elytra";
        if (item instanceof FishingRodItem) return "fishing_rod";
        return null;
    }
}
