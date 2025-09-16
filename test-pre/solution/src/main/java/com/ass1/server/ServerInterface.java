package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInterface extends Remote{
    int getPopulationofCountry(String countryName) throws RemoteException;
    int getNumberofCities(String countryName, int threshold, int comparison) throws RemoteException;
    int getNumberofCountries(int cityCount, int threshold, int comp) throws RemoteException;
    int getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation) throws RemoteException;
    int getCurrentLoad() throws RemoteException;
}
