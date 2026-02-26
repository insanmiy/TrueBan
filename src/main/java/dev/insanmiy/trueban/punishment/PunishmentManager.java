package dev.insanmiy.trueban.punishment;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.storage.StorageManager;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {

    private final TrueBan plugin;
    private final StorageManager storage;
    private final Map<UUID, List<Punishment>> activePunishments;

    public PunishmentManager(TrueBan plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.activePunishments = new ConcurrentHashMap<>();
    }

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

    private CompletableFuture<Void> addPunishment(Punishment punishment) {
        return storage.savePunishment(punishment).thenRun(() -> {
            activePunishments.computeIfAbsent(punishment.getPlayerUUID(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(punishment);
        });
    }

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

    public CompletableFuture<List<Punishment>> getActivePunishments(UUID playerUUID) {
        return storage.getActivePunishments(playerUUID);
    }

    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID playerUUID) {
        return storage.getPunishmentHistory(playerUUID);
    }

    public CompletableFuture<Boolean> isBanned(UUID playerUUID) {
        return getActivePunishments(playerUUID).thenApply(punishments ->
                punishments.stream().anyMatch(p -> p.getType().isBan())
        );
    }

    public CompletableFuture<Boolean> isIPBanned(String ipAddress) {
        return storage.getPunishmentsByIP(ipAddress).thenApply(punishments ->
                punishments.stream().anyMatch(p -> p.getType() == PunishmentType.IPBAN && p.isActive())
        );
    }

    public CompletableFuture<Boolean> isMuted(UUID playerUUID) {
        return getActivePunishments(playerUUID).thenApply(punishments ->
                punishments.stream().anyMatch(p -> p.getType().isMute())
        );
    }

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

    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        var onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(onlinePlayer.getUniqueId());
        }

        return storage.getOfflineUUID(playerName);
    }

    private Map<String, String> createPlaceholders(Punishment p) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", p.getPlayerName());
        placeholders.put("uuid", p.getPlayerUUID().toString());
        placeholders.put("reason", p.getReason());
        placeholders.put("operator", p.getOperator());
        placeholders.put("ip", p.getIpAddress() != null ? p.getIpAddress() : "N/A");
        return placeholders;
    }

    private String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "expired";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        days %= 7;

        StringBuilder result = new StringBuilder();
        
        if (weeks > 0) {
            result.append(weeks).append(" week").append(weeks > 1 ? "s" : "");
        }
        if (days > 0) {
            if (result.length() > 0) result.append(" ");
            result.append(days).append(" day").append(days > 1 ? "s" : "");
        }
        if (hours > 0) {
            if (result.length() > 0) result.append(" ");
            result.append(hours).append(" hour").append(hours > 1 ? "s" : "");
        }
        if (minutes > 0) {
            if (result.length() > 0) result.append(" ");
            result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }
        if (seconds > 0 || result.length() == 0) {
            if (result.length() > 0) result.append(" ");
            result.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        }

        return result.toString();
    }

    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    public static long parseDuration(String duration) throws NumberFormatException {
        duration = duration.trim().toLowerCase();
        
        if (duration.isEmpty()) {
            throw new NumberFormatException("Duration cannot be empty");
        }
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([smhd])");
        java.util.regex.Matcher matcher = pattern.matcher(duration);
        
        long totalMillis = 0;
        boolean found = false;
        
        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            totalMillis += switch (unit) {
                case "s" -> value * 1000;
                case "m" -> value * 60 * 1000;
                case "h" -> value * 60 * 60 * 1000;
                case "d" -> value * 24 * 60 * 60 * 1000;
                default -> throw new NumberFormatException("Unknown time unit: " + unit);
            };
        }
        
        if (!found) {
            throw new NumberFormatException("Invalid duration format. Use: 30s, 5m, 2h, 1d, or combinations like 1h30m");
        }
        
        return totalMillis;
    }
}
