package dev.insanmiy.trueban;

import dev.insanmiy.trueban.commands.*;
import dev.insanmiy.trueban.listeners.ChatListener;
import dev.insanmiy.trueban.listeners.LoginListener;
import dev.insanmiy.trueban.managers.ConfigManager;
import dev.insanmiy.trueban.managers.PunishmentManager;
import dev.insanmiy.trueban.storage.JsonStorage;
import dev.insanmiy.trueban.storage.MySQLStorage;
import dev.insanmiy.trueban.storage.SQLiteStorage;
import dev.insanmiy.trueban.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class TrueBan extends JavaPlugin {
    private ConfigManager configManager;
    private StorageManager storageManager;
    private PunishmentManager punishmentManager;
    private final Map<String, CommandBase> commands = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Starting TrueBan v" + getDescription().getVersion());

        try {
            setupManagers();
            registerCommands();
            registerListeners();
            startTasks();

            getLogger().info("TrueBan has been enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable TrueBan", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (punishmentManager != null) {
            punishmentManager.shutdown();
        }

        getLogger().info("TrueBan has been disabled.");
    }

    private void setupManagers() {
        configManager = new ConfigManager(this);

        ConfigManager.StorageType storageType = configManager.getStorageType();

        switch (storageType) {
            case JSON -> storageManager = new JsonStorage(this);
            case SQLITE -> storageManager = new SQLiteStorage(this, configManager.getSQLiteFilePath());
            case MYSQL -> storageManager = new MySQLStorage(
                this,
                configManager.getMySQLHost(),
                configManager.getMySQLPort(),
                configManager.getMySQLDatabase(),
                configManager.getMySQLUsername(),
                configManager.getMySQLPassword()
            );
        }

        punishmentManager = new PunishmentManager(this, storageManager, configManager);

        punishmentManager.initialize().whenComplete((result, throwable) -> {
            if (throwable != null) {
                getLogger().log(Level.SEVERE, "Failed to initialize punishment manager", throwable);
                getServer().getPluginManager().disablePlugin(this);
            } else {
                getLogger().info("Punishment manager initialized successfully");
            }
        });
    }

    private void registerCommands() {
        commands.put("ban", new BanCommand(this));
        commands.put("tempban", new TempBanCommand(this));
        commands.put("unban", new UnbanCommand(this));
        commands.put("ipban", new IpBanCommand(this));
        commands.put("kick", new KickCommand(this));
        commands.put("mute", new MuteCommand(this));
        commands.put("tempmute", new TempMuteCommand(this));
        commands.put("unmute", new UnmuteCommand(this));
        commands.put("history", new HistoryCommand(this));

        for (Map.Entry<String, CommandBase> entry : commands.entrySet()) {
            getCommand(entry.getKey()).setExecutor(entry.getValue());
            getCommand(entry.getKey()).setTabCompleter(entry.getValue());
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    private void startTasks() {
        punishmentManager.startExpirationTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandBase cmd = commands.get(command.getName().toLowerCase());
        if (cmd != null) {
            return cmd.onCommand(sender, command, label, args);
        }
        return false;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
}
