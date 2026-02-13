package dev.insanmiy.trueban.models;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Punishment {
    private UUID uuid;
    private String playerName;
    private String ipAddress;
    private PunishmentType type;
    private String reason;
    private String operatorUuid;
    private String operatorName;
    private long creationTimestamp;
    private long expirationTimestamp;
    private boolean active;

    public enum PunishmentType {
        BAN, TEMPBAN, IPBAN, MUTE, TEMPMUTE, KICK
    }

    public Punishment() {
        this.creationTimestamp = Instant.now().getEpochSecond();
        this.active = true;
    }

    public Punishment(UUID uuid, String playerName, String ipAddress, PunishmentType type, String reason, String operatorUuid, String operatorName, long expirationTimestamp) {
        this();
        this.uuid = uuid;
        this.playerName = playerName;
        this.ipAddress = ipAddress;
        this.type = type;
        this.reason = reason;
        this.operatorUuid = operatorUuid;
        this.operatorName = operatorName;
        this.expirationTimestamp = expirationTimestamp;
    }

    public boolean isExpired() {
        if (type == PunishmentType.BAN || type == PunishmentType.IPBAN || type == PunishmentType.MUTE || type == PunishmentType.KICK) {
            return false;
        }
        return expirationTimestamp > 0 && Instant.now().getEpochSecond() > expirationTimestamp;
    }

    public boolean isActive() {
        return active && !isExpired();
    }

    public String getDurationString() {
        if (expirationTimestamp <= 0) return "Permanent";

        Instant now = Instant.now();
        Instant expiration = Instant.ofEpochSecond(expirationTimestamp);

        if (now.isAfter(expiration)) return "Expired";

        long days = ChronoUnit.DAYS.between(now, expiration);
        long hours = ChronoUnit.HOURS.between(now, expiration) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, expiration) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");

        return sb.toString().trim();
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public PunishmentType getType() { return type; }
    public void setType(PunishmentType type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperatorUuid() { return operatorUuid; }
    public void setOperatorUuid(String operatorUuid) { this.operatorUuid = operatorUuid; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public long getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(long creationTimestamp) { this.creationTimestamp = creationTimestamp; }
    public long getExpirationTimestamp() { return expirationTimestamp; }
    public void setExpirationTimestamp(long expirationTimestamp) { this.expirationTimestamp = expirationTimestamp; }
    public boolean isActiveFlag() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
