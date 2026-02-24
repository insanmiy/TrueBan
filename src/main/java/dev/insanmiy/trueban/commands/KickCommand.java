package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

/**
 * /kick <player> <reason>
 */
public class KickCommand extends CommandBase implements CommandExecutor {

    public KickCommand(TrueBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkPermission(sender, "trueban.kick")) {
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "commands.invalid-syntax",
                    createPlaceholders("usage", "/kick <player> <reason>"));
            return true;
        }

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String operator = sender.getName();

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            Map<String, String> placeholders = createPlaceholders("player", playerName);
            sendMessage(sender, "commands.player-not-found", placeholders);
            return true;
        }

        // Kick the player
        String kickMessage = messages.getMessage("kick.kicked_message",
                createPlaceholders("reason", reason, "operator", operator));
        player.kick(net.kyori.adventure.text.Component.text(kickMessage));

        Map<String, String> placeholders = createPlaceholders("player", playerName);
        sendMessage(sender, "kick.successfully-kicked", placeholders);

        Map<String, String> notifyPlaceholders = createPlaceholders(
                "player", playerName, "operator", operator, "reason", reason);
        notifyOperators("kick-notification", notifyPlaceholders);

        sendConsoleMessage("player-kicked",
                createPlaceholders("player", playerName, "reason", reason));

        return true;
    }
}
