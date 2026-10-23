package com.volunnear.activity;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Slice 8: completion (Q8a, Q8b), released capacity (Q16) and the volunteer's history (Q8e, Q8f). */
@Transactional
class CompletionIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate DAY = LocalDate.now().plusDays(30);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ActivityRepository activityRepository;

    private Cookie org;
    private Cookie otherOrg;
    private Cookie vol;
    private Cookie otherVol;

    @BeforeEach
    void setUp() throws Exception {
        org = registerOrganization("org");
        otherOrg = registerOrganization("otherOrg");
        vol = registerVolunteer("vol");
        otherVol = registerVolunteer("otherVol");
    }

    @Test
    void ownerCompletesAStartedActivity() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1);
        moveToThePast(activity);

        complete(otherOrg, activity).andExpect(status().isForbidden());
        complete(vol, activity).andExpect(status().isForbidden());

        complete(org, activity)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        complete(org, activity).andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/activities/{id}/cancel", activity).with(csrf()).cookie(org))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyAnOpenActivityThatHasStartedCanBeCompleted() throws Exception {
        long notStarted = publishedActivity(org, "09:00", "12:00", 1);
        complete(org, notStarted)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("not started")));

        long draft = createActivity(org, "09:00", "12:00", 1);
        moveToThePast(draft);
        complete(org, draft).andExpect(status().isConflict());

        long cancelled = publishedActivity(org, "13:00", "14:00", 1);
        mockMvc.perform(post("/api/v1/activities/{id}/cancel", cancelled).with(csrf()).cookie(org))
                .andExpect(status().isOk());
        moveToThePast(cancelled);
        complete(org, cancelled).andExpect(status().isConflict());
    }

    @Test
    void completionReleasesTheVolunteersCapacity() throws Exception {
        long first = publishedActivity(org, "09:00", "10:00", 1);
        long second = publishedActivity(otherOrg, "18:00", "19:00", 1);
        long firstApplication = applicationId(vol, first);
        long secondApplication = applicationId(vol, second);
        decide(org, firstApplication, "accept").andExpect(status().isOk());
        // Default capacity is 1 (Q3e).
        decide(otherOrg, secondApplication, "accept")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("capacity")));

        moveToThePast(first);
        complete(org, first).andExpect(status().isOk());

        // Q16: no counter was updated; the completed activity simply no longer counts.
        decide(otherOrg, secondApplication, "accept").andExpect(status().isOk());
    }

    @Test
    void pendingApplicationOfACompletedActivityStaysPendingAndCannotBeAccepted() throws Exception {
        // Q8b
        long activity = publishedActivity(org, "09:00", "12:00", 2);
        long pending = applicationId(vol, activity);
        moveToThePast(activity);
        complete(org, activity).andExpect(status().isOk());

        decide(org, pending, "accept").andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/volunteers/me/applications").cookie(vol))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].activityStatus").value("COMPLETED"));
    }

    @Test
    void historyListsOnlyTheCallersAcceptedApplicationsOnCompletedActivities() throws Exception {
        setCapacity(vol, 3);
        long older = publishedActivity(org, "07:00", "08:00", 2);
        long newer = publishedActivity(otherOrg, "09:00", "10:00", 2);
        long stillOpen = publishedActivity(org, "11:00", "12:00", 2);
        long pendingOnly = publishedActivity(org, "13:00", "14:00", 2);
        decide(org, applicationId(vol, older), "accept").andExpect(status().isOk());
        long newerApplication = applicationId(vol, newer);
        decide(otherOrg, newerApplication, "accept").andExpect(status().isOk());
        decide(org, applicationId(vol, stillOpen), "accept").andExpect(status().isOk());
        applicationId(vol, pendingOnly);
        decide(org, applicationId(otherVol, older), "reject").andExpect(status().isOk());
        for (long activity : new long[]{older, newer, pendingOnly}) {
            moveToThePast(activity);
        }
        complete(org, older).andExpect(status().isOk());
        complete(otherOrg, newer).andExpect(status().isOk());
        complete(org, pendingOnly).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/volunteers/me/history").cookie(vol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].applicationId").value(newerApplication))
                .andExpect(jsonPath("$[0].activityId").value(newer))
                .andExpect(jsonPath("$[0].title").value("Help"))
                .andExpect(jsonPath("$[0].city").value("Prague"))
                .andExpect(jsonPath("$[0].organizationName").value("otherOrg"))
                .andExpect(jsonPath("$[0].organizationId").isNumber())
                .andExpect(jsonPath("$[0].startsAt").exists())
                .andExpect(jsonPath("$[0].endsAt").exists())
                .andExpect(jsonPath("$[1].activityId").value(older))
                .andExpect(jsonPath("$[1].organizationName").value("org"));
        // A rejected application is not part of the history, and nobody sees another volunteer's history.
        mockMvc.perform(get("/api/v1/volunteers/me/history").cookie(otherVol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/volunteers/me/history").cookie(org)).andExpect(status().isForbidden());
    }

    // --- helpers, as in ApplicationIntegrationTest ---

    private Cookie registerOrganization(String name) throws Exception {
        register("/api/v1/auth/register/organization", """
                { "username": "%s", "password": "password1", "email": "%s@example.com", "organizationName": "%s" }
                """.formatted(name, name, name));
        return login(name, "password1");
    }

    private Cookie registerVolunteer(String name) throws Exception {
        register("/api/v1/auth/register/volunteer", """
                { "username": "%s", "password": "password1", "email": "%s@example.com",
                  "firstName": "%s first", "lastName": "Last", "dateOfBirth": "2000-01-01" }
                """.formatted(name, name, name));
        return login(name, "password1");
    }

    private long createActivity(Cookie session, String start, String end, int spots) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "title": "Help", "lat": 50.08, "lon": 14.42, "city": "Prague",
                                  "startsAt": "%sT%s", "endsAt": "%sT%s", "spotsNeeded": %d }
                                """.formatted(DAY, start, DAY, end, spots)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long publishedActivity(Cookie session, String start, String end, int spots) throws Exception {
        long id = createActivity(session, start, end, spots);
        mockMvc.perform(post("/api/v1/activities/{id}/publish", id).with(csrf()).cookie(session))
                .andExpect(status().isOk());
        return id;
    }

    private ResultActions complete(Cookie session, long activityId) throws Exception {
        return mockMvc.perform(post("/api/v1/activities/{id}/complete", activityId).with(csrf()).cookie(session));
    }

    private long applicationId(Cookie session, long activityId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities/{id}/applications", activityId).with(csrf()).cookie(session))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions decide(Cookie session, long applicationId, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/{action}", applicationId, action).with(csrf()).cookie(session));
    }

    private void setCapacity(Cookie session, int capacity) throws Exception {
        mockMvc.perform(put("/api/v1/volunteers/me")
                        .with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"maxActiveAssignments\": %d }".formatted(capacity)))
                .andExpect(status().isOk());
    }

    // Activities cannot be created in the past (Q4d), so tests move them there afterwards.
    private void moveToThePast(long activityId) {
        entityManager.flush();
        Activity activity = activityRepository.findById(activityId).orElseThrow();
        activity.setStartsAt(activity.getStartsAt().minusYears(10));
        activity.setEndsAt(activity.getEndsAt().minusYears(10));
        entityManager.flush();
    }
}
