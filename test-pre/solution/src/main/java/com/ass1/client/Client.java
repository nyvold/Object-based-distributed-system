package com.ass1.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Client {
    
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
    
    public List<Query> queries = new ArrayList<>();

    public static void main(String[] args) {
        // Overwrite (clear) output.txt at the start
        try (FileWriter fw = new FileWriter("output.txt", false)) {
            // false = overwrite mode
            // Just open and close to clear the file
        } catch (IOException e) {
            e.printStackTrace();
        }

        Client client = new Client();
        client.parseInputFile();
        System.out.println("Total queries: " + client.queries.size());
        // lookup server from registry
        /*
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        */
        client.sendQueries();
    }

    public void sendQueries() { //send queries to server with 10ms delay
        for (Query query : queries) {
            writeToFile(query); //dummy values for times and serverZone
            System.out.println("Sending query: " + query);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
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
    //Testing the filewriter, should be adjusted to write the results from the server invocation
    public void writeToFile(Query query) {
        String outputLine = query.toString() + System.lineSeparator();
        try (FileWriter fw = new FileWriter("output.txt", true)) { // true for append mode
            fw.write(outputLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}