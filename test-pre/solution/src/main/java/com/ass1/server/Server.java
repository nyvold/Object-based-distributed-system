package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;

public class Server implements ServerInterface{
    private String adress;
    private int port;
    private int zone;
    private String bindingName;

    public Server(String adress, int port, int zone, String bindingName) {
        this.adress = adress;
        this.port = port;
        this.zone = zone;
        this.bindingName = bindingName;
    }

    public static void main(String[] args){
        try {
            // temporary!
            int zone = 1;
            int port = 1099 + zone;
            String adress = "localhost";
            String bindingName = "server_zone_" + zone;
            // temporary!

            Server server = new Server(adress, port, zone, bindingName);
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, port);
            Registry registry = LocateRegistry.getRegistry(adress, port);
            registry.bind(bindingName, serverStub);
        } catch (RemoteException | AlreadyBoundException e) {
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

    public String getAdress() { return adress; }
    public int getPort() { return port; }
    public int getZone() {return zone; }
    public String getBindingName() {return bindingName; }
}
