package net.revilodev.boundless.quest;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
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

    // ------------------ Model ------------------

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
                     List<String> dependencies, boolean optional,
                     Rewards rewards, String type, Completion completion, String category) {
            this.id = Objects.requireNonNull(id);
            this.name = name == null ? id : name;
            this.icon = icon == null ? "minecraft:book" : icon;
            this.description = description == null ? "" : description;
            this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
            this.optional = optional;
            this.rewards = rewards;
            this.type = type == null ? "collection" : type;
            this.completion = completion;
            this.category = (category == null || category.isBlank()) ? "misc" : category;
        }

        public Optional<Item> iconItem() {
            try {
                return Optional.ofNullable(BuiltInRegistries.ITEM.get(ResourceLocation.parse(icon)));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    public static final class Rewards {
        public final List<RewardEntry> items;
        public final String command;
        public Rewards(List<RewardEntry> items, String command) {
            this.items = items == null ? List.of() : List.copyOf(items);
            this.command = command == null ? "" : command;
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
        public boolean isItem()        { return "item".equals(kind); }
        public boolean isEntity()      { return "entity".equals(kind); }
        public boolean isEffect()      { return "effect".equals(kind); }
        public boolean isAdvancement() { return "advancement".equals(kind); }
        public boolean isStat()        { return "stat".equals(kind); }
    }

    public static final class Category {
        public final String id;
        public final String icon;
        public final String name;
        public final int order;
        public final boolean excludeFromAll;
        public final String dependency;

        public Category(String id, String icon, String name, int order,
                        boolean excludeFromAll, String dependency) {
            this.id = id;
            this.icon = (icon == null || icon.isBlank()) ? "minecraft:book" : icon;
            this.name = (name == null || name.isBlank()) ? id : name;
            this.order = order;
            this.excludeFromAll = excludeFromAll;
            this.dependency = (dependency == null) ? "" : dependency;
        }

        public Optional<Item> iconItem() {
            try {
                return Optional.ofNullable(BuiltInRegistries.ITEM.get(ResourceLocation.parse(icon)));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    // ------------------ Loading ------------------

    private static synchronized void load(ResourceManager rm, boolean forceReload) {
        if ((loadedClient || loadedServer) && !forceReload && !QUESTS.isEmpty()) return;

        QUESTS.clear();
        CATEGORIES.clear();

        // categories
        var catStacks = rm.listResourceStacks(ROOT + "/categories", rl -> rl.getPath().endsWith(".json"));
        for (var entry : catStacks.entrySet()) {
            if (!"boundless".equals(entry.getKey().getNamespace())) continue;
            var stack = entry.getValue();
            if (stack.isEmpty()) continue;
            var top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                var el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (el == null || !el.isJsonObject()) continue;
                var obj = el.getAsJsonObject();
                String id   = optString(obj, "id");
                String icon = optString(obj, "icon");
                String name = optString(obj, "name");
                int order   = parseIntFlexible(obj, "order", 0);
                boolean exclude = parseBoolFlexible(obj, "exclude_from_all", false);
                String dep  = optString(obj, "dependency");
                if (id != null && !id.isBlank()) {
                    CATEGORIES.put(id, new Category(id, icon, name, order, exclude, dep));
                }
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("Bad category json {}: {}", entry.getKey(), ex.toString());
            }
        }

        // quests (everything under quests/ except categories/)
        var stacks = rm.listResourceStacks(ROOT, rl -> rl.getPath().endsWith(".json"));
        for (var entry : stacks.entrySet()) {
            if (!"boundless".equals(entry.getKey().getNamespace())) continue;
            String path = entry.getKey().getPath();
            if (path.contains("/categories/") || path.contains("/catagories/")) continue; // typo safety

            var stack = entry.getValue();
            if (stack.isEmpty()) continue;
            var top = stack.get(stack.size() - 1);

            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                var el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (el == null || !el.isJsonObject()) continue;
                var obj = el.getAsJsonObject();

                String id = optString(obj, "id");
                if (id == null || id.isBlank()) {
                    String p = entry.getKey().getPath();
                    int i = p.lastIndexOf('/');
                    String n = (i >= 0 ? p.substring(i + 1) : p);
                    id = n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
                }

                String name = optString(obj, "name");
                String icon = optString(obj, "icon");
                String description = optString(obj, "description");
                boolean optional = parseBoolFlexible(obj, "optional", false);
                String type = optString(obj, "type");
                String category = optString(obj, "category");

                List<String> deps = parseDependencies(obj);
                Rewards rewards = parseRewards(obj.get("reward"));
                Completion completion = parseCompletion(obj.get("completion"), type);

                Quest q = new Quest(id, name, icon, description, deps, optional, rewards, type, completion, category);
                QUESTS.put(q.id, q);
            } catch (IOException ex) {
                BoundlessMod.LOGGER.error("Error reading quest json {}: {}", entry.getKey(), ex.toString());
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("Bad quest json {}: {}", entry.getKey(), ex.toString());
            }
        }

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
        }

        BoundlessMod.LOGGER.info("Loaded {} quest(s), {} category(ies).", QUESTS.size(), CATEGORIES.size());
    }

    private static List<String> parseDependencies(JsonObject obj) {
        List<String> out = new ArrayList<>();
        if (obj.has("dependencies")) {
            var el = obj.get("dependencies");
            if (el.isJsonArray()) for (var d : el.getAsJsonArray()) if (d.isJsonPrimitive()) out.add(d.getAsString());
            else if (el.isJsonPrimitive()) {
                String s = el.getAsString();
                if (!s.isBlank()) out.add(s);
            }
        }
        return out;
    }

    private static Rewards parseRewards(JsonElement el) {
        if (el == null) return new Rewards(List.of(), "");
        List<RewardEntry> items = new ArrayList<>();
        String cmd = "";
        if (el.isJsonArray()) {
            for (var e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                var o = e.getAsJsonObject();
                String item = optString(o, "item");
                int count = o.has("count") ? o.get("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
            }
            return new Rewards(items, "");
        }
        if (el.isJsonObject()) {
            var o = el.getAsJsonObject();
            if (o.has("items") && o.get("items").isJsonArray()) {
                for (var e : o.getAsJsonArray("items")) {
                    if (!e.isJsonObject()) continue;
                    var r = e.getAsJsonObject();
                    String item = optString(r, "item");
                    int count = r.has("count") ? r.get("count").getAsInt() : 1;
                    if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
                }
            } else {
                String item = optString(o, "item");
                int count = o.has("count") ? o.get("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
            }
            if (o.has("command") && o.get("command").isJsonPrimitive()) cmd = o.get("command").getAsString();
            return new Rewards(items, cmd);
        }
        return new Rewards(List.of(), "");
    }

    private static Completion parseCompletion(JsonElement el, String type) {
        if (el == null) return new Completion(List.of());
        List<Target> out = new ArrayList<>();
        if (el.isJsonArray()) {
            for (var e : el.getAsJsonArray()) if (e.isJsonObject()) parseTargetObject(e.getAsJsonObject(), out);
            return new Completion(out);
        }
        if (el.isJsonObject()) {
            var obj = el.getAsJsonObject();
            if (obj.has("targets") && obj.get("targets").isJsonArray()) {
                for (var e : obj.getAsJsonArray("targets")) if (e.isJsonObject()) parseTargetObject(e.getAsJsonObject(), out);
                return new Completion(out);
            }
            // legacy conveniences
            if (obj.has("items")) for (var e : obj.getAsJsonArray("items")) if (e.isJsonObject()) {
                var o = e.getAsJsonObject(); out.add(new Target("item", optString(o, "item"), o.has("count") ? o.get("count").getAsInt() : 1));
            }
            if (obj.has("entities")) for (var e : obj.getAsJsonArray("entities")) if (e.isJsonObject()) {
                var o = e.getAsJsonObject(); out.add(new Target("entity", optString(o, "entity"), o.has("count") ? o.get("count").getAsInt() : 1));
            }
            if (obj.has("stats")) for (var e : obj.getAsJsonArray("stats")) if (e.isJsonObject()) {
                var o = e.getAsJsonObject(); out.add(new Target("stat", optString(o, "stat"), o.has("count") ? o.get("count").getAsInt() : 1));
            }
            if (obj.has("item"))       out.add(new Target("item",        optString(obj, "item"),        obj.has("count") ? obj.get("count").getAsInt() : 1));
            if (obj.has("entity"))     out.add(new Target("entity",      optString(obj, "entity"),      obj.has("count") ? obj.get("count").getAsInt() : 1));
            if (obj.has("effect"))     out.add(new Target("effect",      optString(obj, "effect"),      1));
            if (obj.has("advancement"))out.add(new Target("advancement", optString(obj, "advancement"), 1));
            return new Completion(out);
        }
        return new Completion(List.of());
    }

    private static void parseTargetObject(JsonObject o, List<Target> out) {
        if (o.has("item"))        out.add(new Target("item",        optString(o, "item"),        o.has("count") ? o.get("count").getAsInt() : 1));
        else if (o.has("entity")) out.add(new Target("entity",      optString(o, "entity"),      o.has("count") ? o.get("count").getAsInt() : 1));
        else if (o.has("effect")) out.add(new Target("effect",      optString(o, "effect"),      1));
        else if (o.has("advancement")) out.add(new Target("advancement", optString(o, "advancement"), 1));
        else if (o.has("stat"))   out.add(new Target("stat",        optString(o, "stat"),        o.has("count") ? o.get("count").getAsInt() : 1));
    }

    // ------------------ Public API ------------------

    public static synchronized void loadClient(boolean forceReload) {
        if (loadedClient && !forceReload && !QUESTS.isEmpty()) return;
        var rm = Minecraft.getInstance().getResourceManager();
        load(rm, forceReload);
        loadedClient = true;
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        if (loadedServer && !forceReload && !QUESTS.isEmpty()) return;
        var rm = server.getServerResources().resourceManager();
        load(rm, forceReload);
        loadedServer = true;

        // Keep the client copy in sync for singleplayer
        if (FMLEnvironment.dist == Dist.CLIENT) {
            load(Minecraft.getInstance().getResourceManager(), true);
        }
    }

    public static boolean isEmpty() { return QUESTS.isEmpty(); }

    public static Collection<Quest> all() {
        loadClient(false);
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static Collection<Quest> allServer(MinecraftServer server) {
        loadServer(server, false);
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static Optional<Quest> byId(String id) {
        loadClient(false);
        return Optional.ofNullable(QUESTS.get(id));
    }

    public static Optional<Quest> byIdServer(MinecraftServer server, String id) {
        loadServer(server, false);
        return Optional.ofNullable(QUESTS.get(id));
    }

    public static Optional<Category> categoryById(String id) {
        loadClient(false);
        return Optional.ofNullable(CATEGORIES.get(id));
    }

    public static boolean isCategoryUnlocked(Category c, Player player) {
        if (c == null) return true;
        if (c.dependency == null || c.dependency.isBlank()) return true;
        var q = byId(c.dependency).orElse(null);
        if (q == null) return true;
        return QuestTracker.getStatus(q, player) == QuestTracker.Status.REDEEMED;
    }

    public static boolean includeQuestInAll(Quest q, Player player) {
        if (q == null) return false;
        var c = CATEGORIES.get(q.category);
        if (c == null) return true;
        if (c.excludeFromAll) return false;
        return isCategoryUnlocked(c, player);
    }

    public static List<Category> categoriesOrdered() {
        loadClient(false);
        if (!CATEGORIES.containsKey("all"))
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
        var list = new ArrayList<>(CATEGORIES.values());
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;
            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });
        return list;
    }

    // ------------------ helpers ------------------

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
    private static int parseIntFlexible(JsonObject o, String key, int def) {
        if (!o.has(key)) return def;
        var p = o.getAsJsonPrimitive(key);
        if (p.isNumber()) return p.getAsInt();
        try { return Integer.parseInt(p.getAsString()); } catch (Exception ignored) { return def; }
    }
    private static boolean parseBoolFlexible(JsonObject o, String key, boolean def) {
        if (!o.has(key)) return def;
        var p = o.getAsJsonPrimitive(key);
        if (p.isBoolean()) return p.getAsBoolean();
        try { return Boolean.parseBoolean(p.getAsString()); } catch (Exception ignored) { return def; }
    }
}
