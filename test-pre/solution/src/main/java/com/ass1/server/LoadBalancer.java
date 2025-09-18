package com.ass1.server;

import java.util.ArrayList;
import java.util.Map;

public class LoadBalancer {
    private static int MAX_LOAD = 18;

    private Map<Integer, ServerInterface> serverStubs; // <zone, ServerInterface>
    private Map<Integer, Integer> serverLoads; // <zone, serverLoad>

    public LoadBalancer(
        Map<Integer, ServerInterface> serverStubs, 
        Map<Integer, Integer> serverLoads
    ){
        this.serverStubs = serverStubs;
        this.serverLoads = serverLoads;
    }

    public int selectBestServerForZone(int clientZone){
        // returns zone of server most optimal for client to use

        // server zone same as client
        Integer sameZoneLoad = serverLoads.get(clientZone);
        if (sameZoneLoad != null && sameZoneLoad < MAX_LOAD) {
            return clientZone;
        }

        // finds servers with smallest load
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
        // last option, server closest clockwise
        return getNearestServer(clientZone, new ArrayList<>(serverStubs.keySet()));
    }

    public int getNearestServer(int fromZone, ArrayList<Integer> zones) {
        // returns zone closest clockwise

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
