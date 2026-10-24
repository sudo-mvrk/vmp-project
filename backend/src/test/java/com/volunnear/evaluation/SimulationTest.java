package com.volunnear.evaluation;

import com.volunnear.activity.Priority;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.scoring.GeoPoint;
import com.volunnear.distribution.scoring.ScoringWeights;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.volunnear.distribution.Profiles.MONDAY;
import static com.volunnear.distribution.Profiles.PRAGUE;
import static com.volunnear.distribution.Profiles.activity;
import static com.volunnear.distribution.Profiles.volunteer;
import static com.volunnear.distribution.Profiles.window;
import static org.assertj.core.api.Assertions.assertThat;

class SimulationTest {
    static final ScoringWeights WEIGHTS = ScoringWeights.ofHalfDistance(0.4, 0.4, 0.2, 10);

    /** A point the given distance east of Prague (close enough to the haversine distance for these tests). */
    static GeoPoint east(double km) {
        return new GeoPoint(PRAGUE.lat(), PRAGUE.lon() + km / (111.32 * Math.cos(Math.toRadians(PRAGUE.lat()))));
    }

    static Scenario scenario(List<SimVolunteer> volunteers, List<ActivityProfile> activities) {
        return new Scenario(volunteers, activities, WEIGHTS, Map.of());
    }

    static SimVolunteer sim(com.volunnear.distribution.model.VolunteerProfile profile) {
        return new SimVolunteer(profile, null);
    }

    private static List<Assignment> run(Method method, Scenario scenario, Long... order) {
        return Simulation.run(method, scenario, List.of(order), new Random(7));
    }

    @Test
    void nearestTakesTheClosestActivityAndSmartTheBestScoredOne() {
        // Activity 1 is here but needs skill 7, which the volunteer lacks: R = 0.4 * 1 + 0.4 * 0 + 0.2 / 3 = 0.467.
        // Activity 2 is 5 km away and needs skill 5, which the volunteer has: u_geo = 0.5^(5/10) = 0.707,
        // R = 0.4 * 0.707 + 0.4 * 1 + 0.2 / 3 = 0.750.
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).skills(5).build())),
                List.of(activity(1).requiredSkills(7).build(), activity(2).location(east(5)).requiredSkills(5).build()));

        assertThat(run(Method.NEAREST, scenario, 1L)).containsExactly(new Assignment(1, 1));
        assertThat(run(Method.SMART_A, scenario, 1L)).containsExactly(new Assignment(1, 2));
        // smart-B asks from the activity side: activity 1 (equal priority and start, lower id) is served first,
        // and the volunteer is its only candidate.
        assertThat(run(Method.SMART_B, scenario)).containsExactly(new Assignment(1, 1));
    }

    @Test
    void everyVolunteerChoosesOncePerRoundBeforeAnyoneChoosesAgain() {
        // Volunteer 1 has capacity 2 and is first, but takes only one activity in round 1; volunteer 2 takes the other.
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).maxActiveAssignments(2).build()), sim(volunteer(2).build())),
                List.of(activity(1).window(window(MONDAY, "08:00", "10:00")).build(),
                        activity(2).location(east(3)).window(window(MONDAY, "12:00", "14:00")).build()));

        assertThat(run(Method.NEAREST, scenario, 1L, 2L)).containsExactly(new Assignment(1, 1), new Assignment(2, 2));
    }

    @Test
    void aVolunteerWithFreeCapacityKeepsChoosingInLaterRounds() {
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).maxActiveAssignments(2).build())),
                List.of(activity(1).window(window(MONDAY, "08:00", "10:00")).build(),
                        activity(2).window(window(MONDAY, "12:00", "14:00")).build(),
                        // overlaps activity 1, so it stays empty
                        activity(3).window(window(MONDAY, "09:00", "11:00")).build()));

        assertThat(run(Method.NEAREST, scenario, 1L)).containsExactly(new Assignment(1, 1), new Assignment(1, 2));
    }

    @Test
    void smartBServesTheMoreUrgentActivityFirst() {
        // One volunteer, two activities of equal fit: the volunteer goes to the URGENT one although its id is higher.
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).build())),
                List.of(activity(1).priority(Priority.LOW).window(window(MONDAY, "08:00", "10:00")).build(),
                        activity(2).priority(Priority.URGENT).window(window(MONDAY, "12:00", "14:00")).build()));

        assertThat(run(Method.SMART_B, scenario)).containsExactly(new Assignment(1, 2));
    }

    @Test
    void smartBFillsAllSpotsOfAnActivityWithItsBestVolunteers() {
        // Two spots, three volunteers at 0, 2 and 6 km: the two closest are taken.
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).location(east(6)).build()), sim(volunteer(2).build()),
                        sim(volunteer(3).location(east(2)).build())),
                List.of(activity(1).freeSpots(2).build()));

        assertThat(run(Method.SMART_B, scenario)).containsExactly(new Assignment(2, 1), new Assignment(3, 1));
    }

    @Test
    void anActivityOutsideTheRadiusIsNeverTaken() {
        Scenario scenario = scenario(
                List.of(new SimVolunteer(volunteer(1).build(), 4)),
                List.of(activity(1).location(east(5)).build()));

        for (Method method : Method.values()) {
            assertThat(run(method, scenario, 1L)).as(method.label).isEmpty();
        }
    }

    @Test
    void theRadiusIsIgnoredWithoutALocation() {
        Scenario scenario = scenario(
                List.of(new SimVolunteer(volunteer(1).location(null).build(), 4)),
                List.of(activity(1).location(east(50)).build()));

        assertThat(run(Method.NEAREST, scenario, 1L)).containsExactly(new Assignment(1, 1));
    }

    @Test
    void randomIsReproducibleWithTheSameSeed() {
        List<ActivityProfile> activities = List.of(activity(1).freeSpots(5).build(), activity(2).freeSpots(5).build(),
                activity(3).freeSpots(5).build());
        List<SimVolunteer> volunteers = List.of(sim(volunteer(1).build()), sim(volunteer(2).build()),
                sim(volunteer(3).build()), sim(volunteer(4).build()));
        Scenario scenario = scenario(volunteers, activities);
        List<Long> order = List.of(3L, 1L, 4L, 2L);

        assertThat(Simulation.run(Method.RANDOM, scenario, order, new Random(11)))
                .hasSize(4)
                .isEqualTo(Simulation.run(Method.RANDOM, scenario, order, new Random(11)));
    }
}
