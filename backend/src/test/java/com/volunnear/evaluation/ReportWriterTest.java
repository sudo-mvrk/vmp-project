package com.volunnear.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.volunnear.distribution.Profiles.activity;
import static com.volunnear.distribution.Profiles.volunteer;
import static com.volunnear.evaluation.SimulationTest.WEIGHTS;
import static com.volunnear.evaluation.SimulationTest.east;
import static com.volunnear.evaluation.SimulationTest.scenario;
import static com.volunnear.evaluation.SimulationTest.sim;
import static org.assertj.core.api.Assertions.assertThat;

/** A small end-to-end check of the pure part: evaluation over a few runs, then both output files. */
class ReportWriterTest {
    @Test
    void writesNumberedTablesAndOneCsvRowPerRunAndMethod() {
        Scenario scenario = scenario(
                List.of(sim(volunteer(1).skills(1).build()), sim(volunteer(2).location(east(3)).build())),
                List.of(activity(1).requiredSkills(1).build(), activity(2).location(east(2)).build()));
        Evaluation.Result result = Evaluation.evaluate(scenario, 3, true);
        List<ReportWriter.Scale> scales = List.of(new ReportWriter.Scale("T", 1, 2, 2, result));
        List<ReportWriter.WeightSet> sensitivity = List.of(new ReportWriter.WeightSet("default", result));

        String markdown = ReportWriter.markdown(
                new ReportWriter.Setup("2026-10-24 12:00", "abc1234", 42, "2026-11-02 08:00", 3, WEIGHTS, 10), scales, "T", sensitivity);
        String csv = ReportWriter.csv(scales, "T", sensitivity);

        assertThat(markdown).contains("**Table 1. Data sets**", "## Scale T", "### Facts, scale T",
                "M1 Fill rate", "## Sensitivity to the weights (scale T)", "`abc1234`");
        assertThat(markdown).doesNotContain("<");
        // header, then 4 methods x 3 runs for the scale and again for the one weight set
        assertThat(csv.lines()).hasSize(1 + 2 * 4 * 3);
        assertThat(csv.lines().findFirst().orElseThrow()).isEqualTo("scale,weights,run,method,M1,M2,M3,M4,M5,M6,M7,M8,M9,M10");
    }
}
