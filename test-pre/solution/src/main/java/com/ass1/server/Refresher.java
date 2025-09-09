package com.ass1.server;

import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Refresher {

    // Tracks assignment number state for servers, on behalf of the proxy
    // Keeps references to the servers to execute refresh

    private static int MAX_POLL_ASSIGNMENTS = 18;

    private final Registry registry;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService ioPool = Executors.newCachedThreadPool();

    private Map<Integer, Integer> serverLoads;

    public Refresher(Registry registry, Map<Integer, Integer> serverLoads){
        this.registry = registry;
        this.serverLoads = serverLoads;

        // start thread for periodic refreshing
    }

    public void incrementAssignmentCounter(int zone){
        // get server from registry
        // get assignemnt count for server
        // increment assignment count for server
        // counter must run in own thread to not prevent normal server activities
        throw new UnsupportedOperationException("Not implemented.");
    }

    private void refreshServer(Server server){
        try{
            // get server load
            // set the size of the witing queue
            // refresh must happend in own thread to not interupt normal server activity?
        }catch(Exception e){
            // set the queue size to INTEGER max value to indicate that the servicer is unavailable for more tasks
        }
    }
}
