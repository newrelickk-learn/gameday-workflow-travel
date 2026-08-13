package com.gameday.travel.dto;

import jakarta.validation.constraints.NotNull;

public record CostEstimateRequest(
        @NotNull Long departureCityId,
        @NotNull Long arrivalCityId
) {
}
