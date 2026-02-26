package dev.insanmiy.trueban.punishment;

public enum PunishmentType {
    BAN("Ban", "a permanent ban"),
    TEMPBAN("Tempban", "a temporary ban"),
    IPBAN("IP Ban", "an IP ban"),
    MUTE("Mute", "a permanent mute"),
    TEMPMUTE("Tempmute", "a temporary mute"),
    KICK("Kick", "a kick");

    private final String displayName;
    private final String description;

    PunishmentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTemporary() {
        return this == TEMPBAN || this == TEMPMUTE;
    }

    public boolean isBan() {
        return this == BAN || this == TEMPBAN || this == IPBAN;
    }

    public boolean isMute() {
        return this == MUTE || this == TEMPMUTE;
    }
}
