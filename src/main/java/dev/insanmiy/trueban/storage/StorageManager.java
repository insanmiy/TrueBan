package dev.insanmiy.trueban.storage;

import dev.insanmiy.trueban.models.Punishment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageManager {

    CompletableFuture<Void> savePunishment(Punishment punishment);

    CompletableFuture<Punishment> getPunishment(UUID uuid);

    CompletableFuture<List<Punishment>> getAllPunishments();

    CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid);

    CompletableFuture<Void> removePunishment(UUID uuid, Punishment.PunishmentType type);

    CompletableFuture<UUID> getOfflinePlayerUUID(String playerName);

    CompletableFuture<String> getPlayerName(UUID uuid);

    CompletableFuture<List<String>> getAllKnownPlayers();

    CompletableFuture<Void> initialize();

    CompletableFuture<Void> shutdown();
}
