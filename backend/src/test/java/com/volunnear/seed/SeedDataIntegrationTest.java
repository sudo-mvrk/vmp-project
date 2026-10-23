package com.volunnear.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunnear.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 9 (Q33 … Q37): the {@code seed} profile loads the data at startup, and the demo works on it. The loader
 * commits into the shared database, so everything it created is removed after the class.
 */
@ActiveProfiles("seed")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeedDataIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private SeedDataLoader loader;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Clock clock;

    @AfterAll
    void removeSeedData() {
        jdbcTemplate.update("""
                DELETE FROM organizations WHERE id IN (
                    SELECT m.organization_id FROM organization_member m JOIN app_users u ON u.id = m.user_id
                    WHERE u.username LIKE 'seed\\_%')
                """);
        jdbcTemplate.update("DELETE FROM app_users WHERE username LIKE 'seed\\_%'");
    }

    @Test
    void loadsTheConfiguredSizesOnceOnly() throws Exception {
        assertCounts();

        // Q33: a second start finds seed_org1 and adds nothing.
        loader.run(null);

        assertCounts();
    }

    @Test
    void recommendationsForTheSeededActivitiesAreNotEmpty() throws Exception {
        Cookie org = login("seed_org1", SeedDataLoader.PASSWORD);
        JsonNode activities = objectMapper.readTree(mockMvc.perform(get("/api/v1/organizations/me/activities").cookie(org))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        List<Long> upcoming = new ArrayList<>();
        for (JsonNode activity : activities) {
            if (LocalDateTime.parse(activity.get("startsAt").asText()).isAfter(LocalDateTime.now(clock))) {
                upcoming.add(activity.get("id").asLong());
            }
        }
        assertThat(upcoming).isNotEmpty();
        // Q35: the demo activity is the first one created; five volunteers are always available on its day.
        long demoActivity = upcoming.stream().min(Long::compare).orElseThrow();
        mockMvc.perform(get("/api/v1/activities/{id}/recommended-volunteers", demoActivity).cookie(org))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[0].score.total").isNumber())
                .andExpect(jsonPath("$[0].score.geo").isNumber());
        // The volunteer view of the demo: seed_vol08 is always offered the demo activity.
        Cookie volunteer = login("seed_vol08", SeedDataLoader.PASSWORD);
        mockMvc.perform(get("/api/v1/volunteers/me/recommended-activities").cookie(volunteer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].activity.id", hasItem((int) demoActivity)));
    }

    @Test
    void demoStatesAreInPlace() throws Exception {
        // Q35: seed_vol01 took part in the completed activity; the started one can be completed by its organization.
        Cookie volunteer = login("seed_vol01", SeedDataLoader.PASSWORD);
        mockMvc.perform(get("/api/v1/volunteers/me/history").cookie(volunteer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/volunteers/me/applications").cookie(volunteer))
                .andExpect(jsonPath("$", hasSize(2)));

        Long started = jdbcTemplate.queryForObject("""
                SELECT a.id FROM activities a
                WHERE a.status = 'OPEN' AND a.starts_at < now() AND a.organization_id IN (
                    SELECT m.organization_id FROM organization_member m JOIN app_users u ON u.id = m.user_id
                    WHERE u.username = 'seed_org1')
                """, Long.class);
        Cookie org = login("seed_org1", SeedDataLoader.PASSWORD);
        mockMvc.perform(get("/api/v1/activities/{id}/applications", started).cookie(org))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(post("/api/v1/activities/{id}/complete", started).with(csrf()).cookie(org))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private void assertCounts() {
        assertThat(count("SELECT count(*) FROM app_users WHERE username LIKE 'seed\\_org%'")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM app_users WHERE username LIKE 'seed\\_vol%'")).isEqualTo(30);
        // 10 upcoming plus the started and the completed one of Q35.
        assertThat(count("""
                SELECT count(*) FROM activities WHERE organization_id IN (
                    SELECT m.organization_id FROM organization_member m JOIN app_users u ON u.id = m.user_id
                    WHERE u.username LIKE 'seed\\_%')
                """)).isEqualTo(12);
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0 : result;
    }
}
