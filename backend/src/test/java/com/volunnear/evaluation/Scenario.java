package com.volunnear.evaluation;

import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.scoring.ScoringWeights;

import java.util.List;
import java.util.Map;

/**
 * The initial state of one evaluation: volunteers and open upcoming activities as loaded from the database,
 * the global weights and the per-activity weight overrides (Q12).
 */
record Scenario(List<SimVolunteer> volunteers,
                List<ActivityProfile> activities,
                ScoringWeights weights,
                Map<Long, ScoringWeights> overrides) {
    Scenario {
        volunteers = List.copyOf(volunteers);
        activities = List.copyOf(activities);
        overrides = Map.copyOf(overrides);
    }

    /** The weights for ranking volunteers of one activity: its override if it has one (as the endpoint does). */
    ScoringWeights weightsFor(long activityId) {
        return overrides.getOrDefault(activityId, weights);
    }

    Scenario withWeights(ScoringWeights newWeights) {
        return new Scenario(volunteers, activities, newWeights, overrides);
    }

    int spotsToFill() {
        return activities.stream().mapToInt(ActivityProfile::freeSpots).sum();
    }
}
