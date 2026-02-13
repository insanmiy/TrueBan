package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MuteCommand extends CommandBase {

    public MuteCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "trueban.mute")) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "&cUsage: /mute <player> <reason>");
            return true;
        }

        String playerName = args[0];
        String reason = Arrays.stream(Arrays.copyOfRange(args, 1, args.length))
                .collect(Collectors.joining(" "));

        getPlayerUUID(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                sendMessage(sender, "&cPlayer '" + playerName + "' not found.");
                return;
            }

            if (plugin.getPunishmentManager().isMuted(uuid)) {
                sendMessage(sender, "&cPlayer '" + playerName + "' is already muted.");
                return;
            }

            String operatorName = sender instanceof Player ? ((Player) sender).getName() : "Console";
            String operatorUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "00000000-0000-0000-0000-000000000000";

            Punishment punishment = new Punishment();
            punishment.setUuid(uuid);
            punishment.setPlayerName(playerName);
            punishment.setType(Punishment.PunishmentType.MUTE);
            punishment.setReason(reason);
            punishment.setOperatorUuid(operatorUuid);
            punishment.setOperatorName(operatorName);
            punishment.setExpirationTimestamp(-1);

            plugin.getPunishmentManager().addPunishment(punishment).thenRun(() -> {
                sendMessage(sender, "&aPlayer '" + playerName + "' has been muted for: " + reason);

                Player target = Bukkit.getPlayer(uuid);
                if (target != null && target.isOnline()) {
                    target.sendMessage(formatMuteMessage(punishment));
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
        }
        return List.of();
    }

    private String formatMuteMessage(Punishment punishment) {
        String message = plugin.getConfigManager().getMuteMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        message = message.replace("%expiration%", "Never");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
