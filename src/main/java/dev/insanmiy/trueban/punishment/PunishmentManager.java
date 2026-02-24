package dev.insanmiy.trueban.punishment;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.storage.StorageManager;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all punishment operations with async database operations
 */
public class PunishmentManager {

    private final TrueBan plugin;
    private final StorageManager storage;
    private final Map<UUID, List<Punishment>> activePunishments;

    public PunishmentManager(TrueBan plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.activePunishments = new ConcurrentHashMap<>();
    }

    /**
     * Add a permanent punishment
     */
    public CompletableFuture<Void> addPermanentPunishment(
            UUID playerUUID,
            String playerName,
            String ipAddress,
            PunishmentType type,
            String reason,
            String operator
    ) {
        Punishment punishment = Punishment.createPermanent(playerUUID, playerName, ipAddress, type, reason, operator);
        return addPunishment(punishment);
    }

    /**
     * Add a temporary punishment
     */
    public CompletableFuture<Void> addTemporaryPunishment(
            UUID playerUUID,
            String playerName,
            String ipAddress,
            PunishmentType type,
            String reason,
            String operator,
            long durationMillis
    ) {
        Punishment punishment = Punishment.createTemporary(playerUUID, playerName, ipAddress, type, reason, operator, durationMillis);
        return addPunishment(punishment);
    }

    /**
     * Add a punishment to the system
     */
    private CompletableFuture<Void> addPunishment(Punishment punishment) {
        return storage.savePunishment(punishment).thenRun(() -> {
            activePunishments.computeIfAbsent(punishment.getPlayerUUID(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(punishment);
        });
    }

    /**
     * Remove a punishment (unban/unmute)
     */
    public CompletableFuture<Void> removePunishment(UUID playerUUID, PunishmentType type) {
        return CompletableFuture.runAsync(() -> {
            List<Punishment> playerPunishments = activePunishments.getOrDefault(playerUUID, new ArrayList<>());

            for (Punishment p : playerPunishments) {
                if (p.getType() == type && p.isActive()) {
                    p.setActive(false);
                    storage.updatePunishment(p);
                }
            }
        });
    }

    /**
     * Get all active punishments for a player
     */
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID playerUUID) {
        return storage.getActivePunishments(playerUUID);
    }

    /**
     * Get punishment history for a player
     */
    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID playerUUID) {
        return storage.getPunishmentHistory(playerUUID);
    }

    /**
     * Check if a player is banned
     */
    public CompletableFuture<Boolean> isBanned(UUID playerUUID) {
        return getActivePunishments(playerUUID).thenApply(punishments ->
                punishments.stream().anyMatch(p -> p.getType().isBan())
        );
    }

    /**
     * Check if a player IP is banned
     */
    public CompletableFuture<Boolean> isIPBanned(String ipAddress) {
        return storage.getPunishmentsByIP(ipAddress).thenApply(punishments ->
                punishments.stream().anyMatch(p -> p.getType() == PunishmentType.IPBAN && p.isActive())
        );
    }

    /**
     * Check if a player is muted
     */
    public CompletableFuture<Boolean> isMuted(UUID playerUUID) {
        return getActivePunishments(playerUUID).thenApply(punishments ->
                punishments.stream().anyMatch(p -> p.getType().isMute())
        );
    }

    /**
     * Get ban message for a player
     */
    public CompletableFuture<String> getBanMessage(UUID playerUUID) {
        return getActivePunishments(playerUUID).thenApply(punishments -> {
            for (Punishment p : punishments) {
                if (p.getType() == PunishmentType.BAN) {
                    Map<String, String> placeholders = createPlaceholders(p);
                    return plugin.getMessageManager().getMessage("ban.banned_message", placeholders);
                } else if (p.getType() == PunishmentType.TEMPBAN) {
                    Map<String, String> placeholders = createPlaceholders(p);
                    placeholders.put("duration", formatDuration(p.getExpiresAt() - System.currentTimeMillis()));
                    placeholders.put("expiration", formatDate(p.getExpiresAt()));
                    return plugin.getMessageManager().getMessage("ban.tempban_message", placeholders);
                } else if (p.getType() == PunishmentType.IPBAN) {
                    Map<String, String> placeholders = createPlaceholders(p);
                    return plugin.getMessageManager().getMessage("ban.ipban_message", placeholders);
                }
            }
            return "";
        });
    }

    /**
     * Expire active punishments (called periodically)
     */
    public CompletableFuture<Void> expireActivePunishments() {
        return CompletableFuture.runAsync(() -> {
            for (List<Punishment> punishments : activePunishments.values()) {
                for (Punishment p : punishments) {
                    if (p.isActive() && p.hasExpired()) {
                        p.setActive(false);
                        storage.updatePunishment(p).thenRun(() ->
                                plugin.getLogger().info("Expired punishment for " + p.getPlayerName())
                        );
                    }
                }
            }
        });
    }

    /**
     * Get offline player UUID from name
     */
    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        // First try to get online player
        var onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(onlinePlayer.getUniqueId());
        }

        // Try to get from storage
        return storage.getOfflineUUID(playerName);
    }

    /**
     * Create placeholder map from punishment
     */
    private Map<String, String> createPlaceholders(Punishment p) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", p.getPlayerName());
        placeholders.put("uuid", p.getPlayerUUID().toString());
        placeholders.put("reason", p.getReason());
        placeholders.put("operator", p.getOperator());
        placeholders.put("ip", p.getIpAddress() != null ? p.getIpAddress() : "N/A");
        return placeholders;
    }

    /**
     * Format duration in milliseconds to human-readable format
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "expired";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (weeks > 0) {
            return weeks + "w " + (days % 7) + "d";
        } else if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    /**
     * Format date timestamp
     */
    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    /**
     * Parse duration string to milliseconds
     * Format: 30s, 5m, 2h, 1d, 4w
     */
    public static long parseDuration(String duration) throws NumberFormatException {
        duration = duration.trim().toLowerCase();

        String number = "";
        String unit = "";

        for (char c : duration.toCharArray()) {
            if (Character.isDigit(c)) {
                number += c;
            } else {
                unit += c;
            }
        }

        if (number.isEmpty() || unit.isEmpty()) {
            throw new NumberFormatException("Invalid duration format");
        }

        long value = Long.parseLong(number);
        return switch (unit.toLowerCase()) {
            case "s" -> value * 1000;
            case "m" -> value * 60 * 1000;
            case "h" -> value * 60 * 60 * 1000;
            case "d" -> value * 24 * 60 * 60 * 1000;
            case "w" -> value * 7 * 24 * 60 * 60 * 1000;
            default -> throw new NumberFormatException("Unknown time unit: " + unit);
        };
    }
}
