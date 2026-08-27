package com.gameday.travel.service;

import com.gameday.travel.dto.CostEstimateResponse;
import com.gameday.travel.entity.City;
import com.gameday.travel.repository.CityConnectionRepository;
import com.gameday.travel.repository.CityRepository;
import com.newrelic.api.agent.NewRelic;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

@Service
public class CostEstimationService {

    private static final long BASE_AMOUNT = 8000L;
    private static final long PER_DISTANCE_UNIT = 3500L;

    private final CityRepository cityRepository;
    private final CityConnectionRepository cityConnectionRepository;
    private final Random random = new Random();

    public CostEstimationService(CityRepository cityRepository, CityConnectionRepository cityConnectionRepository) {
        this.cityRepository = cityRepository;
        this.cityConnectionRepository = cityConnectionRepository;
    }

    public CostEstimateResponse estimate(Long departureCityId, Long arrivalCityId) {
        City departure = requireCity(departureCityId);
        City arrival = requireCity(arrivalCityId);

        NewRelic.addCustomParameter("travel.departureCityId", departureCityId);
        NewRelic.addCustomParameter("travel.arrivalCityId", arrivalCityId);
        NewRelic.addCustomParameter("travel.departureCityName", departure.getNameJa());
        NewRelic.addCustomParameter("travel.arrivalCityName", arrival.getNameJa());
        NewRelic.addCustomParameter("travel.isUnstableRoute", departure.isUnstable() || arrival.isUnstable());

        resolveRoute(departure.getId(), arrival.getId());

        simulateProcessingDelay();

        long distanceUnits = Math.abs(departure.getId() - arrival.getId());
        long baseAmount = BASE_AMOUNT + distanceUnits * PER_DISTANCE_UNIT;
        double variance = 0.95 + random.nextDouble() * 0.10;
        long amount = Math.round(baseAmount * variance / 100) * 100;

        NewRelic.addCustomParameter("travel.distanceUnits", distanceUnits);
        NewRelic.addCustomParameter("travel.amount", amount);

        return new CostEstimateResponse(amount, "JPY", Instant.now());
    }

    private City requireCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new NoSuchElementException("Unknown city id: " + cityId));
    }

    private void resolveRoute(Long departureCityId, Long arrivalCityId) {
        Optional<Integer> direct = cityConnectionRepository.findDirectDistance(departureCityId, arrivalCityId);
        boolean isDirectRoute = direct.isPresent();
        Integer routeDistance = direct.orElseGet(
                () -> cityConnectionRepository.findCheapestDistanceViaLayovers(departureCityId, arrivalCityId)
                        .orElse(null));

        NewRelic.addCustomParameter("travel.isDirectRoute", isDirectRoute);
        if (routeDistance != null) {
            NewRelic.addCustomParameter("travel.routeDistanceUnits", routeDistance);
        }
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(200 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
