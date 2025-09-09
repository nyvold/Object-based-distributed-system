package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;

public class Server implements ServerInterface{
    public static void main(String[] args){
        try {
            Registry registry = LocateRegistry.getRegistry();
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);
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
}
