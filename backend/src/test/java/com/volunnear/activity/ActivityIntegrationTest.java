package com.volunnear.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunnear.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ActivityIntegrationTest extends AbstractIntegrationTest {
    // Far enough ahead that "start in the future" holds whenever the test runs.
    private static final String DAY = LocalDate.now().plusDays(30).toString();

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ActivityRepository activityRepository;

    private Cookie orgA;
    private Cookie orgB;
    private long skillId;

    @BeforeEach
    void setUp() throws Exception {
        register("/api/v1/auth/register/organization", """
                { "username": "orgA", "password": "orgPassword1", "email": "orgA@example.com", "organizationName": "Org A" }
                """);
        register("/api/v1/auth/register/organization", """
                { "username": "orgB", "password": "orgPassword1", "email": "orgB@example.com", "organizationName": "Org B" }
                """);
        orgA = login("orgA", "orgPassword1");
        orgB = login("orgB", "orgPassword1");
        skillId = objectMapper.readTree(mockMvc.perform(get("/api/v1/skills")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asLong();
    }

    @Test
    void organizationCreatesDraftWithExactCoordinatesAndDefaults() throws Exception {
        create(orgA, activityJson("\"requiredSkillIds\": [%d]".formatted(skillId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.lat").value(50.0875311))
                .andExpect(jsonPath("$.lon").value(14.4213203))
                .andExpect(jsonPath("$.startsAt").value(DAY + "T09:00:00"))
                .andExpect(jsonPath("$.requiredSkills[0].id").value(skillId))
                .andExpect(jsonPath("$.weights").doesNotExist());
    }

    @Test
    void ownerUpdatesWholeActivityIncludingWeightOverride() throws Exception {
        long id = createdId(orgA, activityJson(""));

        update(orgA, id, activityJson("""
                "title": "Renamed", "priority": "URGENT", "weights": { "alpha": 0.2, "beta": 0.5, "gamma": 0.3 }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.weights.beta").value(0.5));

        // Full replacement (Q4c): leaving the override out removes it.
        update(orgA, id, activityJson(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weights").doesNotExist());
    }

    @Test
    void anotherOrganizationCannotChangeTheActivity() throws Exception {
        long id = createdId(orgA, activityJson(""));

        update(orgB, id, activityJson("\"title\": \"Hijacked\"")).andExpect(status().isForbidden());
        action(orgB, id, "publish").andExpect(status().isForbidden());
        action(orgB, id, "cancel").andExpect(status().isForbidden());
    }

    @Test
    void volunteerAndAnonymousCannotCreate() throws Exception {
        register("/api/v1/auth/register/volunteer", """
                { "username": "vol", "password": "volPassword1", "email": "vol@example.com",
                  "firstName": "Vol", "lastName": "Unteer", "dateOfBirth": "2000-01-01" }
                """);
        create(login("vol", "volPassword1"), activityJson("")).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/activities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(activityJson("")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidWindowsAndSpotsAre400() throws Exception {
        create(orgA, activityJson("\"endsAt\": \"%sT08:00\"".formatted(DAY))).andExpect(status().isBadRequest());
        String nextDay = LocalDate.parse(DAY).plusDays(1).toString();
        create(orgA, activityJson("\"endsAt\": \"%sT09:00\"".formatted(nextDay)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("same day")));
        // An end at midnight is the next day (Q4d).
        create(orgA, activityJson("\"endsAt\": \"%sT00:00\"".formatted(nextDay))).andExpect(status().isBadRequest());
        create(orgA, activityJson("\"startsAt\": \"2020-01-01T09:00\", \"endsAt\": \"2020-01-01T12:00\""))
                .andExpect(status().isBadRequest());
        create(orgA, activityJson("\"spotsNeeded\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.spotsNeeded").exists());
        create(orgA, activityJson("\"lat\": null")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidWeightOverridesAre400() throws Exception {
        create(orgA, activityJson("\"weights\": { \"alpha\": 0.5, \"beta\": 0.5 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['weights.gamma']").exists());
        create(orgA, activityJson("\"weights\": { \"alpha\": -0.2, \"beta\": 0.7, \"gamma\": 0.5 }"))
                .andExpect(status().isBadRequest());
        create(orgA, activityJson("\"weights\": { \"alpha\": 0.5, \"beta\": 0.5, \"gamma\": 0.5 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must be 1")));
    }

    @Test
    void unknownRequiredSkillIs400() throws Exception {
        create(orgA, activityJson("\"requiredSkillIds\": [999999]")).andExpect(status().isBadRequest());
    }

    @Test
    void publicListShowsOnlyOpenActivitiesInStartOrder() throws Exception {
        long later = createdId(orgA, activityJson("\"title\": \"Later\", \"startsAt\": \"%sT14:00\", \"endsAt\": \"%sT16:00\"".formatted(DAY, DAY)));
        long sooner = createdId(orgB, activityJson("\"title\": \"Sooner\""));
        long draft = createdId(orgA, activityJson("\"title\": \"Draft\""));
        long cancelled = createdId(orgA, activityJson("\"title\": \"Cancelled\""));
        action(orgA, later, "publish").andExpect(status().isOk());
        action(orgB, sooner, "publish").andExpect(status().isOk());
        action(orgA, cancelled, "publish").andExpect(status().isOk());
        action(orgA, cancelled, "cancel").andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Sooner"))
                .andExpect(jsonPath("$.content[1].title").value("Later"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/activities").param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content[0].title").value("Later"))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/v1/activities").param("size", "500")).andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/organizations/me/activities").cookie(orgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        assertDraftHidden(draft);
    }

    @Test
    void startedOpenActivityDropsOutOfThePublicList() throws Exception {
        long id = createdId(orgA, activityJson(""));
        action(orgA, id, "publish").andExpect(status().isOk());
        moveToThePast(id);

        mockMvc.perform(get("/api/v1/activities")).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void draftIsVisibleOnlyToItsOrganization() throws Exception {
        long id = createdId(orgA, activityJson(""));

        mockMvc.perform(get("/api/v1/activities/{id}", id).cookie(orgA)).andExpect(status().isOk());
        assertDraftHidden(id);

        action(orgA, id, "publish").andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/activities/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void invalidTransitionsAre409() throws Exception {
        long id = createdId(orgA, activityJson(""));
        action(orgA, id, "publish").andExpect(status().isOk());
        action(orgA, id, "publish").andExpect(status().isConflict());

        action(orgA, id, "cancel").andExpect(status().isOk());
        action(orgA, id, "cancel").andExpect(status().isConflict());
        update(orgA, id, activityJson("\"title\": \"Too late\"")).andExpect(status().isConflict());
    }

    @Test
    void draftWhoseStartHasPassedCannotBePublished() throws Exception {
        long id = createdId(orgA, activityJson(""));
        moveToThePast(id);

        action(orgA, id, "publish")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("future")));
    }

    private void assertDraftHidden(long id) throws Exception {
        mockMvc.perform(get("/api/v1/activities/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/activities/{id}", id).cookie(orgB)).andExpect(status().isNotFound());
    }

    private void moveToThePast(long id) {
        entityManager.flush();
        Activity activity = activityRepository.findById(id).orElseThrow();
        activity.setStartsAt(activity.getStartsAt().minusYears(10));
        activity.setEndsAt(activity.getEndsAt().minusYears(10));
        entityManager.flush();
    }

    /** A valid activity on DAY, 09:00 - 12:00; {@code overrides} are JSON members that replace the defaults. */
    private String activityJson(String overrides) throws Exception {
        JsonNode base = objectMapper.readTree("""
                { "title": "Park clean-up", "shortDescription": "Collect litter", "lat": 50.0875311, "lon": 14.4213203,
                  "city": "Prague", "startsAt": "%sT09:00", "endsAt": "%sT12:00", "spotsNeeded": 5 }
                """.formatted(DAY, DAY));
        if (!overrides.isBlank()) {
            JsonNode changes = objectMapper.readTree("{" + overrides + "}");
            ((com.fasterxml.jackson.databind.node.ObjectNode) base).setAll((com.fasterxml.jackson.databind.node.ObjectNode) changes);
        }
        return objectMapper.writeValueAsString(base);
    }

    private ResultActions create(Cookie session, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createdId(Cookie session, String json) throws Exception {
        String body = create(session, json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions update(Cookie session, long id, String json) throws Exception {
        return mockMvc.perform(put("/api/v1/activities/{id}", id).with(csrf()).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions action(Cookie session, long id, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/activities/{id}/{action}", id, action).with(csrf()).cookie(session));
    }
}
