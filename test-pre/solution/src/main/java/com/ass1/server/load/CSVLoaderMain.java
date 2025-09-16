// com/ass1/server/load/CsvLoaderMain.java
package com.ass1.server.load;

import com.ass1.server.db.DataSourceFactory;

import javax.sql.DataSource;
import java.io.File;

/**
 * Simple CLI runner to import the cities dataset into the DB.
 *
 * Usage:
 *   java -cp target/classes com.ass1.server.load.CsvLoaderMain /path/to/cities.csv
 *
 * DB connection details come from environment variables:
 *   DB_URL, DB_USER, DB_PASS
 */
public final class CSVLoaderMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: CsvLoaderMain <path-to-cities.csv>");
            System.exit(1);
        }

        String csvPath = args[0];
        DataSource ds = DataSourceFactory.fromEnv();

        CSVLoader loader = new CSVLoader(ds);
        loader.load(new File(csvPath));

        System.out.println("✅ Import finished for " + csvPath);
    }
}