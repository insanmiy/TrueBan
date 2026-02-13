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

public class KickCommand extends CommandBase {

    public KickCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "trueban.kick")) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "&cUsage: /kick <player> <reason>");
            return true;
        }

        String playerName = args[0];
        String reason = Arrays.stream(Arrays.copyOfRange(args, 1, args.length))
                .collect(Collectors.joining(" "));

        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOnline()) {
            sendMessage(sender, "&cPlayer '" + playerName + "' is not online.");
            return true;
        }

        String operatorName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        String operatorUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "00000000-0000-0000-0000-000000000000";

        Punishment punishment = new Punishment();
        punishment.setUuid(target.getUniqueId());
        punishment.setPlayerName(target.getName());
        punishment.setType(Punishment.PunishmentType.KICK);
        punishment.setReason(reason);
        punishment.setOperatorUuid(operatorUuid);
        punishment.setOperatorName(operatorName);

        plugin.getPunishmentManager().addPunishment(punishment).thenRun(() -> {
            sendMessage(sender, "&aPlayer '" + playerName + "' has been kicked for: " + reason);
            target.kickPlayer(formatKickMessage(punishment));
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private String formatKickMessage(Punishment punishment) {
        String message = plugin.getConfigManager().getKickMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
