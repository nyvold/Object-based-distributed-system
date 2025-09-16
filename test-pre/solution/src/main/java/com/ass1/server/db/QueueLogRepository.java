// com/ass1/server/db/QueueLogRepository.java
package com.ass1.server.db;

import javax.sql.DataSource;
import java.sql.*;

/**
 * Repository for writing queue length samples into the DB.
 * Each server uses this to log queue size over time,
 * so you can later generate the "queue vs time" graphs.
 */
public final class QueueLogRepository {
    private final DataSource ds;

    public QueueLogRepository(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Insert one queue sample.
     *
     * @param serverId  identifier for this server (e.g., "server-1" or "zone-3")
     * @param unixMs    timestamp in milliseconds since epoch
     * @param queueLen  current queue length at that moment
     */
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