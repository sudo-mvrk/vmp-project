package com.volunnear.distribution.ranking;

import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.Priority;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.GeoPoint;
import com.volunnear.distribution.scoring.ScoreBreakdown;
import com.volunnear.distribution.scoring.ScoringWeights;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.volunnear.distribution.Profiles.*;
import static org.junit.jupiter.api.Assertions.*;

class RankerTest {
    private static final ScoringWeights WEIGHTS = new ScoringWeights(0.4, 0.4, 0.2, 0.1);
    /** With all weight on priority every volunteer of one activity has the same total, so only ties decide. */
    private static final ScoringWeights ONLY_PRIORITY = new ScoringWeights(0, 0, 1, 0.1);
    /** About 111 km north of Prague: u_geo is much lower than at the activity itself. */
    private static final GeoPoint FAR = new GeoPoint(51.0755, 14.4378);

    private static List<Long> ids(List<RankedCandidate> ranked) {
        return ranked.stream().map(RankedCandidate::id).toList();
    }

    @Nested
    class Volunteers {
        /** Monday 10:00-14:00 in Prague, needs skills 1 and 2, priority HIGH. */
        private final ActivityProfile activity = activity(100).requiredSkills(1, 2).priority(Priority.HIGH).build();

        @Test
        void ordersByTotalAndReturnsTheBreakdown() {
            // All three are at the activity's location: u_geo = 1. u_prio = 2/3 for HIGH.
            // both skills: u_skill = 1          -> 0.4 + 0.4         + 0.2 * 2/3 = 0.9333
            // one of two:  u_skill = sqrt(1/2)  -> 0.4 + 0.4 * 0.7071 + 0.1333   = 0.8162
            // no skills:   u_skill = 0          -> 0.4 + 0           + 0.1333   = 0.5333
            List<VolunteerProfile> volunteers = List.of(
                    volunteer(1).build(),
                    volunteer(2).skills(1).build(),
                    volunteer(3).skills(1, 2).build());

            List<RankedCandidate> ranked = Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 10);

            assertEquals(List.of(3L, 2L, 1L), ids(ranked));
            assertEquals(0.9333, ranked.get(0).score().total(), 1e-4);
            assertEquals(0.8162, ranked.get(1).score().total(), 1e-4);
            assertEquals(0.5333, ranked.get(2).score().total(), 1e-4);
            ScoreBreakdown best = ranked.get(0).score();
            assertEquals(1.0, best.geo(), 1e-12);
            assertEquals(1.0, best.skill(), 1e-12);
            assertEquals(2.0 / 3, best.priority(), 1e-12);
            assertSame(WEIGHTS, best.weights());
        }

        @Test
        void volunteerFailingAnyOnePredicateNeverAppears() {
            List<VolunteerProfile> volunteers = List.of(
                    volunteer(1).skills(1, 2).availability(slot(DayOfWeek.TUESDAY, "08:00", "18:00")).build(),
                    volunteer(2).skills(1, 2).maxActiveAssignments(0).build(),
                    volunteer(3).skills(1, 2).banned().build(),
                    volunteer(4).skills(1, 2).appliedTo(100).build(),
                    volunteer(5).build());

            ActivityProfile needsCertification = activity(100).requiredSkills(1, 2).requiredCertifications(10).build();

            // volunteer 5 has no skills and the lowest score, but is the only one who passes every check
            assertEquals(List.of(5L), ids(Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 10)));
            // and nobody holds the certification
            assertEquals(List.of(), ids(Ranker.rankVolunteers(needsCertification, volunteers, WEIGHTS, 10)));
        }

        @Test
        void closedOrFullActivityHasNoCandidates() {
            List<VolunteerProfile> volunteers = List.of(volunteer(1).skills(1, 2).build());

            assertTrue(Ranker.rankVolunteers(
                    activity(100).status(ActivityStatus.DRAFT).build(), volunteers, WEIGHTS, 10).isEmpty());
            assertTrue(Ranker.rankVolunteers(activity(100).freeSpots(0).build(), volunteers, WEIGHTS, 10).isEmpty());
        }

        @Test
        void emptyInputGivesAnEmptyList() {
            assertTrue(Ranker.rankVolunteers(activity, List.of(), WEIGHTS, 10).isEmpty());
        }

        @Test
        void listIsCutToTheLimit() {
            List<VolunteerProfile> volunteers = List.of(
                    volunteer(1).build(),
                    volunteer(2).skills(1).build(),
                    volunteer(3).skills(1, 2).build());

            assertEquals(List.of(3L, 2L), ids(Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 2)));
            assertEquals(List.of(3L), ids(Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 1)));
            assertTrue(Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 0).isEmpty());
        }

        @Test
        void negativeLimitIsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> Ranker.rankVolunteers(activity, List.of(), WEIGHTS, -1));
        }

        @Test
        void volunteerWithoutLocationIsListedWithGeoZero() {
            List<RankedCandidate> ranked = Ranker.rankVolunteers(
                    activity, List.of(volunteer(1).location(null).skills(1, 2).build()), WEIGHTS, 10);

            assertEquals(1, ranked.size());
            assertEquals(0.0, ranked.get(0).score().geo());
        }
    }

    @Nested
    class TieBreak {
        private final ActivityProfile activity = activity(100).requiredSkills(1, 2).priority(Priority.HIGH).build();

        @Test
        void equalTotalIsDecidedByLargerSkill() {
            VolunteerProfile oneSkill = volunteer(1).skills(1).build();
            VolunteerProfile bothSkills = volunteer(2).skills(1, 2).build();

            List<RankedCandidate> ranked = Ranker.rankVolunteers(activity, List.of(oneSkill, bothSkills), ONLY_PRIORITY, 10);

            assertEquals(ranked.get(0).score().total(), ranked.get(1).score().total());
            assertEquals(List.of(2L, 1L), ids(ranked));
        }

        @Test
        void equalTotalAndSkillIsDecidedByLargerGeo() {
            VolunteerProfile far = volunteer(1).skills(1, 2).location(FAR).build();
            VolunteerProfile near = volunteer(2).skills(1, 2).build();

            List<RankedCandidate> ranked = Ranker.rankVolunteers(activity, List.of(far, near), ONLY_PRIORITY, 10);

            assertEquals(ranked.get(0).score().skill(), ranked.get(1).score().skill());
            assertEquals(List.of(2L, 1L), ids(ranked));
        }

        @Test
        void skillIsComparedBeforeGeo() {
            VolunteerProfile nearWithOneSkill = volunteer(1).skills(1).build();
            VolunteerProfile farWithBothSkills = volunteer(2).skills(1, 2).location(FAR).build();

            List<RankedCandidate> ranked = Ranker.rankVolunteers(
                    activity, List.of(nearWithOneSkill, farWithBothSkills), ONLY_PRIORITY, 10);

            assertEquals(List.of(2L, 1L), ids(ranked));
        }

        @Test
        void fullyEqualScoresAreOrderedByLowerId() {
            List<VolunteerProfile> volunteers = List.of(
                    volunteer(30).skills(1, 2).build(),
                    volunteer(10).skills(1, 2).build(),
                    volunteer(20).skills(1, 2).build());

            assertEquals(List.of(10L, 20L, 30L), ids(Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 10)));
        }

        @Test
        void orderDoesNotDependOnTheInputOrder() {
            List<VolunteerProfile> volunteers = new ArrayList<>(List.of(
                    volunteer(1).skills(1).build(),
                    volunteer(2).skills(1, 2).build(),
                    volunteer(3).skills(1, 2).location(FAR).build(),
                    volunteer(4).skills(1, 2).build(),
                    volunteer(5).build()));
            List<Long> expected = ids(Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 10));

            Random random = new Random(7);
            for (int i = 0; i < 50; i++) {
                Collections.shuffle(volunteers, random);
                assertEquals(expected, ids(Ranker.rankVolunteers(activity, volunteers, WEIGHTS, 10)));
            }
        }

        @Test
        void comparatorOrdersHandBuiltScores() {
            // total, then skill, then geo, then id; the breakdowns are written out instead of computed
            RankedCandidate highestTotal = new RankedCandidate(9, new ScoreBreakdown(0.1, 0.1, 0.5, 0.9, WEIGHTS));
            RankedCandidate betterSkill = new RankedCandidate(8, new ScoreBreakdown(0.2, 0.8, 0.5, 0.6, WEIGHTS));
            RankedCandidate betterGeo = new RankedCandidate(7, new ScoreBreakdown(0.9, 0.5, 0.5, 0.6, WEIGHTS));
            RankedCandidate lowerId = new RankedCandidate(5, new ScoreBreakdown(0.3, 0.5, 0.5, 0.6, WEIGHTS));
            RankedCandidate higherId = new RankedCandidate(6, new ScoreBreakdown(0.3, 0.5, 0.5, 0.6, WEIGHTS));

            List<RankedCandidate> sorted = new ArrayList<>(List.of(higherId, lowerId, betterGeo, betterSkill, highestTotal));
            sorted.sort(Ranker.ORDER);

            assertEquals(List.of(9L, 8L, 7L, 5L, 6L), ids(sorted));
        }
    }

    @Nested
    class Activities {
        private final VolunteerProfile volunteer = volunteer(1).skills(1).build();

        @Test
        void priorityChangesTheOrderOfActivitiesForAVolunteer() {
            // Same place and same required skill, so u_geo = 1 and u_skill = 1 for both.
            // URGENT: 0.4 + 0.4 + 0.2 * 1 = 1.0;  LOW: 0.4 + 0.4 + 0.2 * 0 = 0.8
            List<ActivityProfile> activities = List.of(
                    activity(100).requiredSkills(1).priority(Priority.LOW).build(),
                    activity(200).requiredSkills(1).priority(Priority.URGENT).build());

            List<RankedCandidate> ranked = Ranker.rankActivities(volunteer, activities, WEIGHTS, 10);

            assertEquals(List.of(200L, 100L), ids(ranked));
            assertEquals(1.0, ranked.get(0).score().total(), 1e-12);
            assertEquals(0.8, ranked.get(1).score().total(), 1e-12);
        }

        @Test
        void activitiesThatDoNotMatchAreAbsent() {
            List<ActivityProfile> activities = List.of(
                    activity(100).build(),
                    activity(200).status(ActivityStatus.CANCELLED).build(),
                    activity(300).freeSpots(0).build(),
                    activity(400).requiredCertifications(10).build(),
                    activity(500).window(window(MONDAY.plusDays(1), "10:00", "14:00")).build());

            assertEquals(List.of(100L), ids(Ranker.rankActivities(volunteer, activities, WEIGHTS, 10)));
        }

        @Test
        void equalActivitiesAreOrderedByLowerIdAndCutToTheLimit() {
            List<ActivityProfile> activities = List.of(activity(300).build(), activity(100).build(), activity(200).build());

            assertEquals(List.of(100L, 200L), ids(Ranker.rankActivities(volunteer, activities, WEIGHTS, 2)));
        }
    }
}
