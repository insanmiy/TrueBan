package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerListTabCompleter implements TabCompleter {

    private final TrueBan plugin;

    public PlayerListTabCompleter(TrueBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }

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
        else if (args.length >= 2) {
        }

        return completions;
    }
}
