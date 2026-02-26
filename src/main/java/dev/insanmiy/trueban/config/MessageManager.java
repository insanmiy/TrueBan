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

public class MessageManager {

    private final JavaPlugin plugin;
    private Map<String, Object> messagesData;
    private final Pattern colorPattern = Pattern.compile("&([0-9a-fk-or])");
    private final Pattern placeholderPattern = Pattern.compile("%([a-zA-Z_]+)%");

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesData = new HashMap<>();
    }

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

    public String getMessage(String path) {
        return formatMessage(getMessageRaw(path));
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        return replacePlaceholders(message, placeholders);
    }

    public String getMessage(String path, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        return getMessage(path, placeholders);
    }

    private String getMessageRaw(String path) {
        Map<String, Object> current = messagesData;
        String[] keys = path.split("\\.");

        for (int i = 0; i < keys.length - 1; i++) {
            Object obj = current.get(keys[i]);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
            }
        }

        Object value = current.get(keys[keys.length - 1]);
        return value != null ? value.toString() : path;
    }

    private String formatMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Matcher matcher = colorPattern.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(sb, "§" + matcher.group(1));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

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

    public String getPrefix() {
        return getMessage("prefix");
    }

    public String getPrefixedMessage(String message) {
        return getPrefix() + " " + message;
    }

    public Map<String, Object> getMessagesData() {
        return messagesData;
    }

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
