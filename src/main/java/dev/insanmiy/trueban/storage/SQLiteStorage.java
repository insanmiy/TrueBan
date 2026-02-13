package dev.insanmiy.trueban.storage;

import dev.insanmiy.trueban.TrueBan;
import dev.insanmiy.trueban.models.Punishment;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SQLiteStorage implements StorageManager {
    private final TrueBan plugin;
    private final String filePath;
    private Connection connection;

    public SQLiteStorage(TrueBan plugin, String filePath) {
        this.plugin = plugin;
        this.filePath = filePath;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                File dbFile = new File(filePath);
                dbFile.getParentFile().mkdirs();
                String url = "jdbc:sqlite:" + filePath;
                connection = DriverManager.getConnection(url);

                createTables();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite storage", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to close SQLite connection", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO punishments (uuid, player_name, ip_address, type, reason, operator_uuid, operator_name, creation_timestamp, expiration_timestamp, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, punishment.getUuid().toString());
                stmt.setString(2, punishment.getPlayerName());
                stmt.setString(3, punishment.getIpAddress());
                stmt.setString(4, punishment.getType().name());
                stmt.setString(5, punishment.getReason());
                stmt.setString(6, punishment.getOperatorUuid());
                stmt.setString(7, punishment.getOperatorName());
                stmt.setLong(8, punishment.getCreationTimestamp());
                stmt.setLong(9, punishment.getExpirationTimestamp());
                stmt.setBoolean(10, punishment.isActiveFlag());
                stmt.executeUpdate();

                cachePlayer(punishment.getPlayerName(), punishment.getUuid());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save punishment", e);
            }
        });
    }

    @Override
    public CompletableFuture<Punishment> getPunishment(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE uuid = ? AND active = 1";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return mapRowToPunishment(rs);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get punishment", e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllPunishments() {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM punishments";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    punishments.add(mapRowToPunishment(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get all punishments", e);
            }
            return punishments;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE uuid = ? AND active = 1";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    punishments.add(mapRowToPunishment(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get active punishments", e);
            }
            return punishments;
        });
    }

    @Override
    public CompletableFuture<Void> removePunishment(UUID uuid, Punishment.PunishmentType type) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE punishments SET active = 0 WHERE uuid = ? AND type = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, type.name());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove punishment", e);
            }
        });
    }

    @Override
    public CompletableFuture<UUID> getOfflinePlayerUUID(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid FROM player_cache WHERE player_name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerName.toLowerCase());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get offline player UUID", e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<String> getPlayerName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player_name FROM player_cache WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("player_name");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player name", e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<List<String>> getAllKnownPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> players = new ArrayList<>();
            String sql = "SELECT player_name FROM player_cache";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    players.add(rs.getString("player_name"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get all known players", e);
            }
            return players;
        });
    }

    private void createTables() throws SQLException {
        String punishmentsTable = """
            CREATE TABLE IF NOT EXISTS punishments (
                uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                ip_address TEXT,
                type TEXT NOT NULL,
                reason TEXT NOT NULL,
                operator_uuid TEXT,
                operator_name TEXT,
                creation_timestamp INTEGER NOT NULL,
                expiration_timestamp INTEGER,
                active BOOLEAN NOT NULL DEFAULT 1
            )
        """;

        String playerCacheTable = """
            CREATE TABLE IF NOT EXISTS player_cache (
                player_name TEXT PRIMARY KEY,
                uuid TEXT NOT NULL
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(punishmentsTable);
            stmt.execute(playerCacheTable);
        }
    }

    private Punishment mapRowToPunishment(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setUuid(UUID.fromString(rs.getString("uuid")));
        punishment.setPlayerName(rs.getString("player_name"));
        punishment.setIpAddress(rs.getString("ip_address"));
        punishment.setType(Punishment.PunishmentType.valueOf(rs.getString("type")));
        punishment.setReason(rs.getString("reason"));
        punishment.setOperatorUuid(rs.getString("operator_uuid"));
        punishment.setOperatorName(rs.getString("operator_name"));
        punishment.setCreationTimestamp(rs.getLong("creation_timestamp"));
        punishment.setExpirationTimestamp(rs.getLong("expiration_timestamp"));
        punishment.setActive(rs.getBoolean("active"));
        return punishment;
    }

    private void cachePlayer(String playerName, UUID uuid) {
        String sql = "INSERT OR REPLACE INTO player_cache (player_name, uuid) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName.toLowerCase());
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to cache player", e);
        }
    }
}
