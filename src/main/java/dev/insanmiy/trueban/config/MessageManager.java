package dev.insanmiy.trueban.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages message loading and formatting with placeholder support
 */
public class MessageManager {

    private final JavaPlugin plugin;
    private Map<String, Object> messagesData;
    private final Pattern colorPattern = Pattern.compile("&([0-9a-fk-or])");
    private final Pattern placeholderPattern = Pattern.compile("%([a-zA-Z_]+)%");

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesData = new HashMap<>();
    }

    /**
     * Load messages from messages.yml
     */
    public void loadMessages() throws IOException {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        Yaml yaml = new Yaml();

        try (FileInputStream fis = new FileInputStream(messagesFile)) {
            messagesData = yaml.load(fis);
        }

        if (messagesData == null) {
            messagesData = new HashMap<>();
        }
    }

    /**
     * Get a message by path with color code conversion
     * Path format: "commands.no-permission"
     */
    public String getMessage(String path) {
        return formatMessage(getMessageRaw(path));
    }

    /**
     * Get a message by path and replace placeholders
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        return replacePlaceholders(message, placeholders);
    }

    /**
     * Get a message by path and replace a single placeholder
     */
    public String getMessage(String path, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        return getMessage(path, placeholders);
    }

    /**
     * Get raw message without formatting
     */
    private String getMessageRaw(String path) {
        Map<String, Object> current = messagesData;
        String[] keys = path.split("\\.");

        for (int i = 0; i < keys.length - 1; i++) {
            Object obj = current.get(keys[i]);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
                return path; // Return path if not found for debugging
            }
        }

        Object value = current.get(keys[keys.length - 1]);
        return value != null ? value.toString() : path;
    }

    /**
     * Format message by converting color codes
     * & codes are converted to § codes
     */
    private String formatMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Convert & color codes to § codes
        Matcher matcher = colorPattern.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(sb, "§" + matcher.group(1));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Replace placeholders in message
     * %placeholder% format
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || placeholders == null || placeholders.isEmpty()) {
            return message;
        }

        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Get the plugin prefix
     */
    public String getPrefix() {
        return getMessage("prefix");
    }

    /**
     * Create a prefixed message
     */
    public String getPrefixedMessage(String message) {
        return getPrefix() + " " + message;
    }

    /**
     * Get messages data
     */
    public Map<String, Object> getMessagesData() {
        return messagesData;
    }

    /**
     * Check if a message path exists
     */
    public boolean hasMessage(String path) {
        Map<String, Object> current = messagesData;
        String[] keys = path.split("\\.");

        for (int i = 0; i < keys.length - 1; i++) {
            Object obj = current.get(keys[i]);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
                return false;
            }
        }

        return current.containsKey(keys[keys.length - 1]);
    }
}
