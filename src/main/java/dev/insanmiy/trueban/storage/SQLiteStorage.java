package dev.insanmiy.trueban.storage;

import dev.insanmiy.trueban.punishment.Punishment;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SqliteStorage implements StorageManager {

    private final JavaPlugin plugin;
    private final String databasePath;
    private final ScheduledExecutorService executor;
    private volatile boolean ready;

    public SqliteStorage(JavaPlugin plugin) throws Exception {
        this.plugin = plugin;
        this.databasePath = Paths.get(plugin.getDataFolder().getAbsolutePath(), "trueban.db").toString();
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TrueBan-SqliteStorage");
            t.setDaemon(true);
            return t;
        });

        initializeDatabase();
        this.ready = true;
    }

    private void initializeDatabase() throws Exception {
        Files.createDirectories(Paths.get(plugin.getDataFolder().getAbsolutePath()));

        try (Connection conn = getConnection()) {
            createTables(conn);
        }
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        return DriverManager.getConnection(url);
    }

    private void createTables(Connection conn) throws SQLException {
        String createPunishmentsTable = """
                CREATE TABLE IF NOT EXISTS punishments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    ip_address TEXT,
                    type TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    operator TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT 1,
                    UNIQUE(player_uuid, created_at)
                )
                """;

        String createIndexes = """
                CREATE INDEX IF NOT EXISTS idx_player_uuid ON punishments(player_uuid);
                CREATE INDEX IF NOT EXISTS idx_ip_address ON punishments(ip_address);
                CREATE INDEX IF NOT EXISTS idx_active ON punishments(active);
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createPunishmentsTable);
            for (String index : createIndexes.split(";")) {
                if (!index.trim().isEmpty()) {
                    stmt.execute(index);
                }
            }
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @Override
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = """
                        INSERT INTO punishments (player_uuid, player_name, ip_address, type, reason, operator, created_at, expires_at, active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, punishment.getPlayerUUID().toString());
                    stmt.setString(2, punishment.getPlayerName());
                    stmt.setString(3, punishment.getIpAddress());
                    stmt.setString(4, punishment.getType().toString());
                    stmt.setString(5, punishment.getReason());
                    stmt.setString(6, punishment.getOperator());
                    stmt.setLong(7, punishment.getCreatedAt());
                    stmt.setLong(8, punishment.getExpiresAt());
                    stmt.setBoolean(9, punishment.isActive());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save punishment: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updatePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = """
                        UPDATE punishments
                        SET active = ?
                        WHERE player_uuid = ? AND created_at = ?
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setBoolean(1, punishment.isActive());
                    stmt.setString(2, punishment.getPlayerUUID().toString());
                    stmt.setLong(3, punishment.getCreatedAt());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update punishment: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = """
                        SELECT player_uuid, player_name, ip_address, type, reason, operator, created_at, expires_at, active
                        FROM punishments
                        WHERE player_uuid = ? AND active = 1
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            punishments.add(resultSetToPunishment(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get active punishments: " + e.getMessage());
            }

            return punishments;
        }, executor);
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = """
                        SELECT player_uuid, player_name, ip_address, type, reason, operator, created_at, expires_at, active
                        FROM punishments
                        WHERE player_uuid = ?
                        ORDER BY created_at DESC
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            punishments.add(resultSetToPunishment(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get punishment history: " + e.getMessage());
            }

            return punishments;
        }, executor);
    }

    @Override
    public CompletableFuture<UUID> getOfflineUUID(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = "SELECT DISTINCT player_uuid FROM punishments WHERE LOWER(player_name) = LOWER(?) LIMIT 1";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return UUID.fromString(rs.getString("player_uuid"));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get offline UUID: " + e.getMessage());
            }

            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<List<String>> getKnownPlayerNames() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> names = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = "SELECT DISTINCT player_name FROM punishments ORDER BY player_name";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            names.add(rs.getString("player_name"));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get known player names: " + e.getMessage());
            }

            return names;
        }, executor);
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentsByIP(String ipAddress) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = """
                        SELECT player_uuid, player_name, ip_address, type, reason, operator, created_at, expires_at, active
                        FROM punishments
                        WHERE ip_address = ? AND active = 1
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, ipAddress);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            punishments.add(resultSetToPunishment(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get punishments by IP: " + e.getMessage());
            }

            return punishments;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deletePunishment(UUID playerUUID, int punishmentIndex) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = """
                        UPDATE punishments
                        SET active = 0
                        WHERE player_uuid = ?
                        LIMIT 1
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to delete punishment: " + e.getMessage());
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> clearAll() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                String sql = "DELETE FROM punishments";
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to clear all data: " + e.getMessage());
            }
        }, executor);
    }

    private Punishment resultSetToPunishment(ResultSet rs) throws SQLException {
        return new Punishment(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getString("ip_address"),
                PunishmentType.valueOf(rs.getString("type")),
                rs.getString("reason"),
                rs.getString("operator"),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                rs.getBoolean("active")
        );
    }
}
