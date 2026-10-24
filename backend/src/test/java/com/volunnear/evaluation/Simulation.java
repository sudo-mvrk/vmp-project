package com.volunnear.evaluation;

import com.volunnear.distribution.feasibility.Feasibility;
import com.volunnear.distribution.feasibility.PlatformEligibility;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.TimeWindow;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.ranking.Ranker;
import com.volunnear.distribution.scoring.ScoringFunction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * One simulation run (METHOD.md section 4, Q42): assigns volunteers to activities by one method and returns
 * the assignments. Pure: works on a copy of the scenario's profiles and changes them as volunteers are accepted.
 */
final class Simulation {
    /** Ascending ids, so every list of options has the same order before a method looks at it. */
    private static final Comparator<ActivityProfile> BY_ID = Comparator.comparingLong(ActivityProfile::id);

    private final Scenario scenario;
    private final Map<Long, SimVolunteer> volunteers = new LinkedHashMap<>();
    private final Map<Long, ActivityProfile> activities = new LinkedHashMap<>();
    private final List<Assignment> assignments = new ArrayList<>();

    private Simulation(Scenario scenario) {
        this.scenario = scenario;
        scenario.volunteers().forEach(volunteer -> volunteers.put(volunteer.id(), volunteer));
        scenario.activities().stream().sorted(BY_ID).forEach(activity -> activities.put(activity.id(), activity));
    }

    /**
     * @param order  the volunteer ids in the order they act; ignored by smart-B
     * @param random the choices of the random method (and of nearest for a volunteer without a location)
     */
    static List<Assignment> run(Method method, Scenario scenario, List<Long> order, Random random) {
        Simulation simulation = new Simulation(scenario);
        if (method == Method.SMART_B) {
            simulation.activityDriven();
        } else {
            simulation.volunteerDriven(method, order, random);
        }
        return List.copyOf(simulation.assignments);
    }

    /** The feasible set of METHOD.md section 3: FUNCTION.md's conditions, the platform's and the radius. */
    static boolean eligible(SimVolunteer volunteer, ActivityProfile activity) {
        return PlatformEligibility.platformEligible(volunteer.profile(), activity)
                && Feasibility.feasible(volunteer.profile(), activity)
                && volunteer.reaches(activity);
    }

    /** The feasible activities of a volunteer, in ascending id order. */
    static List<ActivityProfile> options(SimVolunteer volunteer, List<ActivityProfile> activities) {
        return activities.stream().filter(activity -> eligible(volunteer, activity)).sorted(BY_ID).toList();
    }

    /** Distance in km, or infinity when a location is missing, so such an activity is never the nearest. */
    static double distanceKm(VolunteerProfile volunteer, ActivityProfile activity) {
        if (volunteer.location() == null || activity.location() == null) {
            return Double.POSITIVE_INFINITY;
        }
        return ScoringFunction.haversineKm(volunteer.location(), activity.location());
    }

    /** Rounds in the given order, one activity per volunteer per round, until a round assigns nobody. */
    private void volunteerDriven(Method method, List<Long> order, Random random) {
        boolean assigned = true;
        while (assigned) {
            assigned = false;
            for (long volunteerId : order) {
                SimVolunteer volunteer = volunteers.get(volunteerId);
                List<ActivityProfile> options = options(volunteer, List.copyOf(activities.values()));
                if (options.isEmpty()) {
                    continue;
                }
                assign(volunteer, choose(method, volunteer, options, random));
                assigned = true;
            }
        }
    }

    private ActivityProfile choose(Method method, SimVolunteer volunteer, List<ActivityProfile> options, Random random) {
        return switch (method) {
            case RANDOM -> options.get(random.nextInt(options.size()));
            case NEAREST -> volunteer.profile().location() == null
                    ? options.get(random.nextInt(options.size()))
                    : options.stream()
                    .min(Comparator.comparingDouble((ActivityProfile activity) -> distanceKm(volunteer.profile(), activity))
                            .thenComparing(BY_ID))
                    .orElseThrow();
            case SMART_A -> activities.get(
                    Ranker.rankActivities(volunteer.profile(), options, scenario.weights(), 1).get(0).id());
            case SMART_B -> throw new IllegalArgumentException("smart-B is activity-driven");
        };
    }

    /** Activities by priority (URGENT first), start, id; each takes its best feasible volunteer until full. */
    private void activityDriven() {
        List<Long> activityOrder = activities.values().stream()
                .sorted(Comparator.comparing((ActivityProfile activity) -> activity.priority()).reversed()
                        .thenComparing(activity -> activity.window().start())
                        .thenComparing(BY_ID))
                .map(ActivityProfile::id)
                .toList();
        for (long activityId : activityOrder) {
            while (true) {
                ActivityProfile activity = activities.get(activityId);
                List<VolunteerProfile> candidates = volunteers.values().stream()
                        .filter(volunteer -> eligible(volunteer, activity))
                        .map(SimVolunteer::profile)
                        .toList();
                if (candidates.isEmpty()) {
                    break;
                }
                long best = Ranker.rankVolunteers(activity, candidates, scenario.weightsFor(activityId), 1).get(0).id();
                assign(volunteers.get(best), activity);
            }
        }
    }

    /** Accepts the volunteer at once: one spot less, one assignment more (it uses capacity and the time window). */
    private void assign(SimVolunteer volunteer, ActivityProfile activity) {
        VolunteerProfile profile = volunteer.profile();
        List<TimeWindow> active = new ArrayList<>(profile.activeAssignments());
        active.add(activity.window());
        Set<Long> applied = new HashSet<>(profile.appliedActivityIds());
        applied.add(activity.id());
        volunteers.put(volunteer.id(), new SimVolunteer(new VolunteerProfile(profile.id(), profile.location(),
                profile.skills(), profile.certifications(), profile.availability(), active,
                profile.maxActiveAssignments(), applied, profile.banned()), volunteer.radiusKm()));
        activities.put(activity.id(), new ActivityProfile(activity.id(), activity.location(), activity.requiredSkills(),
                activity.requiredCertificationIds(), activity.window(), activity.priority(), activity.status(),
                activity.freeSpots() - 1));
        assignments.add(new Assignment(volunteer.id(), activity.id()));
    }
}
