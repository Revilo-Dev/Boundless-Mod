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
import net.minecraft.world.item.Item;
import net.revilodev.runic.RunicMod;

import java.util.HashMap;
import java.util.Map;

public final class RuneSlotCapacityData extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String FOLDER = "rune_slots";

    private static Map<Item, Integer> CAPACITIES = new HashMap<>();

    public RuneSlotCapacityData() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<Item, Integer> fresh = new HashMap<>();

        objects.forEach((rl, element) -> {
            if (!element.isJsonObject()) {
                RunicMod.LOGGER.warn("RuneSlots: {} is not a JSON object", rl);
                return;
            }
            JsonObject json = element.getAsJsonObject();

            // --- Format 2: bulk map { "items": { "<id>": slots, ... } } ---
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

            // --- Format 3: bulk list { "list": [ { "item": "...", "slots": n }, ... ] } ---
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

            // --- Format 1: single entry { "item": "...", "slots": n } ---
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
        RunicMod.LOGGER.info("Loaded {} rune slot capacity entries.", CAPACITIES.size());
    }

    private static void putSafe(Map<Item, Integer> map, ResourceLocation itemId, int slots, ResourceLocation source) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            RunicMod.LOGGER.warn("RuneSlots: unknown item '{}' in {}", itemId, source);
            return;
        }
        map.put(item, Math.max(0, slots));
    }

    public static int capacity(Item item) {
        return CAPACITIES.getOrDefault(item, 0);
    }
}
