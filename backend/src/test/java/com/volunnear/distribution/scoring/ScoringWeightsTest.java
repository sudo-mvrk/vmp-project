package com.volunnear.distribution.scoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ScoringWeightsTest {

    @Test
    void acceptsWeightsOnTheSimplex() {
        ScoringWeights weights = new ScoringWeights(0.4, 0.4, 0.2, 0.1);

        assertEquals(0.4, weights.alpha());
        assertEquals(0.4, weights.beta());
        assertEquals(0.2, weights.gamma());
        assertEquals(0.1, weights.lambda());
    }

    @Test
    void acceptsSumThatDiffersFromOneOnlyByRounding() {
        // 0.1 + 0.2 + 0.7 is 0.9999999999999999 in binary floating point
        assertDoesNotThrow(() -> new ScoringWeights(0.1, 0.2, 0.7, 0.1));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.1, 0.6, 0.5",
            "0.6, -0.1, 0.5",
            "0.6, 0.5, -0.1",
    })
    void rejectsNegativeWeight(double alpha, double beta, double gamma) {
        assertThrows(IllegalArgumentException.class, () -> new ScoringWeights(alpha, beta, gamma, 0.1));
    }

    @ParameterizedTest
    @CsvSource({
            "0.4, 0.4, 0.3",       // sum 1.1
            "0.3, 0.3, 0.3",       // sum 0.9
            "0, 0, 0",
            "0.4, 0.4, 0.2000001", // outside the 1e-9 tolerance
    })
    void rejectsSumOtherThanOne(double alpha, double beta, double gamma) {
        assertThrows(IllegalArgumentException.class, () -> new ScoringWeights(alpha, beta, gamma, 0.1));
    }

    @Test
    void rejectsWeightThatIsNotANumber() {
        assertThrows(IllegalArgumentException.class, () -> new ScoringWeights(Double.NaN, 0.5, 0.5, 0.1));
    }

    @ParameterizedTest
    @CsvSource({"0", "-0.1", "NaN", "Infinity"})
    void rejectsLambdaThatIsNotPositiveAndFinite(double lambda) {
        assertThrows(IllegalArgumentException.class, () -> new ScoringWeights(0.4, 0.4, 0.2, lambda));
    }

    @Test
    void halfDistanceOfTenKmGivesLambdaLnTwoOverTen() {
        ScoringWeights weights = ScoringWeights.ofHalfDistance(0.4, 0.4, 0.2, 10);

        // ln 2 / 10 = 0.0693
        assertEquals(0.0693, weights.lambda(), 1e-4);
        // and the utility at the half-distance is one half
        assertEquals(0.5, ScoringFunction.uGeo(10, weights.lambda()), 1e-12);
    }

    @ParameterizedTest
    @CsvSource({"0", "-5", "NaN", "Infinity"})
    void rejectsHalfDistanceThatIsNotPositiveAndFinite(double halfDistanceKm) {
        assertThrows(IllegalArgumentException.class,
                () -> ScoringWeights.ofHalfDistance(0.4, 0.4, 0.2, halfDistanceKm));
    }
}
