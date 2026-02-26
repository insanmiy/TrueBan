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

public class PlayerLoginListener implements Listener {

    private final TrueBan plugin;

    public PlayerLoginListener(TrueBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("trueban.bypass")) {
            return;
        }

        checkBan(event);
        checkIPBan(event);
    }

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

    private String getIPBanMessage(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason());
        placeholders.put("operator", punishment.getOperator());
        placeholders.put("ip", punishment.getIpAddress());
        return plugin.getMessageManager().getMessage("ban.ipban_message", placeholders);
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
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }
}
