package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;

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

    // used to send to client
    private Map<Integer, ServerConnection> serverConnections; // <zone, ServerConnection>
    // used to get queue, i.e proxys own use
    private Map<Integer, ServerInterface> serverStubs; // <zone, ServerInterface>
    // updated by polling servers, used to check for best server
    private Map<Integer, Integer> serverLoads; // <zone, serverLoad>

    public Proxy(
            int size, // the amount of servers in the ring
            Registry registry
    ) {
        this.registry = registry;
        this.balancer = new LoadBalancer(registry, serverStubs, serverLoads);
        this.refresher = new Refresher(registry, serverLoads);
    }

    public void registerServer(String adress, int port, int zone, String bindingName, ServerInterface stub) {
        // should server call proxy to register
        ServerConnection conn = new ServerConnection(adress, port, zone, bindingName);
        serverConnections.put(zone, conn);
        serverStubs.put(zone, stub);
        serverLoads.put(zone, 0); // server load starts at 0
    }

    
    public static void main(String[] args) {
        // proxy binds itself in the registry so servers can find it and call registerServer()
        try {
            int numberOfServers = 5;
            Registry registry = LocateRegistry.createRegistry(1099);
            Proxy proxy = new Proxy(numberOfServers, registry);
            ProxyInterface stub = (ProxyInterface) UnicastRemoteObject.exportObject(proxy, 0);
            registry.rebind("Proxy", stub); // rebind instead of bind so we dont have to unbind "Proxy" every time we restart or redeploy (because of AlreadyBoundException)
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // initialize all servers here?
        // call registerServer N times (where N is amount of zones)
    }

    @Override
    public int registerServer(String address, int port, String bindingName, ServerInterface serverStub) {
        // should server call proxy to register
        int zone = nextZone++;
        ServerConnection conn = new ServerConnection(address, port, zone, bindingName);
        serverConnections.put(zone, conn);
        serverStubs.put(zone, serverStub);
        serverLoads.put(zone, 0); // server load starts at 0
        
        return zone;
    }
}