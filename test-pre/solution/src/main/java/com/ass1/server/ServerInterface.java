package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{
    Result getPopulationofCountry(String countryName, int clientZone) throws RemoteException;
    Result getNumberofCities(String countryName, int threshold, int comparison, int clientZone) throws RemoteException;
    Result getNumberofCountries(int cityCount, int threshold, int comp, int clientZone) throws RemoteException;
    Result getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation, int clientZone) throws RemoteException;
    int getCurrentLoad() throws RemoteException;
}
