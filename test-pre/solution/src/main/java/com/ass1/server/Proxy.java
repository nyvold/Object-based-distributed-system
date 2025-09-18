package com.ass1.server;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class Proxy implements ProxyInterface {

    // Proxy composition: Proxy, Refresher, LoadBalancer
    // Proxy responsability: entry point for client requests, registration of servers, and coordination

    // our proxy must:
    // - distribute requests based on clients zone
    // - have a overview of each servers workload, this overview is updated every 18
    // requests?
    // - request delegation happends in independent thread
    // - workload updating happends in separate thread
    // - assign zone to servers in ascending order, and record servers IP adress and
    // port
    // - simulate connection time between client and server

    // rules:
    // step 1: if server overloaded (18>=requests)
    // try server with least requests in waiting list
    // step 2: if several servers have same amount of requests
    // server closest to the client (clockwise) is chosen
    // step 3: if all servers overloaded
    // server in same zone is chosen
    // step 4: if zone has no server:
    // client is redirected to closest one (clockwise) and repeats steps 1-3

    // functions:
    // 1. get server via registry (zone in query)
    // 2. queue check if server has capasity
    // 3. parse input
    // 4. register servers (via API?)
    // 5. calculate client-server connection delay

    // proxy gets request from client, and returns a server that can handle the
    // request
    // public Proxy(){
    // Registry registry = LocateRegistry.getRegistry();
    // }

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
        // should server call proxy to register
        int zone = nextZone++;
        // In our architecture, servers bind into this proxy's registry.
        // Return connections that point clients to the proxy registry (1099)
        String proxyHost = System.getenv().getOrDefault("PROXY_HOST", "localhost");
        String assignedBindingName = "server_zone_" + zone;

        ServerConnection conn = new ServerConnection(proxyHost, PROXY_PORT, zone, assignedBindingName);

        serverConnections.put(zone, conn);
        serverStubs.put(zone, serverStub);
        serverLoads.put(zone, 0); 
        assignmentCounters.put(zone, 0);

        // Bind the server's stub into this (local) registry so clients can look it up.
        // Doing it here avoids remote containers attempting to rebind, which the
        // RMI registry disallows ("origin is non-local host").
        registry.rebind(assignedBindingName, serverStub);
        System.out.println("[Proxy] Bound server for zone " + zone + " as '" + assignedBindingName + "' in local registry");
        return zone;
    }

    
    public static void main(String[] args) {
        Registry registry = null;
        ProxyInterface stub = null;
        try {
            registry = LocateRegistry.createRegistry(PROXY_PORT);
            Proxy proxy = new Proxy(registry);
            stub = (ProxyInterface) UnicastRemoteObject.exportObject(proxy, 0);
            registry.bind("Proxy", stub); // rebind instead of bind so we dont have to unbind "Proxy" every time we restart or redeploy (because of AlreadyBoundException)
            System.out.println("[Proxy] Proxy started and bound in registry as 'Proxy' on port 1099.");
        } catch (RemoteException | AlreadyBoundException e) {
            System.out.println("[Proxy] Proxy already bound in registry as 'Proxy'.");
            try {
                registry.unbind("Proxy");
                registry.bind("Proxy", stub);
            } catch (RemoteException | NotBoundException | AlreadyBoundException ex) {
                System.out.println("[Proxy] Failed to unbind and bind 'Proxy' in registry");
                ex.printStackTrace();
            }
            
            System.out.println("[Proxy] unbind and bind Proxy in registry as 'Proxy' on port 1099");

        }
    }
}
