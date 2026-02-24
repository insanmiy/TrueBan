package dev.insanmiy.trueban.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Manages configuration loading and access
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private Map<String, Object> configData;
    private Map<String, Object> messagesData;
    private String storageType;
    private long expirationCheckInterval;
    private int asyncThreads;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all configuration files
     */
    public void loadConfigs() throws IOException {
        saveDefaultConfigs();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        Yaml yaml = new Yaml();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            configData = yaml.load(fis);
        }

        parseConfig();
    }

    /**
     * Load messages configuration
     */
    public void loadMessages() throws IOException {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        Yaml yaml = new Yaml();

        try (FileInputStream fis = new FileInputStream(messagesFile)) {
            messagesData = yaml.load(fis);
        }
    }

    /**
     * Save default configuration files if they don't exist
     */
    private void saveDefaultConfigs() throws IOException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        saveResource("config.yml");
        saveResource("messages.yml");
    }

    /**
     * Save a resource from the jar to the file system
     */
    private void saveResource(String resourceName) throws IOException {
        File file = new File(plugin.getDataFolder(), resourceName);

        if (file.exists()) {
            return;
        }

        try (var inputStream = plugin.getResource(resourceName)) {
            if (inputStream == null) {
                plugin.getLogger().warning("Could not find resource: " + resourceName);
                return;
            }

            byte[] bytes = inputStream.readAllBytes();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(new String(bytes));
            }
        }
    }

    /**
     * Parse configuration values
     */
    private void parseConfig() {
        if (configData == null) {
            plugin.getLogger().warning("Config data is null!");
            return;
        }

        // Parse storage type
        Map<String, Object> storage = (Map<String, Object>) configData.get("storage");
        if (storage != null) {
            storageType = (String) storage.getOrDefault("type", "JSON");
        } else {
            storageType = "JSON";
        }

        // Parse settings
        Map<String, Object> settings = (Map<String, Object>) configData.get("settings");
        if (settings != null) {
            Object intervalObj = settings.get("check-expiration-interval-seconds");
            expirationCheckInterval = intervalObj instanceof Number ? ((Number) intervalObj).longValue() : 30L;

            Object threadsObj = settings.get("async-threads");
            asyncThreads = threadsObj instanceof Number ? ((Number) threadsObj).intValue() : 2;
        } else {
            expirationCheckInterval = 30L;
            asyncThreads = 2;
        }
    }

    // Getters

    public String getStorageType() {
        return storageType;
    }

    public long getExpirationCheckInterval() {
        return expirationCheckInterval;
    }

    public int getAsyncThreads() {
        return asyncThreads;
    }

    public Map<String, Object> getConfigData() {
        return configData;
    }

    public Map<String, Object> getMessagesData() {
        return messagesData;
    }

    public String getString(String path, String defaultValue) {
        Map<String, Object> config = configData;
        String[] keys = path.split("\\.");

        for (int i = 0; i < keys.length - 1; i++) {
            Object obj = config.get(keys[i]);
            if (obj instanceof Map) {
                config = (Map<String, Object>) obj;
            } else {
                return defaultValue;
            }
        }

        Object value = config.get(keys[keys.length - 1]);
        return value != null ? value.toString() : defaultValue;
    }

    public int getInt(String path, int defaultValue) {
        try {
            String value = getString(path, "");
            return value.isEmpty() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        String value = getString(path, "").toLowerCase();
        return value.isEmpty() ? defaultValue : value.equals("true") || value.equals("yes") || value.equals("1");
    }
}
