package net.revilodev.boundless.quest;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.revilodev.boundless.BoundlessMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public final class QuestData {
    private QuestData() {}

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String ROOT = "quests";

    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();
    private static boolean loadedOnce = false;

    public static final class Quest {
        public final String id;
        public final String name;
        public final String icon;
        public final String description;
        public final List<String> dependencies;
        public final Reward reward;

        public Quest(String id, String name, String icon, String description,
                     List<String> dependencies, Reward reward) {
            this.id = Objects.requireNonNull(id);
            this.name = name == null ? id : name;
            this.icon = icon == null ? "minecraft:book" : icon;
            this.description = description == null ? "" : description;
            this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
            this.reward = reward;
        }

        public Optional<Item> iconItem() {
            try {
                ResourceLocation rl = ResourceLocation.parse(icon);
                return Optional.ofNullable(BuiltInRegistries.ITEM.get(rl));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    public static final class Reward {
        public final String item;
        public final int count;

        public Reward(String item, int count) {
            this.item = item;
            this.count = Math.max(1, count);
        }
    }

    public static synchronized void loadClient(boolean forceReload) {
        if (loadedOnce && !forceReload) return;
        QUESTS.clear();

        Minecraft mc = Minecraft.getInstance();
        ResourceManager rm = mc.getResourceManager();

        Map<ResourceLocation, Resource> files = rm.listResources(ROOT, path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            ResourceLocation file = entry.getKey();
            try {
                Resource resObj = entry.getValue();
                try (Reader raw = resObj.openAsReader(); Reader reader = new BufferedReader(raw)) {
                    JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                    if (el == null || !el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();

                    String id = optString(obj, "id");
                    if (id == null || id.isBlank()) {
                        String p = file.getPath();
                        int lastSlash = p.lastIndexOf('/');
                        String name = (lastSlash >= 0 ? p.substring(lastSlash + 1) : p);
                        id = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
                    }

                    String name = optString(obj, "name");
                    String icon = optString(obj, "icon");
                    String description = optString(obj, "description");

                    List<String> deps = new ArrayList<>();
                    if (obj.has("dependencies") && obj.get("dependencies").isJsonArray()) {
                        for (JsonElement d : obj.getAsJsonArray("dependencies")) {
                            if (d.isJsonPrimitive()) deps.add(d.getAsString());
                        }
                    }

                    Reward reward = null;
                    if (obj.has("reward") && obj.get("reward").isJsonObject()) {
                        JsonObject rObj = obj.getAsJsonObject("reward");
                        String item = optString(rObj, "item");
                        int count = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                        if (item != null && !item.isBlank()) reward = new Reward(item, count);
                    }

                    Quest q = new Quest(id, name, icon, description, deps, reward);
                    QUESTS.put(q.id, q);
                }
            } catch (IOException ex) {
                BoundlessMod.LOGGER.error("Error reading quest json {}: {}", file, ex.toString());
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("Bad quest json {}: {}", file, ex.toString());
            }
        }

        loadedOnce = true;
        BoundlessMod.LOGGER.info("Loaded {} quest(s).", QUESTS.size());
    }

    public static boolean isEmpty() { return QUESTS.isEmpty(); }

    public static Collection<Quest> all() {
        if (!loadedOnce) loadClient(false);
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static Optional<Quest> byId(String id) {
        if (!loadedOnce) loadClient(false);
        return Optional.ofNullable(QUESTS.get(id));
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
}
