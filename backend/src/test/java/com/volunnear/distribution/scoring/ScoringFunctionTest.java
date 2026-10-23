package com.volunnear.distribution.scoring;

import com.volunnear.activity.Priority;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Expected values are computed by hand from docs/FUNCTION.md; the arithmetic is in the comments.
 */
class ScoringFunctionTest {
    private static final double HAND = 1e-4;   // hand-computed values are given to four decimals
    private static final double EXACT = 1e-12;

    private static final long FIRST_AID = 1L;
    private static final long DRIVING = 2L;
    private static final long COOKING = 3L;
    private static final long IT = 4L;

    private static Map<Long, Double> skills(long... ids) {
        Map<Long, Double> vector = new HashMap<>();
        for (long id : ids) {
            vector.put(id, 1.0);
        }
        return vector;
    }

    @Nested
    class ReferenceCase {
        private final ScoringWeights weights = new ScoringWeights(0.4, 0.4, 0.2, 0.1);

        @Test
        void componentsAndTotalMatchTheHandComputation() {
            // u_geo: lambda = 0.1 /km, d = 5 km -> e^-0.5 = 0.6065
            double geo = ScoringFunction.uGeo(5, 0.1);
            // u_skill: S_v = (1,1,0), S_t = (1,0,1) -> projected S_v = (1,0,0)
            //          dot = 1, |S_v~| = 1, |S_t| = sqrt(2) -> 1/sqrt(2) = 0.7071
            double skill = ScoringFunction.uSkill(skills(1, 2), skills(1, 3));
            // u_prio: HIGH = 3 on the scale 1..4 -> (3 - 1) / (4 - 1) = 0.6667
            double prio = ScoringFunction.uPrio(Priority.HIGH);

            assertEquals(0.6065, geo, HAND);
            assertEquals(0.7071, skill, HAND);
            assertEquals(0.6667, prio, HAND);

            // R = 0.4 * 0.6065 + 0.4 * 0.7071 + 0.2 * 0.6667 = 0.2426 + 0.2828 + 0.1333 = 0.6588
            ScoreBreakdown breakdown = ScoringFunction.combine(geo, skill, prio, weights);
            assertEquals(0.6588, breakdown.total(), HAND);
        }

        @Test
        void scoreFromPositionsGivesTheSameResultAndCarriesTheWeights() {
            // Two points on the equator 5 km apart: d = R * dLon, so dLon = 5 / 6371 rad = 0.0449661 degrees
            GeoPoint volunteer = new GeoPoint(0, 0);
            GeoPoint activity = new GeoPoint(0, 0.0449661);

            ScoreBreakdown breakdown = ScoringFunction.score(
                    volunteer, activity, skills(1, 2), skills(1, 3), Priority.HIGH, weights);

            assertEquals(0.6065, breakdown.geo(), HAND);
            assertEquals(0.7071, breakdown.skill(), HAND);
            assertEquals(0.6667, breakdown.priority(), HAND);
            assertEquals(0.6588, breakdown.total(), HAND);
            assertSame(weights, breakdown.weights());
        }
    }

    @Nested
    class Haversine {
        @Test
        void identicalPointsAreZeroKmApart() {
            GeoPoint prague = new GeoPoint(50.0755, 14.4378);

            assertEquals(0.0, ScoringFunction.haversineKm(prague, prague), EXACT);
        }

        @Test
        void quarterOfTheEquator() {
            // 90 degrees of longitude on the equator: R * pi / 2 = 6371 * 1.570796 = 10007.54 km
            double distance = ScoringFunction.haversineKm(new GeoPoint(0, 0), new GeoPoint(0, 90));

            assertEquals(10007.54, distance, 0.01);
        }

        @Test
        void parisToLondonIsAbout344Km() {
            // published great-circle distance between the two city centres is about 344 km
            double distance = ScoringFunction.haversineKm(
                    new GeoPoint(48.8566, 2.3522), new GeoPoint(51.5074, -0.1278));

            assertEquals(344, distance, 1.0);
        }

        @Test
        void distanceDoesNotDependOnDirection() {
            GeoPoint a = new GeoPoint(48.8566, 2.3522);
            GeoPoint b = new GeoPoint(51.5074, -0.1278);

            assertEquals(ScoringFunction.haversineKm(a, b), ScoringFunction.haversineKm(b, a), EXACT);
        }
    }

    @Nested
    class GeoUtility {
        @Test
        void zeroDistanceGivesOne() {
            assertEquals(1.0, ScoringFunction.uGeo(0, 0.1), EXACT);
        }

        @Test
        void veryLargeDistanceIsCloseToZeroAndNotNegative() {
            double halfwayAroundTheEarth = ScoringFunction.uGeo(20_000, 0.1);
            double beyondDoublePrecision = ScoringFunction.uGeo(1e9, 0.1);

            assertTrue(halfwayAroundTheEarth >= 0 && halfwayAroundTheEarth < 1e-12);
            assertEquals(0.0, beyondDoublePrecision);
        }

        @Test
        void utilityFallsAsDistanceGrows() {
            assertTrue(ScoringFunction.uGeo(1, 0.1) > ScoringFunction.uGeo(2, 0.1));
        }

        @Test
        void volunteerWithoutLocationGetsZero() {
            assertEquals(0.0, ScoringFunction.uGeo(null, new GeoPoint(50, 14), 0.1));
        }

        @Test
        void activityWithoutLocationGetsZero() {
            assertEquals(0.0, ScoringFunction.uGeo(new GeoPoint(50, 14), null, 0.1));
        }

        @Test
        void rejectsNegativeDistanceAndNonPositiveLambda() {
            assertThrows(IllegalArgumentException.class, () -> ScoringFunction.uGeo(-1, 0.1));
            assertThrows(IllegalArgumentException.class, () -> ScoringFunction.uGeo(1, 0));
        }
    }

    @Nested
    class SkillUtility {
        @Test
        void identicalVectorsGiveOne() {
            assertEquals(1.0, ScoringFunction.uSkill(skills(1, 2, 3), skills(1, 2, 3)), EXACT);
        }

        @Test
        void disjointVectorsGiveZero() {
            assertEquals(0.0, ScoringFunction.uSkill(skills(1, 2), skills(3, 4)), EXACT);
        }

        @Test
        void activityThatRequiresNoSkillsGivesOne() {
            assertEquals(1.0, ScoringFunction.uSkill(skills(1, 2), Map.of()), EXACT);
            assertEquals(1.0, ScoringFunction.uSkill(Map.of(), Map.of()), EXACT);
        }

        @Test
        void volunteerWithNoneOfTheRequiredSkillsGetsZero() {
            assertEquals(0.0, ScoringFunction.uSkill(Map.of(), skills(1, 2)), EXACT);
        }

        @Test
        void extraSkillsDoNotLowerTheScore() {
            // ANALYSIS 3.2: the activity needs {first aid}. Plain cosine would give the second volunteer 0.5.
            Map<Long, Double> required = skills(FIRST_AID);

            assertEquals(1.0, ScoringFunction.uSkill(skills(FIRST_AID), required), EXACT);
            assertEquals(1.0, ScoringFunction.uSkill(skills(FIRST_AID, DRIVING, COOKING, IT), required), EXACT);
        }

        @Test
        void oneOfFourRequiredSkillsGivesOneHalf() {
            // binary vectors: sqrt(k / n) = sqrt(1 / 4) = 0.5
            assertEquals(0.5, ScoringFunction.uSkill(skills(1), skills(1, 2, 3, 4)), EXACT);
        }

        @Test
        void twoOfThreeRequiredSkillsGiveSquareRootOfTwoThirds() {
            // sqrt(2 / 3) = 0.8165
            assertEquals(0.8165, ScoringFunction.uSkill(skills(1, 2), skills(1, 2, 3)), HAND);
        }

        @Test
        void weightedVectorsUseTheProjectedCosine() {
            // The volunteer has levels 3 and 4 on the two required skills and 5 on a skill that is not required.
            // S_v~ = (3, 4), S_t = (4, 3): dot = 12 + 12 = 24, |S_v~| = 5, |S_t| = 5 -> 24 / 25 = 0.96
            Map<Long, Double> volunteer = Map.of(1L, 3.0, 2L, 4.0, 9L, 5.0);
            Map<Long, Double> required = Map.of(1L, 4.0, 2L, 3.0);

            assertEquals(0.96, ScoringFunction.uSkill(volunteer, required), EXACT);
        }

        @Test
        void requiredSkillWithWeightZeroIsNotRequired() {
            assertEquals(1.0, ScoringFunction.uSkill(skills(1), Map.of(1L, 1.0, 2L, 0.0)), EXACT);
        }

        @Test
        void rejectsNegativeSkillWeight() {
            assertThrows(IllegalArgumentException.class,
                    () -> ScoringFunction.uSkill(Map.of(1L, -1.0), skills(1)));
            assertThrows(IllegalArgumentException.class,
                    () -> ScoringFunction.uSkill(skills(1), Map.of(1L, -1.0)));
        }
    }

    @Nested
    class PriorityUtility {
        @Test
        void eachPriorityMapsOntoTheFixedScale() {
            // (P - 1) / (4 - 1) for P = 1, 2, 3, 4
            assertEquals(0.0, ScoringFunction.uPrio(Priority.LOW), EXACT);
            assertEquals(1.0 / 3, ScoringFunction.uPrio(Priority.MEDIUM), EXACT);
            assertEquals(2.0 / 3, ScoringFunction.uPrio(Priority.HIGH), EXACT);
            assertEquals(1.0, ScoringFunction.uPrio(Priority.URGENT), EXACT);
        }
    }

    @Nested
    class Aggregate {
        // u_geo = e^-0.5 = 0.6065, u_skill = 0.5, u_prio = 1
        private final GeoPoint volunteer = new GeoPoint(0, 0);
        private final GeoPoint activity = new GeoPoint(0, 0.0449661);
        private final Map<Long, Double> volunteerSkills = skills(1);
        private final Map<Long, Double> requiredSkills = skills(1, 2, 3, 4);

        private ScoreBreakdown scoreWith(double alpha, double beta, double gamma) {
            return ScoringFunction.score(volunteer, activity, volunteerSkills, requiredSkills, Priority.URGENT,
                    new ScoringWeights(alpha, beta, gamma, 0.1));
        }

        @Test
        void allWeightOnGeoReturnsTheGeoComponent() {
            assertEquals(0.6065, scoreWith(1, 0, 0).total(), HAND);
        }

        @Test
        void allWeightOnSkillReturnsTheSkillComponent() {
            assertEquals(0.5, scoreWith(0, 1, 0).total(), EXACT);
        }

        @Test
        void allWeightOnPriorityReturnsThePriorityComponent() {
            assertEquals(1.0, scoreWith(0, 0, 1).total(), EXACT);
        }

        @Test
        void breakdownAlwaysCarriesEveryComponent() {
            ScoreBreakdown breakdown = scoreWith(0, 0, 1);

            assertEquals(0.6065, breakdown.geo(), HAND);
            assertEquals(0.5, breakdown.skill(), EXACT);
            assertEquals(1.0, breakdown.priority(), EXACT);
            assertEquals(new ScoringWeights(0, 0, 1, 0.1), breakdown.weights());
        }

        @Test
        void volunteerWithoutLocationIsStillScored() {
            // u_geo = 0, u_skill = 0.5, u_prio = 1 -> 0.4 * 0 + 0.4 * 0.5 + 0.2 * 1 = 0.4
            ScoreBreakdown breakdown = ScoringFunction.score(null, activity, volunteerSkills, requiredSkills,
                    Priority.URGENT, new ScoringWeights(0.4, 0.4, 0.2, 0.1));

            assertEquals(0.0, breakdown.geo());
            assertEquals(0.4, breakdown.total(), EXACT);
        }

        @Test
        void combineRejectsComponentOutsideTheUnitInterval() {
            ScoringWeights weights = new ScoringWeights(0.4, 0.4, 0.2, 0.1);

            assertThrows(IllegalArgumentException.class, () -> ScoringFunction.combine(1.1, 0.5, 0.5, weights));
            assertThrows(IllegalArgumentException.class, () -> ScoringFunction.combine(0.5, -0.1, 0.5, weights));
        }
    }

    @Test
    void everyComponentAndTheTotalStayInTheUnitIntervalForRandomInputs() {
        Random random = new Random(20261008L); // fixed seed: a failure is reproducible
        Priority[] priorities = Priority.values();

        for (int i = 0; i < 20_000; i++) {
            // a random point of the simplex: cut [0, 1] at two places
            double cut1 = random.nextDouble();
            double cut2 = random.nextDouble();
            double low = Math.min(cut1, cut2);
            double high = Math.max(cut1, cut2);
            ScoringWeights weights = new ScoringWeights(low, high - low, 1 - high, 1e-4 + random.nextDouble() * 5);

            ScoreBreakdown breakdown = ScoringFunction.score(
                    randomPointOrNull(random), randomPointOrNull(random),
                    randomSkills(random), randomSkills(random),
                    priorities[random.nextInt(priorities.length)], weights);

            String input = "iteration " + i + ": " + breakdown;
            assertTrue(breakdown.geo() >= 0 && breakdown.geo() <= 1, input);
            assertTrue(breakdown.skill() >= 0 && breakdown.skill() <= 1, input);
            assertTrue(breakdown.priority() >= 0 && breakdown.priority() <= 1, input);
            assertTrue(breakdown.total() >= 0 && breakdown.total() <= 1, input);
        }
    }

    private static GeoPoint randomPointOrNull(Random random) {
        if (random.nextInt(10) == 0) {
            return null;
        }
        return new GeoPoint(random.nextDouble() * 180 - 90, random.nextDouble() * 360 - 180);
    }

    private static Map<Long, Double> randomSkills(Random random) {
        Map<Long, Double> vector = new HashMap<>();
        int size = random.nextInt(6);
        for (int i = 0; i < size; i++) {
            // weights include 0 and values above 1, so levels are covered as well as the binary case
            vector.put((long) random.nextInt(8), random.nextInt(4) == 0 ? 0.0 : random.nextDouble() * 5);
        }
        return vector;
    }
}
