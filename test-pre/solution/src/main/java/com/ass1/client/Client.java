package com.ass1.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ass1.server.ProxyInterface;
import com.ass1.server.ServerConnection;
import com.ass1.server.ServerInterface;
import com.ass1.server.Result;

public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());
    public List<Query> queries = new ArrayList<>();

    // Query class to store each parsed query
    public static class Query { 
        public String methodName;
        public List<String> args;
        public int zone;

        public Query(String methodName, List<String> args, int zone) {
            this.methodName = methodName;
            this.args = args;
            this.zone = zone;
        }

        @Override
        public String toString() {
            return methodName + " " + String.join(" ", args) + " Zone:" + zone;
        }
    }

    public static void main(String[] args) {
        logger.info("Starting client...");
        String outputPath = System.getenv().getOrDefault("OUTPUT_PATH", "output.txt");
        try {
            File outFile = new File(outputPath);
            File parent = outFile.getParentFile();
            if (parent != null) parent.mkdirs();
            try (FileWriter fw = new FileWriter(outFile, false)) {
                // Header describing the output categories
                fw.write("# Columns: result|ERROR method args Zone:N ServerZone:Z WaitMs ExecMs TurnMs TotalMs [errorType]\n");
                logger.info("Cleared output and wrote header: " + outputPath);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to clear output: " + outputPath, e);
        }

        Client client = new Client();
        client.parseInputFile();
        logger.info("Parsed " + client.queries.size() + " queries from input file.");
        client.sendQueries();
        logger.info("Finished sending all queries.");
    }

    private static final Object OUTPUT_LOCK = new Object();

    public void sendQueries() {
        String outputPath = System.getenv().getOrDefault("OUTPUT_PATH", "output.txt");
        int minT = parseIntEnv("CLIENT_T_MIN_MS", 20);
        int maxT = parseIntEnv("CLIENT_T_MAX_MS", 50);
        if (minT > maxT) { int tmp = minT; minT = maxT; maxT = tmp; }

        ExecutorService pool = Executors.newCachedThreadPool();
        List<Future<?>> futures = new ArrayList<>();
        Random rnd = new Random();

        final int minDelay = minT;
        final int maxDelay = maxT;

        Thread scheduler = new Thread(() -> {
            for (Query q : queries) {
                futures.add(pool.submit(() -> processOne(q, outputPath)));
                int T = minDelay + (maxDelay > minDelay ? rnd.nextInt(maxDelay - minDelay + 1) : 0);
                try { Thread.sleep(T); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        });
        scheduler.setName("client-scheduler");
        scheduler.start();

        try {
            scheduler.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Void processOne(Query query, String outputPath) {
        long t0 = System.nanoTime();
        int serverZone = -1;
        long callStart = -1L;
        try {
            logger.info("Processing query: " + query);
            String proxyHost = System.getenv().getOrDefault("PROXY_HOST", "proxy");
            Registry registry = LocateRegistry.getRegistry(proxyHost, 1099);
            ProxyInterface proxy = (ProxyInterface) registry.lookup("Proxy");
            logger.info("Connected to proxy.");

            ServerConnection serverConn = proxy.connectToServer(query.zone);
            logger.info("Proxy assigned server: " + serverConn.getBindingName() + " at " + serverConn.getServerAddress() + ":" + serverConn.getServerPort());
            serverZone = serverConn.getZone();

            registry = LocateRegistry.getRegistry(serverConn.getServerAddress(), serverConn.getServerPort());
            ServerInterface server = (ServerInterface) registry.lookup(serverConn.getBindingName());
            logger.info("Connected to server: " + serverConn.getBindingName());

            // Simulate extra network latency for cross-zone communication (base 80ms added at server)
            int zoneDiff = Math.abs(serverConn.getZone() - query.zone);
            int extraMs = 30 * zoneDiff;
            if (extraMs > 0) { try { Thread.sleep(extraMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } }

            callStart = System.nanoTime();
            Result r;
            if(query.methodName.equals("getPopulationofCountry") && query.args.size() == 1) {
                String countryName = query.args.get(0);
                r = server.getPopulationofCountry(countryName);
            } else if (query.methodName.equals("getNumberofCities") && query.args.size() == 3) {
                String countryName = query.args.get(0);
                int threshold = Integer.parseInt(query.args.get(1));
                int comparison = Integer.parseInt(query.args.get(2));
                r = server.getNumberofCities(countryName, threshold, comparison);
            } else if (query.methodName.equals("getNumberofCountries") && query.args.size() == 3) {
                int cityCount = Integer.parseInt(query.args.get(0));
                int threshold = Integer.parseInt(query.args.get(1));
                int comparison = Integer.parseInt(query.args.get(2));
                r = server.getNumberofCountries(cityCount, threshold, comparison);
            } else if (query.methodName.equals("getNumberofCountriesMM") && query.args.size() == 3) {
                int cityCount = Integer.parseInt(query.args.get(0));
                int minPopulation = Integer.parseInt(query.args.get(1));
                int maxPopulation = Integer.parseInt(query.args.get(2));
                r = server.getNumberofCountriesMM(cityCount, minPopulation, maxPopulation);
            } else {
                String invalid = "Invalid query: " + query.toString();
                logger.warning(invalid);
                long t1 = System.nanoTime();
                long totalMs = (t1 - t0) / 1_000_000L;
                String line = invalid + " ServerZone:" + serverZone + " TotalMs:" + totalMs + System.lineSeparator();
                synchronized (OUTPUT_LOCK) { try (FileWriter fw = new FileWriter(outputPath, true)) { fw.write(line); } }
                logger.info("Wrote result to: " + outputPath);
                return null;
            }
            long turnaroundMs = (System.nanoTime() - callStart) / 1_000_000L;
            long t1 = System.nanoTime();
            long totalMs = (t1 - t0) / 1_000_000L;
            String line = (r.getValue() + " ") + query.toString() + " ServerZone:" + serverZone +
                    " WaitMs:" + r.getWaitMs() + " ExecMs:" + r.getExecMs() +
                    " TurnMs:" + turnaroundMs + " TotalMs:" + totalMs + System.lineSeparator();
            synchronized (OUTPUT_LOCK) { try (FileWriter fw = new FileWriter(outputPath, true)) { fw.write(line); } }
            logger.info("Wrote result to: " + outputPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while processing query: " + query, e);
            try {
                long t1 = System.nanoTime();
                long totalMs = (t1 - t0) / 1_000_000L;
                Long turnMs = callStart > 0 ? (t1 - callStart) / 1_000_000L : null;
                String line = "ERROR " + query.toString() + " " + e.getClass().getSimpleName() +
                        " ServerZone:" + (serverZone > 0 ? serverZone : "?") +
                        " WaitMs:? ExecMs:?" + (turnMs != null ? (" TurnMs:" + turnMs) : "") +
                        " TotalMs:" + totalMs + System.lineSeparator();
                synchronized (OUTPUT_LOCK) { try (FileWriter fw = new FileWriter(outputPath, true)) { fw.write(line); } }
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Also failed to write error to output", ioe);
            }
        }
        return null;
    }

    private static int parseIntEnv(String name, int def) {
        String v = System.getenv(name);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // Parses the input file and populates the queries list
    private void parseInputFile() {
        try {
            String inputPath = System.getenv().getOrDefault("INPUT_PATH", "exercise_1_input.txt");
            File file = new File(inputPath);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                int zoneIdx = line.lastIndexOf("Zone:");
                if (zoneIdx == -1) continue;

                String beforeZone = line.substring(0, zoneIdx).trim();
                String zoneStr = line.substring(zoneIdx + 5).trim();
                int zone;
                try {
                    zone = Integer.parseInt(zoneStr);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid zone in line: " + line);
                    continue;
                }

                Scanner lineScanner = new Scanner(beforeZone);
                if (!lineScanner.hasNext()) {
                    lineScanner.close();
                    continue;
                }
                String methodName = lineScanner.next();
                String rest = lineScanner.hasNext() ? lineScanner.nextLine().trim() : "";
                lineScanner.close();

                List<String> args = new ArrayList<>();
                switch (methodName) {
                    case "getPopulationofCountry" -> {
                        if (!rest.isEmpty()) {
                            args.add(rest);
                        }
                    }

                    case "getNumberofCities", "getNumberofCountries" -> {
                        // Accepts: [countryName|cityCount] [threshold] [optional operator: >, <, =]
                        String[] parts = rest.split("\\s+");
                        if (parts.length < 2) {
                            logger.warning("Invalid argument count for " + methodName + ": " + line);
                            continue;
                        }
                        args.add(parts[0]);
                        args.add(parts[1]);
                        int compInt = 1; // default 'min' (>= threshold)
                        if (parts.length >= 3) {
                            String compStr = parts[2].trim().toLowerCase();
                            if (compStr.equals(">") || compStr.equals("=") || compStr.equals("min")) {
                                compInt = 1; // min => population >= threshold
                            } else if (compStr.equals("<") || compStr.equals("max")) {
                                compInt = 2; // max => population <= threshold
                            } else {
                                logger.warning("Invalid comparison operator for " + methodName + ": " + line);
                                continue;
                            }
                        }
                        args.add(String.valueOf(compInt));
                    }
                    case "getNumberofCountriesMM" -> {
                        // expects: cityCount minPopulation maxPopulation
                        String[] mmParts = rest.split("\\s+");
                        if (mmParts.length >= 3) {
                            args.add(mmParts[0]);
                            args.add(mmParts[1]);
                            args.add(mmParts[2]);
                        } else {
                            logger.warning("Invalid argument count for " + methodName + ": " + line);
                            continue;
                        }
                    }

                    default -> {
                        logger.warning("Unknown method: " + methodName);
                        continue;
                    }
                }

                queries.add(new Query(methodName, args, zone));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Input file not found: " + System.getenv().getOrDefault("INPUT_PATH", "exercise_1_input.txt"), e);
        }
    }
}
