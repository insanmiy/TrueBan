package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IpBanCommand extends CommandBase {

    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    public IpBanCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "trueban.ipban")) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "&cUsage: /ipban <player|IP> <reason>");
            return true;
        }

        String target = args[0];
        String reason = Arrays.stream(Arrays.copyOfRange(args, 1, args.length))
                .collect(Collectors.joining(" "));

        if (IP_PATTERN.matcher(target).matches()) {
            handleIPBan(sender, target, reason, null);
        } else {
            handlePlayerIPBan(sender, target, reason);
        }

        return true;
    }

    private void handlePlayerIPBan(CommandSender sender, String playerName, String reason) {
        getPlayerUUID(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                sendMessage(sender, "&cPlayer '" + playerName + "' not found.");
                return;
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                String ipAddress = player.getAddress().getAddress().getHostAddress();
                handleIPBan(sender, ipAddress, reason, uuid);
            } else {
                sendMessage(sender, "&cPlayer '" + playerName + "' must be online to IP ban.");
            }
        });
    }

    private void handleIPBan(CommandSender sender, String ipAddress, String reason, UUID playerUuid) {
        String operatorName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        String operatorUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "00000000-0000-0000-0000-000000000000";

        Punishment punishment = new Punishment();
        punishment.setUuid(playerUuid != null ? playerUuid : UUID.fromString("00000000-0000-0000-0000-000000000000"));
        punishment.setPlayerName(playerUuid != null ? Bukkit.getOfflinePlayer(playerUuid).getName() : "Unknown");
        punishment.setIpAddress(ipAddress);
        punishment.setType(Punishment.PunishmentType.IPBAN);
        punishment.setReason(reason);
        punishment.setOperatorUuid(operatorUuid);
        punishment.setOperatorName(operatorName);
        punishment.setExpirationTimestamp(-1);

        plugin.getPunishmentManager().addPunishment(punishment).thenRun(() -> {
            sendMessage(sender, "&aIP address '" + ipAddress + "' has been banned for: " + reason);

            if (playerUuid != null) {
                Player target = Bukkit.getPlayer(playerUuid);
                if (target != null && target.isOnline()) {
                    target.kickPlayer(formatBanMessage(punishment));
                }
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> players = plugin.getPunishmentManager().getAllKnownPlayers().join().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toList());

            if (args[0].contains(".")) {
                players.add(args[0]);
            }

            return players;
        }
        return List.of();
    }

    private String formatBanMessage(Punishment punishment) {
        String message = plugin.getConfigManager().getBanMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        message = message.replace("%expiration%", "Never");
        message = message.replace("%appeal%", plugin.getConfigManager().getAppealURL());
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
