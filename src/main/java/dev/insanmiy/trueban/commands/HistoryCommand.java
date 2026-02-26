package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HistoryCommand extends CommandBase implements CommandExecutor {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public HistoryCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.history")) {
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/history <player>"));
            return true;
        }

        String playerName = args[0];

        getPlayerUUID(playerName, uuid -> {
            if (uuid == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "commands.player-not-found", placeholders);
                return;
            }

            plugin.getPunishmentManager().getPunishmentHistory(uuid).whenComplete((punishments, ex) -> {
                if (ex != null) {
                    sendMessage(sender, "errors.database-error");
                    return;
                }

                if (punishments.isEmpty()) {
                    Map<String, String> placeholders = createPlaceholders("player", playerName);
                    sendMessage(sender, "history.no-history", placeholders);
                    return;
                }

                Map<String, String> headerPlaceholders = createPlaceholders("player", playerName);
                sender.sendMessage(messages.getMessage("history.header", headerPlaceholders));

                for (int i = 0; i < punishments.size(); i++) {
                    Punishment p = punishments.get(i);
                    String message = formatHistoryEntry(i, p);
                    sender.sendMessage(message);
                }

                sender.sendMessage(messages.getMessage("history.footer"));
            });
        });

        return true;
    }

    private String formatHistoryEntry(int index, Punishment punishment) {
        String statusBadge = punishment.isActive() ? 
                messages.getMessage("history.active-badge") : 
                messages.getMessage("history.expired-badge");

        String createdDate = dateFormat.format(new Date(punishment.getCreatedAt()));

        if (punishment.getType().isTemporary()) {
            String expiryDate = punishment.getExpiresAt() == -1 ? 
                    "Never" : 
                    dateFormat.format(new Date(punishment.getExpiresAt()));

            return messages.getMessage("history.tempban-entry-format",
                    createPlaceholders(
                            "id", String.valueOf(index),
                            "type", punishment.getType().getDisplayName(),
                            "duration", formatDuration(punishment.getExpiresAt() - punishment.getCreatedAt()),
                            "reason", punishment.getReason(),
                            "operator", punishment.getOperator(),
                            "expiration", expiryDate
                    )) + " " + statusBadge;
        } else {
            return messages.getMessage("history.entry-format",
                    createPlaceholders(
                            "id", String.valueOf(index),
                            "type", punishment.getType().getDisplayName(),
                            "reason", punishment.getReason(),
                            "operator", punishment.getOperator(),
                            "created", createdDate
                    )) + " " + statusBadge;
        }
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
}
