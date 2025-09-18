package com.ass1.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ass1.server.ProxyInterface;
import com.ass1.server.ServerConnection;
import com.ass1.server.ServerInterface;

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
                logger.info("Cleared output: " + outputPath);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to clear output: " + outputPath, e);
        }

        Client client = new Client();
        client.parseInputFile();
        logger.info("Parsed " + client.queries.size() + " queries from input file.");
        client.sendQueries(10);
        logger.info("Finished sending all queries.");
    }

    public void sendQueries(int delay) {
        String outputPath = System.getenv().getOrDefault("OUTPUT_PATH", "output.txt");
        int processed = 0, successful = 0, failed = 0;
        try (FileWriter fw = new FileWriter(outputPath, true)) { // Append mode
            for (Query query : queries) {
                try {
                    processed++;
                    logger.info("Processing query: " + query);
                    String proxyHost = System.getenv().getOrDefault("PROXY_HOST", "proxy");
                    Registry registry = LocateRegistry.getRegistry(proxyHost, 1099);
                    ProxyInterface proxy = (ProxyInterface) registry.lookup("Proxy");
                    logger.info("Connected to proxy.");

                    ServerConnection serverConn = proxy.connectToServer(query.zone);
                    logger.info("Proxy assigned server: " + serverConn.getBindingName() + " at " + serverConn.getServerAddress() + ":" + serverConn.getServerPort());

                    registry = LocateRegistry.getRegistry(serverConn.getServerAddress(), serverConn.getServerPort());
                    ServerInterface server = (ServerInterface) registry.lookup(serverConn.getBindingName());
                    logger.info("Connected to server: " + serverConn.getBindingName());

                    String result;
                    if(query.methodName.equals("getPopulationofCountry") && query.args.size() == 1) {
                        String countryName = query.args.get(0);
                        result = server.getPopulationofCountry(countryName) + " ";

                    } else if (query.methodName.equals("getNumberofCities") && query.args.size() == 3) {
                        String countryName = query.args.get(0);
                        int threshold = Integer.parseInt(query.args.get(1));
                        int comparison = Integer.parseInt(query.args.get(2));
                        result = server.getNumberofCities(countryName, threshold, comparison) + " ";

                    } else if (query.methodName.equals("getNumberofCountries") && query.args.size() == 3) {
                        int cityCount = Integer.parseInt(query.args.get(0));
                        int threshold = Integer.parseInt(query.args.get(1));
                        int comparison = Integer.parseInt(query.args.get(2));
                        result = server.getNumberofCountries(cityCount, threshold, comparison) + " ";

                    } else if (query.methodName.equals("getNumberofCountriesMM") && query.args.size() == 3) {
                        int cityCount = Integer.parseInt(query.args.get(0));
                        int minPopulation = Integer.parseInt(query.args.get(1));
                        int maxPopulation = Integer.parseInt(query.args.get(2));
                        result = server.getNumberofCountriesMM(cityCount, minPopulation, maxPopulation) + " ";
                    } else {
                        result = "Invalid query: " + query.toString();
                        logger.warning(result);
                    }
                    fw.write(result + query.toString() + System.lineSeparator());
                    fw.flush();
                    successful++;
                    logger.info("Wrote result to: " + outputPath);
                    Thread.sleep(delay);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Exception while processing query: " + query, e);
                    try {
                        fw.write("ERROR " + query.toString() + " " + e.getClass().getSimpleName() + System.lineSeparator());
                        fw.flush();
                        failed++;
                    } catch (IOException ioe) {
                        logger.log(Level.SEVERE, "Also failed to write error to output", ioe);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to output: " + outputPath, e);
        }
        logger.info("Client summary: processed=" + processed + ", successful=" + successful + ", failed=" + failed);
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
