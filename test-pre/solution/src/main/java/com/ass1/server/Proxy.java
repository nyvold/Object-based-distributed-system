package com.ass1.server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Proxy implements ProxyInterface {
    private static final Logger logger = Logger.getLogger(Proxy.class.getName());

    // Proxy composition: Proxy, Refresher, LoadBalancer
    // Proxy responsability: entry point for client requests, registration of servers, and coordination

    private final Registry registry;
    private final LoadBalancer balancer;
    private final Refresher refresher;

    private int nextZone = 1;

    private Map<Integer, ServerConnection> serverConnections = new HashMap<>(); // <zone, ServerConnection>
    private Map<Integer, ServerInterface> serverStubs = new HashMap<>(); // <zone, ServerInterface>
    private Map<Integer, Integer> serverLoads = new HashMap<>(); // <zone, serverLoad>
    private Map<Integer, Integer> assignmentCounters = new HashMap<>(); // <zone, count>

    public static final int PROXY_PORT = 1099;

    public Proxy(Registry registry) {
        this.registry = registry;
        this.balancer = new LoadBalancer(serverStubs, serverLoads);
        this.refresher = new Refresher(registry, serverLoads);
    }

    public ServerConnection connectToServer(int zone) throws RemoteException {
        // used by client, tracks assignments given to server

        int bestZone = balancer.selectBestServerForZone(zone);
        ServerConnection conn = serverConnections.get(bestZone);

        int count = assignmentCounters.getOrDefault(bestZone, 0) + 1;
        if (count >= Refresher.MAX_POLL_ASSIGNMENTS) {
            assignmentCounters.put(bestZone, 0);
            refresher.refreshServerAsync(bestZone);
        } else {
            assignmentCounters.put(bestZone, count);
        }

        return conn;
    }

    public int registerServer(String address, int port, String bindingName, ServerInterface serverStub) throws RemoteException {
        // used by server, assigns zones
        int zone = nextZone++;
        String proxyHost = System.getenv().getOrDefault("PROXY_HOST", "localhost");
        String assignedBindingName = "server_zone_" + zone;

        ServerConnection conn = new ServerConnection(proxyHost, PROXY_PORT, zone, assignedBindingName);

        serverConnections.put(zone, conn);
        serverStubs.put(zone, serverStub);
        serverLoads.put(zone, 0); 
        assignmentCounters.put(zone, 0);

        registry.rebind(assignedBindingName, serverStub);
        logger.info("[Proxy] Bound server for zone " + zone + " as '" + assignedBindingName + "' in local registry");
        return zone;
    }

    
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(PROXY_PORT);
            Proxy proxy = new Proxy(registry);
            ProxyInterface stub = (ProxyInterface) UnicastRemoteObject.exportObject(proxy, 0);
            registry.bind("Proxy", stub); 
            logger.info("[Proxy] Proxy started and bound in registry as 'Proxy' on port 1099.");
        } catch (AlreadyBoundException e) {
            logger.info("[Proxy] Proxy already bound in registry, using existing binding");
        } catch (RemoteException ex) {
            logger.log(Level.SEVERE, "[Proxy] RemoteException in proxy main: " + ex.getLocalizedMessage(), ex);
        }
    }
}
