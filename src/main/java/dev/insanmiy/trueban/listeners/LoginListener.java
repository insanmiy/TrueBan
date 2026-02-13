package dev.insanmiy.trueban.listeners;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LoginListener implements Listener {
    private final TrueBan plugin;

    public LoginListener(TrueBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        if (event.getAddress() != null && plugin.getPunishmentManager().isBanned(event.getUniqueId())) {
            Punishment ban = plugin.getPunishmentManager().getBan(event.getUniqueId());
            if (ban != null && ban.isActive()) {
                String message = formatBanMessage(ban);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
                return;
            }
        }

        if (event.getAddress() != null) {
            String ipAddress = event.getAddress().getHostAddress();
            if (isIPBanned(ipAddress)) {
                String message = ChatColor.RED + "Your IP address is banned from this server!\n\n" +
                               ChatColor.GRAY + "Contact an administrator for assistance.";
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        if (player.hasPermission("trueban.bypass")) {
            return;
        }

        if (plugin.getPunishmentManager().isBanned(player.getUniqueId())) {
            Punishment ban = plugin.getPunishmentManager().getBan(player.getUniqueId());
            if (ban != null && ban.isActive()) {
                String message = formatBanMessage(ban);
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, message);
            }
        }
    }

    private boolean isIPBanned(String ipAddress) {
        return plugin.getPunishmentManager().getAllKnownPlayers()
            .thenCompose(players -> {
                for (String playerName : players) {
                    return plugin.getPunishmentManager().getPlayerUUID(playerName)
                        .thenCompose(uuid -> {
                            if (uuid != null) {
                                return plugin.getPunishmentManager().getPlayerHistory(uuid)
                                    .thenApply(punishments -> {
                                        for (Punishment punishment : punishments) {
                                            if (punishment.getType() == Punishment.PunishmentType.IPBAN &&
                                                punishment.isActive() &&
                                                ipAddress.equals(punishment.getIpAddress())) {
                                                return true;
                                            }
                                        }
                                        return false;
                                    });
                            }
                            return CompletableFuture.completedFuture(false);
                        });
                }
                return CompletableFuture.completedFuture(false);
            })
            .join();
    }

    private String formatBanMessage(Punishment punishment) {
        String message = plugin.getConfigManager().getBanMessageFormat();
        message = message.replace("%reason%", punishment.getReason());
        message = message.replace("%operator%", punishment.getOperatorName());
        message = message.replace("%expiration%", getExpirationDisplay(punishment));
        message = message.replace("%appeal%", plugin.getConfigManager().getAppealURL());
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
