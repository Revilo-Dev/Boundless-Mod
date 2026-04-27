package net.revilodev.boundless.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestObjectiveState extends SavedData {
    private final Map<String, Map<String, Integer>> itemProgressByPlayer = new HashMap<>();
    private final Map<String, Map<String, Boolean>> effectProgressByPlayer = new HashMap<>();
    private final Map<String, Map<String, String>> inputProgressByPlayer = new HashMap<>();

    private QuestObjectiveState() {}

    public static QuestObjectiveState get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(QuestObjectiveState::new, QuestObjectiveState::load, null),
                "boundless_quest_objectives"
        );
    }

    public static QuestObjectiveState load(CompoundTag tag, HolderLookup.Provider provider) {
        QuestObjectiveState s = new QuestObjectiveState();

        if (tag.contains("items", Tag.TAG_COMPOUND)) {
            CompoundTag itemsRoot = tag.getCompound("items");
            for (String playerKey : itemsRoot.getAllKeys()) {
                CompoundTag inner = itemsRoot.getCompound(playerKey);
                Map<String, Integer> m = new HashMap<>();
                for (String k : inner.getAllKeys()) {
                    if (inner.contains(k, Tag.TAG_INT)) m.put(k, inner.getInt(k));
                }
                if (!m.isEmpty()) s.itemProgressByPlayer.put(playerKey, m);
            }
        }

        if (tag.contains("effects", Tag.TAG_COMPOUND)) {
            CompoundTag effectsRoot = tag.getCompound("effects");
            for (String playerKey : effectsRoot.getAllKeys()) {
                CompoundTag inner = effectsRoot.getCompound(playerKey);
                Map<String, Boolean> m = new HashMap<>();
                for (String k : inner.getAllKeys()) {
                    if (inner.contains(k, Tag.TAG_BYTE)) m.put(k, inner.getBoolean(k));
                }
                if (!m.isEmpty()) s.effectProgressByPlayer.put(playerKey, m);
            }
        }

        if (tag.contains("inputs", Tag.TAG_COMPOUND)) {
            CompoundTag inputsRoot = tag.getCompound("inputs");
            for (String playerKey : inputsRoot.getAllKeys()) {
                CompoundTag inner = inputsRoot.getCompound(playerKey);
                Map<String, String> m = new HashMap<>();
                for (String k : inner.getAllKeys()) {
                    if (inner.contains(k, Tag.TAG_STRING)) {
                        String value = inner.getString(k);
                        if (!value.isBlank()) m.put(k, value);
                    }
                }
                if (!m.isEmpty()) s.inputProgressByPlayer.put(playerKey, m);
            }
        }

        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag itemsRoot = new CompoundTag();
        for (Map.Entry<String, Map<String, Integer>> e : itemProgressByPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, Integer> q : e.getValue().entrySet()) {
                inner.putInt(q.getKey(), Math.max(0, q.getValue()));
            }
            itemsRoot.put(e.getKey(), inner);
        }
        tag.put("items", itemsRoot);

        CompoundTag effectsRoot = new CompoundTag();
        for (Map.Entry<String, Map<String, Boolean>> e : effectProgressByPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, Boolean> q : e.getValue().entrySet()) {
                inner.putBoolean(q.getKey(), Boolean.TRUE.equals(q.getValue()));
            }
            effectsRoot.put(e.getKey(), inner);
        }
        tag.put("effects", effectsRoot);

        CompoundTag inputsRoot = new CompoundTag();
        for (Map.Entry<String, Map<String, String>> e : inputProgressByPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, String> q : e.getValue().entrySet()) {
                String value = q.getValue() == null ? "" : q.getValue().trim();
                if (!value.isBlank()) inner.putString(q.getKey(), value);
            }
            if (!inner.isEmpty()) inputsRoot.put(e.getKey(), inner);
        }
        tag.put("inputs", inputsRoot);

        return tag;
    }

    public int getItemProgress(UUID player, String key) {
        Map<String, Integer> m = itemProgressByPlayer.get(player.toString());
        if (m == null) return 0;
        return Math.max(0, m.getOrDefault(key, 0));
    }

    public int updateItemProgress(UUID player, String key, int current, int required) {
        String p = player.toString();
        Map<String, Integer> m = itemProgressByPlayer.computeIfAbsent(p, k -> new HashMap<>());
        int prev = Math.max(0, m.getOrDefault(key, 0));
        int now = Math.max(prev, Math.min(Math.max(0, current), Math.max(0, required)));
        if (now <= 0) {
            m.remove(key);
            if (m.isEmpty()) itemProgressByPlayer.remove(p);
        } else {
            m.put(key, now);
        }
        setDirty();
        return now;
    }

    public boolean getEffectDone(UUID player, String key) {
        Map<String, Boolean> m = effectProgressByPlayer.get(player.toString());
        if (m == null) return false;
        return Boolean.TRUE.equals(m.get(key));
    }

    public boolean updateEffectDone(UUID player, String key, boolean hasNow) {
        String p = player.toString();
        Map<String, Boolean> m = effectProgressByPlayer.computeIfAbsent(p, k -> new HashMap<>());
        boolean prev = Boolean.TRUE.equals(m.get(key));
        boolean now = prev || hasNow;
        if (now) {
            m.put(key, true);
        } else {
            m.remove(key);
            if (m.isEmpty()) effectProgressByPlayer.remove(p);
        }
        setDirty();
        return now;
    }

    public String getInputProgress(UUID player, String key) {
        if (key == null || key.isBlank()) return "";
        Map<String, String> m = inputProgressByPlayer.get(player.toString());
        if (m == null) return "";
        return m.getOrDefault(key, "");
    }

    public void setInputProgress(UUID player, String key, String value) {
        if (key == null || key.isBlank()) return;
        String p = player.toString();
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            Map<String, String> m = inputProgressByPlayer.get(p);
            if (m != null) {
                m.remove(key);
                if (m.isEmpty()) inputProgressByPlayer.remove(p);
            }
            setDirty();
            return;
        }
        inputProgressByPlayer.computeIfAbsent(p, k -> new HashMap<>()).put(key, normalized);
        setDirty();
    }

    public void clearPlayer(UUID player) {
        String p = player.toString();
        itemProgressByPlayer.remove(p);
        effectProgressByPlayer.remove(p);
        inputProgressByPlayer.remove(p);
        setDirty();
    }

    public void clearQuest(UUID player, String questId) {
        String p = player.toString();
        if (questId == null || questId.isBlank()) return;

        Map<String, Integer> items = itemProgressByPlayer.get(p);
        if (items != null) {
            items.entrySet().removeIf(entry -> entry.getKey() != null && entry.getKey().startsWith(questId + ":"));
            if (items.isEmpty()) itemProgressByPlayer.remove(p);
        }

        Map<String, Boolean> effects = effectProgressByPlayer.get(p);
        if (effects != null) {
            effects.entrySet().removeIf(entry -> entry.getKey() != null && entry.getKey().startsWith(questId + ":"));
            if (effects.isEmpty()) effectProgressByPlayer.remove(p);
        }

        Map<String, String> inputs = inputProgressByPlayer.get(p);
        if (inputs != null) {
            inputs.entrySet().removeIf(entry -> entry.getKey() != null && entry.getKey().startsWith(questId + ":"));
            if (inputs.isEmpty()) inputProgressByPlayer.remove(p);
        }

        setDirty();
    }
}
