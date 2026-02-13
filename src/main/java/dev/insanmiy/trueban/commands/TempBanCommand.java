package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TempBanCommand extends CommandBase {

    public TempBanCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "trueban.tempban")) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "&cUsage: /tempban <player> <duration> <reason>");
            return true;
        }

        String playerName = args[0];
        String duration = args[1];
        String reason = Arrays.stream(Arrays.copyOfRange(args, 2, args.length))
                .collect(Collectors.joining(" "));

        long durationSeconds = parseDuration(duration);
        if (durationSeconds == -1) {
            sendMessage(sender, "&cInvalid duration format. Use formats like: 1d, 2h, 30m, 1w");
            return true;
        }

        getPlayerUUID(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                sendMessage(sender, "&cPlayer '" + playerName + "' not found.");
                return;
            }

            if (plugin.getPunishmentManager().isBanned(uuid)) {
                sendMessage(sender, "&cPlayer '" + playerName + "' is already banned.");
                return;
            }

            String operatorName = sender instanceof Player ? ((Player) sender).getName() : "Console";
            String operatorUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "00000000-0000-0000-0000-000000000000";
            long expirationTime = Instant.now().getEpochSecond() + durationSeconds;

            Punishment punishment = new Punishment();
            punishment.setUuid(uuid);
            punishment.setPlayerName(playerName);
            punishment.setType(Punishment.PunishmentType.TEMPBAN);
            punishment.setReason(reason);
            punishment.setOperatorUuid(operatorUuid);
            punishment.setOperatorName(operatorName);
            punishment.setExpirationTimestamp(expirationTime);

            plugin.getPunishmentManager().addPunishment(punishment).thenRun(() -> {
                sendMessage(sender, "&aPlayer '" + playerName + "' has been temporarily banned for " + formatDuration(durationSeconds) + ": " + reason);

                Player target = Bukkit.getPlayer(uuid);
                if (target != null && target.isOnline()) {
                    target.kickPlayer(formatBanMessage(punishment));
                }
            });
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
        } else if (args.length == 2) {
            return List.of("1m", "5m", "10m", "30m", "1h", "2h", "6h", "12h", "1d", "3d", "1w", "2w");
        }
        return List.of();
    }

    private String formatBanMessage(Punishment punishment) {
        String message = plugin.getConfigManager().getBanMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        message = message.replace("%expiration%", punishment.getDurationString());
        message = message.replace("%appeal%", plugin.getConfigManager().getAppealURL());
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
