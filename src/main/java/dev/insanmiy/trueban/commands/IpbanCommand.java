package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public class IpbanCommand extends CommandBase implements CommandExecutor {

    public IpbanCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.ipban")) {
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/ipban <player|ip> [reason]"));
            return true;
        }

        String target = args[0];
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        String operator = sender.getName();

        String ipAddress;

        if (isIPAddress(target)) {
            ipAddress = target;
        } else {
            final Player player = Bukkit.getPlayer(target);
            if (player != null) {
                ipAddress = player.getAddress().getAddress().getHostAddress();
                // Don't kick here - kick after ban is saved on main thread
            } else {
                Map<String, String> placeholders = createPlaceholders("player", target);
                sendMessage(sender, "commands.player-not-found", placeholders);
                return true;
            }
        }

        plugin.getStorageManager().getPunishmentsByIP(ipAddress).whenComplete((existing, ex) -> {
            if (ex != null) {
                sendMessage(sender, "errors.database-error");
                return;
            }

            // Check if IP is already banned
            boolean alreadyIPBanned = existing.stream()
                    .anyMatch(p -> p.getType() == PunishmentType.IPBAN && p.isActive());

            if (alreadyIPBanned) {
                Map<String, String> placeholders = createPlaceholders("ip", ipAddress);
                sendMessage(sender, "ban.already-ipbanned", placeholders);
                return;
            }

            plugin.getPunishmentManager().addPermanentPunishment(
                    java.util.UUID.randomUUID(), "IPBan-" + ipAddress, ipAddress, 
                    PunishmentType.IPBAN, reason, operator
            ).whenComplete((v, ex2) -> {
                if (ex2 != null) {
                    sendMessage(sender, "errors.database-error");
                    return;
                }

                // Kick player on main thread after IP ban is saved
                if (!isIPAddress(target)) {
                    Player player = Bukkit.getPlayer(target);
                    if (player != null && player.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.kickPlayer(
                                messages.getMessage("ban.ipban_message",
                                        createPlaceholders("reason", reason, "operator", operator, "ip", ipAddress))
                            );
                        });
                    }
                }

                Map<String, String> placeholders = createPlaceholders("ip", ipAddress);
                sendMessage(sender, "ban.successfully-ipbanned", placeholders);

                Map<String, String> notifyPlaceholders = createPlaceholders(
                        "ip", ipAddress, "operator", operator, "reason", reason);
                notifyOperators("ban-notification", notifyPlaceholders);
            });
        });

        return true;
    }

    private boolean isIPAddress(String str) {
        return str.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
}
