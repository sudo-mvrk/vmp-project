package com.volunnear.evaluation;

import com.volunnear.activity.Priority;
import com.volunnear.distribution.scoring.ScoringFunction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.volunnear.distribution.Profiles.PRAGUE;
import static com.volunnear.distribution.Profiles.activity;
import static com.volunnear.distribution.Profiles.volunteer;
import static com.volunnear.evaluation.SimulationTest.east;
import static com.volunnear.evaluation.SimulationTest.scenario;
import static com.volunnear.evaluation.SimulationTest.sim;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MetricsTest {
    private static final double EPS = 1e-9;

    /**
     * Activities: 1 HIGH with 2 spots needing skills {1, 2}; 2 LOW with 1 spot needing nothing; 3 URGENT with 1 spot
     * needing {3}. Volunteers 1 (skill 1, 4 km away), 2 (skills 1, 2, 2 km away), 3 (no location), 4 (unused).
     * Assignments: 1 and 2 to activity 1, 3 to activity 2; activity 3 stays empty.
     */
    @Test
    void handComputedExample() {
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).location(east(4)).skills(1).build()),
                        sim(volunteer(2).location(east(2)).skills(1, 2).build()),
                        sim(volunteer(3).location(null).build()),
                        sim(volunteer(4).build())),
                List.of(activity(1).priority(Priority.HIGH).freeSpots(2).requiredSkills(1, 2).build(),
                        activity(2).priority(Priority.LOW).build(),
                        activity(3).priority(Priority.URGENT).requiredSkills(3).build()));

        Metrics metrics = Metrics.of(scenario,
                List.of(new Assignment(1, 1), new Assignment(2, 1), new Assignment(3, 2)));

        double d1 = ScoringFunction.haversineKm(east(4), PRAGUE);
        double d2 = ScoringFunction.haversineKm(east(2), PRAGUE);
        assertThat(metrics.fillRate()).isCloseTo(3.0 / 4, within(EPS));
        assertThat(metrics.fullyStaffed()).isCloseTo(2.0 / 3, within(EPS));
        assertThat(metrics.empty()).isCloseTo(1.0 / 3, within(EPS));
        // only activity 1 requires skills: coverage 1/2 and 2/2
        assertThat(metrics.skillCoverage()).isCloseTo(0.75, within(EPS));
        assertThat(metrics.fullyQualified()).isCloseTo(0.5, within(EPS));
        // volunteer 3 has no location and is left out
        assertThat(metrics.meanDistanceKm()).isCloseTo((d1 + d2) / 2, within(EPS));
        assertThat(metrics.medianDistanceKm()).isCloseTo((d1 + d2) / 2, within(EPS));
        // (3 * 2 + 1 * 1 + 4 * 0) / (3 * 2 + 1 * 1 + 4 * 1)
        assertThat(metrics.priorityWeightedFill()).isCloseTo(7.0 / 11, within(EPS));
        // HIGH and URGENT: 2 of 3 spots
        assertThat(metrics.highUrgentFill()).isCloseTo(2.0 / 3, within(EPS));
        assertThat(metrics.participation()).isCloseTo(3.0 / 4, within(EPS));
    }

    @Test
    void withoutAssignmentsTheMetricsOnAssignmentsAreUndefined() {
        Scenario scenario = scenario(List.of(sim(volunteer(1).build())), List.of(activity(1).requiredSkills(1).build()));

        Metrics metrics = Metrics.of(scenario, List.of());

        assertThat(metrics.fillRate()).isZero();
        assertThat(metrics.empty()).isEqualTo(1.0);
        assertThat(metrics.skillCoverage()).isNaN();
        assertThat(metrics.meanDistanceKm()).isNaN();
        assertThat(metrics.medianDistanceKm()).isNaN();
        assertThat(metrics.participation()).isZero();
    }

    @Test
    void medianOfAnOddNumberIsTheMiddleValue() {
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).location(east(1)).build()), sim(volunteer(2).location(east(3)).build()),
                        sim(volunteer(3).location(east(8)).build())),
                List.of(activity(1).freeSpots(3).build()));

        Metrics metrics = Metrics.of(scenario,
                List.of(new Assignment(1, 1), new Assignment(2, 1), new Assignment(3, 1)));

        assertThat(metrics.medianDistanceKm()).isCloseTo(ScoringFunction.haversineKm(east(3), PRAGUE), within(EPS));
    }

    @Test
    void betterRespectsTheDirectionAndUndefinedValues() {
        assertThat(Metrics.Kind.M1.better(0.8, 0.7)).isTrue();
        assertThat(Metrics.Kind.M6.better(0.8, 0.7)).isFalse();
        assertThat(Metrics.Kind.M6.better(0.7, 0.8)).isTrue();
        assertThat(Metrics.Kind.M1.better(0.7, 0.7)).isFalse();
        assertThat(Metrics.Kind.M1.better(Double.NaN, 0.7)).isFalse();
    }
}
