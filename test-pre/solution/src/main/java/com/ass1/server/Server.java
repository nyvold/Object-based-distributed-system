package com.ass1.server;

import com.ass1.server.core.StatsService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
            String address = System.getenv().getOrDefault("PROXY_HOST", "localhost");
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
            // The proxy binds our stub into its local registry; remote rebinds from
            // this container are disallowed by the registry (non-local host).
            System.out.println("[Server] Awaiting client lookups via proxy binding: " + server.toString());

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

    // Execute a query described by method name and arguments.
    // Returns the numeric result as a string.
    @Override
    public String executeQuery(String methodName, List<String> arguments) {
        // reflect work in load queue
        queue.add(new Object());
        try {
            switch (methodName) {
                case "getPopulationofCountry": {
                    if (arguments == null || arguments.isEmpty()) {
                        return "0";
                    }
                    String country = String.join(" ", arguments);
                    int res = getPopulationofCountry(country);
                    return Integer.toString(res);
                }
                case "getNumberofCities": {
                    if (arguments == null || arguments.size() < 3) {
                        return "0";
                    }
                    String compToken = arguments.get(arguments.size() - 1);
                    String thresholdToken = arguments.get(arguments.size() - 2);
                    String country = String.join(" ", arguments.subList(0, arguments.size() - 2));

                    int threshold;
                    try { threshold = Integer.parseInt(thresholdToken); }
                    catch (NumberFormatException nfe) { return "0"; }

                    int compInt;
                    if (compToken.equalsIgnoreCase("min")) compInt = 1;
                    else if (compToken.equalsIgnoreCase("max")) compInt = -1;
                    else {
                        try { compInt = Integer.parseInt(compToken); }
                        catch (NumberFormatException nfe) { compInt = 1; }
                    }

                    int res = getNumberofCities(country, threshold, compInt);
                    return Integer.toString(res);
                }
                case "getNumberofCountries": {
                    if (arguments == null || arguments.size() < 3) {
                        return "0";
                    }
                    String cityCountToken = arguments.get(0);
                    String thresholdToken = arguments.get(1);
                    String compToken = arguments.get(2);

                    int cityCount, threshold;
                    try {
                        cityCount = Integer.parseInt(cityCountToken);
                        threshold = Integer.parseInt(thresholdToken);
                    } catch (NumberFormatException nfe) {
                        return "0";
                    }

                    int compInt;
                    if (compToken.equalsIgnoreCase("min")) compInt = 1;
                    else if (compToken.equalsIgnoreCase("max")) compInt = -1;
                    else {
                        try { compInt = Integer.parseInt(compToken); }
                        catch (NumberFormatException nfe) { compInt = 1; }
                    }

                    int res = getNumberofCountries(cityCount, threshold, compInt);
                    return Integer.toString(res);
                }
                case "getNumberofCountriesMM": {
                    if (arguments == null || arguments.size() < 3) {
                        return "0";
                    }
                    try {
                        int cityCount = Integer.parseInt(arguments.get(0));
                        int minPop = Integer.parseInt(arguments.get(1));
                        int maxPop = Integer.parseInt(arguments.get(2));
                        int res = getNumberofCountriesMM(cityCount, minPop, maxPop);
                        return Integer.toString(res);
                    } catch (NumberFormatException nfe) {
                        return "0";
                    }
                }
                case "getCurrentLoad": {
                    return Integer.toString(getCurrentLoad());
                }
                default:
                    return "0";
            }
        } finally {
            queue.poll();
        }
    }
}
