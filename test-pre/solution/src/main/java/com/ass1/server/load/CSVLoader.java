// CsvLoader.java
package com.ass1.server.load;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public final class CSVLoader {
    private final DataSource ds;

    public CSVLoader(DataSource ds) { this.ds = ds; }

    // Supports two formats:
    // 1) Comma-separated: geoname_id,name,country_code,country_name,population,timezone,lat,lon
    // 2) Semicolon-separated (assignment dataset): Geoname ID;Name;Country Code;Country name EN;Population;Timezone;Coordinates(lat,lon)
    public void load(File csv) throws Exception {
        List<String[]> rows;
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            rows = br.lines()
                     .skip(1) // header
                     .map(CSVLoader::parseLine)
                     .filter(arr -> arr != null && arr.length == 8)
                     .collect(Collectors.toList());
        }

        // Ensure the app user can read tables (when running as superuser)
        ensureReadPrivileges();

        // Insert countries (dedup)
        Map<String,String> countries = new HashMap<>();
        for (String[] r : rows) {
            String code = r[2].trim();
            String name = r[3].trim();
            if (code.isEmpty()) continue;
            if (name.isEmpty()) name = code; // fallback for entries like XK with missing name
            countries.put(code, name);
        }

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement up = c.prepareStatement(
                    "INSERT INTO country(code,name) VALUES(?,?) ON CONFLICT (code) DO NOTHING")) {
                for (var e : countries.entrySet()) {
                    up.setString(1, e.getKey());
                    up.setString(2, e.getValue());
                    up.addBatch();
                }
                up.executeBatch();
            }

            try (PreparedStatement up = c.prepareStatement(
                    "INSERT INTO city(geoname_id,name,country_code,population,timezone,latitude,longitude) " +
                    "VALUES(?,?,?,?,?,?,?) ON CONFLICT (geoname_id) DO NOTHING")) {
                for (String[] r : rows) {
                    long geonameId = parseLong(r[0]);
                    if (geonameId == 0) continue;
                    up.setLong(1, geonameId);
                    up.setString(2, r[1]);
                    up.setString(3, r[2]);
                    up.setInt(4, parseInt(r[4]));
                    up.setString(5, r[5]);
                    up.setDouble(6, parseDouble(r[6]));
                    up.setDouble(7, parseDouble(r[7]));
                    up.addBatch();
                }
                up.executeBatch();
            }
            c.commit();
        }
    }

    private void ensureReadPrivileges() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            // These will succeed if connected as a superuser/owner, and no-op otherwise
            st.execute("GRANT SELECT ON ALL TABLES IN SCHEMA public TO ass1");
            st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ass1");
            st.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ass1");
        } catch (SQLException ignore) {
            // If we don't have privilege to grant, ignore silently
        }
    }

    private static String[] parseLine(String line) {
        if (line == null || line.isEmpty()) return null;
        // Prefer semicolon if present (assignment dataset)
        if (line.indexOf(';') >= 0) {
            String[] parts = line.split(";", -1);
            if (parts.length < 7) return null;
            String coords = parts[6];
            String lat = "";
            String lon = "";
            if (coords != null && !coords.isEmpty()) {
                String[] ll = coords.split(",", -1);
                if (ll.length >= 2) { lat = ll[0].trim(); lon = ll[1].trim(); }
            }
            return new String[] {
                parts[0].trim(),            // geoname_id
                parts[1].trim(),            // name
                parts[2].trim(),            // country_code
                parts[3].trim(),            // country_name
                parts[4].trim(),            // population
                parts[5].trim(),            // timezone
                lat,
                lon
            };
        } else {
            // Assume comma-separated with expected 8 columns
            String[] parts = line.split(",", -1);
            if (parts.length < 8) return null;
            return parts;
        }
    }

    private static long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; } }
    private static int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static double parseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0d; } }
}
