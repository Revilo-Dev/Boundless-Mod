package net.revilodev.boundless.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BoundlessCommands {
    private static final Path INSTANCE_QUEST_PACKS_ROOT =
            net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("config").resolve("boundless").resolve("questpacks");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("boundless")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            QuestData.loadServer(server, true);
                            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                BoundlessNetwork.syncPlayer(p);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("Quests Reloaded."), true);
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .then(Commands.literal("all")
                                .executes(ctx -> resetAll(ctx.getSource(), selfOrEmpty(ctx.getSource())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> resetAll(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    for (QuestData.Quest q : QuestData.all()) builder.suggest(q.id);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> resetQuest(
                                        ctx.getSource(),
                                        selfOrEmpty(ctx.getSource()),
                                        StringArgumentType.getString(ctx, "id")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> resetQuest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("complete")
                        .then(Commands.literal("all")
                                .executes(ctx -> completeAll(ctx.getSource(), selfOrEmpty(ctx.getSource())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> completeAll(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    for (QuestData.Quest q : QuestData.all()) builder.suggest(q.id);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> completeQuest(
                                        ctx.getSource(),
                                        selfOrEmpty(ctx.getSource()),
                                        StringArgumentType.getString(ctx, "id")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> completeQuest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("toasts")
                        .then(Commands.literal("enable")
                                .executes(ctx -> {
                                    QuestTracker.setServerToastsDisabled(false);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Quest Toasts Enabled"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("disable")
                                .executes(ctx -> {
                                    QuestTracker.setServerToastsDisabled(true);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Quest Toasts Disabled"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    boolean disabled = QuestTracker.serverToastsDisabled();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Quest Toasts Are Currently: " + (disabled ? "Disabled" : "Enabled")),
                                            false);
                                    return 1;
                                })))
                .then(Commands.literal("questpack")
                        .then(Commands.literal("enable")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            listInstanceQuestPacks().keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setQuestPackEnabled(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true))))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            listInstanceQuestPacks().keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setQuestPackEnabled(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false))))
                        .then(Commands.literal("list")
                                .executes(ctx -> listQuestPacks(ctx.getSource())))));
                }

    private static List<ServerPlayer> selfOrEmpty(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null ? List.of() : List.of(player);
    }

    private static int resetAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No target players."));
            return 0;
        }
        for (ServerPlayer player : targets) {
            QuestTracker.reset(player);
        }
        source.sendSuccess(() -> Component.literal("Quest progress reset for " + targets.size() + " player(s)."), false);
        return targets.size();
    }

    private static int resetQuest(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No target players."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        var opt = QuestData.byIdServer(server, id);
        if (opt.isEmpty()) {
            source.sendFailure(Component.literal("Unknown quest: " + id));
            return 0;
        }
        for (ServerPlayer player : targets) {
            QuestTracker.setServerStatus(player, id, null);
            BoundlessNetwork.sendStatus(player, id, QuestTracker.Status.INCOMPLETE.name());
            BoundlessNetwork.sendProgressMeta(player, id);
        }
        source.sendSuccess(() -> Component.literal("Reset " + id + " for " + targets.size() + " player(s)."), false);
        return targets.size();
    }

    private static int completeAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No target players."));
            return 0;
        }
        for (ServerPlayer player : targets) {
            for (QuestData.Quest q : QuestData.allServer(source.getServer())) {
                QuestTracker.forceCompleteWithoutRewards(q, player);
                BoundlessNetwork.sendProgressMeta(player, q.id);
            }
        }
        source.sendSuccess(() -> Component.literal("Completed all quests for " + targets.size() + " player(s)."), false);
        return targets.size();
    }

    private static int completeQuest(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No target players."));
            return 0;
        }
        var opt = QuestData.byIdServer(source.getServer(), id);
        if (opt.isEmpty()) {
            source.sendFailure(Component.literal(id + " Invalid"));
            return 0;
        }
        QuestData.Quest q = opt.get();
        for (ServerPlayer player : targets) {
            QuestTracker.forceCompleteWithoutRewards(q, player);
            BoundlessNetwork.sendProgressMeta(player, q.id);
        }
        source.sendSuccess(() -> Component.literal("Completed " + q.id + " for " + targets.size() + " player(s)."), false);
        return targets.size();
    }

    private static int setQuestPackEnabled(CommandSourceStack source, String id, boolean enabled) {
        String key = id == null ? "" : id.trim();
        if (key.isBlank()) {
            source.sendFailure(Component.literal("Questpack id required."));
            return 0;
        }

        Path packRoot = INSTANCE_QUEST_PACKS_ROOT.resolve(key);
        if (!Files.isDirectory(packRoot)) {
            source.sendFailure(Component.literal("Unknown questpack: " + key));
            return 0;
        }
        if (!writeQuestPackEnabled(packRoot, enabled)) {
            source.sendFailure(Component.literal("Failed to update questpack: " + key));
            return 0;
        }

        MinecraftServer server = source.getServer();
        QuestData.loadServer(server, true);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            BoundlessNetwork.syncPlayer(p);
        }
        source.sendSuccess(() -> Component.literal("Questpack " + key + " " + (enabled ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int listQuestPacks(CommandSourceStack source) {
        Map<String, Boolean> packs = listInstanceQuestPacks();
        if (packs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No instance questpacks found."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Questpacks:"), false);
        packs.forEach((id, enabled) ->
                source.sendSuccess(() -> Component.literal("- " + id + " [" + (enabled ? "enabled" : "disabled") + "]"), false));
        return packs.size();
    }

    private static Map<String, Boolean> listInstanceQuestPacks() {
        Map<String, Boolean> out = new LinkedHashMap<>();
        if (!Files.isDirectory(INSTANCE_QUEST_PACKS_ROOT)) return out;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(INSTANCE_QUEST_PACKS_ROOT)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;
                String id = path.getFileName() == null ? "" : path.getFileName().toString();
                if (id.isBlank()) continue;
                out.put(id, readQuestPackEnabled(path));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static boolean readQuestPackEnabled(Path packRoot) {
        Path metaPath = packRoot.resolve("pack.mcmeta");
        if (!Files.exists(metaPath)) return true;
        try (BufferedReader reader = Files.newBufferedReader(metaPath, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!(parsed instanceof JsonObject root)) return true;
            if (!root.has("boundless") || !root.get("boundless").isJsonObject()) return true;
            JsonObject boundless = root.getAsJsonObject("boundless");
            if (!boundless.has("enabled")) return true;
            JsonElement enabled = boundless.get("enabled");
            if (enabled == null || !enabled.isJsonPrimitive()) return true;
            if (enabled.getAsJsonPrimitive().isBoolean()) return enabled.getAsBoolean();
            return Boolean.parseBoolean(enabled.getAsString());
        } catch (Exception ignored) {
            return true;
        }
    }

    private static boolean writeQuestPackEnabled(Path packRoot, boolean enabled) {
        Path metaPath = packRoot.resolve("pack.mcmeta");
        try {
            JsonObject root;
            if (Files.exists(metaPath)) {
                try (BufferedReader reader = Files.newBufferedReader(metaPath, StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    root = parsed instanceof JsonObject obj ? obj : new JsonObject();
                }
            } else {
                root = new JsonObject();
            }
            JsonObject boundless = root.has("boundless") && root.get("boundless").isJsonObject()
                    ? root.getAsJsonObject("boundless")
                    : new JsonObject();
            boundless.addProperty("enabled", enabled);
            root.add("boundless", boundless);
            Files.createDirectories(packRoot);
            try (BufferedWriter writer = Files.newBufferedWriter(metaPath, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
