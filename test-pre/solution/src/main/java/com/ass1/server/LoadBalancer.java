package com.ass1.server;

import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Map;

public class LoadBalancer {
    // The load balancer should handle load balancing functions
    private static int MAX_LOAD = 18;

    private Registry registry;
    private Map<Integer, ServerInterface> serverStubs;
    private Map<Integer, Integer> serverLoads;

    public LoadBalancer(
        Registry registry,
        Map<Integer, ServerInterface> serverStubs, 
        Map<Integer, Integer> serverLoads
    ){
        this.registry = registry;
        this.serverStubs = serverStubs;
        this.serverLoads = serverLoads;
    }

    public int selectBestServerForZone(int clientZone){
        ArrayList<Integer> availableServers = new ArrayList<Integer>();
        int minLoad = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : serverLoads.entrySet()) {
            int zone = entry.getKey();
            int load = entry.getValue();
            if (load < MAX_LOAD) {
                if (load < minLoad) {
                    minLoad = load;
                    availableServers.clear();
                    availableServers.add(zone);
                } else if (load == minLoad) {
                    availableServers.add(zone);
                }
            }
        }
        if (!availableServers.isEmpty()) {
            return getNearestServer(clientZone, availableServers);
        }
        if (serverLoads.containsKey(clientZone)) {
            return clientZone;
        }

        return getNearestServer(clientZone, new ArrayList<Integer>(serverStubs.keySet()));
    }

    /*
     * @param fromZone the zone which the client is requesting from
     * @return the integer that belongs to the nearest zone
     */
    public int getNearestServer(int fromZone, ArrayList<Integer> zones) {
        // gets closest clockwise
        if (zones == null || zones.isEmpty()) {
            System.out.println("[LoadBalancer] getNearestServer(): zones is empty or null, check if servers active");
            throw new IllegalArgumentException("No zones available to select from.");
        }
        int minDist = 5;
        int closest = -1;
        for (int zone : zones) {
            int dist = distance(fromZone, zone, 5);
            if(dist == 0) dist = 5;
            if(dist < minDist) {
                minDist = dist;
                closest = zone;
            }
        }
        return closest;
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
