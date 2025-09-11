package com.ass1.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//DISCLAIMER: THIS CODE IS NOT COMPLETE, BUT ALMOST
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

            System.out.println("Sending query: " + query);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

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

    public void writeToFile(String filename, String content) {
        // Write results from server to file
        // For each remote invocation, the client will print the 
        // result of the invocation and the time it took
        // Output file format : <result> <input query> (turnaround time: YY ms, execution time:
        // ZZ ms, waiting time: TT ms, processed by Server <server#>)
        // eg:- : 9362428 getPopulationofCountry Sweden Zone:1 (turnaround time: 120
        // ms, execution time: 10 ms, waiting time: 100 ms, processed by Server 1)
    }
}