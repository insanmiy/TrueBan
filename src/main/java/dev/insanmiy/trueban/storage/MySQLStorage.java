package dev.insanmiy.trueban.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.punishment.Punishment;
import dev.insanmiy.trueban.punishment.PunishmentType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MysqlStorage implements StorageManager {

    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;
    private final ScheduledExecutorService executor;
    private volatile boolean ready;

    public MysqlStorage(JavaPlugin plugin) throws Exception {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TrueBan-MysqlStorage");
            t.setDaemon(true);
            return t;
        });

        this.dataSource = setupConnectionPool();
        initializeDatabase();
        this.ready = true;
    }

    private HikariDataSource setupConnectionPool() {
        TrueBan trueBan = (TrueBan) plugin;
        Map<String, Object> mysqlConfig = (Map<String, Object>) trueBan.getConfigManager().getConfigData().get("mysql");

        String host = (String) mysqlConfig.getOrDefault("host", "localhost");
        int port = toInt(mysqlConfig.getOrDefault("port", 3306));
        String database = (String) mysqlConfig.getOrDefault("database", "trueban");
        String username = (String) mysqlConfig.getOrDefault("username", "root");
        String password = (String) mysqlConfig.getOrDefault("password", "password");

        Map<String, Object> poolConfig = (Map<String, Object>) mysqlConfig.getOrDefault("pool", new HashMap<>());
        int maxSize = toInt(poolConfig.getOrDefault("max-size", 10));
        int minIdle = toInt(poolConfig.getOrDefault("min-idle", 2));
        long connTimeout = toLong(poolConfig.getOrDefault("connection-timeout-seconds", 30)) * 1000;
        long idleTimeout = toLong(poolConfig.getOrDefault("idle-timeout-seconds", 600)) * 1000;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setAutoCommit(true);
        config.setPoolName("TrueBan-Pool");

        return new HikariDataSource(config);
    }

    private void initializeDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            createTables(conn);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String createPunishmentsTable = """
                CREATE TABLE IF NOT EXISTS punishments (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    ip_address VARCHAR(15),
                    type VARCHAR(20) NOT NULL,
                    reason TEXT NOT NULL,
                    operator VARCHAR(16) NOT NULL,
                    created_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT true,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_punishment (player_uuid, created_at),
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_ip_address (ip_address),
                    INDEX idx_active (active)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createPunishmentsTable);
        }
    }

    @Override
    public boolean isReady() {
        return ready && !dataSource.isClosed();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdown();
    }

    @Override
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
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
            try (Connection conn = dataSource.getConnection()) {
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

            try (Connection conn = dataSource.getConnection()) {
                String sql = """
                        SELECT player_uuid, player_name, ip_address, type, reason, operator, created_at, expires_at, active
                        FROM punishments
                        WHERE player_uuid = ? AND active = true
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

            try (Connection conn = dataSource.getConnection()) {
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
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT DISTINCT player_uuid FROM punishments WHERE player_name = ? LIMIT 1";

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

            try (Connection conn = dataSource.getConnection()) {
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

            try (Connection conn = dataSource.getConnection()) {
                String sql = """
                        SELECT player_uuid, player_name, ip_address, type, reason, operator, created_at, expires_at, active
                        FROM punishments
                        WHERE ip_address = ? AND active = true
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
            try (Connection conn = dataSource.getConnection()) {
                String sql = """
                        UPDATE punishments
                        SET active = false
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
            try (Connection conn = dataSource.getConnection()) {
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

    private int toInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return Integer.parseInt(obj.toString());
    }

    private long toLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return Long.parseLong(obj.toString());
    }
}
