// CityDTO.java
package com.ass1.server.dto;

public record CityDTO(
    long geonameId,
    String name,
    String countryCode,
    int population,
    String timezone,
    double latitude,
    double longitude
) {}