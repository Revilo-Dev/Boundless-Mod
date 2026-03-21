package net.revilodev.boundless.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestProgressState extends SavedData {
    private final Map<String, Map<String, QuestProgress>> byPlayer = new HashMap<>();

    public static final class QuestProgress {
        private String status;
        private int claimCount;
        private boolean scrollRedeemed;
        private boolean scrollCreated;

        QuestProgress() {
        }

        QuestProgress(String status, int claimCount, boolean scrollRedeemed, boolean scrollCreated) {
            this.status = sanitizeStatus(status);
            this.claimCount = Math.max(0, claimCount);
            this.scrollRedeemed = scrollRedeemed;
            this.scrollCreated = scrollCreated;
        }

        public String status() {
            return status;
        }

        public int claimCount() {
            return claimCount;
        }

        public boolean hasEverClaimed() {
            return claimCount > 0;
        }

        public boolean scrollRedeemed() {
            return scrollRedeemed;
        }

        public boolean scrollCreated() {
            return scrollCreated;
        }

        private boolean isEmpty() {
            return (status == null || status.isBlank()) && claimCount <= 0 && !scrollRedeemed && !scrollCreated;
        }

        private static QuestProgress fromLegacyStatus(String status) {
            return new QuestProgress(status, 0, false, false);
        }
    }

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
            Map<String, QuestProgress> m = new HashMap<>();
            for (String questId : inner.getAllKeys()) {
                if (inner.contains(questId, Tag.TAG_STRING)) {
                    QuestProgress progress = QuestProgress.fromLegacyStatus(inner.getString(questId));
                    if (!progress.isEmpty()) m.put(questId, progress);
                    continue;
                }
                if (!inner.contains(questId, Tag.TAG_COMPOUND)) continue;
                CompoundTag progressTag = inner.getCompound(questId);
                QuestProgress progress = new QuestProgress(
                        progressTag.getString("status"),
                        progressTag.contains("claimCount", Tag.TAG_INT) ? progressTag.getInt("claimCount") : 0,
                        progressTag.getBoolean("scrollRedeemed"),
                        progressTag.getBoolean("scrollCreated")
                );
                if (!progress.isEmpty()) m.put(questId, progress);
            }
            if (!m.isEmpty()) s.byPlayer.put(playerKey, m);
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Map.Entry<String, Map<String, QuestProgress>> e : byPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, QuestProgress> q : e.getValue().entrySet()) {
                QuestProgress progress = q.getValue();
                if (progress == null || progress.isEmpty()) continue;
                CompoundTag progressTag = new CompoundTag();
                if (progress.status() != null && !progress.status().isBlank()) {
                    progressTag.putString("status", progress.status());
                }
                if (progress.claimCount() > 0) {
                    progressTag.putInt("claimCount", progress.claimCount());
                }
                if (progress.scrollRedeemed()) {
                    progressTag.putBoolean("scrollRedeemed", true);
                }
                if (progress.scrollCreated()) {
                    progressTag.putBoolean("scrollCreated", true);
                }
                inner.put(q.getKey(), progressTag);
            }
            tag.put(e.getKey(), inner);
        }
        return tag;
    }

    public Map<String, String> snapshotFor(UUID player) {
        Map<String, QuestProgress> m = byPlayer.get(player.toString());
        if (m == null || m.isEmpty()) return Map.of();
        Map<String, String> out = new HashMap<>();
        m.forEach((questId, progress) -> {
            String status = progress == null ? null : progress.status();
            if (status != null && !status.isBlank()) {
                out.put(questId, status);
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    public Map<String, QuestProgress> progressSnapshotFor(UUID player) {
        Map<String, QuestProgress> m = byPlayer.get(player.toString());
        if (m == null || m.isEmpty()) return Map.of();
        return Map.copyOf(m);
    }

    public String get(UUID player, String questId) {
        return progress(player, questId).status();
    }

    public QuestProgress progress(UUID player, String questId) {
        Map<String, QuestProgress> m = byPlayer.get(player.toString());
        if (m == null) return new QuestProgress();
        QuestProgress progress = m.get(questId);
        return progress == null ? new QuestProgress() : new QuestProgress(progress.status(), progress.claimCount(), progress.scrollRedeemed(), progress.scrollCreated());
    }

    public void set(UUID player, String questId, String status) {
        String key = player.toString();
        Map<String, QuestProgress> m = byPlayer.computeIfAbsent(key, k -> new HashMap<>());
        QuestProgress progress = m.computeIfAbsent(questId, ignored -> new QuestProgress());
        progress.status = sanitizeStatus(status);
        if (progress.isEmpty()) {
            m.remove(questId);
        }
        if (m.isEmpty()) {
            byPlayer.remove(key);
        }
        setDirty();
    }

    public int getClaimCount(UUID player, String questId) {
        return progress(player, questId).claimCount();
    }

    public boolean hasEverClaimed(UUID player, String questId) {
        return getClaimCount(player, questId) > 0;
    }

    public int incrementClaimCount(UUID player, String questId) {
        String key = player.toString();
        Map<String, QuestProgress> m = byPlayer.computeIfAbsent(key, k -> new HashMap<>());
        QuestProgress progress = m.computeIfAbsent(questId, ignored -> new QuestProgress());
        progress.claimCount = Math.max(0, progress.claimCount) + 1;
        setDirty();
        return progress.claimCount;
    }

    public boolean hasRedeemedScroll(UUID player, String questId) {
        return progress(player, questId).scrollRedeemed();
    }

    public boolean hasCreatedScroll(UUID player, String questId) {
        return progress(player, questId).scrollCreated();
    }

    public void setScrollRedeemed(UUID player, String questId, boolean redeemed) {
        String key = player.toString();
        Map<String, QuestProgress> m = byPlayer.computeIfAbsent(key, k -> new HashMap<>());
        QuestProgress progress = m.computeIfAbsent(questId, ignored -> new QuestProgress());
        progress.scrollRedeemed = redeemed;
        if (progress.isEmpty()) {
            m.remove(questId);
        }
        if (m.isEmpty()) {
            byPlayer.remove(key);
        }
        setDirty();
    }

    public void setScrollCreated(UUID player, String questId, boolean created) {
        String key = player.toString();
        Map<String, QuestProgress> m = byPlayer.computeIfAbsent(key, k -> new HashMap<>());
        QuestProgress progress = m.computeIfAbsent(questId, ignored -> new QuestProgress());
        progress.scrollCreated = created;
        if (progress.isEmpty()) {
            m.remove(questId);
        }
        if (m.isEmpty()) {
            byPlayer.remove(key);
        }
        setDirty();
    }

    public void clear(UUID player) {
        byPlayer.remove(player.toString());
        setDirty();
    }

    private static String sanitizeStatus(String status) {
        return status == null || status.isBlank() ? null : status;
    }
}
