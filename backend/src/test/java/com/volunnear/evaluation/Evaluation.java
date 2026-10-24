package com.volunnear.evaluation;

import com.volunnear.evaluation.ListQuality.Ordering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runs every method N times on one scenario (METHOD.md sections 4 and 6, Q47). Run r shuffles the volunteers with
 * seed r, and all methods of the run use that order; random's own choices come from a generator seeded per run,
 * so the whole evaluation is reproducible. Pure: no database.
 */
final class Evaluation {
    static final int[] LIST_SIZES = {5, 10};
    private static final long CHOICE_SEED = 1_000_000;
    private static final long LIST_SEED = 2_000_000;

    /**
     * @param runs  per method, in run order
     * @param lists per list size k and ordering, one entry per run (empty when not measured)
     */
    record Result(Scenario scenario,
                  Map<Method, List<Metrics>> runs,
                  Map<Integer, Map<Ordering, List<ListQuality.Values>>> lists) {
        /** In how many runs method a is strictly better than b on one metric. */
        int wins(Method a, Method b, Metrics.Kind kind) {
            int wins = 0;
            for (int r = 0; r < runs.get(a).size(); r++) {
                wins += kind.better(runs.get(a).get(r).value(kind), runs.get(b).get(r).value(kind)) ? 1 : 0;
            }
            return wins;
        }

        Summary summary(Method method, Metrics.Kind kind) {
            return Summary.of(runs.get(method), metrics -> metrics.value(kind));
        }
    }

    private Evaluation() {
    }

    static Result evaluate(Scenario scenario, int runCount, boolean measureLists) {
        Map<Method, List<Metrics>> runs = new EnumMap<>(Method.class);
        for (Method method : Method.values()) {
            runs.put(method, new ArrayList<>());
        }
        // smart-B ignores the order and draws nothing at random: one run gives the result of every run
        Metrics smartB = Metrics.of(scenario, Simulation.run(Method.SMART_B, scenario, List.of(), new Random(0)));
        Map<Integer, Map<Ordering, List<ListQuality.Values>>> lists = new LinkedHashMap<>();

        for (int r = 1; r <= runCount; r++) {
            List<Long> order = new ArrayList<>(scenario.volunteers().stream().map(SimVolunteer::id).sorted().toList());
            Collections.shuffle(order, new Random(r));
            for (Method method : List.of(Method.RANDOM, Method.NEAREST, Method.SMART_A)) {
                List<Assignment> assignments = Simulation.run(method, scenario, order, new Random(CHOICE_SEED + r));
                runs.get(method).add(Metrics.of(scenario, assignments));
            }
            runs.get(Method.SMART_B).add(smartB);
            if (measureLists) {
                for (int k : LIST_SIZES) {
                    Map<Ordering, ListQuality.Values> measured = ListQuality.measure(scenario, k, new Random(LIST_SEED + r));
                    Map<Ordering, List<ListQuality.Values>> perOrdering =
                            lists.computeIfAbsent(k, key -> new EnumMap<>(Ordering.class));
                    measured.forEach((ordering, values) ->
                            perOrdering.computeIfAbsent(ordering, key -> new ArrayList<>()).add(values));
                }
            }
        }
        return new Result(scenario, runs, lists);
    }
}
