package com.gameday.travel.dto;

import com.gameday.travel.entity.City;

public record CityResponse(Long id, String nameJa, boolean isUnstable) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getNameJa(), city.isUnstable());
    }
}
