package com.ass1.server;

import com.ass1.server.core.StatsService;
import com.ass1.server.ServerBootstrap;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.sql.SQLException;

public class Server implements ServerInterface{
    private String address;
    private int port;
    private int zone;
    private String bindingName;
    private final StatsService stats;

    private final Queue<Object> queue = new LinkedList<>();

    public Server(
        String address, 
        int port, 
        int zone, 
        String bindingName
    ) {
        this.address = address;
        this.port = port;
        this.zone = zone;
        this.bindingName = bindingName;
        // Initialize DB-backed services from environment
        this.stats = ServerBootstrap.initStatsService();
    }

    public static void main(String[] args){
        try {
            String address = "localhost";
            int port = 0; // let RMI choose an ephemeral listening port for the remote object
            int zone = -1;
            String bindingName = "temp";

            Server server = new Server(address, port, zone, bindingName);
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, port);

            Registry registry = LocateRegistry.getRegistry(address, 1099);
            ProxyInterface proxy = (ProxyInterface) registry.lookup("Proxy");
            int assignedZone = proxy.registerServer(address, port, bindingName, serverStub);
            String assignedBindingName = "server_zone_" + assignedZone;

            server.zone = assignedZone;
            server.bindingName = assignedBindingName;
            System.out.println("[Server] Registered server in proxy: " + server.bindingName + " (zone " + server.zone + ", address " + server.address + ", port " + server.port + ")");
            registry.rebind(assignedBindingName, serverStub);
            System.out.println("[Server] " + server.toString() + " bound in registry");

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        } 
    }

    @Override
    public int getPopulationofCountry(String countryName) {
        try {
            long val = stats.getPopulationofCountry(countryName);
            return Math.toIntExact(val);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ArithmeticException ae) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getNumberofCities(String countryName, int threshold, int comparison) {
        String comp = comparison <= 0 ? "max" : "min"; // simple mapping
        try {
            long val = stats.getNumberofCities(countryName, threshold, comp);
            return Math.toIntExact(val);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ArithmeticException ae) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getNumberofCountries(int cityCount, int threshold, int comp) {
        String comparison = comp <= 0 ? "max" : "min";
        try {
            long val = stats.getNumberofCountries(cityCount, threshold, comparison);
            return Math.toIntExact(val);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ArithmeticException ae) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) {
        try {
            long val = stats.getNumberofCountriesMM(cityCount, minPopulation, maxPopulation);
            return Math.toIntExact(val);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ArithmeticException ae) {
            return Integer.MAX_VALUE;
        }
    }

    public int getCurrentLoad() { return queue.size(); }

    @Override
    public String toString() {
        // should return port and zone
        return bindingName + " on port: " + port + ", zone: " + zone;
    }

    public String getAddress() { return address; }
    public int getPort() { return port; }
    public int getZone() {return zone; }
    public String getBindingName() {return bindingName; }
}
