// com/ass1/server/db/QueueLogRepository.java
package com.ass1.server.db;

import javax.sql.DataSource;
import java.sql.*;


public final class QueueLogRepository {
    private final DataSource ds;

    public QueueLogRepository(DataSource ds) {
        this.ds = ds;
    }

    public void insert(String serverId, long unixMs, int queueLen) throws SQLException {
        String sql = "INSERT INTO server_queue_log(server_id, ts_unix_ms, queue_length) VALUES (?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, serverId);
            ps.setLong(2, unixMs);
            ps.setInt(3, queueLen);
            ps.executeUpdate();
        }
    }
}