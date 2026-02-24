package dev.insanmiy.trueban.listeners;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.Punishment;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Listener for player login - checks for bans and IP bans
 */
public class PlayerLoginListener implements Listener {

    private final TrueBan plugin;

    public PlayerLoginListener(TrueBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        // Check if player has bypass permission
        if (event.getPlayer().hasPermission("trueban.bypass")) {
            return;
        }

        // Check for bans
        checkBan(event);
        checkIPBan(event);
    }

    /**
     * Check if player UUID is banned
     */
    private void checkBan(PlayerLoginEvent event) {
        try {
            List<Punishment> punishments = plugin.getPunishmentManager()
                    .getActivePunishments(event.getPlayer().getUniqueId()).join();

            for (Punishment p : punishments) {
                if ((p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN) && p.isActive()) {
                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, getBanMessage(p));
                    return;
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Error checking bans: " + ex.getMessage());
        }
    }

    /**
     * Check if player IP is banned
     */
    private void checkIPBan(PlayerLoginEvent event) {
        String ipAddress = event.getAddress().getHostAddress();
        try {
            List<Punishment> punishments = plugin.getStorageManager().getPunishmentsByIP(ipAddress).join();

            for (Punishment p : punishments) {
                if (p.getType() == PunishmentType.IPBAN && p.isActive()) {
                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, getIPBanMessage(p));
                    return;
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Error checking IP bans: " + ex.getMessage());
        }
    }

    /**
     * Get ban message with placeholders
     */
    private String getBanMessage(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", punishment.getPlayerName());
        placeholders.put("uuid", punishment.getPlayerUUID().toString());
        placeholders.put("reason", punishment.getReason());
        placeholders.put("operator", punishment.getOperator());

        if (punishment.getType() == PunishmentType.TEMPBAN) {
            placeholders.put("duration", formatDuration(punishment.getExpiresAt() - System.currentTimeMillis()));
            placeholders.put("expiration", formatDate(punishment.getExpiresAt()));
            return plugin.getMessageManager().getMessage("ban.tempban_message", placeholders);
        } else {
            return plugin.getMessageManager().getMessage("ban.banned_message", placeholders);
        }
    }

    /**
     * Get IP ban message
     */
    private String getIPBanMessage(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason());
        placeholders.put("operator", punishment.getOperator());
        placeholders.put("ip", punishment.getIpAddress());
        return plugin.getMessageManager().getMessage("ban.ipban_message", placeholders);
    }

    /**
     * Format duration
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "expired";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
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
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }
}
