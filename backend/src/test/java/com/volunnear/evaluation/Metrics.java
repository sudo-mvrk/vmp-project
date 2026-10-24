package com.volunnear.evaluation;

import com.volunnear.activity.Priority;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.ScoringFunction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The metrics M1 … M10 of METHOD.md section 5 for one run. A metric that is undefined in a run (no assignment it
 * could be computed on) is NaN and is left out of the means.
 */
record Metrics(double fillRate,
               double fullyStaffed,
               double empty,
               double skillCoverage,
               double fullyQualified,
               double meanDistanceKm,
               double medianDistanceKm,
               double priorityWeightedFill,
               double highUrgentFill,
               double participation) {

    /** Name, unit and direction of each metric, in the order of the record components. */
    enum Kind {
        M1("Fill rate", true),
        M2("Fully staffed activities", true),
        M3("Empty activities", false),
        M4("Skill coverage", true),
        M5("Fully qualified assignments", true),
        M6("Mean distance, km", false),
        M7("Median distance, km", false),
        M8("Priority-weighted fill", true),
        M9("Fill of HIGH and URGENT activities", true),
        M10("Participation", true);

        final String title;
        final boolean higherIsBetter;

        Kind(String title, boolean higherIsBetter) {
            this.title = title;
            this.higherIsBetter = higherIsBetter;
        }

        String label() {
            return name() + " " + title;
        }

        /** Strictly better; false if either value is undefined. */
        boolean better(double a, double b) {
            if (Double.isNaN(a) || Double.isNaN(b)) {
                return false;
            }
            return higherIsBetter ? a > b : a < b;
        }
    }

    double value(Kind kind) {
        return switch (kind) {
            case M1 -> fillRate;
            case M2 -> fullyStaffed;
            case M3 -> empty;
            case M4 -> skillCoverage;
            case M5 -> fullyQualified;
            case M6 -> meanDistanceKm;
            case M7 -> medianDistanceKm;
            case M8 -> priorityWeightedFill;
            case M9 -> highUrgentFill;
            case M10 -> participation;
        };
    }

    static Metrics of(Scenario scenario, List<Assignment> assignments) {
        Map<Long, ActivityProfile> activities = byId(scenario.activities(), ActivityProfile::id);
        Map<Long, VolunteerProfile> volunteers = byId(
                scenario.volunteers().stream().map(SimVolunteer::profile).toList(), VolunteerProfile::id);
        Map<Long, Integer> assigned = new HashMap<>();
        assignments.forEach(assignment -> assigned.merge(assignment.activityId(), 1, Integer::sum));

        List<ActivityProfile> withSpots = scenario.activities().stream().filter(activity -> activity.freeSpots() > 0).toList();
        int spots = 0;
        int filled = 0;
        int fullyStaffed = 0;
        int empty = 0;
        double weightedSpots = 0;
        double weightedFilled = 0;
        int highSpots = 0;
        int highFilled = 0;
        for (ActivityProfile activity : withSpots) {
            int a = assigned.getOrDefault(activity.id(), 0);
            int p = ScoringFunction.priorityValue(activity.priority());
            spots += activity.freeSpots();
            filled += a;
            fullyStaffed += a == activity.freeSpots() ? 1 : 0;
            empty += a == 0 ? 1 : 0;
            weightedSpots += p * activity.freeSpots();
            weightedFilled += p * a;
            if (isHighOrUrgent(activity.priority())) {
                highSpots += activity.freeSpots();
                highFilled += a;
            }
        }

        double coverageSum = 0;
        int withSkills = 0;
        int qualified = 0;
        double[] distances = assignments.stream()
                .mapToDouble(assignment -> Simulation.distanceKm(
                        volunteers.get(assignment.volunteerId()), activities.get(assignment.activityId())))
                .filter(Double::isFinite)
                .sorted()
                .toArray();
        for (Assignment assignment : assignments) {
            double coverage = skillCoverage(volunteers.get(assignment.volunteerId()), activities.get(assignment.activityId()));
            if (!Double.isNaN(coverage)) {
                coverageSum += coverage;
                withSkills++;
                qualified += coverage == 1.0 ? 1 : 0;
            }
        }
        long participants = assignments.stream().map(Assignment::volunteerId).distinct().count();

        return new Metrics(
                ratio(filled, spots),
                ratio(fullyStaffed, withSpots.size()),
                ratio(empty, withSpots.size()),
                ratio(coverageSum, withSkills),
                ratio(qualified, withSkills),
                distances.length == 0 ? Double.NaN : Arrays.stream(distances).average().orElseThrow(),
                median(distances),
                ratio(weightedFilled, weightedSpots),
                ratio(highFilled, highSpots),
                ratio(participants, volunteers.size()));
    }

    /** |K_v ∩ K_t| / |K_t|, or NaN when the activity requires no skill. */
    static double skillCoverage(VolunteerProfile volunteer, ActivityProfile activity) {
        Set<Long> required = positive(activity.requiredSkills());
        if (required.isEmpty()) {
            return Double.NaN;
        }
        Set<Long> held = positive(volunteer.skills());
        return (double) required.stream().filter(held::contains).count() / required.size();
    }

    static boolean isHighOrUrgent(Priority priority) {
        return priority == Priority.HIGH || priority == Priority.URGENT;
    }

    private static Set<Long> positive(Map<Long, Double> skills) {
        return skills.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private static double median(double[] sorted) {
        if (sorted.length == 0) {
            return Double.NaN;
        }
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }

    private static double ratio(double numerator, double denominator) {
        return denominator == 0 ? Double.NaN : numerator / denominator;
    }

    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
