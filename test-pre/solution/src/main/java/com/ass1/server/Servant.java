package com.ass1.server;
interface Servant{
 int getPopulationofCountry(String countryName);
 int getNumberofCities(String countryName, int threshold, int comparison);
 int getNumberofCountries(int cityCount, int threshold, int comp);
 int getNumberofCountriesMM(int cityCount, int minPopulation, int maxPopulation);
}