package com.volunnear.evaluation;

import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.ranking.RankedCandidate;
import com.volunnear.distribution.ranking.Ranker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * List quality at k (METHOD.md section 5, Q45): what a volunteer sees first in the initial state, in the
 * unsorted list of the old solution, sorted by distance, and as recommendations.
 */
final class ListQuality {
    enum Ordering {
        UNSORTED("unsorted (old)"),
        NEAREST("nearest first"),
        RECOMMENDED("recommended (smart)");

        final String label;

        Ordering(String label) {
            this.label = label;
        }
    }

    /** Averages over volunteers; NaN when no list had an item the value could be computed on. */
    record Values(double skillCoverage, double meanDistanceKm, double highUrgentShare) {
    }

    private ListQuality() {
    }

    /** One run: the unsorted list is a fresh random permutation per volunteer. */
    static Map<Ordering, Values> measure(Scenario scenario, int k, Random random) {
        Map<Long, ActivityProfile> activities = scenario.activities().stream()
                .collect(Collectors.toMap(ActivityProfile::id, Function.identity()));
        Map<Ordering, List<Values>> perVolunteer = new EnumMap<>(Ordering.class);
        for (Ordering ordering : Ordering.values()) {
            perVolunteer.put(ordering, new ArrayList<>());
        }
        for (SimVolunteer volunteer : scenario.volunteers()) {
            List<ActivityProfile> options = Simulation.options(volunteer, scenario.activities());
            if (options.isEmpty()) {
                continue;
            }
            List<ActivityProfile> shuffled = new ArrayList<>(options);
            Collections.shuffle(shuffled, random);
            List<ActivityProfile> unsorted = shuffled.subList(0, Math.min(k, shuffled.size()));
            // nearest is undefined without a location; such a volunteer sees the unsorted list
            List<ActivityProfile> nearest = volunteer.profile().location() == null ? unsorted : options.stream()
                    .sorted(Comparator.comparingDouble((ActivityProfile activity) ->
                                    Simulation.distanceKm(volunteer.profile(), activity))
                            .thenComparingLong(ActivityProfile::id))
                    .limit(k)
                    .toList();
            List<ActivityProfile> recommended = Ranker.rankActivities(volunteer.profile(), options, scenario.weights(), k)
                    .stream().map(RankedCandidate::id).map(activities::get).toList();

            perVolunteer.get(Ordering.UNSORTED).add(values(volunteer, unsorted));
            perVolunteer.get(Ordering.NEAREST).add(values(volunteer, nearest));
            perVolunteer.get(Ordering.RECOMMENDED).add(values(volunteer, recommended));
        }
        Map<Ordering, Values> result = new EnumMap<>(Ordering.class);
        perVolunteer.forEach((ordering, values) -> result.put(ordering, new Values(
                mean(values, Values::skillCoverage), mean(values, Values::meanDistanceKm), mean(values, Values::highUrgentShare))));
        return result;
    }

    static Values values(SimVolunteer volunteer, List<ActivityProfile> list) {
        return new Values(
                mean(list, activity -> Metrics.skillCoverage(volunteer.profile(), activity)),
                mean(list, activity -> {
                    double distance = Simulation.distanceKm(volunteer.profile(), activity);
                    return Double.isFinite(distance) ? distance : Double.NaN;
                }),
                mean(list, activity -> Metrics.isHighOrUrgent(activity.priority()) ? 1.0 : 0.0));
    }

    /** Mean of the defined values; NaN if there is none. */
    static <T> double mean(List<T> items, ToDoubleFunction<T> value) {
        return items.stream().mapToDouble(value).filter(x -> !Double.isNaN(x)).average().orElse(Double.NaN);
    }
}
