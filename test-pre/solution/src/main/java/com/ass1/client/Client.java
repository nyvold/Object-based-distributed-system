package com.ass1.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
    private ProxyInterface proxy = new ProxyInterface() {

        @Override
        public int registerServer(String address, int port, String bindingName, ServerInterface serverStub) {
            return proxy.registerServer(address, port, bindingName, serverStub);
        }

        @Override
        public com.ass1.server.ServerConnection connectToServer(int zone) {
            return proxy.connectToServer(zone);
        }
    };

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
        //System.out.println("Total queries: " + client.queries.size()); 
        client.sendQueries();

        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");
        }   catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            }
    }

    public void sendQueries() {
        for (Query query : queries) {
            try {
                // 1. Connect to proxy to get ServerConnection for the query's zone
                ServerConnection serverConn = proxy.connectToServer(query.zone);

                // 2. Lookup the correct server in the registry using ServerConnection info
                Registry registry = LocateRegistry.getRegistry(serverConn.getServerAddress(), serverConn.getServerPort());
                ServerInterface server = (ServerInterface) registry.lookup(serverConn.getBindingName());

                // 3. Send the query to the server and get the result
                String result = server.executeQuery(query.methodName, query.args);

                // 4. Write the result to output.txt
                writeToFile(result);

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