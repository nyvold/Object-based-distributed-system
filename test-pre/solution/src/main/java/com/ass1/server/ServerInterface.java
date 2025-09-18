package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{
    Result getPopulationofCountry(String countryName) throws RemoteException;
    Result getNumberofCities(String countryName, int threshold, int comparison) throws RemoteException;
    Result getNumberofCountries(int cityCount, int threshold, int comp) throws RemoteException;
    Result getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) throws RemoteException;
    int getCurrentLoad() throws RemoteException;
}
