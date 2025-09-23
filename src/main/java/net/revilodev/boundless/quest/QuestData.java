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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
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
    private static final Map<String, Category> CATEGORIES = new LinkedHashMap<>();
    private static boolean loadedClient = false;
    private static boolean loadedServer = false;

    public static final class Quest {
        public final String id;
        public final String name;
        public final String icon;
        public final String description;
        public final List<String> dependencies;
        public final boolean optional;
        public final Rewards rewards;
        public final String type;
        public final Completion completion;
        public final String category;

        public Quest(String id, String name, String icon, String description,
                     List<String> dependencies, boolean optional, Rewards rewards, String type, Completion completion,
                     String category) {
            this.id = Objects.requireNonNull(id);
            this.name = name == null ? id : name;
            this.icon = icon == null ? "minecraft:book" : icon;
            this.description = description == null ? "" : description;
            this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
            this.optional = optional;
            this.rewards = rewards;
            this.type = type == null ? "collection" : type;
            this.completion = completion;
            this.category = category == null || category.isBlank() ? "misc" : category;
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

    public static final class Rewards {
        public final List<RewardEntry> items;

        public Rewards(List<RewardEntry> items) {
            this.items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public static final class RewardEntry {
        public final String item;
        public final int count;

        public RewardEntry(String item, int count) {
            this.item = item;
            this.count = Math.max(1, count);
        }
    }

    public static final class Completion {
        public final List<Target> targets;

        public Completion(List<Target> targets) {
            this.targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }

    public static final class Target {
        public final String kind;
        public final String id;
        public final int count;

        public Target(String kind, String id, int count) {
            this.kind = kind;
            this.id = id;
            this.count = Math.max(1, count);
        }

        public boolean isItem() { return "item".equals(kind); }
        public boolean isEntity() { return "entity".equals(kind); }
        public boolean isEffect() { return "effect".equals(kind); }
        public boolean isAdvancement() { return "advancement".equals(kind); }
    }

    public static final class Category {
        public final String id;
        public final String icon;
        public final String name;
        public final int order;

        public Category(String id, String icon, String name, int order) {
            this.id = id;
            this.icon = icon == null || icon.isBlank() ? "minecraft:book" : icon;
            this.name = name == null || name.isBlank() ? id : name;
            this.order = order;
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

    private static synchronized void load(ResourceManager rm, boolean forceReload) {
        if ((loadedClient || loadedServer) && !forceReload && !QUESTS.isEmpty()) return;
        QUESTS.clear();
        CATEGORIES.clear();

        Map<ResourceLocation, List<Resource>> catFilesA = rm.listResourceStacks(ROOT + "/categories", p -> p.getPath().endsWith(".json"));
        Map<ResourceLocation, List<Resource>> catFiles = new LinkedHashMap<>();
        catFiles.putAll(catFilesA);

        for (Map.Entry<ResourceLocation, List<Resource>> entry : catFiles.entrySet()) {
            List<Resource> stack = entry.getValue();
            if (stack.isEmpty()) continue;
            Resource top = stack.get(stack.size() - 1); // top pack wins
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                String id = optString(obj, "id");
                String icon = optString(obj, "icon");
                String cname = optString(obj, "name");
                int order = 0;
                if (obj.has("order")) {
                    JsonPrimitive prim = obj.getAsJsonPrimitive("order");
                    if (prim.isNumber()) order = prim.getAsInt();
                    else try { order = Integer.parseInt(prim.getAsString()); } catch (NumberFormatException ignored) {}
                }
                if (id != null && !id.isBlank()) {
                    CATEGORIES.put(id, new Category(id, icon, cname, order));
                }
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("Bad category json {}: {}", entry.getKey(), ex.toString());
            }
        }

        Map<ResourceLocation, List<Resource>> files = rm.listResourceStacks(ROOT, path -> path.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, List<Resource>> entry : files.entrySet()) {
            String path = entry.getKey().getPath();
            if (path.contains("/categories/") || path.contains("/catagories/")) continue;
            List<Resource> stack = entry.getValue();
            if (stack.isEmpty()) continue;
            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();

                String id = optString(obj, "id");
                if (id == null || id.isBlank()) {
                    String p = entry.getKey().getPath();
                    int lastSlash = p.lastIndexOf('/');
                    String n = (lastSlash >= 0 ? p.substring(lastSlash + 1) : p);
                    id = n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
                }

                String name = optString(obj, "name");
                String icon = optString(obj, "icon");
                String description = optString(obj, "description");

                List<String> deps = parseDependencies(obj);

                boolean optional = false;
                if (obj.has("optional") && obj.get("optional").isJsonPrimitive()) {
                    JsonPrimitive prim = obj.getAsJsonPrimitive("optional");
                    if (prim.isBoolean()) optional = prim.getAsBoolean();
                    else optional = Boolean.parseBoolean(prim.getAsString());
                }

                Rewards rewards = parseRewards(obj.get("reward"));
                String type = optString(obj, "type");
                Completion completion = parseCompletion(obj.get("completion"), type);
                String category = optString(obj, "category");

                Quest q = new Quest(id, name, icon, description, deps, optional, rewards, type, completion, category);
                QUESTS.put(q.id, q);
            } catch (IOException ex) {
                BoundlessMod.LOGGER.error("Error reading quest json {}: {}", entry.getKey(), ex.toString());
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("Bad quest json {}: {}", entry.getKey(), ex.toString());
            }
        }

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE));
        }
        BoundlessMod.LOGGER.info("Loaded {} quest(s), {} category(ies).", QUESTS.size(), CATEGORIES.size());
    }

    private static List<String> parseDependencies(JsonObject obj) {
        List<String> deps = new ArrayList<>();
        if (obj.has("dependencies") && obj.get("dependencies").isJsonArray()) {
            for (JsonElement d : obj.getAsJsonArray("dependencies")) {
                if (d.isJsonPrimitive()) deps.add(d.getAsString());
            }
        } else if (obj.has("dependencies") && obj.get("dependencies").isJsonPrimitive()) {
            String s = obj.get("dependencies").getAsString();
            if (!s.isBlank()) deps.add(s);
        }
        return deps;
    }

    private static Rewards parseRewards(JsonElement el) {
        if (el == null) return new Rewards(List.of());
        List<RewardEntry> out = new ArrayList<>();
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject r = e.getAsJsonObject();
                String item = optString(r, "item");
                int count = r.has("count") ? r.get("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) out.add(new RewardEntry(item, count));
            }
            return new Rewards(out);
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("items") && obj.get("items").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("items")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject r = e.getAsJsonObject();
                    String item = optString(r, "item");
                    int count = r.has("count") ? r.get("count").getAsInt() : 1;
                    if (item != null && !item.isBlank()) out.add(new RewardEntry(item, count));
                }
                return new Rewards(out);
            }
            String item = optString(obj, "item");
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            if (item != null && !item.isBlank()) out.add(new RewardEntry(item, count));
            return new Rewards(out);
        }
        return new Rewards(List.of());
    }

    public static Collection<Quest> allServer(MinecraftServer server) {
        loadServer(server, false);
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    private static Completion parseCompletion(JsonElement el, String type) {
        if (el == null) return new Completion(List.of());
        List<Target> out = new ArrayList<>();
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                parseTargetObject(e.getAsJsonObject(), out);
            }
            return new Completion(out);
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("targets") && obj.get("targets").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("targets")) {
                    if (!e.isJsonObject()) continue;
                    parseTargetObject(e.getAsJsonObject(), out);
                }
                return new Completion(out);
            }
            if (obj.has("items") && obj.get("items").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("items")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject o = e.getAsJsonObject();
                    String item = optString(o, "item");
                    int count = o.has("count") ? o.get("count").getAsInt() : 1;
                    if (item != null && !item.isBlank()) out.add(new Target("item", item, count));
                }
                return new Completion(out);
            }
            if (obj.has("entities") && obj.get("entities").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("entities")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject o = e.getAsJsonObject();
                    String entity = optString(o, "entity");
                    int count = o.has("count") ? o.get("count").getAsInt() : 1;
                    if (entity != null && !entity.isBlank()) out.add(new Target("entity", entity, count));
                }
                return new Completion(out);
            }
            if (obj.has("item")) {
                String item = optString(obj, "item");
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) out.add(new Target("item", item, count));
                return new Completion(out);
            }
            if (obj.has("entity")) {
                String entity = optString(obj, "entity");
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                if (entity != null && !entity.isBlank()) out.add(new Target("entity", entity, count));
                return new Completion(out);
            }
            if (obj.has("effect")) {
                String effect = optString(obj, "effect");
                if (effect != null && !effect.isBlank()) out.add(new Target("effect", effect, 1));
                return new Completion(out);
            }
            if (obj.has("advancement")) {
                String adv = optString(obj, "advancement");
                if (adv != null && !adv.isBlank()) out.add(new Target("advancement", adv, 1));
                return new Completion(out);
            }
        }
        return new Completion(List.of());
    }

    private static void parseTargetObject(JsonObject o, List<Target> out) {
        if (o.has("item")) {
            String item = optString(o, "item");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (item != null && !item.isBlank()) out.add(new Target("item", item, count));
        } else if (o.has("entity")) {
            String entity = optString(o, "entity");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (entity != null && !entity.isBlank()) out.add(new Target("entity", entity, count));
        } else if (o.has("effect")) {
            String effect = optString(o, "effect");
            if (effect != null && !effect.isBlank()) out.add(new Target("effect", effect, 1));
        } else if (o.has("advancement")) {
            String adv = optString(o, "advancement");
            if (adv != null && !adv.isBlank()) out.add(new Target("advancement", adv, 1));
        }
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

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ResourceManager crm = Minecraft.getInstance().getResourceManager();
            load(crm, true);
        }
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

    public static List<Category> categoriesOrdered() {
        if (!loadedClient) loadClient(false);
        if (!CATEGORIES.containsKey("all")) CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE));
        List<Category> list = new ArrayList<>(CATEGORIES.values());
        list.sort(Comparator.comparingInt(c -> c.order));
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;
            return Integer.compare(a.order, b.order);
        });
        return list;
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
}
