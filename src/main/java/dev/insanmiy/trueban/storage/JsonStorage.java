package dev.insanmiy.trueban.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class JsonStorage implements StorageManager {
    private final TrueBan plugin;
    private final File dataFile;
    private final File playerCacheFile;
    private final Gson gson;
    private final Map<UUID, List<Punishment>> punishments;
    private final Map<String, UUID> playerNameCache;
    private final Map<UUID, String> uuidNameCache;

    public JsonStorage(TrueBan plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "punishments.json");
        this.playerCacheFile = new File(plugin.getDataFolder(), "players.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.punishments = new ConcurrentHashMap<>();
        this.playerNameCache = new ConcurrentHashMap<>();
        this.uuidNameCache = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!dataFile.exists()) {
                    dataFile.getParentFile().mkdirs();
                    dataFile.createNewFile();
                    saveData();
                }
                if (!playerCacheFile.exists()) {
                    playerCacheFile.createNewFile();
                    savePlayerCache();
                }
                loadData();
                loadPlayerCache();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize JSON storage", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(this::saveData);
    }

    @Override
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            punishments.computeIfAbsent(punishment.getUuid(), k -> new ArrayList<>()).add(punishment);
            saveData();
        });
    }

    @Override
    public CompletableFuture<Punishment> getPunishment(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> playerPunishments = punishments.get(uuid);
            if (playerPunishments == null) return null;

            return playerPunishments.stream()
                    .filter(Punishment::isActive)
                    .findFirst()
                    .orElse(null);
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllPunishments() {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> all = new ArrayList<>();
            punishments.values().forEach(all::addAll);
            return all;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> playerPunishments = punishments.get(uuid);
            if (playerPunishments == null) return new ArrayList<>();

            return playerPunishments.stream()
                    .filter(Punishment::isActive)
                    .toList();
        });
    }

    @Override
    public CompletableFuture<Void> removePunishment(UUID uuid, Punishment.PunishmentType type) {
        return CompletableFuture.runAsync(() -> {
            List<Punishment> playerPunishments = punishments.get(uuid);
            if (playerPunishments != null) {
                playerPunishments.removeIf(p -> p.getType() == type);
                if (playerPunishments.isEmpty()) {
                    punishments.remove(uuid);
                }
                saveData();
            }
        });
    }

    @Override
    public CompletableFuture<UUID> getOfflinePlayerUUID(String playerName) {
        return CompletableFuture.supplyAsync(() -> playerNameCache.get(playerName.toLowerCase()));
    }

    @Override
    public CompletableFuture<String> getPlayerName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> uuidNameCache.get(uuid));
    }

    @Override
    public CompletableFuture<List<String>> getAllKnownPlayers() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(playerNameCache.keySet()));
    }

    private void loadData() {
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, List<Punishment>>>(){}.getType();
            Map<UUID, List<Punishment>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                punishments.clear();
                punishments.putAll(loaded);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load punishments data", e);
        }
    }

    private void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(punishments, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save punishments data", e);
        }
    }

    private void loadPlayerCache() {
        try (FileReader reader = new FileReader(playerCacheFile)) {
            Type type = new TypeToken<Map<String, UUID>>(){}.getType();
            Map<String, UUID> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                playerNameCache.clear();
                playerNameCache.putAll(loaded);
                loaded.forEach((name, uuid) -> uuidNameCache.put(uuid, name));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player cache", e);
        }
    }

    private void savePlayerCache() {
        try (FileWriter writer = new FileWriter(playerCacheFile)) {
            gson.toJson(playerNameCache, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player cache", e);
        }
    }

    public void cachePlayer(String playerName, UUID uuid) {
        playerNameCache.put(playerName.toLowerCase(), uuid);
        uuidNameCache.put(uuid, playerName);
        savePlayerCache();
    }
}
