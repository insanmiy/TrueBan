package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class CommandBase implements CommandExecutor, TabCompleter {
    protected final TrueBan plugin;

    public CommandBase(TrueBan plugin) {
        this.plugin = plugin;
    }

    public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);
    public abstract List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args);

    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
    }

    protected CompletableFuture<UUID> getPlayerUUID(String playerName) {
        return plugin.getPunishmentManager().getPlayerUUID(playerName);
    }

    protected CompletableFuture<String> getPlayerName(UUID uuid) {
        return plugin.getPunishmentManager().getPlayerName(uuid);
    }

    protected long parseDuration(String duration) {
        if (duration.equalsIgnoreCase("permanent") || duration.equalsIgnoreCase("perm")) {
            return -1;
        }

        long seconds = 0;
        String[] parts = duration.toLowerCase().split("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");

        for (int i = 0; i < parts.length; i += 2) {
            if (i + 1 >= parts.length) break;

            try {
                int amount = Integer.parseInt(parts[i]);
                String unit = parts[i + 1];

                switch (unit) {
                    case "s", "sec", "seconds", "second" -> seconds += amount;
                    case "m", "min", "minutes", "minute" -> seconds += amount * 60L;
                    case "h", "hour", "hours" -> seconds += amount * 3600L;
                    case "d", "day", "days" -> seconds += amount * 86400L;
                    case "w", "week", "weeks" -> seconds += amount * 604800L;
                    case "mo", "month", "months" -> seconds += amount * 2592000L;
                    case "y", "year", "years" -> seconds += amount * 31536000L;
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        return seconds > 0 ? seconds : -1;
    }

    protected String formatDuration(long seconds) {
        if (seconds <= 0) return "Permanent";

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0) sb.append(secs).append("s");

        return sb.toString().trim();
    }
}
