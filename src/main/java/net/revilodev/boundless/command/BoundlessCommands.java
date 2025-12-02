package net.revilodev.boundless.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

public final class BoundlessCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("boundless")
                        .requires(s -> s.hasPermission(2))

                        // ----------------------------------------------------
                        // RELOAD
                        // ----------------------------------------------------
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    MinecraftServer server = ctx.getSource().getServer();
                                    QuestData.loadServer(server, true);
                                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                        BoundlessNetwork.syncPlayer(p);
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("Boundless quests reloaded."), true);
                                    return 1;
                                })
                        )

                        // ----------------------------------------------------
                        // RESET
                        // ----------------------------------------------------
                        .then(Commands.literal("reset")

                                // RESET ALL
                                .then(Commands.literal("all")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player != null) {
                                                QuestTracker.reset(player);
                                                ctx.getSource().sendSuccess(() -> Component.literal("All quests reset."), false);
                                            }
                                            return 1;
                                        })
                                )

                                // RESET ONE QUEST
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            for (QuestData.Quest q : QuestData.all()) builder.suggest(q.id);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;

                                            String id = StringArgumentType.getString(ctx, "id");
                                            var opt = QuestData.byIdServer(player.server, id);

                                            if (opt.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown quest: " + id));
                                                return 1;
                                            }

                                            // Reset only this quest
                                            QuestTracker.setServerStatus(player, id, null); // null = INCOMPLETE
                                            BoundlessNetwork.sendStatus(player, id, QuestTracker.Status.INCOMPLETE.name());

                                            ctx.getSource().sendSuccess(() -> Component.literal("Quest Reset: " + id), false);
                                            return 1;
                                        })
                                )
                        )

                        // ----------------------------------------------------
                        // COMPLETE
                        // ----------------------------------------------------
                        .then(Commands.literal("complete")

                                // COMPLETE ALL (no rewards)
                                .then(Commands.literal("all")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;

                                            for (QuestData.Quest q : QuestData.all()) {
                                                QuestTracker.setServerStatus(player, q.id, QuestTracker.Status.COMPLETED);
                                                BoundlessNetwork.sendStatus(player, q.id, QuestTracker.Status.COMPLETED.name());

                                                QuestTracker.setServerStatus(player, q.id, QuestTracker.Status.REDEEMED);
                                                BoundlessNetwork.sendStatus(player, q.id, QuestTracker.Status.REDEEMED.name());
                                            }

                                            ctx.getSource().sendSuccess(() -> Component.literal("All quests completed."), false);
                                            return 1;
                                        })
                                )

                                .then(Commands.argument("id", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            for (QuestData.Quest q : QuestData.all()) builder.suggest(q.id);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {

                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;

                                            String id = StringArgumentType.getString(ctx, "id");

                                            var opt = QuestData.byIdServer(player.server, id);
                                            if (opt.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("Invalid quest: " + id));
                                                return 1;
                                            }

                                            QuestData.Quest q = opt.get();

                                            QuestTracker.setServerStatus(player, q.id, QuestTracker.Status.COMPLETED);
                                            BoundlessNetwork.sendStatus(player, q.id, QuestTracker.Status.COMPLETED.name());

                                            QuestTracker.setServerStatus(player, q.id, QuestTracker.Status.REDEEMED);
                                            BoundlessNetwork.sendStatus(player, q.id, QuestTracker.Status.REDEEMED.name());

                                            ctx.getSource().sendSuccess(() -> Component.literal("Quest Completed: " + q.id), false);
                                            return 1;
                                        })
                                )
                        )

                        // ----------------------------------------------------
                        // TOASTS
                        // ----------------------------------------------------
                        .then(Commands.literal("toasts")
                                .then(Commands.literal("enable")
                                        .executes(ctx -> {
                                            QuestTracker.setServerToastsDisabled(false);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Boundless toasts: ENABLED"), true);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("disable")
                                        .executes(ctx -> {
                                            QuestTracker.setServerToastsDisabled(true);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Boundless toasts: DISABLED"), true);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("status")
                                        .executes(ctx -> {
                                            boolean disabled = QuestTracker.serverToastsDisabled();
                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Boundless toasts are currently: " +
                                                                    (disabled ? "DISABLED" : "ENABLED")),
                                                    false);
                                            return 1;
                                        })
                                )
                        )

        );
    }
}
