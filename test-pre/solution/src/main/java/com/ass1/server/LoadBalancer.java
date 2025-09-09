package com.ass1.server;

import java.rmi.registry.Registry;
import java.util.Map;

public class LoadBalancer {
    // The load balancer should handle load balancing functions

    private Registry registry;
    private Map<Integer, ServerInterface> serverStubs;
    private Map<Integer, Integer> serverLoads;

    public LoadBalancer(Registry registry, Map<Integer, ServerInterface> serverStubs, Map<Integer, Integer> serverLoads){
        this.registry = registry;
        this.serverStubs = serverStubs;
        this.serverLoads = serverLoads;
    }

    public ServerInterface selectBestServerForZone(int clientZone){
        throw new UnsupportedOperationException("Not implemented.");
    }

    /*
     * @param fromZone the zone which the client is requesting from
     * @return the integer that belongs to the nearest zone
     */
    public int getNearestServer(int fromZone){
        throw new UnsupportedOperationException("Not implemented.");
    }

    /*
     * @param from the zone client is requesting from
     * @param to the zone number that is responding to the client
     * @param N the number of zones / servers in the ring
     */
    private static int distance(int from, int to, int N){
        if (to >= from) return to - from;
        return N - (from - to);
    }

}
