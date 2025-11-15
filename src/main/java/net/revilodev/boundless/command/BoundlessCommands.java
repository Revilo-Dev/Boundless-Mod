package net.revilodev.boundless.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

public final class BoundlessCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("boundless")
                        .requires(s -> s.hasPermission(0))
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
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;

                                            String id = ResourceLocationArgument.getId(ctx, "id").toString();
                                            QuestData.byId(id).ifPresent(q -> {
                                                if (QuestTracker.isReady(q, player)) {
                                                    if (QuestTracker.completeAndRedeem(q, player)) {
                                                        BoundlessNetwork.sendToastTo(player, q.id);
                                                        ctx.getSource().sendSuccess(() -> Component.literal("Completed quest " + q.id), false);
                                                    }
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal("Not ready: " + id));
                                                }
                                            });
                                            return 1;
                                        })))
                        .then(Commands.literal("toast")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;

                                            String id = ResourceLocationArgument.getId(ctx, "id").toString();
                                            BoundlessNetwork.sendToastTo(player, id);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Toast sent for " + id), false);
                                            return 1;
                                        })))
        );
    }
}
