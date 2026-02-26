package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.PunishmentManager;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public class TempbanCommand extends CommandBase implements CommandExecutor {

    public TempbanCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.tempban")) {
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/tempban <player> <duration> <reason>"));
            return true;
        }

        String playerName = args[0];
        String durationStr = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String operator = sender.getName();

        long durationMillis;
        try {
            durationMillis = PunishmentManager.parseDuration(durationStr);
        } catch (NumberFormatException e) {
            sendMessage(sender, "commands.invalid-duration");
            return true;
        }

        getPlayerUUID(playerName, uuid -> {
            if (uuid == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "commands.player-not-found", placeholders);
                return;
            }

            String ipAddress = null;
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null) {
                ipAddress = onlinePlayer.getAddress().getAddress().getHostAddress();
                onlinePlayer.kickPlayer(
                    messages.getMessage("ban.tempban_message",
                            createPlaceholders("player", playerName, "uuid", uuid.toString(),
                                    "reason", reason, "operator", operator, "duration", durationStr,
                                    "expiration", formatDate(System.currentTimeMillis() + durationMillis),
                                    "ip", ipAddress != null ? ipAddress : "N/A"))
                );
            }

            plugin.getPunishmentManager().addTemporaryPunishment(
                    uuid, playerName, ipAddress, PunishmentType.TEMPBAN, reason, operator, durationMillis
            ).whenComplete((v, ex) -> {
                if (ex != null) {
                    sendMessage(sender, "errors.database-error");
                    return;
                }

                Map<String, String> placeholders = createPlaceholders("player", playerName, "duration", durationStr);
                sendMessage(sender, "ban.successfully-tempbanned", placeholders);

                Map<String, String> notifyPlaceholders = createPlaceholders(
                        "player", playerName, "operator", operator, "reason", reason, "duration", durationStr);
                notifyOperators("tempban-notification", notifyPlaceholders);

                sendConsoleMessage("player-banned",
                        createPlaceholders("player", playerName, "reason", reason));
            });
        });

        return true;
    }

    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }
}
