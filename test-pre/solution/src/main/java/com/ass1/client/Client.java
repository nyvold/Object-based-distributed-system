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

import com.ass1.server.ProxyInterface;
import com.ass1.server.ServerConnection;
import com.ass1.server.ServerInterface;


public class Client {
    
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
        try (FileWriter fw = new FileWriter("output.txt", false)) { // false = overwrite mode
        } catch (IOException e) {
            e.printStackTrace();
        }
		
        Client client = new Client();
        client.parseInputFile();
        System.out.println("Total queries: " + client.queries.size()); 
        client.sendQueries();
    }

    public void sendQueries() {
        for (Query query : queries) {
            try {
                //Hi proxy! Where are you? 
                Registry registry = LocateRegistry.getRegistry();
                ProxyInterface proxy = (ProxyInterface) registry.lookup("Proxy");

                //There you are! I'm from zone X, which server should I use?
                ServerConnection serverConn = proxy.connectToServer(query.zone);

                //Thanks! Now I can look up the server in the RMI registry
                registry = LocateRegistry.getRegistry(serverConn.getServerAddress(), serverConn.getServerPort());
                ServerInterface server = (ServerInterface) registry.lookup(serverConn.getBindingName());

                //Now that I have the server stub, I can execute my query! Thanks proxy!
                if(query.methodName.equals("getPopulationofCountry") && query.args.size() == 1) {
                    String countryName = query.args.get(0);
                    String result = "Population of " + countryName + ": " + server.getPopulationofCountry(countryName);
                    writeToFile(result);
                } else if (query.methodName.equals("getNumberofCities") && query.args.size() == 3) {
                    String countryName = query.args.get(0);
                    int threshold = Integer.parseInt(query.args.get(1));
                    int comparison = Integer.parseInt(query.args.get(2));
                    String compStr = (comparison == 1) ? ">" : (comparison == 2) ? "<" : "=";
                    String result = "Number of cities in " + countryName + " with population " + compStr + " " + threshold + ": " + server.getNumberofCities(countryName, threshold, comparison);
                    writeToFile(result);
                } else if (query.methodName.equals("getNumberofCountries") && query.args.size() == 3) {
                    int cityCount = Integer.parseInt(query.args.get(0));
                    int threshold = Integer.parseInt(query.args.get(1));
                    int comparison = Integer.parseInt(query.args.get(2));
                    String compStr = (comparison == 1) ? ">" : (comparison == 2) ? "<" : "=";
                    String result = "Number of countries with number of cities " + compStr + " " + threshold + ": " + server.getNumberofCountries(cityCount, threshold, comparison);
                    writeToFile(result);
                } else if (query.methodName.equals("getNumberofCountriesMM") && query.args.size() == 3) {
                    int cityCount = Integer.parseInt(query.args.get(0));
                    int minPopulation = Integer.parseInt(query.args.get(1));
                    int maxPopulation = Integer.parseInt(query.args.get(2));
                    String result = "Number of countries with number of cities > " + cityCount + " and population between " + minPopulation + " and " + maxPopulation + ": " + server.getNumberofCountriesMM(cityCount, minPopulation, maxPopulation);
                    writeToFile(result);
                } else {
                    writeToFile("Invalid query: " + query.toString());
                }
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Parses the input file and populates the queries list
    private void parseInputFile() {
        try {
            File file = new File("exercise_1_input.txt");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                // Find the last "Zone:" part
                int zoneIdx = line.lastIndexOf("Zone:");
                if (zoneIdx == -1) continue; // skip malformed lines

                String beforeZone = line.substring(0, zoneIdx).trim();
                String zoneStr = line.substring(zoneIdx + 5).trim();
                int zone;
                try {
                    zone = Integer.parseInt(zoneStr);
                } catch (NumberFormatException e) {
                    continue; // skip malformed lines
                }

                // Split beforeZone into methodName and args
                Scanner lineScanner = new Scanner(beforeZone);
                if (!lineScanner.hasNext()) {
                    lineScanner.close();
                    continue;
                }
                String methodName = lineScanner.next();
                List<String> args = new ArrayList<>();
                while (lineScanner.hasNext()) {
                    args.add(lineScanner.next());
                }
                lineScanner.close();

                queries.add(new Query(methodName, args, zone));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    // Helper method to write result to file
    public void writeToFile(String result) {
        try (FileWriter fw = new FileWriter("output.txt", true)) {
            fw.write(result + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}