package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.PunishmentManager;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public class TempmuteCommand extends CommandBase implements CommandExecutor {

    public TempmuteCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.tempmute")) {
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/tempmute <player> <duration> <reason>"));
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

                plugin.getPunishmentManager().addTemporaryPunishment(
                        uuid, playerName, null, PunishmentType.TEMPMUTE, reason, operator, durationMillis
                ).whenComplete((v, ex2) -> {
                    if (ex2 != null) {
                        sendMessage(sender, "errors.database-error");
                        return;
                    }

                    Player player = org.bukkit.Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(messages.getMessage("mute.tempmuted_message",
                                createPlaceholders("reason", reason, "duration", durationStr)));
                    }

                    Map<String, String> placeholders = createPlaceholders("player", playerName, "duration", durationStr);
                    sendMessage(sender, "mute.successfully-tempmuted", placeholders);

                    Map<String, String> notifyPlaceholders = createPlaceholders(
                            "player", playerName, "operator", operator, "reason", reason, "duration", durationStr);
                    notifyOperators("tempmute-notification", notifyPlaceholders);

                    sendConsoleMessage("player-muted",
                            createPlaceholders("player", playerName, "reason", reason));
                });
            });
        });

        return true;
    }
}
