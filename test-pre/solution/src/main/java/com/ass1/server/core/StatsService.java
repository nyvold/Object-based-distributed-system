// StatsService.java
package com.ass1.server.core;

import java.sql.SQLException;
import com.ass1.server.db.CityRepository;

public final class StatsService {
    private final CityRepository cityRepo;

    public StatsService(CityRepository cityRepo) {
        this.cityRepo = cityRepo;
    }

    // a) getPopulationofCountry(countryName)
    public long getPopulationofCountry(String countryName) throws SQLException {
        return cityRepo.sumPopulationByCountryName(countryName);
    }

    // b) getNumberofCities(countryName, threshold, comp)
    public long getNumberofCities(String countryName, long threshold, String comp) throws SQLException {
        return cityRepo.countCitiesByCountryThreshold(countryName, threshold, comp);
    }

    // c) getNumberofCountries(citycount, threshold, comp)
    public long getNumberofCountries(long citycount, long threshold, String comp) throws SQLException {
        return cityRepo.countCountriesWithCitycountAndThreshold(citycount, threshold, comp);
    }

    // d) getNumberofCountriesMM(citycount, minpopulation, maxpopulation)
    public long getNumberofCountriesMM(long citycount, long minPop, long maxPop) throws SQLException {
        return cityRepo.countCountriesWithCitycountBetween(citycount, minPop, maxPop);
    }
}