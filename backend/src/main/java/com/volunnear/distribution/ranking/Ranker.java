package com.volunnear.distribution.ranking;

import com.volunnear.distribution.feasibility.Feasibility;
import com.volunnear.distribution.feasibility.PlatformEligibility;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.ScoreBreakdown;
import com.volunnear.distribution.scoring.ScoringFunction;
import com.volunnear.distribution.scoring.ScoringWeights;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The constrained argmax of docs/FUNCTION.md section 4 as a ranked top-N list: drop every pair that is
 * not feasible or not eligible, score the rest, order them and cut the list. The first element is v*.
 */
public final class Ranker {
    /**
     * Highest total first. Ties: larger u_skill, then larger u_geo, then lower id, so the order of equal
     * scores is always the same.
     */
    static final Comparator<RankedCandidate> ORDER = Comparator
            .comparingDouble((RankedCandidate candidate) -> candidate.score().total()).reversed()
            .thenComparing(Comparator.comparingDouble((RankedCandidate candidate) -> candidate.score().skill()).reversed())
            .thenComparing(Comparator.comparingDouble((RankedCandidate candidate) -> candidate.score().geo()).reversed())
            .thenComparingLong(RankedCandidate::id);

    private Ranker() {
    }

    /** The best volunteers for one activity. */
    public static List<RankedCandidate> rankVolunteers(ActivityProfile activity,
                                                       Collection<VolunteerProfile> volunteers,
                                                       ScoringWeights weights,
                                                       int limit) {
        return top(volunteers.stream()
                .filter(volunteer -> matches(volunteer, activity))
                .map(volunteer -> new RankedCandidate(volunteer.id(), score(volunteer, activity, weights))), limit);
    }

    /** The best activities for one volunteer: the same score and the same conditions, seen from the other side. */
    public static List<RankedCandidate> rankActivities(VolunteerProfile volunteer,
                                                       Collection<ActivityProfile> activities,
                                                       ScoringWeights weights,
                                                       int limit) {
        return top(activities.stream()
                .filter(activity -> matches(volunteer, activity))
                .map(activity -> new RankedCandidate(activity.id(), score(volunteer, activity, weights))), limit);
    }

    private static boolean matches(VolunteerProfile volunteer, ActivityProfile activity) {
        return PlatformEligibility.platformEligible(volunteer, activity) && Feasibility.feasible(volunteer, activity);
    }

    private static ScoreBreakdown score(VolunteerProfile volunteer, ActivityProfile activity, ScoringWeights weights) {
        return ScoringFunction.score(volunteer.location(), activity.location(),
                volunteer.skills(), activity.requiredSkills(), activity.priority(), weights);
    }

    private static List<RankedCandidate> top(Stream<RankedCandidate> candidates, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must not be negative, got " + limit);
        }
        return candidates.sorted(ORDER).limit(limit).toList();
    }
}
