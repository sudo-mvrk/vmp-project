package com.volunnear.evaluation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SummaryTest {
    @Test
    void meanStandardDeviationAndIntervalSkipUndefinedValues() {
        Summary summary = Summary.of(new double[]{1, 2, 3, Double.NaN});

        assertThat(summary.n()).isEqualTo(3);
        assertThat(summary.mean()).isCloseTo(2, within(1e-12));
        // sample deviation: sqrt((1 + 0 + 1) / 2) = 1
        assertThat(summary.std()).isCloseTo(1, within(1e-12));
        assertThat(summary.ci95()).isCloseTo(1.96 / Math.sqrt(3), within(1e-12));
    }

    @Test
    void noDefinedValueGivesUndefined() {
        assertThat(Summary.of(new double[]{Double.NaN}).mean()).isNaN();
        assertThat(Summary.of(new double[]{5}).std()).isZero();
    }
}
