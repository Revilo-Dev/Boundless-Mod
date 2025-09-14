package net.revilodev.boundless.quest;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
    private static boolean loadedClient = false;
    private static boolean loadedServer = false;

    public static final class Quest {
        public final String id;
        public final String name;
        public final String icon;
        public final String description;
        public final List<String> dependencies;
        public final Reward reward;
        public final String type;
        public final Completion completion;
        public final boolean optional;

        public Quest(String id, String name, String icon, String description,
                     List<String> dependencies, Reward reward, String type, Completion completion, boolean optional) {
            this.id = Objects.requireNonNull(id);
            this.name = name == null ? id : name;
            this.icon = icon == null ? "minecraft:book" : icon;
            this.description = description == null ? "" : description;
            this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
            this.reward = reward;
            this.type = type == null ? "collection" : type;
            this.completion = completion;
            this.optional = optional;
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

    public static final class Completion {
        public final String item;
        public final String entity;
        public final int count;

        public Completion(String item, String entity, int count) {
            this.item = item;
            this.entity = entity;
            this.count = Math.max(1, count);
        }
    }

    private static synchronized void load(ResourceManager rm, boolean forceReload) {
        if ((loadedClient || loadedServer) && !forceReload && !QUESTS.isEmpty()) return;
        QUESTS.clear();
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
                    } else if (obj.has("dependencies") && obj.get("dependencies").isJsonPrimitive()) {
                        String s = obj.get("dependencies").getAsString();
                        if (!s.isBlank()) deps.add(s);
                    }

                    Reward reward = null;
                    if (obj.has("reward") && obj.get("reward").isJsonObject()) {
                        JsonObject rObj = obj.getAsJsonObject("reward");
                        String rItem = optString(rObj, "item");
                        int rCount = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                        if (rItem != null && !rItem.isBlank()) reward = new Reward(rItem, rCount);
                    }

                    String type = optString(obj, "type");
                    Completion completion = null;
                    if (obj.has("completion") && obj.get("completion").isJsonObject()) {
                        JsonObject cObj = obj.getAsJsonObject("completion");
                        String cItem = optString(cObj, "item");
                        String cEntity = optString(cObj, "entity");
                        int cCount = cObj.has("count") ? cObj.get("count").getAsInt() : 1;
                        if ((cItem != null && !cItem.isBlank()) || (cEntity != null && !cEntity.isBlank())) {
                            completion = new Completion(cItem, cEntity, cCount);
                        }
                    }

                    boolean optional = optBoolean(obj, "optional");

                    Quest q = new Quest(id, name, icon, description, deps, reward, type, completion, optional);
                    QUESTS.put(q.id, q);
                }
            } catch (IOException ex) {
                BoundlessMod.LOGGER.error("Error reading quest json {}: {}", file, ex.toString());
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("Bad quest json {}: {}", file, ex.toString());
            }
        }
        BoundlessMod.LOGGER.info("Loaded {} quest(s).", QUESTS.size());
    }

    public static synchronized void loadClient(boolean forceReload) {
        if (loadedClient && !forceReload && !QUESTS.isEmpty()) return;
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        load(rm, forceReload);
        loadedClient = true;
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        if (loadedServer && !forceReload && !QUESTS.isEmpty()) return;
        var resources = server.getServerResources();
        ResourceManager rm = resources.resourceManager();
        load(rm, forceReload);
        loadedServer = true;
    }

    public static boolean isEmpty() { return QUESTS.isEmpty(); }

    public static Collection<Quest> all() {
        if (!loadedClient) loadClient(false);
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static Optional<Quest> byId(String id) {
        if (!loadedClient) loadClient(false);
        return Optional.ofNullable(QUESTS.get(id));
    }

    public static Optional<Quest> byIdServer(MinecraftServer server, String id) {
        if (!loadedServer) loadServer(server, false);
        return Optional.ofNullable(QUESTS.get(id));
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static boolean optBoolean(JsonObject o, String key) {
        if (!o.has(key)) return false;
        JsonElement e = o.get(key);
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isString()) {
                String s = p.getAsString();
                return "true".equalsIgnoreCase(s) || "1".equals(s);
            }
            if (p.isNumber()) return p.getAsInt() != 0;
        }
        return false;
    }
}
