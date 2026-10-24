package com.volunnear.evaluation;

import com.volunnear.activity.Activity;
import com.volunnear.activity.ActivityRepository;
import com.volunnear.activity.ActivityStatus;
import com.volunnear.activity.ActivityWeights;
import com.volunnear.application.ApplicationRepository;
import com.volunnear.auth.AppUserRepository;
import com.volunnear.common.config.VolunnearProperties;
import com.volunnear.dictionary.CertificationRepository;
import com.volunnear.dictionary.SkillRepository;
import com.volunnear.distribution.adapter.ProfileLoader;
import com.volunnear.distribution.model.ActivityProfile;
import com.volunnear.distribution.model.VolunteerProfile;
import com.volunnear.distribution.scoring.ScoringWeights;
import com.volunnear.organization.OrganizationMemberRepository;
import com.volunnear.organization.OrganizationRepository;
import com.volunnear.seed.SeedDataLoader;
import com.volunnear.seed.SeedProperties;
import com.volunnear.support.AbstractIntegrationTest;
import com.volunnear.volunteer.Volunteer;
import com.volunnear.volunteer.VolunteerRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The evaluation (Q41 … Q50, docs/evaluation/METHOD.md). Seeds each scale into the test database, reads the
 * profiles exactly as the recommendation endpoint does ({@link ProfileLoader}), runs the pure {@link Evaluation}
 * and writes docs/evaluation/RESULTS.md and runs.csv. Tagged {@code evaluation}, so {@code ./mvnw clean verify}
 * skips it; run it with {@code ./mvnw test -Pevaluation}. Expects a database without other data (that profile
 * runs only this class) and removes the seed data again.
 */
@Tag("evaluation")
class EvaluationRunnerTest extends AbstractIntegrationTest {
    private static final int RUNS = 100;
    private static final long DATA_SEED = 42;
    // The seed places activities relative to "now"; a fixed "now" makes the data the same on every day (Q47).
    private static final LocalDateTime DATA_NOW = LocalDateTime.of(2026, 11, 2, 8, 0);
    private static final Path OUTPUT = Path.of("..", "docs", "evaluation");

    private record ScaleConfig(String name, int organizations, int volunteers, int activities) {
    }

    // Q46; M-scarce (Q51) has fewer volunteers than spots, so the fill metrics can differ at a useful size
    private static final List<ScaleConfig> SCALES = List.of(
            new ScaleConfig("S", 3, 30, 10),
            new ScaleConfig("M-scarce", 5, 120, 60),
            new ScaleConfig("M", 5, 200, 40),
            new ScaleConfig("L", 10, 1000, 150));
    private static final String SENSITIVITY_SCALE = "M";

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private OrganizationMemberRepository memberRepository;
    @Autowired
    private VolunteerRepository volunteerRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private CertificationRepository certificationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Clock clock;
    @Autowired
    private ProfileLoader profileLoader;
    @Autowired
    private VolunnearProperties properties;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Clock dataClock;

    @Test
    void evaluate() throws IOException {
        dataClock = Clock.fixed(DATA_NOW.atZone(clock.getZone()).toInstant(), clock.getZone());
        VolunnearProperties.Scoring scoring = properties.scoring();
        List<ReportWriter.Scale> scales = new ArrayList<>();
        List<ReportWriter.WeightSet> sensitivity = new ArrayList<>();
        try {
            for (ScaleConfig config : SCALES) {
                removeSeedData();
                seed(config);
                Scenario scenario = loadScenario(config);
                scales.add(new ReportWriter.Scale(config.name(), config.organizations(), config.volunteers(),
                        config.activities(), Evaluation.evaluate(scenario, RUNS, true)));
                if (config.name().equals(SENSITIVITY_SCALE)) {
                    // Q48
                    sensitivity.add(new ReportWriter.WeightSet("default " + weightsLabel(scoring.weights()),
                            scales.get(scales.size() - 1).result()));
                    for (double[] w : new double[][]{{0.6, 0.2, 0.2}, {0.2, 0.6, 0.2}}) {
                        ScoringWeights weights = scoring.weights(w[0], w[1], w[2]);
                        sensitivity.add(new ReportWriter.WeightSet(
                                (w[0] > w[1] ? "distance-heavy " : "skill-heavy ") + weightsLabel(weights),
                                Evaluation.evaluate(scenario.withWeights(weights), RUNS, false)));
                    }
                }
            }
        } finally {
            removeSeedData();
        }

        ReportWriter.Setup setup = new ReportWriter.Setup(
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES).toString().replace('T', ' '),
                gitCommit(), DATA_SEED, DATA_NOW.toString().replace('T', ' '), RUNS, scoring.weights(), scoring.halfDistanceKm());
        Files.createDirectories(OUTPUT);
        Files.writeString(OUTPUT.resolve("RESULTS.md"),
                ReportWriter.markdown(setup, scales, SENSITIVITY_SCALE, sensitivity), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT.resolve("runs.csv"),
                ReportWriter.csv(scales, SENSITIVITY_SCALE, sensitivity), StandardCharsets.UTF_8);
    }

    /**
     * The loader of Slice 9 with the sizes of one scale. It is built by hand because its bean exists only under the
     * {@code seed} profile with one fixed size; the transaction stands in for its {@code @Transactional} proxy
     * (the demo states change saved entities and rely on dirty checking).
     */
    private void seed(ScaleConfig config) {
        SeedDataLoader loader = new SeedDataLoader(
                new SeedProperties(config.organizations(), config.volunteers(), config.activities(), DATA_SEED),
                appUserRepository, organizationRepository, memberRepository, volunteerRepository, activityRepository,
                applicationRepository, skillRepository, certificationRepository, passwordEncoder, dataClock);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> loader.run(null));
    }

    /** The open upcoming activities and every seed volunteer, mapped like the recommendation endpoint maps them. */
    private Scenario loadScenario(ScaleConfig config) {
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        return readOnly.execute(status -> {
            List<Volunteer> volunteers = volunteerRepository.findRecommendationCandidates().stream()
                    .filter(volunteer -> volunteer.getAppUser().getUsername().startsWith("seed_vol"))
                    .toList();
            List<Activity> activities = activityRepository.findByStatusAndStartsAtAfter(
                    ActivityStatus.OPEN, LocalDateTime.now(dataClock));
            // a different count means other data in the database, which would distort the evaluation
            assertThat(volunteers).hasSize(config.volunteers());
            assertThat(activities).hasSize(config.activities());

            Map<Long, Integer> radius = volunteers.stream()
                    .collect(HashMap::new, (map, volunteer) -> map.put(volunteer.getId(), volunteer.getRadius()), Map::putAll);
            List<SimVolunteer> simVolunteers = profileLoader.volunteerProfiles(volunteers).stream()
                    .map((VolunteerProfile profile) -> new SimVolunteer(profile, radius.get(profile.id())))
                    .toList();
            List<ActivityProfile> activityProfiles = profileLoader.activityProfiles(activities);
            Map<Long, ScoringWeights> overrides = activities.stream()
                    .filter(activity -> activity.getWeights() != null && activity.getWeights().alpha() != null)
                    .collect(Collectors.toMap(Activity::getId, activity -> {
                        ActivityWeights override = activity.getWeights();
                        return properties.scoring().weights(override.alpha(), override.beta(), override.gamma());
                    }));
            return new Scenario(simVolunteers, activityProfiles, properties.scoring().weights(), overrides);
        });
    }

    // Same as SeedDataIntegrationTest: deleting the organizations cascades to activities and applications.
    private void removeSeedData() {
        jdbcTemplate.update("""
                DELETE FROM organizations WHERE id IN (
                    SELECT m.organization_id FROM organization_member m JOIN app_users u ON u.id = m.user_id
                    WHERE u.username LIKE 'seed\\_%')
                """);
        jdbcTemplate.update("DELETE FROM app_users WHERE username LIKE 'seed\\_%'");
    }

    private static String weightsLabel(ScoringWeights weights) {
        return "%.1f / %.1f / %.1f".formatted(weights.alpha(), weights.beta(), weights.gamma()).replace(',', '.');
    }

    /** Short hash of HEAD, marked when the working tree has other changes than the output; "unknown" without git. */
    private static String gitCommit() {
        try {
            String hash = run("git", "rev-parse", "--short", "HEAD");
            // the runner's own output files do not count as changes of the code
            boolean changed = run("git", "status", "--porcelain").lines()
                    .anyMatch(line -> !line.endsWith("docs/evaluation/RESULTS.md") && !line.endsWith("docs/evaluation/runs.csv"));
            return changed ? hash + " (with uncommitted changes)" : hash;
        } catch (IOException | InterruptedException e) {
            return "unknown";
        }
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0) {
            throw new IOException(String.join(" ", command) + " failed: " + output);
        }
        return output;
    }
}
