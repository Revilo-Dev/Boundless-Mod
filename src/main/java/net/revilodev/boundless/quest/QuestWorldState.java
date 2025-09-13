package net.revilodev.boundless.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class QuestWorldState extends SavedData {
    private final Map<String, String> states = new HashMap<>();

    public static QuestWorldState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(QuestWorldState::new, QuestWorldState::load),
                "boundless_quests"
        );
    }

    public static QuestWorldState get(ServerLevel level) {
        return get(level.getServer());
    }

    private QuestWorldState() {}

    public static QuestWorldState load(CompoundTag tag, HolderLookup.Provider provider) {
        QuestWorldState data = new QuestWorldState();
        for (String key : tag.getAllKeys()) {
            data.states.put(key, tag.getString(key));
        }
        return data;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Map.Entry<String, String> e : states.entrySet()) {
            tag.putString(e.getKey(), e.getValue());
        }
        return tag;
    }

    public void set(String questId, QuestTracker.Status status) {
        states.put(questId, status.name());
        setDirty();
    }

    public QuestTracker.Status get(String questId) {
        String s = states.get(questId);
        if (s == null) return QuestTracker.Status.INCOMPLETE;
        try {
            return QuestTracker.Status.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return QuestTracker.Status.INCOMPLETE;
        }
    }

    public Map<String, String> all() {
        return new HashMap<>(states);
    }

    public void reset() {
        states.clear();
        setDirty();
    }
}
