package dev.insanmiy.trueban.managers;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import dev.insanmiy.trueban.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class PunishmentManager {
    private final TrueBan plugin;
    private final StorageManager storage;
    private final ConfigManager config;
    private final ConcurrentMap<UUID, Punishment> activeBans;
    private final ConcurrentMap<UUID, Punishment> activeMutes;

    public PunishmentManager(TrueBan plugin, StorageManager storage, ConfigManager config) {
        this.plugin = plugin;
        this.storage = storage;
        this.config = config;
        this.activeBans = new ConcurrentHashMap<>();
        this.activeMutes = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Void> initialize() {
        return storage.initialize().thenCompose(v -> loadActivePunishments());
    }

    private CompletableFuture<Void> loadActivePunishments() {
        return storage.getAllPunishments().thenAccept(punishments -> {
            for (Punishment punishment : punishments) {
                if (punishment.isActive()) {
                    if (punishment.getType() == Punishment.PunishmentType.BAN ||
                        punishment.getType() == Punishment.PunishmentType.TEMPBAN ||
                        punishment.getType() == Punishment.PunishmentType.IPBAN) {
                        activeBans.put(punishment.getUuid(), punishment);
                    } else if (punishment.getType() == Punishment.PunishmentType.MUTE ||
                               punishment.getType() == Punishment.PunishmentType.TEMPMUTE) {
                        activeMutes.put(punishment.getUuid(), punishment);
                    }
                }
            }
        });
    }

    public CompletableFuture<Void> addPunishment(Punishment punishment) {
        return storage.savePunishment(punishment).thenRun(() -> {
            if (punishment.isActive()) {
                if (punishment.getType() == Punishment.PunishmentType.BAN ||
                    punishment.getType() == Punishment.PunishmentType.TEMPBAN ||
                    punishment.getType() == Punishment.PunishmentType.IPBAN) {
                    activeBans.put(punishment.getUuid(), punishment);

                    Player player = Bukkit.getPlayer(punishment.getUuid());
                    if (player != null && player.isOnline()) {
                        player.kickPlayer(formatBanMessage(punishment));
                    }
                } else if (punishment.getType() == Punishment.PunishmentType.MUTE ||
                           punishment.getType() == Punishment.PunishmentType.TEMPMUTE) {
                    activeMutes.put(punishment.getUuid(), punishment);
                }
            }

            if (config.isLogPunishments()) {
                plugin.getLogger().info(String.format("Punishment added: %s - %s - %s",
                    punishment.getPlayerName(), punishment.getType(), punishment.getReason()));
            }

            notifyStaff(punishment);
        });
    }

    public CompletableFuture<Void> removePunishment(UUID uuid, Punishment.PunishmentType type) {
        return storage.removePunishment(uuid, type).thenRun(() -> {
            if (type == Punishment.PunishmentType.BAN ||
                type == Punishment.PunishmentType.TEMPBAN ||
                type == Punishment.PunishmentType.IPBAN) {
                activeBans.remove(uuid);
            } else if (type == Punishment.PunishmentType.MUTE ||
                       type == Punishment.PunishmentType.TEMPMUTE) {
                activeMutes.remove(uuid);
            }

            if (config.isLogPunishments()) {
                plugin.getLogger().info(String.format("Punishment removed: %s - %s",
                    Bukkit.getOfflinePlayer(uuid).getName(), type));
            }
        });
    }

    public CompletableFuture<List<Punishment>> getPlayerHistory(UUID uuid) {
        return storage.getActivePunishments(uuid);
    }

    public boolean isBanned(UUID uuid) {
        Punishment ban = activeBans.get(uuid);
        return ban != null && ban.isActive();
    }

    public boolean isMuted(UUID uuid) {
        Punishment mute = activeMutes.get(uuid);
        return mute != null && mute.isActive();
    }

    public Punishment getBan(UUID uuid) {
        return activeBans.get(uuid);
    }

    public Punishment getMute(UUID uuid) {
        return activeMutes.get(uuid);
    }

    public void startExpirationTask() {
        int interval = config.getExpirationCheckInterval();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkExpiredPunishments,
            interval * 20L, interval * 20L);
    }

    private void checkExpiredPunishments() {
        long currentTime = Instant.now().getEpochSecond();

        activeBans.entrySet().removeIf(entry -> {
            Punishment punishment = entry.getValue();
            if (punishment.isExpired()) {
                storage.removePunishment(entry.getKey(), punishment.getType());
                plugin.getLogger().info(String.format("Ban expired: %s", punishment.getPlayerName()));
                return true;
            }
            return false;
        });

        activeMutes.entrySet().removeIf(entry -> {
            Punishment punishment = entry.getValue();
            if (punishment.isExpired()) {
                storage.removePunishment(entry.getKey(), punishment.getType());
                plugin.getLogger().info(String.format("Mute expired: %s", punishment.getPlayerName()));

                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.GREEN + "Your mute has expired!");
                }
                return true;
            }
            return false;
        });
    }

    private String formatBanMessage(Punishment punishment) {
        String message = config.getBanMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        message = message.replace("%expiration%", getExpirationDisplay(punishment));
        message = message.replace("%appeal%", config.getAppealURL());
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String formatMuteMessage(Punishment punishment) {
        String message = config.getMuteMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        message = message.replace("%expiration%", getExpirationDisplay(punishment));
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String getExpirationDisplay(Punishment punishment) {
        if (punishment.getExpirationTimestamp() <= 0) {
            return "Never";
        }

        Instant expiration = Instant.ofEpochSecond(punishment.getExpirationTimestamp());
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(expiration) +
               " (" + punishment.getDurationString() + ")";
    }

    private void notifyStaff(Punishment punishment) {
        if (!config.isNotifyStaff()) return;

        String message = String.format("&c[TrueBan] &7%s was %s by &7%s&7: &f%s",
            punishment.getPlayerName(),
            punishment.getType().toString().toLowerCase(),
            punishment.getOperatorName(),
            punishment.getReason());
        message = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("trueban.notify")) {
                player.sendMessage(message);
            }
        }

        if (Bukkit.getConsoleSender() != null) {
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(onlinePlayer.getUniqueId());
        }

        return storage.getOfflinePlayerUUID(playerName);
    }

    public CompletableFuture<String> getPlayerName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(onlinePlayer.getName());
        }

        return storage.getPlayerName(uuid);
    }

    public CompletableFuture<List<String>> getAllKnownPlayers() {
        return storage.getAllKnownPlayers();
    }

    public void shutdown() {
        storage.shutdown();
    }
}
