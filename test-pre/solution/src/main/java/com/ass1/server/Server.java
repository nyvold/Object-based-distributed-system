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
    private static final int BASE_NETWORK_MS = 80; // base communication latency per assignment

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
            // The proxy binds our stub into its local registry; remote rebinds from
            // this container are disallowed by the registry (non-local host).
            System.out.println("[Server] Awaiting client lookups via proxy binding: " + server.toString());

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        } 
    }

    @Override
    public Result getPopulationofCountry(String countryName) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            try {
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
            return new Result(value, assignment.waitMs(), assignment.execMs());
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Error processing request", e);
        }
        
    }

    @Override
    public Result getNumberofCities(String countryName, int threshold, int comparison) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            String comp = (comparison == 2) ? "max" : "min"; // 1=min (>=), 2=max (<=)
            try {
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
            return new Result(value, assignment.waitMs(), assignment.execMs());
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Error processing request", e);
        }
        
    }

    @Override
    public Result getNumberofCountries(int cityCount, int threshold, int comp) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            String comparison = (comp == 2) ? "max" : "min"; // 1=min (>=), 2=max (<=)
            try {
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
            return new Result(value, assignment.waitMs(), assignment.execMs());
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Error processing request", e);
        }
        
    }

    @Override
    public Result getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) throws RemoteException {
        simulateBaseNetworkLatency();
        TimedTask<Integer> assignment = new TimedTask<>(() -> {
            try {
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
        try {
            Thread.sleep(BASE_NETWORK_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
