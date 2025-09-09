package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;

public class Server implements ServerInterface{
    private String address;
    private int port;
    private int zone;
    private String bindingName;

    public Server(String address, int port, int zone, String bindingName) {
        this.address = address;
        this.port = port;
        this.zone = zone;
        this.bindingName = bindingName;
    }

    public static void main(String[] args){
        try {
            String address = "localhost";
            int port = 0; 
            int zone = -1;
            String bindingName = "temp";

            Server server = new Server(address, port, zone, bindingName);
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, port);

            Registry registry = LocateRegistry.getRegistry(address, 1099);
            ProxyInterface proxy = (ProxyInterface) registry.lookup("Proxy");
            int assignedZone = proxy.registerServer(address, port, bindingName, serverStub);
            String assignedBindingName = "server_zone_" + assignedZone;

            server.zone = assignedZone;
            server.bindingName = assignedBindingName;
            registry.rebind(assignedBindingName, serverStub);
            
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        } 
    }

    @Override
    public int getPopulationofCountry(String countryName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPopulationofCountry'");
    }

    @Override
    public int getNumberofCities(String countryName, int threshold, int comparison) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNumberofCities'");
    }

    @Override
    public int getNumberofCountries(int cityCount, int threshold, int comp) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNumberofCountries'");
    }

    @Override
    public int getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNumberofCountriesMM'");
    }

    @Override
    public String toString() {
        // should return port and zone
         return "";
    }

    public String getAddress() { return address; }
    public int getPort() { return port; }
    public int getZone() {return zone; }
    public String getBindingName() {return bindingName; }
}
