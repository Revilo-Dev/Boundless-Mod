package net.revilodev.boundless.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.KillCounterState;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

public final class BoundlessCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("boundless")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    if (player == null) return 0;
                                    QuestTracker.reset(player);
                                    KillCounterState.get(player.serverLevel()).clearFor(player.getUUID());
                                    BoundlessNetwork.syncClear(player);
                                    BoundlessNetwork.syncPlayer(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Quest progress reset."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("id", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return 0;
                                            var id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "id");
                                            QuestData.byIdServer(player.server, id.toString()).ifPresent(q -> {
                                                if (QuestTracker.isReady(q, player)) {
                                                    if (QuestTracker.completeAndRedeem(q, player)) {
                                                        BoundlessNetwork.syncPlayer(player);
                                                        ctx.getSource().sendSuccess(() -> Component.literal("Completed quest " + q.id), true);
                                                    }
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal("Quest not ready: " + q.id));
                                                }
                                            });
                                            return 1;
                                        })))
        );
    }
}
