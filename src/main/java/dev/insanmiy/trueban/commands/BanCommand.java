package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class BanCommand extends CommandBase implements CommandExecutor {

    public BanCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.ban")) {
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/ban <player> [reason]"));
            return true;
        }

        String playerName = args[0];
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        String operator = sender.getName();

        getPlayerUUID(playerName, uuid -> {
            if (uuid == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "commands.player-not-found", placeholders);
                return;
            }

            // Check if player is already banned
            plugin.getPunishmentManager().getActivePunishments(uuid).whenComplete((existingPunishments, ex) -> {
                if (ex != null) {
                    sendMessage(sender, "errors.database-error");
                    return;
                }

                boolean alreadyBanned = existingPunishments.stream()
                        .anyMatch(p -> (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN) && p.isActive());

                if (alreadyBanned) {
                    Map<String, String> placeholders = createPlaceholders("player", playerName);
                    sendMessage(sender, "ban.already-banned", placeholders);
                    return;
                }

                final Player onlinePlayer = Bukkit.getPlayer(uuid);
                final String ipAddress;
                if (onlinePlayer != null) {
                    ipAddress = onlinePlayer.getAddress().getAddress().getHostAddress();
                } else {
                    ipAddress = null;
                }

                plugin.getPunishmentManager().addPermanentPunishment(
                        uuid, playerName, ipAddress, PunishmentType.BAN, reason, operator
                ).whenComplete((v, ex2) -> {
                    if (ex2 != null) {
                        sendMessage(sender, "errors.database-error");
                        return;
                    }

                    // Kick player on main thread after ban is saved
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            onlinePlayer.kickPlayer(
                                messages.getMessage("ban.banned_message",
                                        createPlaceholders("player", playerName, "uuid", uuid.toString(),
                                                "reason", reason, "operator", operator, "ip", ipAddress != null ? ipAddress : "N/A"))
                            );
                        });
                    }

                    Map<String, String> placeholders = createPlaceholders("player", playerName);
                    sendMessage(sender, "ban.successfully-banned", placeholders);

                    Map<String, String> notifyPlaceholders = createPlaceholders(
                            "player", playerName, "operator", operator, "reason", reason);
                    notifyOperators("ban-notification", notifyPlaceholders);
                });
            });
        });

        return true;
    }
}
