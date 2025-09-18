package com.ass1.server;

import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Map;

public class LoadBalancer {
    private static int MAX_LOAD = 18;

    private Map<Integer, ServerInterface> serverStubs;
    private Map<Integer, Integer> serverLoads;

    public LoadBalancer(
        Map<Integer, ServerInterface> serverStubs, 
        Map<Integer, Integer> serverLoads
    ){
        this.serverStubs = serverStubs;
        this.serverLoads = serverLoads;
    }

    public int selectBestServerForZone(int clientZone){
        Integer sameZoneLoad = serverLoads.get(clientZone);
        if (sameZoneLoad != null && sameZoneLoad < MAX_LOAD) {
            return clientZone;
        }

        int minLoad = Integer.MAX_VALUE;
        ArrayList<Integer> leastLoadedZones = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : serverLoads.entrySet()) {
            int zone = entry.getKey();
            int load = entry.getValue();
            if (zone == clientZone) continue; 
            if (load < MAX_LOAD) {
                if (load < minLoad) {
                    minLoad = load;
                    leastLoadedZones.clear();
                    leastLoadedZones.add(zone);
                } else if (load == minLoad) {
                    leastLoadedZones.add(zone);
                }
            }
        }
        if (!leastLoadedZones.isEmpty()) {
            return getNearestServer(clientZone, leastLoadedZones);
        }

        if (serverLoads.containsKey(clientZone)) {
            return clientZone;
        }

        return getNearestServer(clientZone, new ArrayList<>(serverStubs.keySet()));
    }

    public int getNearestServer(int fromZone, ArrayList<Integer> zones) {
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

    private static int distance(int from, int to, int N){
        if (to >= from) return to - from;
        return N - (from - to);
    }

}
