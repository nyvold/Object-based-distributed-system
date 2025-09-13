// CountryRepository.java
package com.ass1.server.db;

import com.ass1.server.dto.CountryDTO;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public final class CountryRepository {
    private final DataSource ds;
    public CountryRepository(DataSource ds) { this.ds = ds; }

    public Optional<CountryDTO> findByName(String countryName) throws SQLException {
        String sql = "SELECT code, name FROM country WHERE LOWER(name) = LOWER(?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, countryName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CountryDTO(rs.getString("code"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<CountryDTO> findByCode(String code) throws SQLException {
        String sql = "SELECT code, name FROM country WHERE UPPER(code) = UPPER(?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CountryDTO(rs.getString("code"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }
}