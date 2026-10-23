package com.volunnear.distribution.scoring;

import com.volunnear.activity.Priority;

import java.util.Map;

/**
 * The scoring function of docs/FUNCTION.md. Pure: no database, no Spring, no state.
 * Skill vectors are maps from skill id to a non-negative weight; a skill that is absent has weight 0.
 */
public final class ScoringFunction {
    /** Mean Earth radius in km, used by the haversine distance. */
    public static final double EARTH_RADIUS_KM = 6371.0;
    /** Bounds of the priority scale (FUNCTION.md 1.3): LOW = 1 ... URGENT = 4. */
    public static final int PRIORITY_MIN = 1;
    public static final int PRIORITY_MAX = 4;

    private ScoringFunction() {
    }

    /**
     * R(v,t) with its breakdown (FUNCTION.md section 3).
     *
     * @param volunteerLocation may be null: a volunteer without a location gets u_geo = 0
     * @param activityLocation  may be null, with the same effect
     */
    public static ScoreBreakdown score(GeoPoint volunteerLocation,
                                       GeoPoint activityLocation,
                                       Map<Long, Double> volunteerSkills,
                                       Map<Long, Double> requiredSkills,
                                       Priority priority,
                                       ScoringWeights weights) {
        double geo = uGeo(volunteerLocation, activityLocation, weights.lambda());
        double skill = uSkill(volunteerSkills, requiredSkills);
        double prio = uPrio(priority);
        return combine(geo, skill, prio, weights);
    }

    /** Aggregate score: alpha * geo + beta * skill + gamma * priority. */
    public static ScoreBreakdown combine(double geo, double skill, double priority, ScoringWeights weights) {
        requireUnitInterval("geo", geo);
        requireUnitInterval("skill", skill);
        requireUnitInterval("priority", priority);
        double total = weights.alpha() * geo + weights.beta() * skill + weights.gamma() * priority;
        return new ScoreBreakdown(geo, skill, priority, clamp(total), weights);
    }

    /** Great-circle distance in km (haversine formula). */
    public static double haversineKm(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        // rounding can push h slightly above 1 for antipodal points
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(Math.min(1.0, h)));
    }

    /** u_geo = exp(-lambda * d) for a distance in km (FUNCTION.md 1.1). */
    public static double uGeo(double distanceKm, double lambda) {
        if (!(distanceKm >= 0)) {
            throw new IllegalArgumentException("Distance must not be negative, got " + distanceKm);
        }
        if (!(lambda > 0)) {
            throw new IllegalArgumentException("lambda must be greater than 0, got " + lambda);
        }
        return clamp(Math.exp(-lambda * distanceKm));
    }

    /** u_geo from two positions. A missing position gives 0. */
    public static double uGeo(GeoPoint volunteerLocation, GeoPoint activityLocation, double lambda) {
        if (volunteerLocation == null || activityLocation == null) {
            return 0.0;
        }
        return uGeo(haversineKm(volunteerLocation, activityLocation), lambda);
    }

    /**
     * u_skill: cosine between the required skills and the volunteer's skills restricted to the required ones
     * (FUNCTION.md 1.2). Skills the activity does not require are ignored, so they never lower the score.
     * No skills required gives 1; none of the required skills gives 0.
     */
    public static double uSkill(Map<Long, Double> volunteerSkills, Map<Long, Double> requiredSkills) {
        double dot = 0;
        double volunteerNormSquared = 0;
        double requiredNormSquared = 0;
        for (Map.Entry<Long, Double> required : requiredSkills.entrySet()) {
            double st = requireSkillWeight(required.getValue());
            if (st == 0) {
                continue;
            }
            double sv = requireSkillWeight(volunteerSkills.getOrDefault(required.getKey(), 0.0));
            dot += sv * st;
            volunteerNormSquared += sv * sv;
            requiredNormSquared += st * st;
        }
        if (requiredNormSquared == 0) {
            return 1.0;
        }
        if (volunteerNormSquared == 0) {
            return 0.0;
        }
        return clamp(dot / (Math.sqrt(volunteerNormSquared) * Math.sqrt(requiredNormSquared)));
    }

    /** u_prio = (P - P_min) / (P_max - P_min) on the fixed scale 1..4 (FUNCTION.md 1.3). */
    public static double uPrio(Priority priority) {
        return (double) (priorityValue(priority) - PRIORITY_MIN) / (PRIORITY_MAX - PRIORITY_MIN);
    }

    /** Numeric value P(t) of a priority. Written out so that reordering the enum cannot change scores. */
    public static int priorityValue(Priority priority) {
        return switch (priority) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case URGENT -> 4;
        };
    }

    private static double requireSkillWeight(Double weight) {
        if (weight == null || !(weight >= 0) || weight.isInfinite()) {
            throw new IllegalArgumentException("Skill weight must be a finite number that is not negative, got " + weight);
        }
        return weight;
    }

    private static void requireUnitInterval(String name, double value) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException(name + " must be in [0, 1], got " + value);
        }
    }

    /** Floating point can give 1.0000000000000002; the result must stay in [0, 1]. */
    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
