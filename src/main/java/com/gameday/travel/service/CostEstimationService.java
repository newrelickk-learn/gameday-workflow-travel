package com.gameday.travel.service;

import com.gameday.travel.dto.CostEstimateResponse;
import com.gameday.travel.entity.City;
import com.gameday.travel.repository.CityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Random;

@Service
public class CostEstimationService {

    // 距離に応じた概算費用の目安として使う単価。実際の距離テーブルではなく、
    // 都市ID差から機械的に算出する簡易ロジック（都市が増えても手動でペア表を保守しなくてよい）。
    private static final long BASE_AMOUNT = 8000L;
    private static final long PER_DISTANCE_UNIT = 3500L;

    private final CityRepository cityRepository;
    private final Random random = new Random();

    public CostEstimationService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public CostEstimateResponse estimate(Long departureCityId, Long arrivalCityId) {
        City departure = requireCity(departureCityId);
        City arrival = requireCity(arrivalCityId);

        simulateProcessingDelay();

        long distanceUnits = Math.abs(departure.getId() - arrival.getId());
        long baseAmount = BASE_AMOUNT + distanceUnits * PER_DISTANCE_UNIT;
        // 「ちゃんと計算している感」を出すための±5%の変動。実際の障害注入はIstio側のみで行う。
        double variance = 0.95 + random.nextDouble() * 0.10;
        long amount = Math.round(baseAmount * variance / 100) * 100;

        return new CostEstimateResponse(amount, "JPY", Instant.now());
    }

    private City requireCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new NoSuchElementException("Unknown city id: " + cityId));
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(200 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
