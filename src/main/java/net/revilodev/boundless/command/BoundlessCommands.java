package net.revilodev.boundless.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
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
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    MinecraftServer server = ctx.getSource().getServer();
                                    QuestData.loadServer(server, true);
                                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                        BoundlessNetwork.syncPlayer(p);
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("Boundless quests reloaded."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    if (player != null) {
                                        QuestTracker.reset(player);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Quest progress reset."), false);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {
                                            for (QuestData.Quest q : QuestData.all()) {
                                                builder.suggest(q.id);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;
                                            String id = ResourceLocationArgument.getId(ctx, "id").toString();
                                            QuestData.byIdServer(player.server, id).ifPresent(q -> {
                                                if (QuestTracker.isReady(q, player)) {
                                                    if (QuestTracker.serverRedeem(q, player)) {
                                                        BoundlessNetwork.sendStatus(player, q.id, QuestTracker.Status.REDEEMED.name());
                                                        ctx.getSource().sendSuccess(() -> Component.literal("Redeemed quest " + q.id), false);
                                                    }
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal("Not ready: " + id));
                                                }
                                            });
                                            return 1;
                                        })))
                        .then(Commands.literal("toasts")
                                .then(Commands.literal("disable")
                                        .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean v = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value");
                                                    QuestTracker.setServerToastsDisabled(v);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Boundless toasts disabled set to " + v), true);
                                                    return 1;
                                                }))))

        );
    }
}
