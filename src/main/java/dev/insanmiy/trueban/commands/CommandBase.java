package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public abstract class CommandBase {

    protected final TrueBan plugin;
    protected final MessageManager messages;

    public CommandBase(TrueBan plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
    }

    protected void sendMessage(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        String message = messages.getMessage(messageKey, placeholders);
        sender.sendMessage(message);
    }

    protected void sendMessage(CommandSender sender, String messageKey) {
        String message = messages.getMessage(messageKey);
        sender.sendMessage(message);
    }

    protected void notifyOperators(String messageKey, Map<String, String> placeholders) {
        if (!messages.hasMessage("notifications." + messageKey)) {
            return;
        }

        String message = messages.getMessage("notifications." + messageKey, placeholders);

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("trueban.notify"))
                .forEach(p -> p.sendMessage(message));

        Bukkit.getConsoleSender().sendMessage(message);
    }

    protected void sendConsoleMessage(String messageKey, Map<String, String> placeholders) {
        String message = messages.getMessage("console." + messageKey, placeholders);
        Bukkit.getConsoleSender().sendMessage(message);
    }

    protected boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sendMessage(sender, "commands.no-permission");
            return false;
        }
        return true;
    }

    protected Map<String, String> createPlaceholders(String... keyValuePairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                map.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        return map;
    }

    protected void getPlayerUUID(String playerName, java.util.function.Consumer<java.util.UUID> callback) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            callback.accept(onlinePlayer.getUniqueId());
            return;
        }

        plugin.getPunishmentManager().getPlayerUUID(playerName).whenComplete((uuid, ex) -> {
            if (ex != null || uuid == null) {
                callback.accept(null);
            } else {
                callback.accept(uuid);
            }
        });
    }

    protected String getPlayerIP(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return null;
    }
}
