package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class UnmuteCommand extends CommandBase implements CommandExecutor {

    public UnmuteCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.unmute")) {
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/unmute <player>"));
            return true;
        }

        String playerName = args[0];

        getPlayerUUID(playerName, uuid -> {
            if (uuid == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "commands.player-not-found", placeholders);
                return;
            }

            plugin.getPunishmentManager().getActivePunishments(uuid).whenComplete((punishments, ex) -> {
                if (ex != null) {
                    sendMessage(sender, "errors.database-error");
                    return;
                }

                boolean found = false;
                for (Punishment p : punishments) {
                    if (p.getType().isMute()) {
                        p.setActive(false);
                        plugin.getStorageManager().updatePunishment(p);
                        found = true;
                    }
                }

                if (!found) {
                    Map<String, String> placeholders = createPlaceholders("player", playerName);
                    sendMessage(sender, "unmute.not-muted", placeholders);
                    return;
                }

                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "unmute.successfully-unmuted", placeholders);

                Map<String, String> notifyPlaceholders = createPlaceholders(
                        "player", playerName, "operator", sender.getName());
                notifyOperators("unmute-notification", notifyPlaceholders);

                sendConsoleMessage("player-unmuted",
                        createPlaceholders("player", playerName));
            });
        });

        return true;
    }
}
