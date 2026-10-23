package com.volunnear.distribution.scoring;

/**
 * Result of scoring one volunteer against one activity: every component, the total and the weights that
 * produced it. All four values are in [0, 1].
 */
public record ScoreBreakdown(double geo, double skill, double priority, double total, ScoringWeights weights) {
}
