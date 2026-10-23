package com.volunnear.activity.dto;

import jakarta.validation.constraints.NotNull;

/** Weight override (Q12): all three are required; the service checks they are non-negative and sum to 1. */
public record ActivityWeightsDto(
        @NotNull Double alpha,
        @NotNull Double beta,
        @NotNull Double gamma
) {
}
