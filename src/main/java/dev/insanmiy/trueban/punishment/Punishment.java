package dev.insanmiy.trueban.punishment;

import java.util.UUID;

public class Punishment {

    private final UUID playerUUID;
    private final String playerName;
    private final String ipAddress;
    private final PunishmentType type;
    private final String reason;
    private final String operator;
    private final long createdAt;
    private final long expiresAt;
    private boolean active;

    public Punishment(
            UUID playerUUID,
            String playerName,
            String ipAddress,
            PunishmentType type,
            String reason,
            String operator,
            long createdAt,
            long expiresAt,
            boolean active
    ) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.ipAddress = ipAddress;
        this.type = type;
        this.reason = reason;
        this.operator = operator;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public static Punishment createPermanent(
            UUID playerUUID,
            String playerName,
            String ipAddress,
            PunishmentType type,
            String reason,
            String operator
    ) {
        return new Punishment(playerUUID, playerName, ipAddress, type, reason, operator, System.currentTimeMillis(), -1, true);
    }

    public static Punishment createTemporary(
            UUID playerUUID,
            String playerName,
            String ipAddress,
            PunishmentType type,
            String reason,
            String operator,
            long durationMillis
    ) {
        long now = System.currentTimeMillis();
        return new Punishment(playerUUID, playerName, ipAddress, type, reason, operator, now, now + durationMillis, true);
    }

    public boolean hasExpired() {
        if (expiresAt == -1) {
            return false;
        }
        return System.currentTimeMillis() >= expiresAt;
    }

    public boolean isActive() {
        return active && !hasExpired();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public String getOperator() {
        return operator;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Punishment{" +
                "playerUUID=" + playerUUID +
                ", playerName='" + playerName + '\'' +
                ", type=" + type +
                ", reason='" + reason + '\'' +
                ", operator='" + operator + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", active=" + active +
                '}';
    }
}
