package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab completer for player list (online and offline players)
 */
public class PlayerListTabCompleter implements TabCompleter {

    private final TrueBan plugin;

    public PlayerListTabCompleter(TrueBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // First argument: player names
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Add online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }

            // Add known offline players from storage
            plugin.getStorageManager().getKnownPlayerNames().whenComplete((names, ex) -> {
                if (names != null) {
                    for (String name : names) {
                        if (name.toLowerCase().startsWith(input) && !completions.contains(name)) {
                            completions.add(name);
                        }
                    }
                }
            });
        }
        // Second argument: reason or duration (no specific completions)
        else if (args.length >= 2) {
            // Could add suggestions for common reasons or durations
            // For now, leaving empty to show player can type anything
        }

        return completions;
    }
}
