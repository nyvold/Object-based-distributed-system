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
        String outputPath = System.getenv().getOrDefault("OUTPUT_PATH", "naive_server.txt");
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
        String outputPath = System.getenv().getOrDefault("OUTPUT_PATH", "naive_server.txt");
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
            File file = new File("exercise_1_input.txt");
            Scanner scanner = new Scanner(file);
            int lineNumber = 0;
            
            while (scanner.hasNextLine()) {
                String originalLine = scanner.nextLine();
                lineNumber++;
                String line = originalLine.trim();
                
                if (line.isEmpty()) continue;
                
                System.out.println("DEBUG Line " + lineNumber + ": '" + originalLine + "'");
                
                // Handle the case where Zone: is concatenated with the next method call
                String processedLine = line.replaceAll("Zone:(\\d+)([a-zA-Z])", "Zone:$1 $2");
                if (!processedLine.equals(line)) {
                    System.out.println("DEBUG Line " + lineNumber + " after regex: '" + processedLine + "'");
                }
                line = processedLine;
                
                int zoneIdx = line.lastIndexOf("Zone:");
                if (zoneIdx == -1) {
                    System.out.println("DEBUG Line " + lineNumber + ": No Zone: found, skipping");
                    continue;
                }
                
                String beforeZone = line.substring(0, zoneIdx).trim();
                String zoneStr = line.substring(zoneIdx + 5).trim();
                
                System.out.println("DEBUG Line " + lineNumber + ": beforeZone='" + beforeZone + "', zoneStr='" + zoneStr + "'");
                
                int zone;
                try {
                    zone = Integer.parseInt(zoneStr);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid zone in line " + lineNumber + ": " + line);
                    continue;
                }
                
                // Parse the method call part
                String[] tokens = beforeZone.split("\\s+");
                if (tokens.length == 0) continue;
                
                String methodName = tokens[0];
                System.out.println("DEBUG Line " + lineNumber + ": methodName='" + methodName + "'");
                System.out.println("DEBUG Line " + lineNumber + ": tokens=" + java.util.Arrays.toString(tokens));
                
                List<String> args = new ArrayList<>();
                
                switch (methodName) {
                    case "getPopulationofCountry": {
                        if (tokens.length >= 2) {
                            StringBuilder countryName = new StringBuilder();
                            for (int i = 1; i < tokens.length; i++) {
                                if (i > 1) countryName.append(" ");
                                countryName.append(tokens[i]);
                            }
                            args.add(countryName.toString());
                            System.out.println("DEBUG Line " + lineNumber + ": getPopulationofCountry args=" + args);
                        } else {
                            logger.warning("Missing country name for getPopulationofCountry at line " + lineNumber + ": " + line);
                            continue;
                        }
                        break;
                    }
                    
                    case "getNumberofCities": {
                        // Expected signature: getNumberofCities(String countryName, int threshold, int comparison)
                        // Input format: getNumberofCities CountryName threshold [min|max|=]
                        // Example: "getNumberofCities Equatorial Guinea 42670 min"
                        
                        if (tokens.length >= 3) {
                            // Find the numeric threshold by scanning from the end
                            int thresholdIdx = -1;
                            String operatorStr = null;
                            
                            // Check if last token is an operator (min/max/=)
                            String lastToken = tokens[tokens.length - 1];
                            if (lastToken.equals("min") || lastToken.equals("max") || lastToken.equals("=") || 
                                lastToken.equals(">") || lastToken.equals("<")) {
                                operatorStr = lastToken;
                                // Look for threshold in second-to-last position
                                if (tokens.length >= 4) {
                                    try {
                                        Integer.parseInt(tokens[tokens.length - 2]);
                                        thresholdIdx = tokens.length - 2;
                                    } catch (NumberFormatException e) {
                                        // Threshold not in expected position
                                    }
                                }
                            } else {
                                // No operator, last token should be threshold
                                try {
                                    Integer.parseInt(lastToken);
                                    thresholdIdx = tokens.length - 1;
                                } catch (NumberFormatException e) {
                                    // Last token is not numeric, look backwards
                                }
                            }
                            
                            // If we haven't found threshold yet, scan backwards
                            if (thresholdIdx == -1) {
                                for (int j = tokens.length - 1; j >= 1; j--) {
                                    try {
                                        Integer.parseInt(tokens[j]);
                                        thresholdIdx = j;
                                        break;
                                    } catch (NumberFormatException e) {
                                        // Continue looking
                                    }
                                }
                            }
                            
                            if (thresholdIdx == -1) {
                                logger.warning("No numeric threshold found for getNumberofCities at line " + lineNumber + ": " + line);
                                continue;
                            }
                            
                            // Build country name from tokens before threshold
                            StringBuilder countryName = new StringBuilder();
                            for (int j = 1; j < thresholdIdx; j++) {
                                if (j > 1) countryName.append(" ");
                                countryName.append(tokens[j]);
                            }
                            
                            args.add(countryName.toString());
                            args.add(tokens[thresholdIdx]); // threshold
                            
                            // Parse comparison operator
                            int compInt = 1; // default '>' (greater than)
                            if (operatorStr != null) {
                                switch (operatorStr) {
                                    case "min":
                                    case ">":
                                        compInt = 1; // greater than
                                        break;
                                    case "max":
                                    case "<":
                                        compInt = 2; // less than
                                        break;
                                    case "=":
                                        compInt = 3; // equal to
                                        break;
                                    default:
                                        logger.warning("Unknown comparison operator: " + operatorStr + " at line " + lineNumber + ": " + line);
                                        compInt = 1;
                                }
                            }
                            args.add(String.valueOf(compInt));
                            System.out.println("DEBUG Line " + lineNumber + ": getNumberofCities args=" + args);
                        } else {
                            logger.warning("Invalid argument count for getNumberofCities at line " + lineNumber + ": " + line);
                            continue;
                        }
                        break;
                    }
                    
                    case "getNumberofCountries": {
                        // Expected signature: getNumberofCountries(int cityCount, int threshold, int comp)
                        // Input format: getNumberofCountries cityCount threshold [min|max|=]
                        // Example: "getNumberofCountries 4 68626 max"
                        
                        if (tokens.length >= 3) {
                            // First argument should be cityCount (numeric)
                            try {
                                Integer.parseInt(tokens[1]); // Validate it's numeric
                                args.add(tokens[1]); // cityCount
                            } catch (NumberFormatException e) {
                                logger.warning("Invalid cityCount (not numeric) for getNumberofCountries at line " + lineNumber + ": " + line);
                                continue;
                            }
                            
                            // Second argument should be threshold (numeric)
                            try {
                                Integer.parseInt(tokens[2]); // Validate it's numeric
                                args.add(tokens[2]); // threshold
                            } catch (NumberFormatException e) {
                                logger.warning("Invalid threshold (not numeric) for getNumberofCountries at line " + lineNumber + ": " + line);
                                continue;
                            }
                            
                            // Third argument is optional comparison operator
                            int compInt = 1; // default '>' (greater than)
                            if (tokens.length >= 4) {
                                String compStr = tokens[3];
                                switch (compStr) {
                                    case "min":
                                    case ">":
                                        compInt = 1; // greater than
                                        break;
                                    case "max":
                                    case "<":
                                        compInt = 2; // less than
                                        break;
                                    case "=":
                                        compInt = 3; // equal to
                                        break;
                                    default:
                                        logger.warning("Unknown comparison operator: " + compStr + " at line " + lineNumber + ": " + line);
                                        compInt = 1;
                                }
                            }
                            args.add(String.valueOf(compInt));
                            System.out.println("DEBUG Line " + lineNumber + ": getNumberofCountries args=" + args);
                        } else {
                            logger.warning("Invalid argument count for getNumberofCountries at line " + lineNumber + ": " + line);
                            continue;
                        }
                        break;
                    }
                    
                    case "getNumberofCountriesMM": {
                        if (tokens.length >= 4) {
                            args.add(tokens[1]);
                            args.add(tokens[2]);
                            args.add(tokens[3]);
                            System.out.println("DEBUG Line " + lineNumber + ": getNumberofCountriesMM args=" + args);
                        } else {
                            logger.warning("Invalid argument count for getNumberofCountriesMM at line " + lineNumber + ": " + line);
                            continue;
                        }
                        break;
                    }
                    
                    default: {
                        logger.warning("Unknown method: " + methodName + " at line " + lineNumber);
                        continue;
                    }
                }
                
                Query query = new Query(methodName, args, zone);
                queries.add(query);
                System.out.println("DEBUG Line " + lineNumber + ": Created query: " + query.toString());
                System.out.println("DEBUG Line " + lineNumber + ": Query methodName field: '" + query.methodName + "'");
                System.out.println("---");
            }
            
            scanner.close();
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Input file not found: exercise_1_input.txt", e);
        }
    }
}
