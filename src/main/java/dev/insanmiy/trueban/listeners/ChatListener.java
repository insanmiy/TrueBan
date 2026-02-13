package dev.insanmiy.trueban.listeners;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class ChatListener implements Listener {
    private final TrueBan plugin;

    public ChatListener(TrueBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("trueban.bypass")) {
            return;
        }

        if (plugin.getPunishmentManager().isMuted(player.getUniqueId())) {
            Punishment mute = plugin.getPunishmentManager().getMute(player.getUniqueId());
            if (mute != null && mute.isActive()) {
                event.setCancelled(true);
                player.sendMessage(formatMuteMessage(mute));
            }
        }
    }

    private String formatMuteMessage(Punishment punishment) {
        String message = plugin.getConfigManager().getMuteMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        message = message.replace("%expiration%", getExpirationDisplay(punishment));
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String getExpirationDisplay(Punishment punishment) {
        if (punishment.getExpirationTimestamp() <= 0) {
            return "Never";
        }

        Instant expiration = Instant.ofEpochSecond(punishment.getExpirationTimestamp());
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(expiration) +
               " (" + punishment.getDurationString() + ")";
    }
}
