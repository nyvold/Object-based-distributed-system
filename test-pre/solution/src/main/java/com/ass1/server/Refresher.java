package com.ass1.server;

import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Refresher {
    
    public static int MAX_POLL_ASSIGNMENTS = 18;
    private final Registry registry;
    private final ExecutorService ioPool = Executors.newCachedThreadPool();
    private Map<Integer, Integer> serverLoads;

    public Refresher(
        Registry registry, 
        Map<Integer, Integer> serverLoads
    ){
        this.registry = registry;
        this.serverLoads = serverLoads;
    }

    public void refreshServerAsync(int zone) {
        ioPool.submit(() -> refreshServer(zone));
    }

    private void refreshServer(int zone){
        try{
            // get server load
            // set the size of the witing queue
            // refresh must happend in own thread to not interupt normal server activity?
            String bindingName = "server_zone_" + zone;
            ServerInterface server = (ServerInterface) registry.lookup(bindingName);
            int load = server.getCurrentLoad();
            serverLoads.put(zone, load);
        } catch(Exception e) {
            serverLoads.put(zone, Integer.MAX_VALUE);
        }
    }
}
