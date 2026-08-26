package com.gameday.travel.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "city_connections")
public class CityConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_city_id", nullable = false)
    private Long fromCityId;

    @Column(name = "to_city_id", nullable = false)
    private Long toCityId;

    @Column(name = "distance_units", nullable = false)
    private int distanceUnits;

    protected CityConnection() {
    }

    public Long getId() {
        return id;
    }

    public Long getFromCityId() {
        return fromCityId;
    }

    public Long getToCityId() {
        return toCityId;
    }

    public int getDistanceUnits() {
        return distanceUnits;
    }
}
