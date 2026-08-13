package com.gameday.travel.controller;

import com.gameday.travel.dto.CityResponse;
import com.gameday.travel.dto.CostEstimateRequest;
import com.gameday.travel.dto.CostEstimateResponse;
import com.gameday.travel.repository.CityRepository;
import com.gameday.travel.service.CostEstimationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
public class TravelController {

    private final CityRepository cityRepository;
    private final CostEstimationService costEstimationService;

    public TravelController(CityRepository cityRepository, CostEstimationService costEstimationService) {
        this.cityRepository = cityRepository;
        this.costEstimationService = costEstimationService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return cityRepository.findAll().stream()
                .map(CityResponse::from)
                .toList();
    }

    @PostMapping("/estimate")
    public CostEstimateResponse estimate(@Valid @RequestBody CostEstimateRequest request) {
        return costEstimationService.estimate(request.departureCityId(), request.arrivalCityId());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleUnknownCity(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
