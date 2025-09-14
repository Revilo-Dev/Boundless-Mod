package net.revilodev.boundless.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KillCounterState extends SavedData {
    private final Map<String, Map<String, Integer>> byPlayer = new HashMap<>();

    public static KillCounterState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(KillCounterState::new, KillCounterState::load),
                "boundless_kills"
        );
    }

    private KillCounterState() {}

    public static KillCounterState load(CompoundTag tag, HolderLookup.Provider provider) {
        KillCounterState s = new KillCounterState();
        for (String player : tag.getAllKeys()) {
            CompoundTag inner = tag.getCompound(player);
            Map<String, Integer> m = new HashMap<>();
            for (String k : inner.getAllKeys()) {
                m.put(k, inner.getInt(k));
            }
            s.byPlayer.put(player, m);
        }
        return s;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Map.Entry<String, Map<String, Integer>> e : byPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, Integer> v : e.getValue().entrySet()) {
                inner.putInt(v.getKey(), v.getValue());
            }
            tag.put(e.getKey(), inner);
        }
        return tag;
    }

    public void inc(UUID player, String entityId) {
        Map<String, Integer> m = byPlayer.computeIfAbsent(player.toString(), k -> new HashMap<>());
        m.put(entityId, m.getOrDefault(entityId, 0) + 1);
        setDirty();
    }

    public int get(UUID player, String entityId) {
        Map<String, Integer> m = byPlayer.get(player.toString());
        if (m == null) return 0;
        return m.getOrDefault(entityId, 0);
    }

    public Map<String, Integer> snapshotFor(UUID player) {
        Map<String, Integer> m = byPlayer.get(player.toString());
        if (m == null) return Map.of();
        return Map.copyOf(m);
    }

    public void clearFor(UUID player) {
        byPlayer.remove(player.toString());
        setDirty();
    }
}
