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
    // - 

    // rules:
    // pri 1: if server overloaded (18>=requests)
    // try server with least requests in waiting list
    // pri 2: if several servers have same amount of requests
    // server closest to the client (clockwise) is chosen
    // pri 3: if all servers overloaded
    // server in same zone is chosen

    // functions:
    // 1. get server via registry (zone in query)
    // 2. queue check if server has capasity
    // 3. parse input
    // 4. register servers

    // proxy gets request from client, and returns a server that can handle the request
    // public Proxy(){
    //     Registry registry = LocateRegistry.getRegistry();
    // }


    public ServerInterface registerServer() {
        
    }
    
}