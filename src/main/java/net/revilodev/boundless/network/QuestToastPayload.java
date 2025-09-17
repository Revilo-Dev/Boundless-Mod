package net.revilodev.boundless.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.BoundlessMod;

public record QuestToastPayload(String questId) {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BoundlessMod.MOD_ID, "quest_toast");

    public static final StreamCodec<FriendlyByteBuf, QuestToastPayload> CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeUtf(msg.questId),
                    buf -> new QuestToastPayload(buf.readUtf())
            );
}
