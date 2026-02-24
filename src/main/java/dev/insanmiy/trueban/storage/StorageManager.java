package dev.insanmiy.trueban.storage;

import dev.insanmiy.trueban.punishment.Punishment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for storage operations - all operations are asynchronous
 */
public interface StorageManager {

    /**
     * Check if the storage is ready and connected
     */
    boolean isReady();

    /**
     * Close storage connections
     */
    void close();

    /**
     * Save a new punishment to storage
     */
    CompletableFuture<Void> savePunishment(Punishment punishment);

    /**
     * Update an existing punishment
     */
    CompletableFuture<Void> updatePunishment(Punishment punishment);

    /**
     * Get all active punishments for a player
     */
    CompletableFuture<List<Punishment>> getActivePunishments(UUID playerUUID);

    /**
     * Get punishment history for a player
     */
    CompletableFuture<List<Punishment>> getPunishmentHistory(UUID playerUUID);

    /**
     * Get a player's UUID from their username (from cache)
     */
    CompletableFuture<UUID> getOfflineUUID(String playerName);

    /**
     * Get all known player names
     */
    CompletableFuture<List<String>> getKnownPlayerNames();

    /**
     * Get all punishments by IP address
     */
    CompletableFuture<List<Punishment>> getPunishmentsByIP(String ipAddress);

    /**
     * Delete a punishment (mark as inactive)
     */
    CompletableFuture<Void> deletePunishment(UUID playerUUID, int punishmentIndex);

    /**
     * Clear all data (use with caution)
     */
    CompletableFuture<Void> clearAll();
}
