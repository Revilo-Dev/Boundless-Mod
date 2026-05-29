package net.revilodev.boundless.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class LevelUpCompat {
    private static final ResourceLocation BOUNDLESS_QUEST_SOURCE =
            ResourceLocation.fromNamespaceAndPath("boundless", "quest_reward");

    private static boolean initialized = false;
    private static boolean available = false;
    private static Method getLevelMethod;
    private static Method awardXpMethod;

    private LevelUpCompat() {}

    public static boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    public static int getLevel(Player player) {
        if (player == null) return 0;
        ensureInitialized();
        if (!available || getLevelMethod == null) return 0;
        try {
            Object value = getLevelMethod.invoke(null, player);
            return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean meetsLevelRequirement(Player player, int requiredLevel) {
        return getLevel(player) >= Math.max(0, requiredLevel);
    }

    public static boolean awardXp(ServerPlayer player, long amount) {
        if (player == null || amount <= 0L) return false;
        ensureInitialized();
        if (!available || awardXpMethod == null) return false;
        try {
            awardXpMethod.invoke(null, player, amount, BOUNDLESS_QUEST_SOURCE);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded("levelup")) return;
        try {
            Class<?> apiClass = Class.forName("com.revilo.levelup.api.LevelUpApi");
            getLevelMethod = apiClass.getMethod("getLevel", Player.class);
            awardXpMethod = apiClass.getMethod("awardXp", ServerPlayer.class, long.class, ResourceLocation.class);
            available = true;
        } catch (Throwable ignored) {
            available = false;
            getLevelMethod = null;
            awardXpMethod = null;
        }
    }
}
