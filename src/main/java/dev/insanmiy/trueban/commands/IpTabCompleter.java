package dev.insanmiy.trueban.commands;

import dev.insanmiy.trueban.TrueBan;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tab completer for IP ban/unban commands
 */
public class IpTabCompleter implements TabCompleter {

    private final TrueBan plugin;

    public IpTabCompleter(TrueBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            Set<String> seen = new HashSet<>();

            // Add online players (show their IPs)
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (name.toLowerCase().startsWith(input) && seen.add(name)) {
                    completions.add(name);
                }
            }

            // Add IP addresses if user is typing digits
            if (input.matches("\\d.*")) {
                // Could add known IPs from storage, but for now keep it simple
            }
        }

        return completions;
    }
}
