// src/main/java/net/revilodev/boundless/quest/QuestData.java
package net.revilodev.boundless.quest;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
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

    private static String lastWorldId = null;

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
            this.category = (category == null || category.isBlank()) ? "misc" : category;
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
        public final List<CommandReward> commands;
        public final List<FunctionReward> functions;
        public final String expType;
        public final int expAmount;

        public Rewards(List<RewardEntry> items,
                       List<CommandReward> commands,
                       List<FunctionReward> functions,
                       String expType,
                       int expAmount) {
            this.items = items == null ? List.of() : List.copyOf(items);
            this.commands = commands == null ? List.of() : List.copyOf(commands);
            this.functions = functions == null ? List.of() : List.copyOf(functions);
            this.expType = expType == null ? "" : expType;
            this.expAmount = Math.max(0, expAmount);
        }

        public boolean hasExp() { return !expType.isBlank() && expAmount > 0; }
        public boolean hasCommands() { return commands != null && !commands.isEmpty(); }
        public boolean hasFunctions() { return functions != null && !functions.isEmpty(); }
        public boolean hasAny() {
            return (items != null && !items.isEmpty()) || hasCommands() || hasFunctions() || hasExp();
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

    public static final class CommandReward {
        public final String command;
        public final String icon;
        public final String title;

        public CommandReward(String command, String icon, String title) {
            this.command = command == null ? "" : command;
            this.icon = icon == null ? "" : icon;
            this.title = title == null ? "" : title;
        }
    }

    public static final class FunctionReward {
        public final String function;
        public final String icon;
        public final String title;

        public FunctionReward(String function, String icon, String title) {
            this.function = function == null ? "" : function;
            this.icon = icon == null ? "" : icon;
            this.title = title == null ? "" : title;
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
        public boolean isSubmit() { return "submit".equals(kind); }
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

        public Category(String id, String icon, String name, int order,
                        boolean excludeFromAll, String dependency) {
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

    private static boolean isQuestDisabled(Quest q) {
        return Config.disabledCategories().contains(q.category);
    }

    private static boolean shouldIgnoreQuestJson(ResourceLocation loc) {
        if (loc == null) return false;
        String p = loc.getPath();
        if (p == null || p.isBlank()) return false;

        String pl = p.toLowerCase(Locale.ROOT);

        if (pl.equals("quests/example.json")) return true;
        if (pl.equals("quests/_example.json")) return true;

        if (pl.endsWith("/example.json")) return true;
        if (pl.endsWith("/_example.json")) return true;

        return false;
    }

    public static void forceReloadAll(MinecraftServer server) {
        loadedClient = false;
        loadedServer = false;
        lastWorldId = null;
        loadServer(server, true);
    }

    private static synchronized void load(ResourceManager rm, boolean forceReload) {
        if ((loadedClient || loadedServer) && !forceReload && !QUESTS.isEmpty()) return;

        QUESTS.clear();
        CATEGORIES.clear();

        Map<ResourceLocation, List<Resource>> catStacks =
                rm.listResourceStacks(PATH_CATEGORIES, rl -> rl.getPath().endsWith(".json"));

        for (var entry : catStacks.entrySet()) {
            List<Resource> stack = entry.getValue();
            if (stack == null || stack.isEmpty()) continue;

            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonObject obj = safeObject(reader);
                if (obj == null) continue;

                String id = optString(obj, "id");
                if (id == null || id.isBlank()) continue;

                String icon = optString(obj, "icon");
                String cname = optString(obj, "name");
                int order = parseIntFlexible(obj, "order", 0);
                boolean excludeFromAll = parseBoolFlexible(obj, "exclude_from_all", false);
                String dependency = optString(obj, "dependency");

                CATEGORIES.put(id, new Category(id, icon, cname, order, excludeFromAll, dependency));
            } catch (Exception ignored) {}
        }

        Map<ResourceLocation, List<Resource>> questStacks =
                rm.listResourceStacks(PATH_QUESTS, rl -> rl.getPath().endsWith(".json"));

        for (var entry : questStacks.entrySet()) {
            ResourceLocation loc = entry.getKey();
            if (loc.getPath().contains("/categories/")) continue;
            if (shouldIgnoreQuestJson(loc)) continue;

            List<Resource> stack = entry.getValue();
            if (stack == null || stack.isEmpty()) continue;

            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonObject obj = safeObject(reader);
                if (obj == null) continue;

                Quest q = parseQuestObject(obj, loc);
                if (q != null && !isQuestDisabled(q)) QUESTS.put(q.id, q);
            } catch (Exception ignored) {}
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            scanSelectedResourcePacksData();
        }

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All",
                    Integer.MIN_VALUE, false, ""));
        }
    }

    private static void scanSelectedResourcePacksData() {
        try {
            var repo = Minecraft.getInstance().getResourcePackRepository();
            var selected = repo.getSelectedPacks();

            for (var pack : selected) {
                try (PackResources res = pack.open()) {
                    Set<String> namespaces = res.getNamespaces(PackType.SERVER_DATA);
                    for (String ns : namespaces) {
                        listDataCategoriesFromPack(res, ns);
                        listDataQuestsFromPack(res, ns);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static int listDataCategoriesFromPack(PackResources res, String ns) {
        final int[] count = {0};
        res.listResources(PackType.SERVER_DATA, ns, PATH_CATEGORIES, (loc, supplier) -> {
            if (!loc.getPath().endsWith(".json")) return;
            try (Reader r = supplierToReader(supplier)) {
                JsonObject obj = safeObject(r);
                if (obj == null) return;

                String id = optString(obj, "id");
                if (id == null || id.isBlank()) return;

                String icon = optString(obj, "icon");
                String cname = optString(obj, "name");
                int order = parseIntFlexible(obj, "order", 0);
                boolean excludeFromAll = parseBoolFlexible(obj, "exclude_from_all", false);
                String dependency = optString(obj, "dependency");

                CATEGORIES.put(id, new Category(id, icon, cname, order, excludeFromAll, dependency));
                count[0]++;
            } catch (Exception ignored) {}
        });
        return count[0];
    }

    private static int listDataQuestsFromPack(PackResources res, String ns) {
        final int[] count = {0};
        res.listResources(PackType.SERVER_DATA, ns, PATH_QUESTS, (loc, supplier) -> {
            if (!loc.getPath().endsWith(".json")) return;
            if (loc.getPath().contains("/categories/")) return;
            if (shouldIgnoreQuestJson(loc)) return;

            try (Reader r = supplierToReader(supplier)) {
                JsonObject obj = safeObject(r);
                if (obj == null) return;

                Quest q = parseQuestObject(obj, loc);
                if (q != null && !isQuestDisabled(q)) {
                    QUESTS.put(q.id, q);
                    count[0]++;
                }
            } catch (Exception ignored) {}
        });
        return count[0];
    }

    private static Reader supplierToReader(IoSupplier<java.io.InputStream> supplier) throws IOException {
        return new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8));
    }

    public static synchronized void loadClient(boolean forceReload) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        Minecraft mc = Minecraft.getInstance();
        String worldId = null;

        try {
            if (mc.getSingleplayerServer() != null) {
                var swd = mc.getSingleplayerServer().getWorldData();
                worldId = swd.getLevelName() + "@" + swd.worldGenOptions().seed();
            }
        } catch (Throwable ignored) {}

        if (!forceReload && !QUESTS.isEmpty()
                && lastWorldId != null
                && Objects.equals(lastWorldId, worldId)
                && loadedClient) {
            return;
        }

        lastWorldId = worldId;

        ResourceManager rm = mc.getResourceManager();
        load(rm, forceReload);
        loadedClient = true;
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        String worldId;
        try {
            worldId = server.getWorldData().getLevelName() + "@" + server.getWorldData().worldGenOptions().seed();
        } catch (Throwable t) {
            worldId = server.getWorldData().getLevelName();
        }

        if (!forceReload && !QUESTS.isEmpty()
                && lastWorldId != null
                && Objects.equals(lastWorldId, worldId)
                && loadedServer) {
            return;
        }

        lastWorldId = worldId;

        ResourceManager rm = server.getServerResources().resourceManager();
        load(rm, forceReload);
        loadedServer = true;
    }

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

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All",
                    Integer.MIN_VALUE, false, ""));
        }

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

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All",
                    Integer.MIN_VALUE, false, ""));
        }

        List<Category> list = new ArrayList<>(CATEGORIES.values());
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;

            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });

        return list;
    }

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

        JsonElement rewardEl = obj.has("reward") ? obj.get("reward") : null;
        Rewards rewards = parseRewards(rewardEl);

        String type = optString(obj, "type");
        Completion completion = parseCompletion(obj.get("completion"), type);

        String category = optString(obj, "category");

        return new Quest(id, name, icon, description, deps, optional, rewards, type, completion, category);
    }

    private static List<String> parseDependencies(JsonObject obj) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (!obj.has("dependencies")) return List.of();

        JsonElement depEl = obj.get("dependencies");

        if (depEl.isJsonArray()) {
            for (JsonElement d : depEl.getAsJsonArray()) {
                if (d.isJsonPrimitive()) {
                    String s = d.getAsString();
                    if (s != null && !s.isBlank()) set.add(s);
                }
            }
        } else if (depEl.isJsonPrimitive()) {
            String s = depEl.getAsString();
            if (s != null && !s.isBlank()) set.add(s);
        }

        return new ArrayList<>(set);
    }

    private static Rewards parseRewards(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return new Rewards(List.of(), List.of(), List.of(), "", 0);
        }

        List<RewardEntry> items = new ArrayList<>();
        List<CommandReward> commands = new ArrayList<>();
        List<FunctionReward> functions = new ArrayList<>();
        String expType = "";
        int expAmount = 0;

        if (el.isJsonPrimitive()) {
            String cmd = el.getAsString();
            if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, "", ""));
            return new Rewards(items, commands, functions, expType, expAmount);
        }

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject r = e.getAsJsonObject();
                String item = optString(r, "item");
                int count = r.has("count") && r.get("count").isJsonPrimitive() && r.getAsJsonPrimitive("count").isNumber()
                        ? r.getAsJsonPrimitive("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
            }
            return new Rewards(items, commands, functions, expType, expAmount);
        }

        if (!el.isJsonObject()) {
            return new Rewards(items, commands, functions, expType, expAmount);
        }

        JsonObject obj = el.getAsJsonObject();

        if (obj.has("items") && obj.get("items").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("items")) {
                if (!e.isJsonObject()) continue;
                JsonObject r = e.getAsJsonObject();
                String item = optString(r, "item");
                int count = r.has("count") && r.get("count").isJsonPrimitive() && r.getAsJsonPrimitive("count").isNumber()
                        ? r.getAsJsonPrimitive("count").getAsInt() : 1;
                if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
            }
        } else if (obj.has("item")) {
            String item = optString(obj, "item");
            int count = obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                    ? obj.getAsJsonPrimitive("count").getAsInt() : 1;
            if (item != null && !item.isBlank()) items.add(new RewardEntry(item, count));
        }

        if (obj.has("command") && obj.get("command").isJsonPrimitive()) {
            String cmd = obj.get("command").getAsString();
            if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, "", ""));
        }

        if (obj.has("commands") && obj.get("commands").isJsonArray()) {
            for (JsonElement ce : obj.getAsJsonArray("commands")) {
                if (ce == null) continue;
                if (ce.isJsonPrimitive()) {
                    String cmd = ce.getAsString();
                    if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, "", ""));
                    continue;
                }
                if (!ce.isJsonObject()) continue;
                JsonObject co = ce.getAsJsonObject();
                String cmd = optString(co, "command");
                String icon = optString(co, "icon");
                String title = optString(co, "title");
                if (cmd != null && !cmd.isBlank()) commands.add(new CommandReward(cmd, icon, title));
            }
        }

        if (obj.has("functions") && obj.get("functions").isJsonArray()) {
            for (JsonElement fe : obj.getAsJsonArray("functions")) {
                if (fe == null) continue;
                if (fe.isJsonPrimitive()) {
                    String fn = fe.getAsString();
                    if (fn != null && !fn.isBlank()) functions.add(new FunctionReward(fn, "", ""));
                    continue;
                }
                if (!fe.isJsonObject()) continue;
                JsonObject fo = fe.getAsJsonObject();
                String fn = optString(fo, "function");
                String icon = optString(fo, "icon");
                String title = optString(fo, "title");
                if (fn != null && !fn.isBlank()) functions.add(new FunctionReward(fn, icon, title));
            }
        }

        if (obj.has("exp") && obj.get("exp").isJsonPrimitive()) {
            expType = obj.get("exp").getAsString();
        }
        if (obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()) {
            expAmount = obj.getAsJsonPrimitive("count").getAsInt();
        }

        return new Rewards(items, commands, functions, expType, expAmount);
    }

    private static Completion parseCompletion(JsonElement el, String type) {
        if (el == null || el.isJsonNull()) return new Completion(List.of());
        List<Target> out = new ArrayList<>();

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("complete") && obj.get("complete").isJsonArray()) {
                for (JsonElement e : obj.getAsJsonArray("complete")) {
                    if (e.isJsonObject()) parseNewFormatTarget(e.getAsJsonObject(), out);
                }
                return new Completion(out);
            }
        }

        if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) {
                if (e.isJsonObject()) parseTargetObject(e.getAsJsonObject(), out);
            }
            return new Completion(out);
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();

            if (obj.has("collect")) {
                JsonElement cEl = obj.get("collect");
                int count = obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                        ? obj.getAsJsonPrimitive("count").getAsInt() : 1;

                if (cEl.isJsonArray()) {
                    for (JsonElement ce : cEl.getAsJsonArray()) {
                        if (!ce.isJsonPrimitive()) continue;
                        out.add(new Target("item", ce.getAsString(), count));
                    }
                } else if (cEl.isJsonPrimitive()) {
                    out.add(new Target("item", cEl.getAsString(), count));
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

            if (obj.has("item")) {
                out.add(new Target("item", optString(obj, "item"),
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
            if (obj.has("submit")) {
                out.add(new Target("submit", optString(obj, "submit"),
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
            if (obj.has("entity")) {
                out.add(new Target("entity", optString(obj, "entity"),
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
            if (obj.has("effect")) {
                out.add(new Target("effect", optString(obj, "effect"), 1));
                return new Completion(out);
            }
            if (obj.has("advancement")) {
                out.add(new Target("advancement", optString(obj, "advancement"), 1));
                return new Completion(out);
            }
            if (obj.has("stat")) {
                out.add(new Target("stat", optString(obj, "stat"),
                        obj.has("count") && obj.get("count").isJsonPrimitive() && obj.getAsJsonPrimitive("count").isNumber()
                                ? obj.getAsJsonPrimitive("count").getAsInt() : 1));
                return new Completion(out);
            }
        }

        return new Completion(List.of());
    }

    private static void parseNewFormatTarget(JsonObject o, List<Target> out) {
        if (o.has("collect")) {
            String id = o.get("collect").getAsString();
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            out.add(new Target("item", id, count));
            return;
        }

        if (o.has("submit")) {
            String id = o.get("submit").getAsString();
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            out.add(new Target("submit", id, count));
            return;
        }

        if (o.has("kill")) {
            String entity = o.get("kill").getAsString();
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            out.add(new Target("entity", entity, count));
            return;
        }

        if (o.has("achieve")) {
            String adv = o.get("achieve").getAsString();
            out.add(new Target("advancement", adv, 1));
            return;
        }

        if (o.has("effect")) {
            String eff = o.get("effect").getAsString();
            out.add(new Target("effect", eff, 1));
            return;
        }

        if (o.has("stat")) {
            String st = o.get("stat").getAsString();
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            out.add(new Target("stat", st, count));
        }
    }

    private static void parseTargetObject(JsonObject o, List<Target> out) {
        if (o.has("submit")) {
            String item = optString(o, "submit");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (item != null && !item.isBlank()) out.add(new Target("submit", item, count));
            return;
        }

        if (o.has("item")) {
            String item = optString(o, "item");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (item != null && !item.isBlank()) out.add(new Target("item", item, count));
            return;
        }

        if (o.has("entity")) {
            String entity = optString(o, "entity");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (entity != null && !entity.isBlank()) out.add(new Target("entity", entity, count));
            return;
        }

        if (o.has("effect")) {
            String eff = optString(o, "effect");
            if (eff != null && !eff.isBlank()) out.add(new Target("effect", eff, 1));
            return;
        }

        if (o.has("advancement")) {
            String adv = optString(o, "advancement");
            if (adv != null && !adv.isBlank()) out.add(new Target("advancement", adv, 1));
            return;
        }

        if (o.has("stat")) {
            String stat = optString(o, "stat");
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (stat != null && !stat.isBlank()) out.add(new Target("stat", stat, count));
        }
    }

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

                    Rewards rewards = new Rewards(List.of(), List.of(), List.of(), "", 0);
                    if (o.has("rewards") && o.get("rewards").isJsonObject()) {
                        JsonObject ro = o.getAsJsonObject("rewards");

                        List<RewardEntry> rItems = new ArrayList<>();
                        if (ro.has("items") && ro.get("items").isJsonArray()) {
                            for (JsonElement ie : ro.getAsJsonArray("items")) {
                                if (!ie.isJsonObject()) continue;
                                JsonObject io = ie.getAsJsonObject();
                                String item = optString(io, "item");
                                int cnt = io.has("count") ? io.get("count").getAsInt() : 1;
                                if (item != null && !item.isBlank()) rItems.add(new RewardEntry(item, cnt));
                            }
                        }

                        List<CommandReward> cmds = new ArrayList<>();
                        if (ro.has("command") && ro.get("command").isJsonPrimitive()) {
                            String c = ro.get("command").getAsString();
                            if (c != null && !c.isBlank()) cmds.add(new CommandReward(c, "", ""));
                        }
                        if (ro.has("commands") && ro.get("commands").isJsonArray()) {
                            for (JsonElement ce : ro.getAsJsonArray("commands")) {
                                if (ce == null) continue;
                                if (ce.isJsonPrimitive()) {
                                    String c = ce.getAsString();
                                    if (c != null && !c.isBlank()) cmds.add(new CommandReward(c, "", ""));
                                    continue;
                                }
                                if (!ce.isJsonObject()) continue;
                                JsonObject co = ce.getAsJsonObject();
                                String c = optString(co, "command");
                                String ic = optString(co, "icon");
                                String ti = optString(co, "title");
                                if (c != null && !c.isBlank()) cmds.add(new CommandReward(c, ic, ti));
                            }
                        }

                        List<FunctionReward> fns = new ArrayList<>();
                        if (ro.has("functions") && ro.get("functions").isJsonArray()) {
                            for (JsonElement fe : ro.getAsJsonArray("functions")) {
                                if (fe == null) continue;
                                if (fe.isJsonPrimitive()) {
                                    String f = fe.getAsString();
                                    if (f != null && !f.isBlank()) fns.add(new FunctionReward(f, "", ""));
                                    continue;
                                }
                                if (!fe.isJsonObject()) continue;
                                JsonObject fo = fe.getAsJsonObject();
                                String f = optString(fo, "function");
                                String ic = optString(fo, "icon");
                                String ti = optString(fo, "title");
                                if (f != null && !f.isBlank()) fns.add(new FunctionReward(f, ic, ti));
                            }
                        }

                        String expType = optString(ro, "expType");
                        int expAmount = ro.has("expAmount") && ro.get("expAmount").isJsonPrimitive() && ro.getAsJsonPrimitive("expAmount").isNumber()
                                ? ro.getAsJsonPrimitive("expAmount").getAsInt() : 0;

                        rewards = new Rewards(rItems, cmds, fns, expType, expAmount);
                    }

                    String type = optString(o, "type");

                    Completion completion = new Completion(List.of());
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
                                if (kind != null && !kind.isBlank() && tid != null && !tid.isBlank()) {
                                    targets.add(new Target(kind, tid, count));
                                }
                            }
                        }
                        completion = new Completion(targets);
                    }

                    String category = optString(o, "category");

                    Quest q = new Quest(id, name, icon, description, deps, optional, rewards, type, completion, category);
                    if (!isQuestDisabled(q)) QUESTS.put(q.id, q);
                }
            }

            if (!CATEGORIES.containsKey("all")) {
                CATEGORIES.put("all", new Category("all", "minecraft:book", "All",
                        Integer.MIN_VALUE, false, ""));
            }

            loadedClient = true;
        } catch (Exception e) {
            QUESTS.clear();
            CATEGORIES.clear();
            loadedClient = false;
        }
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static int parseIntFlexible(JsonObject o, String key, int def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p == null) return def;
        if (p.isNumber()) return p.getAsInt();
        try { return Integer.parseInt(p.getAsString()); } catch (Exception ignored) { return def; }
    }

    private static boolean parseBoolFlexible(JsonObject o, String key, boolean def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p == null) return def;
        if (p.isBoolean()) return p.getAsBoolean();
        try { return Boolean.parseBoolean(p.getAsString()); } catch (Exception ignored) { return def; }
    }

    private static JsonObject safeObject(Reader reader) {
        JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    @OnlyIn(Dist.CLIENT)
    private static String debugWorldIdClient() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                var swd = mc.getSingleplayerServer().getWorldData();
                return swd.getLevelName() + "@" + swd.worldGenOptions().seed();
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
