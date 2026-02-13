package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class UnmuteCommand extends CommandBase {

    public UnmuteCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "trueban.unmute")) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sendMessage(sender, "&cUsage: /unmute <player>");
            return true;
        }

        String playerName = args[0];

        getPlayerUUID(playerName).thenAccept(uuid -> {
            if (uuid == null) {
                sendMessage(sender, "&cPlayer '" + playerName + "' not found.");
                return;
            }

            if (!plugin.getPunishmentManager().isMuted(uuid)) {
                sendMessage(sender, "&cPlayer '" + playerName + "' is not muted.");
                return;
            }

            Punishment mute = plugin.getPunishmentManager().getMute(uuid);
            if (mute != null) {
                plugin.getPunishmentManager().removePunishment(uuid, mute.getType()).thenRun(() -> {
                    sendMessage(sender, "&aPlayer '" + playerName + "' has been unmuted.");
                });
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
