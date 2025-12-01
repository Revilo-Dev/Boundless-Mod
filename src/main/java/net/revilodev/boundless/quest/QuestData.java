package net.revilodev.boundless.quest;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.revilodev.boundless.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public final class QuestData {

    private QuestData() {}

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String PATH_QUESTS = "quests";
    private static final String PATH_CATEGORIES = "quests/categories";

    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();
    private static final Map<String, Category> CATEGORIES = new LinkedHashMap<>();

    private static boolean loadedClient = false;
    private static boolean loadedServer = false;

    // ------------------------------------------------------------
    //  DATA CLASSES
    // ------------------------------------------------------------

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
                     List<String> dependencies, boolean optional, Rewards rewards,
                     String type, Completion completion, String category)
        {
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
                return Optional.ofNullable(
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse(icon))
                );
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    public static final class Rewards {
        public final List<RewardEntry> items;
        public final String command;
        public final String expType;
        public final int expAmount;

        public Rewards(List<RewardEntry> items, String command, String expType, int expAmount) {
            this.items = items == null ? List.of() : List.copyOf(items);
            this.command = command == null ? "" : command;
            this.expType = expType == null ? "" : expType;
            this.expAmount = Math.max(0, expAmount);
        }

        public boolean hasExp() {
            return !expType.isBlank() && expAmount > 0;
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
        public boolean isStat() { return "stat".equals(kind); }
    }

    public static final class Category {
        public final String id;
        public final String icon;
        public final String name;
        public final int order;
        public final boolean excludeFromAll;
        public final String dependency;

        public Category(String id, String icon, String name, int order, boolean excludeFromAll, String dependency) {
            this.id = id;
            this.icon = (icon == null || icon.isBlank()) ? "minecraft:book" : icon;
            this.name = (name == null || name.isBlank()) ? id : name;
            this.order = order;
            this.excludeFromAll = excludeFromAll;
            this.dependency = dependency == null ? "" : dependency;
        }

        public Optional<Item> iconItem() {
            try {
                return Optional.ofNullable(
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse(icon))
                );
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    // ------------------------------------------------------------
    //  TOP-LEVEL LOAD ENTRYPOINTS
    // ------------------------------------------------------------

    public static synchronized void forceReloadAll(MinecraftServer server) {
        loadedClient = false;
        loadedServer = false;
        loadServer(server, true);
    }

    public static synchronized void loadClient(boolean forceReload) {
        if (loadedClient && !forceReload && !QUESTS.isEmpty()) return;
        load(Minecraft.getInstance().getResourceManager(), forceReload);
        loadedClient = true;
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        if (loadedServer && !forceReload && !QUESTS.isEmpty()) return;

        ResourceManager rm = server.getServerResources().resourceManager();
        load(rm, forceReload);

        loadedServer = true;

        // Force client-side reload to include resource packs:
        if (FMLEnvironment.dist == Dist.CLIENT) {
            load(Minecraft.getInstance().getResourceManager(), true);
        }
    }

    // ------------------------------------------------------------
    //  MAIN LOAD LOGIC (DATAPACKS + RESOURCE PACKS)
    // ------------------------------------------------------------

    private static synchronized void load(ResourceManager rm, boolean forceReload) {
        QUESTS.clear();
        CATEGORIES.clear();

        // 1. Load datapacks (server or client mirror)
        loadFromResourceManager(rm);

        // 2. Load resource packs (client only)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            loadFromClientResourcePacks();
        }

        // 3. Ensure category "all" exists
        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all",
                    new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
        }
    }

    // ------------------------------------------------------------
    //  DATAPACK LOADING
    // ------------------------------------------------------------

    private static void loadFromResourceManager(ResourceManager rm) {
        loadCategoriesFromStacks(rm);
        loadQuestsFromStacks(rm);
    }

    private static void loadCategoriesFromStacks(ResourceManager rm) {
        Map<ResourceLocation, List<Resource>> stacks =
                rm.listResourceStacks(PATH_CATEGORIES, rl -> rl.getPath().endsWith(".json"));

        for (var entry : stacks.entrySet()) {
            List<Resource> stack = entry.getValue();
            if (stack.isEmpty()) continue;

            Resource top = stack.get(stack.size() - 1); // vanilla priority

            try (Reader r = new BufferedReader(top.openAsReader())) {
                JsonObject obj = safeObject(r);
                if (obj == null) continue;

                String id = optString(obj, "id");
                if (id == null || id.isBlank()) continue;

                Category c = new Category(
                        id,
                        optString(obj, "icon"),
                        optString(obj, "name"),
                        parseIntFlexible(obj, "order", 0),
                        parseBoolFlexible(obj, "exclude_from_all", false),
                        optString(obj, "dependency")
                );

                CATEGORIES.put(id, c);

            } catch (Exception ignored) {}
        }
    }
    // ------------------------------------------------------------
    //  EXTRA API (required by Boundless Mod)
    // ------------------------------------------------------------

    /**
     * Returns true if ANY quest uses an entity target with given ID.
     */
    public static boolean usesEntityTarget(String entityId) {
        if (entityId == null || entityId.isBlank()) return false;
        for (Quest q : QUESTS.values()) {
            if (q.completion == null) continue;
            for (Target t : q.completion.targets) {
                if (t.isEntity() && t.id.equals(entityId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Server-side ordered categories.
     */
    public static List<Category> categoriesOrderedServer(MinecraftServer server) {
        loadServer(server, false);
        List<Category> list = new ArrayList<>(CATEGORIES.values());

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All",
                    Integer.MIN_VALUE, false, ""));
        }

        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;
            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });

        return list;
    }

    /**
     * Used by the server to push a JSON representation of quests to the client.
     * NOTE: This fully replaces the client's QUEST and CATEGORY maps.
     */
    public static void applyNetworkJson(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            QUESTS.clear();
            CATEGORIES.clear();

            // ----------------------------
            // Load categories
            // ----------------------------
            if (root.has("categories") && root.get("categories").isJsonArray()) {
                for (JsonElement elem : root.getAsJsonArray("categories")) {
                    if (!elem.isJsonObject()) continue;
                    JsonObject c = elem.getAsJsonObject();

                    String id = optString(c, "id");
                    if (id == null || id.isBlank()) continue;

                    Category cat = new Category(
                            id,
                            optString(c, "icon"),
                            optString(c, "name"),
                            parseIntFlexible(c, "order", 0),
                            parseBoolFlexible(c, "excludeFromAll", false),
                            optString(c, "dependency")
                    );

                    CATEGORIES.put(id, cat);
                }
            }

            // ----------------------------
            // Load quests
            // ----------------------------
            if (root.has("quests") && root.get("quests").isJsonArray()) {
                for (JsonElement elem : root.getAsJsonArray("quests")) {
                    if (!elem.isJsonObject()) continue;
                    JsonObject q = elem.getAsJsonObject();

                    String id = optString(q, "id");
                    if (id == null || id.isBlank()) continue;

                    // ----------------------------
                    // Reconstruct completion object
                    // ----------------------------
                    JsonElement completionEl = q.get("completion");
                    QuestData.Completion completion = parseCompletion(completionEl, optString(q, "type"));

                    // ----------------------------
                    // Reconstruct rewards object
                    // ----------------------------
                    JsonElement rewardEl = q.has("reward") ? q.get("reward") : q.get("rewards");
                    QuestData.Rewards rewards = parseRewards(rewardEl);


                    QuestData.Quest quest = new QuestData.Quest(
                            id,
                            optString(q, "name"),
                            optString(q, "icon"),
                            optString(q, "description"),
                            parseDependencies(q),
                            parseBoolFlexible(q, "optional", false),
                            rewards,
                            optString(q, "type"),
                            completion,
                            optString(q, "category")
                    );

                    QUESTS.put(id, quest);
                }
            }

            // Ensure default category
            if (!CATEGORIES.containsKey("all")) {
                CATEGORIES.put("all",
                        new Category("all", "minecraft:book", "All",
                                Integer.MIN_VALUE, false, ""));
            }

            VERSION++;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Quest version counter used by client caching.
     */
    private static int VERSION = 0;

    public static int version() {
        return VERSION;
    }



    private static void loadQuestsFromStacks(ResourceManager rm) {
        Map<ResourceLocation, List<Resource>> stacks =
                rm.listResourceStacks(PATH_QUESTS, rl -> rl.getPath().endsWith(".json"));

        for (var entry : stacks.entrySet()) {
            ResourceLocation loc = entry.getKey();
            if (loc.getPath().contains("/categories/")) continue;

            List<Resource> stack = entry.getValue();
            if (stack.isEmpty()) continue;

            Resource top = stack.get(stack.size() - 1);

            try (Reader r = new BufferedReader(top.openAsReader())) {
                JsonObject obj = safeObject(r);
                if (obj == null) continue;

                Quest q = parseQuestObject(obj, loc);
                if (q == null || isQuestDisabled(q)) continue;

                QUESTS.put(q.id, q);

            } catch (Exception ignored) {}
        }
    }

    // ------------------------------------------------------------
    //  RESOURCE PACK LOADING (CLIENT ONLY)
    // ------------------------------------------------------------

    private static void loadFromClientResourcePacks() {
        try {
            var repo = Minecraft.getInstance().getResourcePackRepository();
            Collection<Pack> packs = repo.getSelectedPacks();

            for (Pack pack : packs) {
                try (PackResources res = pack.open()) {

                    for (String ns : res.getNamespaces(net.minecraft.server.packs.PackType.SERVER_DATA)) {

                        // categories
                        res.listResources(net.minecraft.server.packs.PackType.SERVER_DATA, ns, PATH_CATEGORIES,
                                (loc, supplier) -> {
                                    if (!loc.getPath().endsWith(".json")) return;

                                    try (Reader r = supplierToReader(supplier)) {
                                        JsonObject obj = safeObject(r);
                                        if (obj == null) return;

                                        String id = optString(obj, "id");
                                        if (id == null || id.isBlank()) return;

                                        // Skip if datapack already defined this category (datapack wins)
                                        if (CATEGORIES.containsKey(id)) return;

                                        Category c = new Category(
                                                id,
                                                optString(obj, "icon"),
                                                optString(obj, "name"),
                                                parseIntFlexible(obj, "order", 0),
                                                parseBoolFlexible(obj, "exclude_from_all", false),
                                                optString(obj, "dependency")
                                        );

                                        CATEGORIES.put(id, c);

                                    } catch (Exception ignored) {}
                                });

                        // quests
                        res.listResources(net.minecraft.server.packs.PackType.SERVER_DATA, ns, PATH_QUESTS,
                                (loc, supplier) -> {
                                    if (!loc.getPath().endsWith(".json")) return;
                                    if (loc.getPath().contains("/categories/")) return;

                                    try (Reader r = supplierToReader(supplier)) {
                                        JsonObject obj = safeObject(r);
                                        if (obj == null) return;

                                        Quest q = parseQuestObject(obj, loc);
                                        if (q == null || isQuestDisabled(q)) return;

                                        // Skip if datapack already defined this ID (datapack wins)
                                        if (QUESTS.containsKey(q.id)) return;

                                        QUESTS.put(q.id, q);

                                    } catch (Exception ignored) {}
                                });
                    }

                } catch (Throwable ignored) {}
            }

        } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------
    //  JSON PARSING
    // ------------------------------------------------------------

    private static JsonObject safeObject(Reader reader) {
        JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private static Quest parseQuestObject(JsonObject obj, ResourceLocation src) {
        String id = optString(obj, "id");

        if (id == null || id.isBlank()) {
            String path = src.getPath();
            int i = path.lastIndexOf('/');
            String base = (i >= 0 ? path.substring(i + 1) : path);
            id = base.endsWith(".json") ? base.substring(0, base.length() - 5) : base;
        }

        // Old syntax uses "type", new syntax often omits it
        String type = optString(obj, "type");

        // Prefer NEW field "complete" if present, otherwise fall back to legacy "completion"
        JsonElement completionEl = null;
        if (obj.has("complete")) {
            completionEl = obj.get("complete");
        } else if (obj.has("completion")) {
            completionEl = obj.get("completion");
        }

        // If no explicit type but "submit" is used in new syntax, infer "submission"
        if ((type == null || type.isBlank())
                && completionEl != null
                && completionEl.isJsonObject()
                && completionEl.getAsJsonObject().has("submit")) {
            type = "submission";
        }

        Completion completion = parseCompletion(completionEl, type);

        return new Quest(
                id,
                optString(obj, "name"),
                optString(obj, "icon"),
                optString(obj, "description"),
                parseDependencies(obj),
                parseBoolFlexible(obj, "optional", false),
                parseRewards(obj.get("reward")),
                type,
                completion,
                optString(obj, "category")
        );
    }


    private static List<String> parseDependencies(JsonObject obj) {
        List<String> out = new ArrayList<>();
        if (!obj.has("dependencies")) return out;

        JsonElement el = obj.get("dependencies");

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    String s = e.getAsString();
                    if (!s.isBlank()) out.add(s);
                }
            }
        } else if (el.isJsonPrimitive()) {
            String s = el.getAsString();
            if (!s.isBlank()) out.add(s);
        }

        return out;
    }

    private static Rewards parseRewards(JsonElement el) {
        if (el == null) return new Rewards(List.of(), "", "", 0);

        List<RewardEntry> list = new ArrayList<>();
        String command = "";
        String expType = "";
        int expAmount = 0;

        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();

            // -------------------------
            // items (network + datapack)
            // -------------------------
            if (o.has("items") && o.get("items").isJsonArray()) {
                for (JsonElement r : o.getAsJsonArray("items")) {
                    if (!r.isJsonObject()) continue;
                    String item = optString(r.getAsJsonObject(), "item");
                    int count = r.getAsJsonObject().has("count")
                            ? r.getAsJsonObject().get("count").getAsInt() : 1;
                    list.add(new RewardEntry(item, count));
                }
            }

            // single item
            if (o.has("item")) {
                String item = optString(o, "item");
                int count = o.has("count") ? o.get("count").getAsInt() : 1;
                list.add(new RewardEntry(item, count));
            }

            // -------------------------
            // command reward
            // -------------------------
            if (o.has("command")) {
                command = o.get("command").getAsString();
            }

            // -------------------------
            // XP reward (network format)
            // -------------------------
            if (o.has("expType")) {
                expType = o.get("expType").getAsString();
            }
            if (o.has("expAmount") && o.get("expAmount").isJsonPrimitive()) {
                expAmount = o.get("expAmount").getAsInt();
            }


            return new Rewards(list, command, expType, expAmount);
        }

        // array = item list
        if (el.isJsonArray()) {
            for (JsonElement r : el.getAsJsonArray()) {
                if (!r.isJsonObject()) continue;
                String item = optString(r.getAsJsonObject(), "item");
                int count = r.getAsJsonObject().has("count")
                        ? r.getAsJsonObject().get("count").getAsInt() : 1;
                list.add(new RewardEntry(item, count));
            }
            return new Rewards(list, "", "", 0);
        }

        // primitive = command reward
        if (el.isJsonPrimitive()) {
            return new Rewards(List.of(), el.getAsString(), "", 0);
        }

        return new Rewards(List.of(), "", "", 0);
    }




    private static Completion parseCompletion(JsonElement el, String type) {
        if (el == null) return new Completion(List.of());

        List<Target> out = new ArrayList<>();

        // -------------------------------------------------
        // CASE 1: completion / complete as array of targets
        // -------------------------------------------------
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (e.isJsonObject()) {
                    parseTargetObject(e.getAsJsonObject(), out);
                }
            }
            return new Completion(out);
        }

        if (!el.isJsonObject()) {
            return new Completion(List.of());
        }

        JsonObject o = el.getAsJsonObject();

        // -------------------------------------------------
        // CASE 2: explicit "targets" array (generic)
        // -------------------------------------------------
        if (o.has("targets") && o.get("targets").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("targets")) {
                if (e.isJsonObject()) {
                    parseTargetObject(e.getAsJsonObject(), out);
                }
            }
            return new Completion(out);
        }

        // -------------------------------------------------
        // CASE 3: NEW SYNTAX – "collect", "submit", "kill",
        //          "achieve", "effect"
        // Works for both:
        //   - completion: { "collect":"minecraft:crafting_table", "count":1 }
        //   - complete: { "collect":[ { "item":"...", "count":1 } ], ... }
        // -------------------------------------------------
        boolean anyNewSyntax = false;

        // helper: get default count from parent object if present
        int defaultCount = 1;
        if (o.has("count") && o.get("count").isJsonPrimitive() && o.get("count").getAsJsonPrimitive().isNumber()) {
            defaultCount = o.get("count").getAsInt();
            if (defaultCount <= 0) defaultCount = 1;
        }

        // collect / submit -> ITEM targets
        if (o.has("collect")) {
            appendItemTargetsFromField(out, o.get("collect"), defaultCount);
            anyNewSyntax = true;
        }
        if (o.has("submit")) {
            appendItemTargetsFromField(out, o.get("submit"), defaultCount);
            anyNewSyntax = true;
        }

        // kill -> ENTITY targets
        if (o.has("kill")) {
            appendEntityTargetsFromField(out, o.get("kill"), defaultCount);
            anyNewSyntax = true;
        }

        // achieve -> ADVANCEMENT targets
        if (o.has("achieve")) {
            appendAdvancementTargetsFromField(out, o.get("achieve"), defaultCount);
            anyNewSyntax = true;
        }

        // effect -> EFFECT targets
        if (o.has("effect")) {
            appendEffectTargetsFromField(out, o.get("effect"), defaultCount);
            anyNewSyntax = true;
        }

        if (anyNewSyntax) {
            return new Completion(out);
        }

        // -------------------------------------------------
        // CASE 4: legacy "collect" in completion object
        //   "completion": { "collect":"minecraft:crafting_table", "count":1 }
        // (Handled above already, but keep fallback if only "collect" exists)
        // -------------------------------------------------
        if (o.has("collect")) {
            appendItemTargetsFromField(out, o.get("collect"), defaultCount);
            return new Completion(out);
        }

        // -------------------------------------------------
        // CASE 5: legacy "items"/"entities" arrays
        // -------------------------------------------------
        if (o.has("items") && o.get("items").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("items")) {
                if (!e.isJsonObject()) continue;
                JsonObject je = e.getAsJsonObject();
                String item = optString(je, "item");
                int c = je.has("count") && je.get("count").isJsonPrimitive()
                        ? je.get("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) {
                    out.add(new Target("item", item, c));
                }
            }
            return new Completion(out);
        }

        if (o.has("entities") && o.get("entities").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("entities")) {
                if (!e.isJsonObject()) continue;
                JsonObject je = e.getAsJsonObject();
                String ent = optString(je, "entity");
                int c = je.has("count") && je.get("count").isJsonPrimitive()
                        ? je.get("count").getAsInt() : 1;
                if (ent != null && !ent.isBlank()) {
                    out.add(new Target("entity", ent, c));
                }
            }
            return new Completion(out);
        }

        // -------------------------------------------------
        // CASE 6: plain single-object formats (old)
        //   { "item":"...", "count":1 }
        //   { "entity":"...", "count":1 }
        //   { "effect":"..." }
        //   { "advancement":"..." }
        //   { "stat":"...", "count":1 }
        // -------------------------------------------------
        parseTargetObject(o, out);
        return new Completion(out);
    }

    private static void appendItemTargetsFromField(List<Target> out, JsonElement el, int defaultCount) {
        if (el == null) return;

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (e.isJsonObject()) {
                    JsonObject jo = e.getAsJsonObject();
                    String item = optString(jo, "item");
                    int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                            ? jo.get("count").getAsInt() : defaultCount;
                    if (c <= 0) c = 1;
                    if (item != null && !item.isBlank()) {
                        out.add(new Target("item", item, c));
                    }
                } else if (e.isJsonPrimitive()) {
                    String item = e.getAsString();
                    if (item != null && !item.isBlank()) {
                        int c = defaultCount <= 0 ? 1 : defaultCount;
                        out.add(new Target("item", item, c));
                    }
                }
            }
            return;
        }

        if (el.isJsonObject()) {
            JsonObject jo = el.getAsJsonObject();
            String item = optString(jo, "item");
            int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                    ? jo.get("count").getAsInt() : defaultCount;
            if (c <= 0) c = 1;
            if (item != null && !item.isBlank()) {
                out.add(new Target("item", item, c));
            }
            return;
        }

        if (el.isJsonPrimitive()) {
            String item = el.getAsString();
            if (item != null && !item.isBlank()) {
                int c = defaultCount <= 0 ? 1 : defaultCount;
                out.add(new Target("item", item, c));
            }
        }
    }

    private static void appendEntityTargetsFromField(List<Target> out, JsonElement el, int defaultCount) {
        if (el == null) return;

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject jo = e.getAsJsonObject();
                String ent = optString(jo, "entity");
                int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                        ? jo.get("count").getAsInt() : defaultCount;
                if (c <= 0) c = 1;
                if (ent != null && !ent.isBlank()) {
                    out.add(new Target("entity", ent, c));
                }
            }
            return;
        }

        if (el.isJsonObject()) {
            JsonObject jo = el.getAsJsonObject();
            String ent = optString(jo, "entity");
            int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                    ? jo.get("count").getAsInt() : defaultCount;
            if (c <= 0) c = 1;
            if (ent != null && !ent.isBlank()) {
                out.add(new Target("entity", ent, c));
            }
            return;
        }

        if (el.isJsonPrimitive()) {
            String ent = el.getAsString();
            if (ent != null && !ent.isBlank()) {
                int c = defaultCount <= 0 ? 1 : defaultCount;
                out.add(new Target("entity", ent, c));
            }
        }
    }

    private static void appendAdvancementTargetsFromField(List<Target> out, JsonElement el, int defaultCount) {
        if (el == null) return;

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject jo = e.getAsJsonObject();
                String adv = optString(jo, "advancement");
                int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                        ? jo.get("count").getAsInt() : defaultCount;
                if (c <= 0) c = 1;
                if (adv != null && !adv.isBlank()) {
                    out.add(new Target("advancement", adv, c));
                }
            }
            return;
        }

        if (el.isJsonObject()) {
            JsonObject jo = el.getAsJsonObject();
            String adv = optString(jo, "advancement");
            int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                    ? jo.get("count").getAsInt() : defaultCount;
            if (c <= 0) c = 1;
            if (adv != null && !adv.isBlank()) {
                out.add(new Target("advancement", adv, c));
            }
            return;
        }

        if (el.isJsonPrimitive()) {
            String adv = el.getAsString();
            if (adv != null && !adv.isBlank()) {
                int c = defaultCount <= 0 ? 1 : defaultCount;
                out.add(new Target("advancement", adv, c));
            }
        }
    }

    private static void appendEffectTargetsFromField(List<Target> out, JsonElement el, int defaultCount) {
        if (el == null) return;

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject jo = e.getAsJsonObject();
                String eff = optString(jo, "effect");
                int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                        ? jo.get("count").getAsInt() : defaultCount;
                if (c <= 0) c = 1;
                if (eff != null && !eff.isBlank()) {
                    out.add(new Target("effect", eff, c));
                }
            }
            return;
        }

        if (el.isJsonObject()) {
            JsonObject jo = el.getAsJsonObject();
            String eff = optString(jo, "effect");
            int c = jo.has("count") && jo.get("count").isJsonPrimitive()
                    ? jo.get("count").getAsInt() : defaultCount;
            if (c <= 0) c = 1;
            if (eff != null && !eff.isBlank()) {
                out.add(new Target("effect", eff, c));
            }
            return;
        }

        if (el.isJsonPrimitive()) {
            String eff = el.getAsString();
            if (eff != null && !eff.isBlank()) {
                int c = defaultCount <= 0 ? 1 : defaultCount;
                out.add(new Target("effect", eff, c));
            }
        }
    }


    private static void parseTargetObject(JsonObject o, List<Target> out) {
        // NEW: network format { "kind": "...", "id": "...", "count": N }
        if (o.has("kind") && o.has("id")) {
            String kind = optString(o, "kind");
            String id   = optString(o, "id");
            int count   = o.has("count") && o.get("count").isJsonPrimitive()
                    ? o.get("count").getAsInt()
                    : 1;

            if (kind != null && id != null && !id.isBlank()) {
                out.add(new Target(kind, id, count));
            }
            return;
        }

        // EXISTING legacy/datapack formats
        if (o.has("item")) {
            out.add(new Target("item", optString(o, "item"),
                    o.has("count") ? o.get("count").getAsInt() : 1));
        } else if (o.has("entity")) {
            out.add(new Target("entity", optString(o, "entity"),
                    o.has("count") ? o.get("count").getAsInt() : 1));
        } else if (o.has("effect")) {
            out.add(new Target("effect", optString(o, "effect"), 1));
        } else if (o.has("advancement")) {
            out.add(new Target("advancement", optString(o, "advancement"), 1));
        } else if (o.has("stat")) {
            out.add(new Target("stat", optString(o, "stat"),
                    o.has("count") ? o.get("count").getAsInt() : 1));
        }
    }


    // ------------------------------------------------------------
    //  HELPERS
    // ------------------------------------------------------------

    private static Reader supplierToReader(IoSupplier<InputStream> supplier) throws IOException {
        return new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8));
    }

    private static boolean isQuestDisabled(Quest q) {
        return Config.disabledCategories().contains(q.category);
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive()
                ? o.get(key).getAsString() : null;
    }

    private static int parseIntFlexible(JsonObject o, String key, int def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p.isNumber()) return p.getAsInt();
        try { return Integer.parseInt(p.getAsString()); } catch (Exception ignored) { return def; }
    }

    private static boolean parseBoolFlexible(JsonObject o, String key, boolean def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p.isBoolean()) return p.getAsBoolean();
        try { return Boolean.parseBoolean(p.getAsString()); } catch (Exception ignored) { return def; }
    }

    // ------------------------------------------------------------
    //  PUBLIC API
    // ------------------------------------------------------------

    public static boolean isEmpty() {
        return QUESTS.isEmpty();
    }

    public static Collection<Quest> all() {
        if (!loadedClient) loadClient(false);
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static Collection<Quest> allServer(MinecraftServer server) {
        loadServer(server, false);
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

    public static Optional<Category> categoryById(String id) {
        if (!loadedClient) loadClient(false);
        return Optional.ofNullable(CATEGORIES.get(id));
    }

    public static boolean isCategoryUnlocked(Category c, net.minecraft.world.entity.player.Player player) {
        if (c == null) return true;
        if (c.dependency == null || c.dependency.isBlank()) return true;
        var q = byId(c.dependency).orElse(null);
        if (q == null) return true;
        return QuestTracker.getStatus(q, player) == QuestTracker.Status.REDEEMED;
    }

    public static boolean includeQuestInAll(Quest q, net.minecraft.world.entity.player.Player player) {
        if (q == null) return false;
        Category c = CATEGORIES.getOrDefault(q.category, null);
        if (c == null) return true;
        if (c.excludeFromAll) return false;
        return isCategoryUnlocked(c, player);
    }

    public static List<Category> categoriesOrdered() {
        if (!loadedClient) loadClient(false);

        if (!CATEGORIES.containsKey("all"))
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));

        List<Category> list = new ArrayList<>(CATEGORIES.values());
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;
            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });
        return list;
    }
}
