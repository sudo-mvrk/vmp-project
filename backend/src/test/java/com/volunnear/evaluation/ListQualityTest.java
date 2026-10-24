package com.volunnear.evaluation;

import com.volunnear.activity.Priority;
import com.volunnear.distribution.scoring.ScoringFunction;
import com.volunnear.evaluation.ListQuality.Ordering;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.volunnear.distribution.Profiles.PRAGUE;
import static com.volunnear.distribution.Profiles.activity;
import static com.volunnear.distribution.Profiles.volunteer;
import static com.volunnear.evaluation.SimulationTest.east;
import static com.volunnear.evaluation.SimulationTest.scenario;
import static com.volunnear.evaluation.SimulationTest.sim;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ListQualityTest {
    private static final double EPS = 1e-9;

    @Test
    void valuesOfOneList() {
        SimVolunteer volunteer = sim(volunteer(1).skills(1).build());
        // coverage 1/2 at 0 km, HIGH; no skills required at 4 km, LOW
        ListQuality.Values values = ListQuality.values(volunteer, List.of(
                activity(1).requiredSkills(1, 2).priority(Priority.HIGH).build(),
                activity(2).location(east(4)).priority(Priority.LOW).build()));

        assertThat(values.skillCoverage()).isCloseTo(0.5, within(EPS));
        assertThat(values.meanDistanceKm()).isCloseTo(ScoringFunction.haversineKm(east(4), PRAGUE) / 2, within(EPS));
        assertThat(values.highUrgentShare()).isCloseTo(0.5, within(EPS));
    }

    @Test
    void nearestAndRecommendedListsTakeTheirTopK() {
        // Activity 1 at 1 km needs skill 9 (missing); 2 at 3 km needs skill 1 (held); 3 at 20 km needs nothing.
        // k = 1: nearest is activity 1; recommended is activity 2 (R 0.4 * 0.81 + 0.4 > 0.4 * 0.93 + 0).
        Scenario scenario = scenario(List.of(sim(volunteer(1).skills(1).build())), List.of(
                activity(1).location(east(1)).requiredSkills(9).build(),
                activity(2).location(east(3)).requiredSkills(1).build(),
                activity(3).location(east(20)).build()));

        Map<Ordering, ListQuality.Values> measured = ListQuality.measure(scenario, 1, new Random(3));

        assertThat(measured.get(Ordering.NEAREST).skillCoverage()).isZero();
        assertThat(measured.get(Ordering.NEAREST).meanDistanceKm())
                .isCloseTo(ScoringFunction.haversineKm(east(1), PRAGUE), within(EPS));
        assertThat(measured.get(Ordering.RECOMMENDED).skillCoverage()).isEqualTo(1.0);
        assertThat(measured.get(Ordering.RECOMMENDED).meanDistanceKm())
                .isCloseTo(ScoringFunction.haversineKm(east(3), PRAGUE), within(EPS));
    }

    @Test
    void aVolunteerWithoutFeasibleActivitiesIsLeftOut() {
        Scenario scenario = scenario(List.of(sim(volunteer(1).maxActiveAssignments(0).build())),
                List.of(activity(1).build()));

        Map<Ordering, ListQuality.Values> measured = ListQuality.measure(scenario, 5, new Random(3));

        assertThat(measured.get(Ordering.UNSORTED).highUrgentShare()).isNaN();
    }
}
