package com.ass1.server;

public final class Latency {
    public static long commMs(int clientZone, int serverZone, int ringSize) {
        int d = (serverZone >= clientZone) ? (serverZone - clientZone)                                          : (ringSize - (clientZone - serverZone));
        return 80L + d * 30L; // same-zone = 80ms
    }
}