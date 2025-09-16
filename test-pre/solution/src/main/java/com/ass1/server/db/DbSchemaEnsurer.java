// DbSchemaEnsurer.java
package com.ass1.server.db;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DbSchemaEnsurer {
    private DbSchemaEnsurer() {}

    public static void ensure(DataSource ds) {
        List<String> statements = loadStatementsFromResource("/db/schema.sql");
        if (statements.isEmpty()) return;

        try (Connection c = ds.getConnection()) {
            // If core tables already exist (from container init), skip.
            if (tableExists(c, "country") && tableExists(c, "city")) {
                return;
            }

            // Execute statements one-by-one with autocommit to avoid aborting the batch
            c.setAutoCommit(true);
            try (Statement st = c.createStatement()) {
                for (String sql : statements) {
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        String msg = e.getMessage();
                        if (msg != null && (
                                msg.contains("already exists") ||
                                msg.contains("duplicate key value")
                        )) {
                            // idempotent object creation failure; ignore and continue
                            continue;
                        }
                        throw e;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure DB schema", e);
        }
    }

    private static boolean tableExists(Connection c, String table) {
        String sql = "SELECT to_regclass(?) IS NOT NULL AS exists";
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, "public." + table);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBoolean(1);
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static List<String> loadStatementsFromResource(String resourcePath) {
        List<String> stmts = new ArrayList<>();
        try (InputStream in = DbSchemaEnsurer.class.getResourceAsStream(resourcePath)) {
            if (in == null) return stmts;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    if (trimmed.startsWith("--")) continue; // skip comments
                    sb.append(line).append('\n');
                }
            }
            // naive split on semicolons; schema file is simple enough
            for (String stmt : sb.toString().split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) stmts.add(s + ";");
            }
        } catch (Exception e) {
            // ignore
        }
        return stmts;
    }
}
