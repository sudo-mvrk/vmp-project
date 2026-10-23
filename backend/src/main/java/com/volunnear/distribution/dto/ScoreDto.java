package com.volunnear.distribution.dto;

/** The score breakdown (Q17): every component, the total and the weights used. Not rounded, so results reproduce. */
public record ScoreDto(double geo, double skill, double priority, double total, ScoringWeightsDto weights) {
}
