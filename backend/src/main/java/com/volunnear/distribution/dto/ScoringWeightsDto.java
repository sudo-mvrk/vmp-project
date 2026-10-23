package com.volunnear.distribution.dto;

/** The weights that produced a score (Q12): the activity override or the global ones, and the global half-distance. */
public record ScoringWeightsDto(double alpha, double beta, double gamma, double halfDistanceKm) {
}
