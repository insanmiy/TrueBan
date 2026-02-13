package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HistoryCommand extends CommandBase {

    public HistoryCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "trueban.history")) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sendMessage(sender, "&cUsage: /history <player>");
            return true;
        }

        String playerName = args[0];

        getPlayerUUID(playerName).thenCompose(uuid -> {
            if (uuid == null) {
                sendMessage(sender, "&cPlayer '" + playerName + "' not found.");
                return CompletableFuture.completedFuture(null);
            }

            return plugin.getPunishmentManager().getPlayerHistory(uuid);
        }).thenAccept(punishments -> {
            if (punishments == null || punishments.isEmpty()) {
                sendMessage(sender, "&aPlayer '" + playerName + "' has no punishment history.");
                return;
            }

            sendMessage(sender, "&6=== Punishment History for " + playerName + " ===");

            for (int i = 0; i < punishments.size(); i++) {
                Punishment punishment = punishments.get(i);
                String status = punishment.isActive() ? "&aActive" : "&cExpired";
                String expiration = punishment.getExpirationTimestamp() > 0 ? 
                    DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochSecond(punishment.getExpirationTimestamp())) : 
                    "Never";

                String entry = String.format(
                    "&7%d. &e%s &7- %s &7- %s &7- %s &7- %s &7- %s",
                    i + 1,
                    punishment.getType(),
                    punishment.getReason(),
                    punishment.getOperatorName(),
                    DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochSecond(punishment.getCreationTimestamp())),
                    expiration,
                    status
                );

                sendMessage(sender, entry);
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.getPunishmentManager().getAllKnownPlayers().join().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
