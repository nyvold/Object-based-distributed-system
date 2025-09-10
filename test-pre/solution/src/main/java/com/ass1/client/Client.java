package com.ass1.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.ass1.server.ServerInterface;

//DISCLAIMER: THIS CODE IS NOT COMPLETE
public class Client {
    public HashMap<String, ArrayList<String[]>> queries = new HashMap<>();

    // TODO: 
    // parse an input file containing a a sequence of queries:
    // (each line specifies a method name and an argument that should be invoked on the remote server)
    public static void main(String[] args) {
        Client client = new Client();
        client.parseInputFile();
        System.out.println(client.queries.size());

        // lookup server from registry
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private void parseInputFile() {
        try {
            File file = new File("exercise_1_input.txt");
            Scanner scanner = new Scanner(file);
            // input file format: <method_name> <arg1> <arg2> <arg3> <zone:#>
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(" ");
                String methodName = parts[0];
                String[] args = new String[parts.length - 1];
                System.arraycopy(parts, 1, args, 1, args.length - 1); // exclude method name
                // Create a Client object or a Request object to store the query
                String zone = parts[parts.length - 1].split(":")[1]; // extract zone
                queries.putIfAbsent(zone, new ArrayList<>());
                queries.get(zone).add(args);

            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
    }

    // For each remote invocation, the client will print the result of the invocation and the time it took.
    // the output should also be printed to a file. 
    // the delay between each invoaction is T milliseconds (T = [50, 20], meaning that the next invocation must be delayed T milliseconds, regardless of whether the current invocation is finished or not.)
    // method name describes one of the 4 interfaces the server provides

    //Note: The Client represents a simulator for set of clients. In real-world scenarios, there
    // will be tens of thousands of clients that will send request to the servers. But we use a
    // single Client to represent all the clients of all zones.

    //  TODO: Implement the client application that will
    // 1. Lookup the server object from the RMI registry
    // 2. Call the remote method on the server object
    }
}