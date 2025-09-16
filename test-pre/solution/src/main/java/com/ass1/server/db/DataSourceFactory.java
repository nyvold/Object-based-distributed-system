// DataSourceFactory.java
package com.ass1.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public final class DataSourceFactory {
    public static DataSource fromEnv() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/ass1"));
        cfg.setUsername(System.getenv().getOrDefault("DB_USER", "ass1"));
        cfg.setPassword(System.getenv().getOrDefault("DB_PASS", "ass1"));
        cfg.setMaximumPoolSize(8);
        return new HikariDataSource(cfg);
    }
}