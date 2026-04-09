package net.revilodev.boundless.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.compat.LevelUpCompat;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class PinnedQuestHud {
    private static final ResourceLocation TEX_BG =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pinned_quest_toast.png");

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final int TEX_W = 160;
    private static final int TEX_H = 32;

    private static final int MAX_PINS = 3;

    private static final float TOAST_SCALE = 0.66f;
    private static final float ICON_SCALE = 0.80f;

    private static final int TITLE_COLOR = 0x2B2B2B;
    private static final int SUB_COLOR = 0x4A4A4A;

    private static final int CORNER_PAD = 10;
    private static final int TOP_EXTRA_DOWN = 12;
    private static final int LEFT_PAD = 6;

    private static final Deque<String> PINS = new ArrayDeque<>();
    private static final Map<String, ItemStack> ITEM_ICON_CACHE = new HashMap<>();
    private static boolean REGISTERED = false;

    private static boolean LOADED = false;
    private static String ACTIVE_KEY = null;

    private static String CURRENT_QUEST_ID = null;

    private PinnedQuestHud() {}

    public static void init() {
        ensureRegistered();
    }

    public static void setCurrentQuestId(String questId) {
        ensureRegistered();
        CURRENT_QUEST_ID = questId;
    }

    public static String getCurrentQuestId() {
        return CURRENT_QUEST_ID;
    }

    public static boolean isPinnedCurrentQuest() {
        String id = CURRENT_QUEST_ID;
        return id != null && isPinned(id);
    }

    public static void toggleCurrentQuest() {
        String id = CURRENT_QUEST_ID;
        if (id != null && !id.isBlank()) toggle(id);
    }

    public static void toggle(String questId) {
        if (questId == null || questId.isBlank()) return;
        if (Config.disableQuestPinning()) return;
        ensureRegistered();
        ensureLoaded();

        if (PINS.remove(questId)) {
            save();
            return;
        }

        while (PINS.size() >= MAX_PINS) PINS.pollFirst();
        PINS.addLast(questId);
        save();
    }

    public static boolean isPinned(String questId) {
        if (questId == null || questId.isBlank()) return false;
        if (Config.disableQuestPinning()) return false;
        ensureRegistered();
        ensureLoaded();
        return PINS.contains(questId);
    }

    public static void resetPinsOnLeave() {
        ensureRegistered();
        resetPins(true);
    }

    private static void ensureRegistered() {
        if (REGISTERED) return;
        REGISTERED = true;
        NeoForge.EVENT_BUS.addListener(PinnedQuestHud::onRenderGui);
        NeoForge.EVENT_BUS.addListener(PinnedQuestHud::onClientLogout);
    }

    private static void ensureLoaded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        String key = computeClientKey(mc);
        if (!LOADED || ACTIVE_KEY == null || !ACTIVE_KEY.equals(key)) {
            LOADED = true;
            ACTIVE_KEY = key;
            load();
        }
    }

    private static String computeClientKey(Minecraft mc) {
        try {
            if (mc.getSingleplayerServer() != null) {
                String name = mc.getSingleplayerServer().getWorldData().getLevelName();
                if (name == null || name.isBlank()) name = "world";
                return "sp_" + sanitize(name);
            }
            if (mc.getCurrentServer() != null) {
                String ip = mc.getCurrentServer().ip;
                if (ip == null || ip.isBlank()) ip = "multiplayer";
                return "mp_" + sanitize(ip);
            }
        } catch (Throwable ignored) {}
        return "default";
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "default";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '.'
                    || ch == '_'
                    || ch == '-') {
                out.append(ch);
            } else {
                out.append('_');
            }
        }
        return out.isEmpty() ? "default" : out.toString();
    }

    private static Path savePath() {
        Minecraft mc = Minecraft.getInstance();
        File dir = new File(mc.gameDirectory, "config/boundless/pins");
        String k = ACTIVE_KEY == null ? "default" : ACTIVE_KEY;
        return new File(dir, k + ".json").toPath();
    }

    private static void load() {
        PINS.clear();
        try {
            Path p = savePath();
            if (!Files.exists(p)) return;
            try (BufferedReader r = new BufferedReader(new FileReader(p.toFile()))) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                if (obj == null) return;
                JsonElement arrEl = obj.get("pins");
                if (arrEl != null && arrEl.isJsonArray()) {
                    JsonArray arr = arrEl.getAsJsonArray();
                    for (JsonElement el : arr) {
                        if (!el.isJsonPrimitive()) continue;
                        String id = el.getAsString();
                        if (id != null && !id.isBlank()) PINS.addLast(id);
                    }
                }
            }
        } catch (Throwable ignored) {}
        dedupeClamp();
        ITEM_ICON_CACHE.clear();
    }

    private static void save() {
        try {
            Path p = savePath();
            Files.createDirectories(p.getParent());

            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String id : PINS) arr.add(id);
            obj.add("pins", arr);

            try (BufferedWriter w = new BufferedWriter(new FileWriter(p.toFile()))) {
                GSON.toJson(obj, w);
            }
        } catch (Throwable ignored) {}
    }

    private static void dedupeClamp() {
        LinkedHashSet<String> set = new LinkedHashSet<>(PINS);
        PINS.clear();
        for (String s : set) PINS.addLast(s);
        while (PINS.size() > MAX_PINS) PINS.pollFirst();
    }

    private static void resetPins(boolean deleteFile) {
        try {
            if (deleteFile) {
                try {
                    Files.deleteIfExists(savePath());
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        PINS.clear();
        ITEM_ICON_CACHE.clear();
        LOADED = false;
        ACTIVE_KEY = null;
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        resetPins(true);
    }

    public static void onRenderGui(RenderGuiEvent.Post e) {
        if (Config.disableQuestPinning()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.screen != null) return;

        ensureLoaded();
        QuestData.loadClient(false);

        Player player = mc.player;

        if (!PINS.isEmpty()) {
            boolean changed = false;
            Iterator<String> it = PINS.iterator();
            while (it.hasNext()) {
                String qid = it.next();
                QuestData.Quest q = QuestData.byId(qid).orElse(null);
                if (q == null) continue;
                QuestTracker.Status st = QuestTracker.getStatus(q, player);
                if (st == QuestTracker.Status.REDEEMED || st == QuestTracker.Status.REJECTED) {
                    it.remove();
                    changed = true;
                }
            }
            if (changed) save();
        }

        if (PINS.isEmpty()) return;

        GuiGraphics gg = e.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int drawW = Mth.floor(TEX_W * TOAST_SCALE);
        int drawH = Mth.floor(TEX_H * TOAST_SCALE);
        int gap = Mth.floor(4 * TOAST_SCALE);

        String pos = Config.pinnedQuestHudPosition();
        boolean top = pos != null && pos.startsWith("top");
        boolean right = pos != null && pos.endsWith("right");

        int baseX = right ? (sw - drawW - CORNER_PAD) : CORNER_PAD;
        int count = Math.min(MAX_PINS, PINS.size());
        int topPad = CORNER_PAD + TOP_EXTRA_DOWN;
        int baseY = top ? topPad : (sh - CORNER_PAD - (count * drawH) - ((count - 1) * gap) - 24);

        int idx = 0;
        for (String qid : PINS) {
            if (idx >= MAX_PINS) break;

            QuestData.Quest q = QuestData.byId(qid).orElse(null);
            if (q == null) {
                idx++;
                continue;
            }

            int x = baseX;
            int y = baseY + idx * (drawH + gap);

            gg.pose().pushPose();
            gg.pose().translate(x, y, 0);
            gg.pose().scale(TOAST_SCALE, TOAST_SCALE, 1f);

            gg.blit(TEX_BG, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

            gg.drawString(mc.font, mc.font.plainSubstrByWidth(q.name, TEX_W - LEFT_PAD - 2), LEFT_PAD, 6, TITLE_COLOR, false);
            renderTargetsRow(gg, mc, q, player, LEFT_PAD, 18, TEX_W - 2);

            gg.pose().popPose();

            idx++;
        }
    }

    private record TargetView(ItemStack icon, String text, boolean done) {}

    private static void renderTargetsRow(GuiGraphics gg, Minecraft mc, QuestData.Quest q, Player player, int startX, int baseY, int maxX) {
        if (q.completion == null || q.completion.targets == null || q.completion.targets.isEmpty()) return;

        List<TargetView> all = new ArrayList<>();
        for (QuestData.Target t : q.completion.targets) {
            TargetView tv = toTargetView(mc, q, player, t);
            if (tv != null) all.add(tv);
        }
        if (all.isEmpty()) return;

        List<TargetView> remaining = new ArrayList<>();
        for (TargetView tv : all) if (!tv.done) remaining.add(tv);

        List<TargetView> pick = all;
        boolean overflow = all.size() > 3 || wouldOverflow(mc, all, startX, maxX);
        if (overflow && !remaining.isEmpty()) pick = remaining;

        int cx = startX;
        boolean first = true;
        int drawn = 0;

        for (TargetView tv : pick) {
            if (drawn >= 3) break;

            int sepW = first ? 0 : mc.font.width(" | ");
            int textW = mc.font.width(tv.text);
            int iconSlot = tv.icon.isEmpty() ? 0 : (Mth.ceil(16 * ICON_SCALE) + 2);
            int needed = sepW + iconSlot + textW;

            if (cx + needed > maxX) break;

            if (!first) {
                gg.drawString(mc.font, " | ", cx, baseY, SUB_COLOR, false);
                cx += sepW;
            }

            if (!tv.icon.isEmpty()) {
                gg.pose().pushPose();
                gg.pose().translate(cx, baseY - 3, 0);
                gg.pose().scale(ICON_SCALE, ICON_SCALE, 1f);
                gg.renderItem(tv.icon, 0, 0);
                gg.pose().popPose();
                cx += iconSlot;
            }

            gg.drawString(mc.font, tv.text, cx, baseY, SUB_COLOR, false);
            cx += textW;

            first = false;
            drawn++;
        }
    }

    private static boolean wouldOverflow(Minecraft mc, List<TargetView> list, int startX, int maxX) {
        int cx = startX;
        boolean first = true;
        int drawn = 0;

        for (TargetView tv : list) {
            if (drawn >= 3) break;

            int sepW = first ? 0 : mc.font.width(" | ");
            int textW = mc.font.width(tv.text);
            int iconSlot = tv.icon.isEmpty() ? 0 : (Mth.ceil(16 * ICON_SCALE) + 2);
            int needed = sepW + iconSlot + textW;

            if (cx + needed > maxX) return true;

            cx += needed;
            first = false;
            drawn++;
        }

        return false;
    }

    private static TargetView toTargetView(Minecraft mc, QuestData.Quest q, Player player, QuestData.Target t) {
        try {
            if (t.isItem()) {
                int need = Math.max(1, t.count);
                int found = QuestTracker.getCountInInventory(t.id, player);

                int perm = found;
                try {
                    String progressKey = q.id + ":" + t.id;
                    perm = QuestTracker.getPermanentItemProgress(progressKey, found, need);
                } catch (Throwable ignored) {}

                int shown = Math.min(perm, need);
                boolean done = shown >= need;
                ItemStack icon = resolveItemIcon(t.id);
                return new TargetView(icon, shown + "/" + need, done);
            }

            if (t.isEntity()) {
                int need = Math.max(1, t.count);
                int have = Math.min(QuestTracker.getKillCount(player, t.id), need);
                boolean done = have >= need;

                ResourceLocation rl = ResourceLocation.tryParse(t.id);
                ItemStack icon = new ItemStack(Items.DIAMOND_SWORD);
                if (rl != null) {
                    ResourceLocation eggRl = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath() + "_spawn_egg");
                    Item egg = BuiltInRegistries.ITEM.getOptional(eggRl).orElse(null);
                    if (egg != null) icon = new ItemStack(egg);
                }
                return new TargetView(icon, have + "/" + need, done);
            }

            if (t.isEffect()) {
                boolean done = QuestTracker.hasEffect(player, t.id);
                return new TargetView(new ItemStack(Items.POTION), done ? "1/1" : "0/1", done);
            }

            if (t.isAdvancement()) {
                boolean done = QuestTracker.hasAdvancement(player, t.id);
                return new TargetView(new ItemStack(Items.BOOK), done ? "1/1" : "0/1", done);
            }

            if (t.isStat()) {
                int need = Math.max(1, t.count);
                int have = Math.min(QuestTracker.getStatCount(player, t.id), need);
                boolean done = have >= need;
                return new TargetView(new ItemStack(Items.PAPER), have + "/" + need, done);
            }

            if (t.isLevelUpLevel()) {
                int need = Math.max(1, t.count);
                int have = Math.min(LevelUpCompat.getLevel(player), need);
                boolean done = have >= need;
                return new TargetView(new ItemStack(Items.EXPERIENCE_BOTTLE), have + "/" + need, done);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static ItemStack resolveItemIcon(String rawId) {
        ItemStack cached = ITEM_ICON_CACHE.get(rawId);
        if (cached != null) return cached.copy();

        ItemStack resolved = ItemStack.EMPTY;
        try {
            if (rawId == null || rawId.isBlank()) return ItemStack.EMPTY;

            boolean isTagSyntax = rawId.startsWith("#");
            String key = isTagSyntax ? rawId.substring(1) : rawId;

            ResourceLocation rl = ResourceLocation.tryParse(key);
            if (rl == null) return ItemStack.EMPTY;

            Item direct = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            boolean treatAsTag = isTagSyntax || direct == null;

            if (!treatAsTag && direct != null) {
                resolved = new ItemStack(direct);
            } else {
                var itemTag = net.minecraft.tags.TagKey.create(Registries.ITEM, rl);
                for (Item it : BuiltInRegistries.ITEM) {
                    if (it.builtInRegistryHolder().is(itemTag)) {
                        resolved = new ItemStack(it);
                        break;
                    }
                }

                if (resolved.isEmpty()) {
                    var blockTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, rl);
                    for (Item it : BuiltInRegistries.ITEM) {
                        if (it instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag)) {
                            resolved = new ItemStack(it);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        ITEM_ICON_CACHE.put(rawId, resolved.copy());
        return resolved;
    }
}
