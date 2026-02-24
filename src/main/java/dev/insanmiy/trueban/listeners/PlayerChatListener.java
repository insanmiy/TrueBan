package dev.insanmiy.trueban.listeners;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.Punishment;
import dev.insanmiy.trueban.punishment.PunishmentType;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for player chat - checks for mutes
 */
public class PlayerChatListener implements Listener {

    private final TrueBan plugin;

    public PlayerChatListener(TrueBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        // Check if player has bypass permission
        if (event.getPlayer().hasPermission("trueban.bypass")) {
            return;
        }

        // Check for mutes
        checkMute(event);
    }

    /**
     * Check if player is muted
     */
    private void checkMute(AsyncChatEvent event) {
        plugin.getPunishmentManager().getActivePunishments(event.getPlayer().getUniqueId())
                .whenComplete((punishments, ex) -> {
                    if (ex != null) {
                        plugin.getLogger().warning("Error checking mutes: " + ex.getMessage());
                        return;
                    }

                    for (Punishment p : punishments) {
                        if ((p.getType() == PunishmentType.MUTE || p.getType() == PunishmentType.TEMPMUTE) && p.isActive()) {
                            event.setCancelled(true);
                            getMuteMessage(p).whenComplete((message, msgEx) -> {
                                if (msgEx == null) {
                                    event.getPlayer().sendMessage(message);
                                }
                            });
                            return;
                        }
                    }
                });
    }

    /**
     * Get mute message with placeholders
     */
    private java.util.concurrent.CompletableFuture<String> getMuteMessage(Punishment punishment) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", punishment.getPlayerName());
            placeholders.put("reason", punishment.getReason());
            placeholders.put("operator", punishment.getOperator());

            if (punishment.getType() == PunishmentType.TEMPMUTE) {
                placeholders.put("duration", formatDuration(punishment.getExpiresAt() - System.currentTimeMillis()));
                return plugin.getMessageManager().getMessage("mute.tempmuted_message", placeholders);
            } else {
                return plugin.getMessageManager().getMessage("mute.muted_message", placeholders);
            }
        });
    }

    /**
     * Format duration
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "expired";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
