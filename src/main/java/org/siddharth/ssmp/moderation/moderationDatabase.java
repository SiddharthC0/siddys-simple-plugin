package org.siddharth.ssmp.moderation;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

final class moderationDatabase implements AutoCloseable {
    record Term(String value, String category) {}
    record ViolationState(int count, long lastViolation) {}
    record Mute(long expiresAt) {}
    record History(String action, String reason, String moderator, long createdAt) {}
    record Snapshot(Map<String, String> terms, Map<UUID, ViolationState> violations, Map<UUID, Mute> mutes, Map<UUID, Mute> bans) {}

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SSMP-Moderation-Database");
        thread.setDaemon(true);
        return thread;
    });
    private Connection connection;

    moderationDatabase(File file) throws SQLException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Could not create moderation data directory.");
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("SQLite JDBC is missing. Include sqlite-jdbc in the shaded plugin JAR.", exception);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS blocked_terms (term TEXT PRIMARY KEY, category TEXT NOT NULL, created_by TEXT NOT NULL, created_at INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS violations (player_uuid TEXT PRIMARY KEY, count INTEGER NOT NULL, last_violation INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS punishments (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, player_name TEXT NOT NULL, moderator TEXT NOT NULL, type TEXT NOT NULL, reason TEXT NOT NULL, created_at INTEGER NOT NULL, expires_at INTEGER NOT NULL, active INTEGER NOT NULL DEFAULT 1)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_active ON punishments(player_uuid, type, active)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS moderation_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, player_name TEXT NOT NULL, moderator TEXT NOT NULL, action TEXT NOT NULL, reason TEXT NOT NULL, created_at INTEGER NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_moderation_logs_player ON moderation_logs(player_uuid, created_at DESC)");
        }
    }

    Snapshot loadSnapshot() throws SQLException {
        Map<String, String> terms = new LinkedHashMap<>();
        Map<UUID, ViolationState> violations = new HashMap<>();
        Map<UUID, Mute> mutes = new HashMap<>();
        Map<UUID, Mute> bans = new HashMap<>();
        long now = System.currentTimeMillis();

        try (PreparedStatement ps = connection.prepareStatement("SELECT term, category FROM blocked_terms"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) terms.put(rs.getString(1), rs.getString(2));
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT player_uuid, count, last_violation FROM violations"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) violations.put(UUID.fromString(rs.getString(1)), new ViolationState(rs.getInt(2), rs.getLong(3)));
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT player_uuid, expires_at FROM punishments WHERE type='MUTE' AND active=1"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString(1));
                long expires = rs.getLong(2);
                if (expires == 0 || expires > now) mutes.put(uuid, new Mute(expires));
                else deactivateExpiredMute(uuid, now);
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT player_uuid, expires_at FROM punishments WHERE type='BAN' AND active=1"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString(1));
                long expires = rs.getLong(2);
                if (expires == 0 || expires > now) bans.put(uuid, new Mute(expires));
                else deactivateExpiredPunishment(uuid, "BAN", now);
            }
        }
        return new Snapshot(terms, violations, mutes, bans);
    }

    private void deactivateExpiredMute(UUID uuid, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active=0 WHERE player_uuid=? AND type='MUTE' AND active=1 AND expires_at>0 AND expires_at<=?")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, now);
            ps.executeUpdate();
        }
    }

    CompletableFuture<Void> saveViolation(UUID uuid, int count, long timestamp) {
        return submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO violations(player_uuid,count,last_violation) VALUES(?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET count=excluded.count,last_violation=excluded.last_violation")) {
                ps.setString(1, uuid.toString()); ps.setInt(2, count); ps.setLong(3, timestamp); ps.executeUpdate();
            }
            return null;
        });
    }

    CompletableFuture<Void> resetViolations(UUID uuid) {
        return submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM violations WHERE player_uuid=?")) {
                ps.setString(1, uuid.toString()); ps.executeUpdate();
            }
            return null;
        });
    }

    CompletableFuture<Void> addTerm(String term, String category, String actor) {
        return submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO blocked_terms(term,category,created_by,created_at) VALUES(?,?,?,?) ON CONFLICT(term) DO UPDATE SET category=excluded.category,created_by=excluded.created_by,created_at=excluded.created_at")) {
                ps.setString(1, term); ps.setString(2, category); ps.setString(3, actor); ps.setLong(4, System.currentTimeMillis()); ps.executeUpdate();
            }
            return null;
        });
    }

    CompletableFuture<Boolean> removeTerm(String term) {
        return submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM blocked_terms WHERE term=?")) {
                ps.setString(1, term); return ps.executeUpdate() > 0;
            }
        });
    }

    CompletableFuture<Void> saveMute(UUID uuid, String name, String moderator, String reason, long expiresAt) {
        return submit(() -> {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active=0 WHERE player_uuid=? AND type='MUTE' AND active=1")) {
                    ps.setString(1, uuid.toString()); ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO punishments(player_uuid,player_name,moderator,type,reason,created_at,expires_at,active) VALUES(?,?,?,'MUTE',?,?,?,1)")) {
                    ps.setString(1, uuid.toString()); ps.setString(2, name); ps.setString(3, moderator); ps.setString(4, reason); ps.setLong(5, System.currentTimeMillis()); ps.setLong(6, expiresAt); ps.executeUpdate();
                }
                insertLog(uuid, name, moderator, "MUTE", reason);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback(); throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    private void deactivateExpiredPunishment(UUID uuid, String type, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active=0 WHERE player_uuid=? AND type=? AND active=1 AND expires_at>0 AND expires_at<=?")) {
            ps.setString(1, uuid.toString()); ps.setString(2, type); ps.setLong(3, now); ps.executeUpdate();
        }
    }

    CompletableFuture<Void> saveBan(UUID uuid, String name, String moderator, String reason, long expiresAt) {
        return submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active=0 WHERE player_uuid=? AND type='BAN' AND active=1")) {
                ps.setString(1, uuid.toString()); ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO punishments(player_uuid,player_name,moderator,type,reason,created_at,expires_at,active) VALUES(?,?,?,'BAN',?,?,?,1)")) {
                ps.setString(1, uuid.toString()); ps.setString(2, name); ps.setString(3, moderator); ps.setString(4, reason); ps.setLong(5, System.currentTimeMillis()); ps.setLong(6, expiresAt); ps.executeUpdate();
            }
            insertLog(uuid, name, moderator, "BAN", reason);
            return null;
        });
    }

    CompletableFuture<Void> unban(UUID uuid, String name, String moderator) {
        return submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active=0 WHERE player_uuid=? AND type='BAN' AND active=1")) {
                ps.setString(1, uuid.toString()); ps.executeUpdate();
            }
            insertLog(uuid, name, moderator, "UNBAN", "Ban removed");
            return null;
        });
    }

    CompletableFuture<Void> unmute(UUID uuid, String name, String moderator) {
        return submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active=0 WHERE player_uuid=? AND type='MUTE' AND active=1")) {
                ps.setString(1, uuid.toString()); ps.executeUpdate();
            }
            insertLog(uuid, name, moderator, "UNMUTE", "Mute removed");
            return null;
        });
    }

    CompletableFuture<Void> log(UUID uuid, String name, String moderator, String action, String reason) {
        return submit(() -> { insertLog(uuid, name, moderator, action, reason); return null; });
    }

    private void insertLog(UUID uuid, String name, String moderator, String action, String reason) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO moderation_logs(player_uuid,player_name,moderator,action,reason,created_at) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, uuid.toString()); ps.setString(2, name); ps.setString(3, moderator); ps.setString(4, action); ps.setString(5, reason); ps.setLong(6, System.currentTimeMillis()); ps.executeUpdate();
        }
    }

    CompletableFuture<List<History>> history(UUID uuid, int limit) {
        return submit(() -> {
            List<History> entries = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT action,reason,moderator,created_at FROM moderation_logs WHERE player_uuid=? ORDER BY created_at DESC LIMIT ?")) {
                ps.setString(1, uuid.toString()); ps.setInt(2, Math.max(1, Math.min(50, limit)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) entries.add(new History(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)));
                }
            }
            return entries;
        });
    }

    private <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try { return task.call(); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(2, TimeUnit.SECONDS);
            }
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception exception) {
            executor.shutdownNow();
        }
    }
}
