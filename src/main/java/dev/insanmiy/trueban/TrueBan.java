package dev.insanmiy.trueban;

import dev.insanmiy.trueban.config.ConfigManager;
import dev.insanmiy.trueban.config.MessageManager;
import dev.insanmiy.trueban.punishment.PunishmentManager;
import dev.insanmiy.trueban.storage.StorageManager;
import dev.insanmiy.trueban.storage.SqliteStorage;
import dev.insanmiy.trueban.storage.MysqlStorage;
import dev.insanmiy.trueban.commands.*;
import dev.insanmiy.trueban.listeners.PlayerLoginListener;
import dev.insanmiy.trueban.listeners.PlayerChatListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TrueBan - Production-ready Minecraft moderation plugin for PaperMC
 * Version: 1.0.0
 * Main plugin class handles initialization and management
 */
public class TrueBan extends JavaPlugin {

    private static TrueBan instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private StorageManager storageManager;
    private PunishmentManager punishmentManager;
    private ScheduledExecutorService expirationExecutor;

    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("TrueBan is loading...");
    }

    @Override
    public void onEnable() {
        try {
            getLogger().info("================================");
            getLogger().info("TrueBan v" + getDescription().getVersion() + " is starting...");
            getLogger().info("================================");

            // Step 1: Load configurations
            this.configManager = new ConfigManager(this);
            configManager.loadConfigs();
            getLogger().info("✓ Configuration loaded");

            // Step 2: Load messages
            this.messageManager = new MessageManager(this);
            messageManager.loadMessages();
            getLogger().info("✓ Messages loaded");

            // Step 3: Initialize storage system
            this.storageManager = initializeStorage();
            getLogger().info("✓ Storage system initialized (" + configManager.getStorageType() + ")");

            // Step 4: Initialize punishment manager
            this.punishmentManager = new PunishmentManager(this, storageManager);
            getLogger().info("✓ Punishment manager initialized");

            // Step 5: Register commands with tab completers
            registerCommands();
            getLogger().info("✓ Commands registered");

            // Step 6: Register listeners
            registerListeners();
            getLogger().info("✓ Listeners registered");

            // Step 7: Schedule async expiration task
            scheduleExpirationTask();
            getLogger().info("✓ Expiration task scheduled");

            getLogger().info("================================");
            getLogger().info("TrueBan successfully enabled!");
            getLogger().info("================================");

        } catch (Exception e) {
            getLogger().severe("Failed to enable TrueBan!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("TrueBan is shutting down...");

        // Shutdown executors
        if (expirationExecutor != null && !expirationExecutor.isShutdown()) {
            expirationExecutor.shutdown();
            try {
                if (!expirationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    expirationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                expirationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close storage
        if (storageManager != null) {
            storageManager.close();
        }

        getLogger().info("TrueBan disabled successfully!");
    }

    /**
     * Initialize the storage system based on configuration
     */
    private StorageManager initializeStorage() throws Exception {
        String storageType = configManager.getStorageType().toUpperCase();

        // Only DB-backed storage is supported (SQLite by default)
        return switch (storageType) {
            case "SQLITE" -> new SqliteStorage(this);
            case "MYSQL" -> new MysqlStorage(this);
            default -> {
                getLogger().warning("Unknown storage type: " + storageType + ". Defaulting to SQLITE.");
                yield new SqliteStorage(this);
            }
        };
    }

    /**
     * Register all commands
     */
    private void registerCommands() {
        Objects.requireNonNull(getCommand("ban")).setExecutor(new BanCommand(this));
        Objects.requireNonNull(getCommand("tempban")).setExecutor(new TempbanCommand(this));
        Objects.requireNonNull(getCommand("unban")).setExecutor(new UnbanCommand(this));
        Objects.requireNonNull(getCommand("ipban")).setExecutor(new IpbanCommand(this));
        Objects.requireNonNull(getCommand("kick")).setExecutor(new KickCommand(this));
        Objects.requireNonNull(getCommand("mute")).setExecutor(new MuteCommand(this));
        Objects.requireNonNull(getCommand("tempmute")).setExecutor(new TempmuteCommand(this));
        Objects.requireNonNull(getCommand("unmute")).setExecutor(new UnmuteCommand(this));
        Objects.requireNonNull(getCommand("history")).setExecutor(new HistoryCommand(this));

        // Tab completers
        Objects.requireNonNull(getCommand("ban")).setTabCompleter(new PlayerListTabCompleter(this));
        Objects.requireNonNull(getCommand("tempban")).setTabCompleter(new PlayerListTabCompleter(this));
        Objects.requireNonNull(getCommand("unban")).setTabCompleter(new UnbanTabCompleter(this));
        Objects.requireNonNull(getCommand("ipban")).setTabCompleter(new IpTabCompleter(this));
        Objects.requireNonNull(getCommand("kick")).setTabCompleter(new PlayerListTabCompleter(this));
        Objects.requireNonNull(getCommand("mute")).setTabCompleter(new PlayerListTabCompleter(this));
        Objects.requireNonNull(getCommand("tempmute")).setTabCompleter(new PlayerListTabCompleter(this));
        Objects.requireNonNull(getCommand("unmute")).setTabCompleter(new PlayerListTabCompleter(this));
        Objects.requireNonNull(getCommand("history")).setTabCompleter(new PlayerListTabCompleter(this));
    }

    /**
     * Register all event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
    }

    /**
     * Schedule the expiration check task
     */
    private void scheduleExpirationTask() {
        expirationExecutor = Executors.newScheduledThreadPool(configManager.getAsyncThreads());
        long interval = configManager.getExpirationCheckInterval();

        expirationExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        punishmentManager.expireActivePunishments();
                    } catch (Exception e) {
                        getLogger().severe("Error during punishment expiration check: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                interval, interval, TimeUnit.SECONDS
        );
    }

    // Getters

    public static TrueBan getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public ScheduledExecutorService getExpirationExecutor() {
        return expirationExecutor;
    }
}
