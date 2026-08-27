package com.gameday.travel.repository;

import com.gameday.travel.entity.CityConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CityConnectionRepository extends JpaRepository<CityConnection, Long> {

    @Query(value = "SELECT MIN(distance_units) FROM city_connections WHERE from_city_id = :fromCityId AND to_city_id = :toCityId",
            nativeQuery = true)
    Optional<Integer> findDirectDistance(@Param("fromCityId") Long fromCityId, @Param("toCityId") Long toCityId);


    @Query(value = """
            WITH RECURSIVE route_search AS (
                SELECT
                    cc.to_city_id AS current_city_id,
                    ARRAY[cc.from_city_id, cc.to_city_id] AS visited,
                    cc.distance_units AS total_distance,
                    GREATEST(1 :: integer, 1 :: integer) AS hop_count
                FROM city_connections cc
                WHERE cc.from_city_id = :fromCityId

                UNION ALL

                SELECT
                    cc.to_city_id,
                    rs.visited || cc.to_city_id,
                    rs.total_distance + cc.distance_units,
                    rs.hop_count + GREATEST(1 :: integer, 1 :: integer)
                FROM route_search rs
                JOIN city_connections cc ON cc.from_city_id = rs.current_city_id
                WHERE rs.hop_count < 7
                  AND NOT (cc.to_city_id = ANY (rs.visited))
            )
            SELECT MIN(total_distance)
            FROM route_search
            WHERE current_city_id = :toCityId
            """, nativeQuery = true)
    Optional<Integer> findCheapestDistanceViaLayovers(@Param("fromCityId") Long fromCityId, @Param("toCityId") Long toCityId);
}
