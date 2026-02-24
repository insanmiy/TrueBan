package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.Punishment;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/**
 * /unban <player|ip>
 */
public class UnbanCommand extends CommandBase implements CommandExecutor {

    public UnbanCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.unban")) {
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/unban <player|ip>"));
            return true;
        }

        String target = args[0];

        // Check if it's an IP address
        if (isIPAddress(target)) {
            unbanIP(sender, target);
        } else {
            unbanPlayer(sender, target);
        }

        return true;
    }

    /**
     * Unban a player
     */
    private void unbanPlayer(CommandSender sender, String playerName) {
        getPlayerUUID(playerName, uuid -> {
            if (uuid == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "commands.player-not-found", placeholders);
                return;
            }

            // Get active bans
            plugin.getPunishmentManager().getActivePunishments(uuid).whenComplete((punishments, ex) -> {
                if (ex != null) {
                    sendMessage(sender, "errors.database-error");
                    return;
                }

                boolean found = false;
                for (Punishment p : punishments) {
                    if (p.getType().isBan()) {
                        p.setActive(false);
                        plugin.getStorageManager().updatePunishment(p);
                        found = true;
                    }
                }

                if (!found) {
                    Map<String, String> placeholders = createPlaceholders("player", playerName);
                    sendMessage(sender, "unban.not-banned", placeholders);
                    return;
                }

                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "unban.successfully-unbanned", placeholders);

                Map<String, String> notifyPlaceholders = createPlaceholders(
                        "player", playerName, "operator", sender.getName());
                notifyOperators("unban-notification", notifyPlaceholders);

                sendConsoleMessage("player-unbanned",
                        createPlaceholders("player", playerName));
            });
        });
    }

    /**
     * Unban an IP address
     */
    private void unbanIP(CommandSender sender, String ipAddress) {
        plugin.getStorageManager().getPunishmentsByIP(ipAddress).whenComplete((punishments, ex) -> {
            if (ex != null) {
                sendMessage(sender, "errors.database-error");
                return;
            }

            boolean found = false;
            for (Punishment p : punishments) {
                if (p.getType() == PunishmentType.IPBAN) {
                    p.setActive(false);
                    plugin.getStorageManager().updatePunishment(p);
                    found = true;
                }
            }

            if (!found) {
                Map<String, String> placeholders = createPlaceholders("ip", ipAddress);
                sendMessage(sender, "unban.not-ipbanned", placeholders);
                return;
            }

            Map<String, String> placeholders = createPlaceholders("ip", ipAddress);
            sendMessage(sender, "unban.successfully-unipbanned", placeholders);

            Map<String, String> notifyPlaceholders = createPlaceholders(
                    "ip", ipAddress, "operator", sender.getName());
            notifyOperators("unban-notification", notifyPlaceholders);

            sendConsoleMessage("ip-unbanned",
                    createPlaceholders("ip", ipAddress));
        });
    }

    /**
     * Check if string is an IP address
     */
    private boolean isIPAddress(String str) {
        return str.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
}
