package dev.insanmiy.trueban.managers;

import dev.insanmiy.trueban.TrueBan;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class ConfigManager {
    private final TrueBan plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(TrueBan plugin) {
        this.plugin = plugin;
        setupConfig();
    }

    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    public StorageType getStorageType() {
        String type = config.getString("storage.type", "JSON").toUpperCase();
        try {
            return StorageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid storage type '" + type + "', falling back to JSON");
            return StorageType.JSON;
        }
    }

    public String getSQLiteFilePath() {
        return config.getString("storage.sqlite.file-path", "plugins/TrueBan/punishments.db");
    }

    public String getMySQLHost() {
        return config.getString("storage.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("storage.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("storage.mysql.database", "minecraft");
    }

    public String getMySQLUsername() {
        return config.getString("storage.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("storage.mysql.password", "");
    }

    public String getBanMessageFormat() {
        return config.getString("messages.ban", "&cYou have been banned from this server!\n\n&7Reason: &f%reason%\n&7Banned by: &f%operator%\n&7Expires: &f%expiration%\n\n&7Appeal at: &f%appeal%");
    }

    public String getMuteMessageFormat() {
        return config.getString("messages.mute", "&cYou are muted and cannot chat!\n\n&7Reason: &f%reason%\n&7Muted by: &f%operator%\n&7Expires: &f%expiration%");
    }

    public String getKickMessageFormat() {
        return config.getString("messages.kick", "&cYou have been kicked from the server!\n\n&7Reason: &f%reason%\n&7Kicked by: &f%operator%");
    }

    public String getAppealURL() {
        return config.getString("messages.appeal-url", "https://example.com/appeal");
    }

    public boolean isNotifyStaff() {
        return config.getBoolean("settings.notify-staff", true);
    }

    public boolean isLogPunishments() {
        return config.getBoolean("settings.log-punishments", true);
    }

    public int getExpirationCheckInterval() {
        return config.getInt("settings.expiration-check-interval", 60);
    }

    public List<String> getBypassPermissions() {
        return config.getStringList("settings.bypass-permissions");
    }

    public enum StorageType {
        JSON, SQLITE, MYSQL
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
