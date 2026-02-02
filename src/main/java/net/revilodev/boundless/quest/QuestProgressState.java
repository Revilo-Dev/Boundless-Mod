// src/main/java/net/revilodev/boundless/quest/QuestProgressState.java
package net.revilodev.boundless.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestProgressState extends SavedData {
    private final Map<String, Map<String, String>> byPlayer = new HashMap<>();

    private QuestProgressState() {
    }

    public static QuestProgressState get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(QuestProgressState::new, QuestProgressState::load),
                "boundless_quests"
        );
    }

    public static QuestProgressState load(CompoundTag tag, HolderLookup.Provider provider) {
        QuestProgressState s = new QuestProgressState();
        for (String playerKey : tag.getAllKeys()) {
            CompoundTag inner = tag.getCompound(playerKey);
            Map<String, String> m = new HashMap<>();
            for (String questId : inner.getAllKeys()) {
                String v = inner.getString(questId);
                if (v != null && !v.isBlank()) m.put(questId, v);
            }
            if (!m.isEmpty()) s.byPlayer.put(playerKey, m);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Map.Entry<String, Map<String, String>> e : byPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, String> q : e.getValue().entrySet()) {
                if (q.getValue() != null && !q.getValue().isBlank()) inner.putString(q.getKey(), q.getValue());
            }
            tag.put(e.getKey(), inner);
        }
        return tag;
    }

    public Map<String, String> snapshotFor(UUID player) {
        Map<String, String> m = byPlayer.get(player.toString());
        if (m == null || m.isEmpty()) return Map.of();
        return Map.copyOf(m);
    }

    public String get(UUID player, String questId) {
        Map<String, String> m = byPlayer.get(player.toString());
        if (m == null) return null;
        return m.get(questId);
    }

    public void set(UUID player, String questId, String status) {
        String key = player.toString();
        if (status == null || status.isBlank()) {
            Map<String, String> m = byPlayer.get(key);
            if (m != null) {
                m.remove(questId);
                if (m.isEmpty()) byPlayer.remove(key);
            }
        } else {
            Map<String, String> m = byPlayer.computeIfAbsent(key, k -> new HashMap<>());
            m.put(questId, status);
        }
        setDirty();
    }

    public void clear(UUID player) {
        byPlayer.remove(player.toString());
        setDirty();
    }
}
