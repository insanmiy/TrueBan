package dev.insanmiy.trueban.storage;

import dev.insanmiy.trueban.punishment.Punishment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageManager {

    boolean isReady();

    void close();

    CompletableFuture<Void> savePunishment(Punishment punishment);

    CompletableFuture<Void> updatePunishment(Punishment punishment);

    CompletableFuture<List<Punishment>> getActivePunishments(UUID playerUUID);

    CompletableFuture<List<Punishment>> getPunishmentHistory(UUID playerUUID);

    CompletableFuture<UUID> getOfflineUUID(String playerName);

    CompletableFuture<List<String>> getKnownPlayerNames();

    CompletableFuture<List<Punishment>> getPunishmentsByIP(String ipAddress);

    CompletableFuture<Void> deletePunishment(UUID playerUUID, int punishmentIndex);

    CompletableFuture<Void> clearAll();
}
