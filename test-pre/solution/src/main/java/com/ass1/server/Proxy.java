package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;

public class Proxy{

    // our proxy must:
    // - distribute requests based on clients zone
    // - have a overview of each servers workload, this overview is updated every 18 requests?
    // - request delegation happends in independent thread
    // - workload updating happends in separate thread
    // - assign zone to servers in ascending order, and record servers IP adress and port
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

    // proxy gets request from client, and returns a server that can handle the request
    // public Proxy(){
    //     Registry registry = LocateRegistry.getRegistry();
    // }


    public ServerInterface registerServer() {
        
    }
    
}