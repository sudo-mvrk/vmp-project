package com.volunnear.common.config;

import com.volunnear.distribution.scoring.ScoringWeights;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * Application settings under {@code volunnear.*}, checked at startup.
 *
 * @param timeZone the one zone in which activity times and availability slots are read (Q13, Q4a)
 * @param scoring  the global weights of the scoring function (Q4, Q12)
 */
@Validated
@ConfigurationProperties(prefix = "volunnear")
public record VolunnearProperties(@NotNull ZoneId timeZone, @NotNull @Valid Scoring scoring) {

    /**
     * Global weights and the distance decay as a half-distance in km (Q4: lambda = ln 2 / halfDistanceKm).
     * The constructor applies the scoring rules, so invalid values stop the application at startup.
     */
    public record Scoring(@NotNull Double alpha, @NotNull Double beta, @NotNull Double gamma,
                          @NotNull Double halfDistanceKm) {
        public Scoring {
            if (alpha != null && beta != null && gamma != null && halfDistanceKm != null) {
                ScoringWeights.ofHalfDistance(alpha, beta, gamma, halfDistanceKm);
            }
        }

        public ScoringWeights weights() {
            return ScoringWeights.ofHalfDistance(alpha, beta, gamma, halfDistanceKm);
        }

        /** The activity override (Q12) replaces the three weights; the distance decay stays global. */
        public ScoringWeights weights(double alpha, double beta, double gamma) {
            return ScoringWeights.ofHalfDistance(alpha, beta, gamma, halfDistanceKm);
        }
    }
}
