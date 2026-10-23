package com.volunnear.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * The global weights are checked when the properties are bound (Q12, Q4): invalid values stop the application.
 * Runs only the configuration that binds them, so no database is needed.
 */
class ScoringPropertiesStartupTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TimeConfig.class)
            .withPropertyValues("volunnear.time-zone=Europe/Prague",
                    "volunnear.scoring.alpha=0.4", "volunnear.scoring.beta=0.4", "volunnear.scoring.gamma=0.2",
                    "volunnear.scoring.half-distance-km=10");

    @Test
    void validWeightsStart() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            VolunnearProperties.Scoring scoring = context.getBean(VolunnearProperties.class).scoring();
            assertThat(scoring.weights().alpha()).isEqualTo(0.4);
            // lambda = ln 2 / 10 km
            assertThat(scoring.weights().lambda()).isCloseTo(0.0693147, offset(1e-6));
        });
    }

    @Test
    void weightsThatDoNotSumToOneStopTheStart() {
        runner.withPropertyValues("volunnear.scoring.gamma=0.3")
                .run(context -> assertThat(context).getFailure()
                        .rootCause().hasMessageContaining("alpha + beta + gamma must be 1"));
    }

    @Test
    void negativeWeightStopsTheStart() {
        runner.withPropertyValues("volunnear.scoring.alpha=-0.2", "volunnear.scoring.beta=1.0")
                .run(context -> assertThat(context).getFailure()
                        .rootCause().hasMessageContaining("alpha must not be negative"));
    }

    @Test
    void halfDistanceMustBePositive() {
        runner.withPropertyValues("volunnear.scoring.half-distance-km=0")
                .run(context -> assertThat(context).getFailure()
                        .rootCause().hasMessageContaining("Half-distance"));
    }

    @Test
    void missingWeightStopsTheStart() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
                .withUserConfiguration(TimeConfig.class)
                .withPropertyValues("volunnear.time-zone=Europe/Prague", "volunnear.scoring.alpha=0.4")
                .run(context -> assertThat(context).hasFailed());
    }
}
