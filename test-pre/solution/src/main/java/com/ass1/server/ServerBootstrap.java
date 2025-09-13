// ServerBootstrap.java
package com.ass1.server;

import com.ass1.server.db.*;
import com.ass1.server.core.StatsService;

import javax.sql.DataSource;

public final class ServerBootstrap {
    public static StatsService initStatsService() {
        DataSource ds = DataSourceFactory.fromEnv();
        CityRepository cityRepo = new CityRepository(ds);
        return new StatsService(cityRepo);
    }
}