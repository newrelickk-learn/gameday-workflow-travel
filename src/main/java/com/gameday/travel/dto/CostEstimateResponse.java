package com.gameday.travel.dto;

import java.time.Instant;

public record CostEstimateResponse(long amount, String currency, Instant calculatedAt) {
}
