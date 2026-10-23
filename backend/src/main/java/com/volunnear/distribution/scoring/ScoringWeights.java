package com.volunnear.distribution.scoring;

/**
 * Weights of the aggregate score (FUNCTION.md section 2) and the distance-decay rate.
 *
 * @param alpha  weight of the geospatial utility
 * @param beta   weight of the skill utility
 * @param gamma  weight of the priority utility
 * @param lambda distance-decay rate in 1/km, greater than 0
 */
public record ScoringWeights(double alpha, double beta, double gamma, double lambda) {
    /** 0.4 + 0.4 + 0.2 is not exactly 1 in binary floating point, so the sum is checked with a tolerance. */
    public static final double SUM_TOLERANCE = 1e-9;

    public ScoringWeights {
        requireOnSimplex(alpha, beta, gamma);
        if (!(lambda > 0) || Double.isInfinite(lambda)) {
            throw new IllegalArgumentException("lambda must be a finite number greater than 0, got " + lambda);
        }
    }

    /**
     * Builds the weights from a half-distance: the distance in km at which the geospatial utility is 0.5,
     * so lambda = ln 2 / halfDistanceKm.
     */
    public static ScoringWeights ofHalfDistance(double alpha, double beta, double gamma, double halfDistanceKm) {
        if (!(halfDistanceKm > 0) || Double.isInfinite(halfDistanceKm)) {
            throw new IllegalArgumentException(
                    "Half-distance must be a finite number of km greater than 0, got " + halfDistanceKm);
        }
        return new ScoringWeights(alpha, beta, gamma, Math.log(2) / halfDistanceKm);
    }

    /**
     * The weight rule on its own (Q12): each weight non-negative, the three summing to 1.
     * Also used to validate a per-activity override before it is stored.
     *
     * @throws IllegalArgumentException if the weights are not on the simplex
     */
    public static void requireOnSimplex(double alpha, double beta, double gamma) {
        requireWeight("alpha", alpha);
        requireWeight("beta", beta);
        requireWeight("gamma", gamma);
        double sum = alpha + beta + gamma;
        if (Math.abs(sum - 1.0) > SUM_TOLERANCE) {
            throw new IllegalArgumentException("alpha + beta + gamma must be 1, got " + sum);
        }
    }

    private static void requireWeight(String name, double value) {
        // written as a negated comparison so that NaN is rejected as well
        if (!(value >= 0)) {
            throw new IllegalArgumentException(name + " must not be negative, got " + value);
        }
    }
}
