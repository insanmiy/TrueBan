package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

/**
 * /mute <player> <reason>
 */
public class MuteCommand extends CommandBase implements CommandExecutor {

    public MuteCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.mute")) {
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/mute <player> <reason>"));
            return true;
        }

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String operator = sender.getName();

        getPlayerUUID(playerName, uuid -> {
            if (uuid == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerName);
                sendMessage(sender, "commands.player-not-found", placeholders);
                return;
            }

            // Check if already muted
            plugin.getPunishmentManager().getActivePunishments(uuid).whenComplete((punishments, ex) -> {
                if (ex != null) {
                    sendMessage(sender, "errors.database-error");
                    return;
                }

                boolean alreadyMuted = punishments.stream()
                        .anyMatch(p -> p.getType().isMute() && p.isActive());

                if (alreadyMuted) {
                    Map<String, String> placeholders = createPlaceholders("player", playerName);
                    sendMessage(sender, "mute.already-muted", placeholders);
                    return;
                }

                // Save mute
                plugin.getPunishmentManager().addPermanentPunishment(
                        uuid, playerName, null, PunishmentType.MUTE, reason, operator
                ).whenComplete((v, ex2) -> {
                    if (ex2 != null) {
                        sendMessage(sender, "errors.database-error");
                        return;
                    }

                    Player player = org.bukkit.Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(messages.getMessage("mute.muted_message",
                                createPlaceholders("reason", reason)));
                    }

                    Map<String, String> placeholders = createPlaceholders("player", playerName);
                    sendMessage(sender, "mute.successfully-muted", placeholders);

                    Map<String, String> notifyPlaceholders = createPlaceholders(
                            "player", playerName, "operator", operator, "reason", reason);
                    notifyOperators("mute-notification", notifyPlaceholders);

                    sendConsoleMessage("player-muted",
                            createPlaceholders("player", playerName, "reason", reason));
                });
            });
        });

        return true;
    }
}
