package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{
    // should these methods throw remote exceptions?
    int getPopulationofCountry(String countryName);
    int getNumberofCities(String countryName, int threshold, int comparison);
    int getNumberofCountries(int cityCount, int threshold, int comp);
    int getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation);
    int getCurrentLoad();
}
