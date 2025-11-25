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
import net.revilodev.boundless.BoundlessMod;
import net.revilodev.boundless.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
    // Model classes
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
                     String type, Completion completion, String category) {
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
        public final String command;
        public final String expType;   // "points" / "levels" or ""
        public final int expAmount;    // >= 0

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

        public Category(String id, String icon, String name, int order, boolean excludeFromAll, String dependency) {
            this.id = id;
            this.icon = icon == null || icon.isBlank() ? "minecraft:book" : icon;
            this.name = name == null || name.isBlank() ? id : name;
            this.order = order;
            this.excludeFromAll = excludeFromAll;
            this.dependency = dependency == null ? "" : dependency;
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

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static boolean isQuestDisabled(Quest q) {
        return Config.disabledCategories().contains(q.category);
    }

    public static void forceReloadAll(MinecraftServer server) {
        loadedClient = false;
        loadedServer = false;
        loadServer(server, true);
    }

    // ------------------------------------------------------------
    // Core loading logic (old working logic, with disabled-category filtering)
    // ------------------------------------------------------------

    private static synchronized void load(ResourceManager rm, boolean forceReload) {
        if ((loadedClient || loadedServer) && !forceReload && !QUESTS.isEmpty()) return;
        QUESTS.clear();
        CATEGORIES.clear();

        BoundlessMod.LOGGER.info("QuestData: Begin load, forceReload={}", forceReload);

        // ---------- Categories from normal resource manager stacks ----------
        Map<ResourceLocation, List<Resource>> catStacks =
                rm.listResourceStacks(PATH_CATEGORIES, rl -> rl.getPath().endsWith(".json"));
        Map<String, Integer> catByNs = new HashMap<>();
        for (ResourceLocation rl : catStacks.keySet()) catByNs.merge(rl.getNamespace(), 1, Integer::sum);
        BoundlessMod.LOGGER.info("QuestData: category stacks total={}, namespaces={}", catStacks.size(), catByNs);

        for (Map.Entry<ResourceLocation, List<Resource>> e : catStacks.entrySet()) {
            List<Resource> stack = e.getValue();
            if (stack.isEmpty()) continue;
            Resource top = stack.get(stack.size() - 1);
            String topPack = safePackId(top);
            if (stack.size() > 1) {
                List<String> packs = new ArrayList<>();
                for (Resource r : stack) packs.add(safePackId(r));
                BoundlessMod.LOGGER.info("QuestData: category overlay {} packs={}", e.getKey(), packs);
            }
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                String id = optString(obj, "id");
                String icon = optString(obj, "icon");
                String cname = optString(obj, "name");
                int order = parseIntFlexible(obj, "order", 0);
                boolean excludeFromAll = parseBoolFlexible(obj, "exclude_from_all", false);
                String dependency = optString(obj, "dependency");
                if (id != null && !id.isBlank()) {
                    CATEGORIES.put(id, new Category(id, icon, cname, order, excludeFromAll, dependency));
                    BoundlessMod.LOGGER.info("QuestData: category loaded id={} from {}", id, e.getKey() + "@" + topPack);
                }
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("QuestData: bad category {}: {}", e.getKey(), ex.toString());
            }
        }

        // ---------- Quests from normal resource manager stacks ----------
        Map<ResourceLocation, List<Resource>> questStacks =
                rm.listResourceStacks(PATH_QUESTS, rl -> rl.getPath().endsWith(".json"));
        Map<String, Integer> questsByNs = new HashMap<>();
        for (ResourceLocation rl : questStacks.keySet()) questsByNs.merge(rl.getNamespace(), 1, Integer::sum);
        BoundlessMod.LOGGER.info("QuestData: quest stacks total={}, namespaces={}", questStacks.size(), questsByNs);

        for (Map.Entry<ResourceLocation, List<Resource>> e : questStacks.entrySet()) {
            ResourceLocation rl = e.getKey();
            if (rl.getPath().contains("/categories/")) continue;
            List<Resource> stack = e.getValue();
            if (stack.isEmpty()) continue;
            Resource top = stack.get(stack.size() - 1);
            String topPack = safePackId(top);
            if (stack.size() > 1) {
                List<String> packs = new ArrayList<>();
                for (Resource r : stack) packs.add(safePackId(r));
                BoundlessMod.LOGGER.info("QuestData: quest overlay {} packs={}", rl, packs);
            }
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonObject obj = safeObject(reader);
                if (obj == null) continue;
                Quest q = parseQuestObject(obj, rl);
                if (q != null) {
                    if (!isQuestDisabled(q)) {
                        QUESTS.put(q.id, q);
                        BoundlessMod.LOGGER.info("QuestData: quest loaded id={} from {}", q.id, rl + "@" + topPack);
                    } else {
                        BoundlessMod.LOGGER.info("QuestData: quest DISABLED id={} (category={})", q.id, q.category);
                    }
                }
            } catch (IOException ex) {
                BoundlessMod.LOGGER.error("QuestData: read error {}: {}", rl, ex.toString());
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("QuestData: bad quest {}: {}", rl, ex.toString());
            }
        }

        // ---------- Extra: scan selected resource packs for SERVER_DATA (the bit that made it work) ----------
        if (FMLEnvironment.dist == Dist.CLIENT) {
            scanSelectedResourcePacksData();
        }

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
        }
        BoundlessMod.LOGGER.info("QuestData: Loaded {} quest(s), {} category(ies).", QUESTS.size(), CATEGORIES.size());
    }

    private static void scanSelectedResourcePacksData() {
        try {
            var repo = Minecraft.getInstance().getResourcePackRepository();
            Collection<Pack> selected = repo.getSelectedPacks();
            List<String> packIds = new ArrayList<>();
            for (Pack p : selected) packIds.add(p.getId());
            BoundlessMod.LOGGER.info("QuestData: RP selected packs={}", packIds);
            int dataCatCount = 0;
            int dataQuestCount = 0;
            for (Pack p : selected) {
                try (PackResources res = p.open()) {
                    Set<String> namespaces = res.getNamespaces(net.minecraft.server.packs.PackType.SERVER_DATA);
                    BoundlessMod.LOGGER.info("QuestData: RP '{}' data namespaces={}", p.getId(), namespaces);
                    for (String ns : namespaces) {
                        dataCatCount += listDataCategoriesFromPack(p.getId(), res, ns);
                        dataQuestCount += listDataQuestsFromPack(p.getId(), res, ns);
                    }
                } catch (Throwable t) {
                    BoundlessMod.LOGGER.error("QuestData: RP open failed {}: {}", p.getId(), t.toString());
                }
            }
            BoundlessMod.LOGGER.info("QuestData: RP data totals categories={}, quests={}", dataCatCount, dataQuestCount);
        } catch (Throwable t) {
            BoundlessMod.LOGGER.error("QuestData: RP scan failed: {}", t.toString());
        }
    }

    private static int listDataCategoriesFromPack(String packId, PackResources res, String ns) {
        final int[] count = {0};
        res.listResources(net.minecraft.server.packs.PackType.SERVER_DATA, ns, PATH_CATEGORIES, (loc, supplier) -> {
            if (!loc.getPath().endsWith(".json")) return;
            try (Reader r = supplierToReader(supplier)) {
                JsonObject obj = safeObject(r);
                if (obj == null) return;
                String id = optString(obj, "id");
                String icon = optString(obj, "icon");
                String cname = optString(obj, "name");
                int order = parseIntFlexible(obj, "order", 0);
                boolean excludeFromAll = parseBoolFlexible(obj, "exclude_from_all", false);
                String dependency = optString(obj, "dependency");
                if (id != null && !id.isBlank()) {
                    CATEGORIES.put(id, new Category(id, icon, cname, order, excludeFromAll, dependency));
                    BoundlessMod.LOGGER.info("QuestData: RP-DATA category id={} from {}@{}", id, loc, packId);
                    count[0]++;
                }
            } catch (IOException ex) {
                BoundlessMod.LOGGER.error("QuestData: RP-DATA category read error {}@{}: {}", loc, packId, ex.toString());
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("QuestData: RP-DATA category bad {}@{}: {}", loc, packId, ex.toString());
            }
        });
        return count[0];
    }

    private static int listDataQuestsFromPack(String packId, PackResources res, String ns) {
        final int[] count = {0};
        res.listResources(net.minecraft.server.packs.PackType.SERVER_DATA, ns, PATH_QUESTS, (loc, supplier) -> {
            if (!loc.getPath().endsWith(".json")) return;
            if (loc.getPath().contains("/categories/")) return;
            try (Reader r = supplierToReader(supplier)) {
                JsonObject obj = safeObject(r);
                if (obj == null) return;
                Quest q = parseQuestObject(obj, loc);
                if (q != null) {
                    if (!isQuestDisabled(q)) {
                        QUESTS.put(q.id, q);
                        BoundlessMod.LOGGER.info("QuestData: RP-DATA quest id={} from {}@{}", q.id, loc, packId);
                        count[0]++;
                    } else {
                        BoundlessMod.LOGGER.info("QuestData: RP-DATA quest DISABLED id={} (category={})", q.id, q.category);
                    }
                }
            } catch (IOException ex) {
                BoundlessMod.LOGGER.error("QuestData: RP-DATA quest read error {}@{}: {}", loc, packId, ex.toString());
            } catch (Exception ex) {
                BoundlessMod.LOGGER.error("QuestData: RP-DATA quest bad {}@{}: {}", loc, packId, ex.toString());
            }
        });
        return count[0];
    }

    private static Reader supplierToReader(IoSupplier<java.io.InputStream> supplier) throws IOException {
        return new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8));
    }

    private static String safePackId(Resource r) {
        try { return r.sourcePackId(); } catch (Throwable ignored) { return "unknown"; }
    }

    private static JsonObject safeObject(Reader reader) {
        JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    // ------------------------------------------------------------
    // Parsing JSON pieces
    // ------------------------------------------------------------

    private static Quest parseQuestObject(JsonObject obj, ResourceLocation src) {
        String id = optString(obj, "id");
        if (id == null || id.isBlank()) {
            String p = src.getPath();
            int lastSlash = p.lastIndexOf('/');
            String n = (lastSlash >= 0 ? p.substring(lastSlash + 1) : p);
            id = n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
        }
        String name = optString(obj, "name");
        String icon = optString(obj, "icon");
        String description = optString(obj, "description");
        List<String> deps = parseDependencies(obj);
        boolean optional = parseBoolFlexible(obj, "optional", false);
        Rewards rewards = parseRewards(obj.get("reward"));
        String type = optString(obj, "type");
        Completion completion = parseCompletion(obj.get("completion"), type);
        String category = optString(obj, "category");
        return new Quest(id, name, icon, description, deps, optional, rewards, type, completion, category);
    }

    private static List<String> parseDependencies(JsonObject obj) {
        List<String> deps = new ArrayList<>();
        if (obj.has("dependencies")) {
            JsonElement el = obj.get("dependencies");
            if (el.isJsonArray()) {
                for (JsonElement d : el.getAsJsonArray()) {
                    if (d.isJsonPrimitive()) {
                        String s = d.getAsString();
                        if (!s.isBlank()) deps.add(s);
                    }
                }
            } else if (el.isJsonPrimitive()) {
                String s = el.getAsString();
                if (!s.isBlank()) deps.add(s);
            }
        }
        return deps;
    }

    /**
     * Extended rewards parser:
     *  - Original formats still work (arrays of items, object with item/count, string command).
     *  - New format with exp:
     *      {
     *        "items": [...],
     *        "command": "...",
     *        "exp": "points" | "levels",
     *        "count": <number>
     *      }
     */
    private static Rewards parseRewards(JsonElement el) {
        if (el == null) return new Rewards(List.of(), "", "", 0);

        List<RewardEntry> out = new ArrayList<>();
        String cmd = "";
        String expType = "";
        int expAmount = 0;

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();

            // Items (either "items": [] or "item"/"count")
            if (obj.has("items") && obj.get("items").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("items")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject r = e.getAsJsonObject();
                    String item = optString(r, "item");
                    int count = r.has("count") ? r.get("count").getAsInt() : 1;
                    if (item != null && !item.isBlank()) out.add(new RewardEntry(item, count));
                }
            } else if (obj.has("item")) {
                String item = optString(obj, "item");
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) out.add(new RewardEntry(item, count));
            }

            // Optional command
            if (obj.has("command") && obj.get("command").isJsonPrimitive()) {
                cmd = obj.get("command").getAsString();
            }

            // Optional exp reward: "exp": "points"/"levels", "count": number
            if (obj.has("exp")) {
                JsonElement ex = obj.get("exp");
                if (ex.isJsonPrimitive()) {
                    expType = ex.getAsString();
                }
            }
            if (obj.has("count") && obj.get("count").isJsonPrimitive()
                    && obj.getAsJsonPrimitive("count").isNumber()) {
                expAmount = obj.getAsJsonPrimitive("count").getAsInt();
            }

            return new Rewards(out, cmd, expType, expAmount);
        }

        // Old array form: just items
        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject r = e.getAsJsonObject();
                String item = optString(r, "item");
                int count = r.has("count") ? r.get("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) out.add(new RewardEntry(item, count));
            }
            return new Rewards(out, "", "", 0);
        }

        // Old string form: treated as command
        if (el.isJsonPrimitive()) {
            String maybeCmd = el.getAsString();
            if (!maybeCmd.isBlank()) return new Rewards(List.of(), maybeCmd, "", 0);
        }

        return new Rewards(List.of(), "", "", 0);
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

            if (obj.has("collect")) {
                JsonElement cEl = obj.get("collect");
                int count = obj.has("count") && obj.get("count").isJsonPrimitive()
                        && obj.getAsJsonPrimitive("count").isNumber()
                        ? obj.get("count").getAsInt() : 1;
                if (cEl.isJsonArray()) {
                    for (JsonElement ce : cEl.getAsJsonArray()) {
                        if (!ce.isJsonPrimitive()) continue;
                        String id = ce.getAsString();
                        if (!id.isBlank()) out.add(new Target("item", id, count));
                    }
                } else if (cEl.isJsonPrimitive()) {
                    String id = cEl.getAsString();
                    if (!id.isBlank()) out.add(new Target("item", id, count));
                }
                return new Completion(out);
            }

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
            if (obj.has("stats") && obj.get("stats").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("stats")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject o = e.getAsJsonObject();
                    String stat = optString(o, "stat");
                    int count = o.has("count") ? o.get("count").getAsInt() : 1;
                    if (stat != null && !stat.isBlank()) out.add(new Target("stat", stat, count));
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
        } else if (o.has("stat")) {
            String stat = optString(o, "stat");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (stat != null && !stat.isBlank()) out.add(new Target("stat", stat, count));
        }
    }

    // ------------------------------------------------------------
    // Load entrypoints (client/server)
    // ------------------------------------------------------------

    public static synchronized void loadClient(boolean forceReload) {
        if (loadedClient && !forceReload && !QUESTS.isEmpty()) return;
        BoundlessMod.LOGGER.info("QuestData: loadClient()");
        try {
            var repo = Minecraft.getInstance().getResourcePackRepository();
            Collection<Pack> selected = repo.getSelectedPacks();
            List<String> packIds = new ArrayList<>();
            for (Pack p : selected) packIds.add(p.getId());
            BoundlessMod.LOGGER.info("QuestData: RP selected at loadClient: {}", packIds);
        } catch (Throwable t) {
            BoundlessMod.LOGGER.warn("QuestData: RP list failed: {}", t.toString());
        }
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        load(rm, forceReload);
        loadedClient = true;
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        if (loadedServer && !forceReload && !QUESTS.isEmpty()) return;
        BoundlessMod.LOGGER.info("QuestData: loadServer()");
        var resources = server.getServerResources();
        ResourceManager rm = resources.resourceManager();
        load(rm, forceReload);
        loadedServer = true;

        // When running an integrated server, also resync client-side view
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BoundlessMod.LOGGER.info("QuestData: client resync after server load");
            ResourceManager crm = Minecraft.getInstance().getResourceManager();
            load(crm, true);
            loadedClient = true;
        }
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    public static boolean isEmpty() { return QUESTS.isEmpty(); }

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

    public static synchronized List<Category> categoriesOrderedServer(MinecraftServer server) {
        loadServer(server, false);
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

    // ------------------------------------------------------------
    // Network sync (SyncQuests → client)
    // ------------------------------------------------------------

    public static synchronized void applyNetworkJson(String json) {
        QUESTS.clear();
        CATEGORIES.clear();
        try {
            JsonElement rootEl = GSON.fromJson(json, JsonElement.class);
            if (rootEl == null || !rootEl.isJsonObject()) {
                loadedClient = false;
                return;
            }
            JsonObject root = rootEl.getAsJsonObject();

            // Categories
            if (root.has("categories") && root.get("categories").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("categories")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = optString(o, "id");
                    if (id == null || id.isBlank()) continue;
                    String icon = optString(o, "icon");
                    String name = optString(o, "name");
                    int order = parseIntFlexible(o, "order", 0);
                    boolean excludeFromAll = parseBoolFlexible(o, "excludeFromAll", false);
                    String dependency = optString(o, "dependency");
                    CATEGORIES.put(id, new Category(id, icon, name, order, excludeFromAll, dependency));
                }
            }

            // Quests
            if (root.has("quests") && root.get("quests").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("quests")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = optString(o, "id");
                    if (id == null || id.isBlank()) continue;
                    String name = optString(o, "name");
                    String icon = optString(o, "icon");
                    String description = optString(o, "description");

                    List<String> deps = new ArrayList<>();
                    if (o.has("dependencies") && o.get("dependencies").isJsonArray()) {
                        for (JsonElement d : o.getAsJsonArray("dependencies")) {
                            if (d.isJsonPrimitive()) {
                                String s = d.getAsString();
                                if (!s.isBlank()) deps.add(s);
                            }
                        }
                    }

                    boolean optional = parseBoolFlexible(o, "optional", false);

                    Rewards rewards;
                    if (o.has("rewards") && o.get("rewards").isJsonObject()) {
                        JsonObject ro = o.getAsJsonObject("rewards");
                        List<RewardEntry> rItems = new ArrayList<>();
                        if (ro.has("items") && ro.get("items").isJsonArray()) {
                            for (JsonElement ie : ro.getAsJsonArray("items")) {
                                if (!ie.isJsonObject()) continue;
                                JsonObject io = ie.getAsJsonObject();
                                String item = optString(io, "item");
                                int count = io.has("count") ? io.get("count").getAsInt() : 1;
                                if (item != null && !item.isBlank()) rItems.add(new RewardEntry(item, count));
                            }
                        }
                        String cmd = optString(ro, "command");
                        String expType = optString(ro, "expType");
                        int expAmount = ro.has("expAmount")
                                && ro.get("expAmount").isJsonPrimitive()
                                && ro.getAsJsonPrimitive("expAmount").isNumber()
                                ? ro.get("expAmount").getAsInt() : 0;
                        rewards = new Rewards(rItems, cmd, expType, expAmount);
                    } else {
                        rewards = new Rewards(List.of(), "", "", 0);
                    }

                    String type = optString(o, "type");

                    Completion completion;
                    if (o.has("completion") && o.get("completion").isJsonObject()) {
                        JsonObject co = o.getAsJsonObject("completion");
                        List<Target> targets = new ArrayList<>();
                        if (co.has("targets") && co.get("targets").isJsonArray()) {
                            for (JsonElement te : co.getAsJsonArray("targets")) {
                                if (!te.isJsonObject()) continue;
                                JsonObject to = te.getAsJsonObject();
                                String kind = optString(to, "kind");
                                String tid = optString(to, "id");
                                int count = to.has("count") ? to.get("count").getAsInt() : 1;
                                if (kind != null && !kind.isBlank() && tid != null && !tid.isBlank())
                                    targets.add(new Target(kind, tid, count));
                            }
                        }
                        completion = new Completion(targets);
                    } else {
                        completion = new Completion(List.of());
                    }

                    String category = optString(o, "category");
                    Quest q = new Quest(id, name, icon, description, deps, optional, rewards, type, completion, category);
                    if (!isQuestDisabled(q)) {
                        QUESTS.put(q.id, q);
                    }
                }
            }

            if (!CATEGORIES.containsKey("all"))
                CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));

            loadedClient = true;
        } catch (Exception e) {
            QUESTS.clear();
            CATEGORIES.clear();
            loadedClient = false;
        }
    }

    // ------------------------------------------------------------
    // Small JSON helpers
    // ------------------------------------------------------------

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
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
}
