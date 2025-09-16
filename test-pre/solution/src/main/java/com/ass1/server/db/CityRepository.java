// CityRepository.java
package com.ass1.server.db;

import javax.sql.DataSource;
import java.sql.*;

public final class CityRepository {
    private final DataSource ds;
    public CityRepository(DataSource ds) { this.ds = ds; }

    public long sumPopulationByCountryName(String countryName) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(ci.population),0) AS pop
            FROM city ci
            JOIN country c ON c.code = ci.country_code
            WHERE LOWER(c.name) = LOWER(?)
        """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, countryName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("pop");
            }
        }
    }

    public long countCitiesByCountryThreshold(String countryName, long threshold, String comp) throws SQLException {
        String op = switch (comp.toLowerCase()) {
            case "min" -> ">=";
            case "max" -> "<=";
            default -> throw new IllegalArgumentException("comp must be 'min' or 'max'");
        };
        String sql = """
            SELECT COUNT(*) AS cnt
            FROM city ci
            JOIN country c ON c.code = ci.country_code
            WHERE LOWER(c.name) = LOWER(?) AND ci.population %s ?
        """.formatted(op);
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, countryName);
            ps.setLong(2, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("cnt");
            }
        }
    }

    public long countCountriesWithCitycountAndThreshold(long citycount, long threshold, String comp) throws SQLException {
        String op = switch (comp.toLowerCase()) {
            case "min" -> ">=";
            case "max" -> "<=";
            default -> throw new IllegalArgumentException("comp must be 'min' or 'max'");
        };
        String sql = """
          SELECT COUNT(*) AS countries
          FROM (
            SELECT c.code
            FROM city ci
            JOIN country c ON c.code = ci.country_code
            WHERE ci.population %s ?
            GROUP BY c.code
            HAVING COUNT(*) >= ?
          ) t
        """.formatted(op);
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, threshold);
            ps.setLong(2, citycount);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("countries");
            }
        }
    }

    public long countCountriesWithCitycountBetween(long citycount, long minPop, long maxPop) throws SQLException {
        String sql = """
          SELECT COUNT(*) AS countries
          FROM (
            SELECT c.code
            FROM city ci
            JOIN country c ON c.code = ci.country_code
            WHERE ci.population BETWEEN ? AND ?
            GROUP BY c.code
            HAVING COUNT(*) >= ?
          ) t
        """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, minPop);
            ps.setLong(2, maxPop);
            ps.setLong(3, citycount);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("countries");
            }
        }
    }
}