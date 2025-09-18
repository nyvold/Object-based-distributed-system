package com.ass1.server;

import com.ass1.server.core.StatsService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.rmi.NotBoundException;
import java.sql.SQLException;

public class Server implements ServerInterface{
    private String address;
    private int port;
    private int zone;
    private String bindingName;
    private final StatsService stats;

    private final BlockingQueue<FutureTask<?>> queue = new LinkedBlockingQueue<>();
    private static volatile boolean SERVER_CACHE_ENABLED = parseBoolEnv("SERVER_CACHE", false);
    private static volatile int SERVER_CACHE_CAP = parseIntEnv("SERVER_CACHE_CAP", 150);
    private final Map<String, Integer> cache = new LinkedHashMap<String, Integer>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > SERVER_CACHE_CAP;
        }
    };
    private static final int BASE_NETWORK_MS = 80; // base communication latency per assignment
    private static final int EXEC_DELAY_MS = parseIntEnv("SERVER_EXEC_DELAY_MS", 0); // optional simulated exec time

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

        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    FutureTask<?> assignment = queue.take();
                    assignment.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    public static void main(String[] args){
        try {
            // Parse CLI flags: --server-cache[=true|false], --server-cache-cap=N
            if (args != null) {
                for (String a : args) {
                    if (a == null) continue;
                    if (a.startsWith("--server-cache-cap=")) {
                        try { SERVER_CACHE_CAP = Integer.parseInt(a.substring(a.indexOf('=')+1).trim()); } catch (Exception ignore) {}
                    } else if (a.equals("--server-cache") || a.equals("--server-cache=true")) {
                        SERVER_CACHE_ENABLED = true;
                    } else if (a.equals("--server-cache=false")) {
                        SERVER_CACHE_ENABLED = false;
                    }
                }
            }
            String address = System.getenv().getOrDefault("PROXY_HOST", "localhost");
            int port = 0; // let RMI choose an ephemeral listening port for the remote object
            int zone = -1;
            String bindingName = "temp";

            Server server = new Server(address, port, zone, bindingName);
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, port);

            Registry registry = LocateRegistry.getRegistry(address, Proxy.PROXY_PORT);
            ProxyInterface proxy = (ProxyInterface) registry.lookup("Proxy");
            int assignedZone = proxy.registerServer(address, port, bindingName, serverStub);
            String assignedBindingName = "server_zone_" + assignedZone;

            server.zone = assignedZone;
            server.bindingName = assignedBindingName;
            System.out.println("[Server] Registered server in proxy: " + server.bindingName + " (zone " + server.zone + ", address " + server.address + ", port " + server.port + ")");
            System.out.println("[Server] Cache enabled=" + SERVER_CACHE_ENABLED + ", cap=" + SERVER_CACHE_CAP);
            // The proxy binds our stub into its local registry; remote rebinds from
            // this container are disallowed by the registry (non-local host).
            System.out.println("[Server] Awaiting client lookups via proxy binding: " + server.toString());

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        } 
    }

    @Override
    public Result getPopulationofCountry(String countryName, int clientZone) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            try {
                simulateClientDelay(clientZone);
                String key = "getPopulationofCountry|" + countryName;
                if (SERVER_CACHE_ENABLED) {
                    Integer hit = cache.get(key);
                    if (hit != null) return hit;
                }
                long val = stats.getPopulationofCountry(countryName);
                return Math.toIntExact(val);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ArithmeticException ae) {
                return Integer.MAX_VALUE;
            }
        });
        queue.add(assignment);
        try {
            int value = assignment.get();
            if (SERVER_CACHE_ENABLED) {
                String key = "getPopulationofCountry|" + countryName;
                cache.put(key, value);
            }
            return new Result(value, assignment.waitMs(), assignment.execMs());
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Error processing request", e);
        }
        
    }

    @Override
    public Result getNumberofCities(String countryName, int threshold, int comparison, int clientZone) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            String comp = (comparison == 2) ? "max" : "min"; // 1=min (>=), 2=max (<=)
            try {
                simulateClientDelay(clientZone);
                String key = "getNumberofCities|" + countryName + "|" + threshold + "|" + comparison;
                if (SERVER_CACHE_ENABLED) {
                    Integer hit = cache.get(key);
                    if (hit != null) return hit;
                }
                long val = stats.getNumberofCities(countryName, threshold, comp);
                return Math.toIntExact(val);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ArithmeticException ae) {
                return Integer.MAX_VALUE;
            }
        });
        queue.add(assignment);
        try {
            int value = assignment.get();
            if (SERVER_CACHE_ENABLED) {
                String key = "getNumberofCities|" + countryName + "|" + threshold + "|" + comparison;
                cache.put(key, value);
            }
            return new Result(value, assignment.waitMs(), assignment.execMs());
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Error processing request", e);
        }
        
    }

    @Override
    public Result getNumberofCountries(int cityCount, int threshold, int comp, int clientZone) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            String comparison = (comp == 2) ? "max" : "min"; // 1=min (>=), 2=max (<=)
            try {
                simulateClientDelay(clientZone);
                String key = "getNumberofCountries|" + cityCount + "|" + threshold + "|" + comp;
                if (SERVER_CACHE_ENABLED) {
                    Integer hit = cache.get(key);
                    if (hit != null) return hit;
                }
                long val = stats.getNumberofCountries(cityCount, threshold, comparison);
                return Math.toIntExact(val);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ArithmeticException ae) {
                return Integer.MAX_VALUE;
            }
        });
        queue.add(assignment);
        try {
            int value = assignment.get();
            if (SERVER_CACHE_ENABLED) {
                String key = "getNumberofCountries|" + cityCount + "|" + threshold + "|" + comp;
                cache.put(key, value);
            }
            return new Result(value, assignment.waitMs(), assignment.execMs());
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Error processing request", e);
        }
        
    }

    @Override
    public Result getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation, int clientZone) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            try {
                simulateClientDelay(clientZone);
                String key = "getNumberofCountriesMM|" + cityCount + "|" + minPopulation + "|" + maxPopulation;
                if (SERVER_CACHE_ENABLED) {
                    Integer hit = cache.get(key);
                    if (hit != null) return hit;
                }
                long val = stats.getNumberofCountriesMM(cityCount, minPopulation, maxPopulation);
                return Math.toIntExact(val);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ArithmeticException ae) {
                return Integer.MAX_VALUE;
            }
        });
        queue.add(assignment);
        try {
            int value = assignment.get();
            if (SERVER_CACHE_ENABLED) {
                String key = "getNumberofCountriesMM|" + cityCount + "|" + minPopulation + "|" + maxPopulation;
                cache.put(key, value);
            }
            return new Result(value, assignment.waitMs(), assignment.execMs());
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Error processing request", e);
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

    private static void simulateBaseNetworkLatency() {
        try { Thread.sleep(BASE_NETWORK_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void simulateClientDelay(int clientZone){
        try {
            Thread.sleep(Latency.commMs(clientZone, this.zone, 5));
        } catch (Exception e) {
            System.out.println("[Server " + this.zone + "] could not simulate delay.");
        }
    }

    private static int parseIntEnv(String name, int def) {
        String v = System.getenv(name);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }
    private static boolean parseBoolEnv(String name, boolean def) {
        String v = System.getenv(name);
        if (v == null) return def;
        switch (v.trim().toLowerCase()) {
            case "1": case "true": case "yes": case "on": return true;
            case "0": case "false": case "no": case "off": return false;
            default: return def;
        }
    }

    private static final class TimedTask<V> extends FutureTask<V> {
        private final long enqueuedAtNs;
        private volatile long startedAtNs;
        private volatile long finishedAtNs;

        TimedTask(Callable<V> c) {
            super(c);
            this.enqueuedAtNs = System.nanoTime();
        }

        @Override
        public void run() {
            this.startedAtNs = System.nanoTime();
            try {
                super.run();
            } finally {
                this.finishedAtNs = System.nanoTime();
            }
        }

        long waitMs() { return Math.max(0L, (startedAtNs - enqueuedAtNs) / 1_000_000L); }
        long execMs() { return Math.max(0L, (finishedAtNs - startedAtNs) / 1_000_000L); }
    }
}
