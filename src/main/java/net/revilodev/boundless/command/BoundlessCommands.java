package net.revilodev.boundless.command;

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

import java.util.Collection;
import java.util.List;

public final class BoundlessCommands {

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
                                }))));
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
}
