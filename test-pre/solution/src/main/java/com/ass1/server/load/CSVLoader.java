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

    // Assumes CSV columns: geoname_id,name,country_code,country_name,population,timezone,lat,lon
    public void load(File csv) throws Exception {
        List<String[]> rows;
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            rows = br.lines()
                     .skip(1) // header
                     .map(line -> line.split(",", -1))
                     .collect(Collectors.toList());
        }

        // Insert countries (dedup)
        Map<String,String> countries = new HashMap<>();
        for (String[] r : rows) {
            String code = r[2].trim();
            String name = r[3].trim();
            if (!code.isEmpty() && !name.isEmpty()) countries.put(code, name);
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

    private static long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; } }
    private static int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static double parseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0d; } }
}