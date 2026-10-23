package com.volunnear.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunnear.activity.Activity;
import com.volunnear.activity.ActivityRepository;
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

@Transactional
class ApplicationIntegrationTest extends AbstractIntegrationTest {
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
    private long certificationId;

    @BeforeEach
    void setUp() throws Exception {
        org = registerOrganization("org");
        otherOrg = registerOrganization("otherOrg");
        vol = registerVolunteer("vol");
        otherVol = registerVolunteer("otherVol");
        certificationId = objectMapper.readTree(mockMvc.perform(get("/api/v1/certifications"))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    @Test
    void volunteerAppliesAndSeesOwnApplication() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");

        apply(vol, activity)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.origin").value("APPLICATION"))
                .andExpect(jsonPath("$.activityId").value(activity));

        mockMvc.perform(get("/api/v1/volunteers/me/applications").cookie(vol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].activityTitle").value("Help"));
        mockMvc.perform(get("/api/v1/volunteers/me/applications").cookie(otherVol))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void secondApplicationToTheSameActivityIs409() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        long application = applicationId(vol, activity);

        apply(vol, activity).andExpect(status().isConflict());
        // Q5b: also after a withdrawal.
        decide(vol, application, "withdraw").andExpect(status().isOk());
        apply(vol, activity).andExpect(status().isConflict());
    }

    @Test
    void applyingNeedsAnOpenActivityThatHasNotStarted() throws Exception {
        long draft = createActivity(org, "09:00", "12:00", 1, "");
        apply(vol, draft).andExpect(status().isNotFound());

        long cancelled = publishedActivity(org, "09:00", "12:00", 1, "");
        action(org, cancelled, "cancel");
        apply(vol, cancelled).andExpect(status().isConflict());

        long started = publishedActivity(org, "13:00", "14:00", 1, "");
        moveToThePast(started);
        apply(vol, started).andExpect(status().isConflict());
    }

    @Test
    void applyingToAFullActivityIs409() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1, "");
        decide(org, applicationId(vol, activity), "accept").andExpect(status().isOk());

        apply(otherVol, activity)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("no free spots")));
    }

    @Test
    void requiredCertificationMustBeHeldAndValidOnTheActivityDate() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 3, "\"requiredCertificationIds\": [%d]".formatted(certificationId));

        apply(vol, activity)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("certifications")));

        // Valid until the day before the activity: still missing (Q14a).
        setCertification(otherVol, DAY.minusDays(1));
        apply(otherVol, activity).andExpect(status().isConflict());

        // Valid through the activity day: accepted (inclusive).
        setCertification(vol, DAY);
        apply(vol, activity).andExpect(status().isCreated());
    }

    @Test
    void ownerListsApplicationsWithoutAccountData() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        applicationId(vol, activity);

        mockMvc.perform(get("/api/v1/activities/{id}/applications", activity).cookie(org))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].volunteer.firstName").value("vol first"))
                .andExpect(jsonPath("$[0].volunteer.username").doesNotExist())
                .andExpect(jsonPath("$[0].volunteer.email").doesNotExist());
        mockMvc.perform(get("/api/v1/activities/{id}/applications", activity).cookie(otherOrg))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/activities/{id}/applications", activity).cookie(vol))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyTheOwnerAcceptsOrRejectsAndOnlyOnce() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        long application = applicationId(vol, activity);

        decide(otherOrg, application, "accept").andExpect(status().isForbidden());
        decide(otherOrg, application, "reject").andExpect(status().isForbidden());
        decide(vol, application, "accept").andExpect(status().isForbidden());

        decide(org, application, "accept")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedAt").exists());
        decide(org, application, "accept").andExpect(status().isConflict());
        decide(org, application, "reject").andExpect(status().isConflict());
    }

    @Test
    void rejectedApplicationIsFinal() throws Exception {
        long application = applicationId(vol, publishedActivity(org, "09:00", "12:00", 2, ""));

        decide(org, application, "reject").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        decide(org, application, "accept").andExpect(status().isConflict());
        decide(vol, application, "withdraw").andExpect(status().isConflict());
    }

    @Test
    void acceptRespectsVolunteerCapacity() throws Exception {
        long morning = publishedActivity(org, "09:00", "10:00", 2, "");
        long evening = publishedActivity(org, "18:00", "19:00", 2, "");
        long first = applicationId(vol, morning);
        long second = applicationId(vol, evening);

        decide(org, first, "accept").andExpect(status().isOk());
        // Default capacity is 1 (Q3e).
        decide(org, second, "accept")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("capacity")));
    }

    @Test
    void acceptRejectsOverlappingAssignments() throws Exception {
        setCapacity(vol, 2);
        long first = applicationId(vol, publishedActivity(org, "09:00", "12:00", 2, ""));
        long overlapping = applicationId(vol, publishedActivity(otherOrg, "11:00", "13:00", 2, ""));
        long touching = applicationId(vol, publishedActivity(otherOrg, "12:00", "13:00", 2, ""));

        decide(org, first, "accept").andExpect(status().isOk());
        decide(otherOrg, overlapping, "accept")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("overlapping")));
        // Half-open windows (Q13b): 12:00 - 13:00 only touches 09:00 - 12:00.
        decide(otherOrg, touching, "accept").andExpect(status().isOk());
    }

    @Test
    void withdrawingAnAcceptedApplicationFreesTheSpotAndCapacity() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1, "");
        long mine = applicationId(vol, activity);
        decide(org, mine, "accept").andExpect(status().isOk());
        apply(otherVol, activity).andExpect(status().isConflict());

        decide(otherVol, mine, "withdraw").andExpect(status().isForbidden());
        decide(org, mine, "withdraw").andExpect(status().isForbidden());
        decide(vol, mine, "withdraw").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WITHDRAWN"));

        decide(org, applicationId(otherVol, activity), "accept").andExpect(status().isOk());
        long elsewhere = applicationId(vol, publishedActivity(otherOrg, "14:00", "15:00", 1, ""));
        decide(otherOrg, elsewhere, "accept").andExpect(status().isOk());
    }

    @Test
    void acceptedApplicationCannotBeWithdrawnOnceTheActivityStarted() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1, "");
        long application = applicationId(vol, activity);
        decide(org, application, "accept").andExpect(status().isOk());
        moveToThePast(activity);

        decide(vol, application, "withdraw").andExpect(status().isConflict());
    }

    @Test
    void cancelledActivityBlocksAcceptAndReleasesCapacity() throws Exception {
        long cancelledLater = publishedActivity(org, "09:00", "10:00", 2, "");
        long accepted = applicationId(vol, cancelledLater);
        long pending = applicationId(otherVol, cancelledLater);
        decide(org, accepted, "accept").andExpect(status().isOk());

        action(org, cancelledLater, "cancel");

        decide(org, pending, "accept").andExpect(status().isConflict());
        // Q5e, Q16: the accepted application on a cancelled activity no longer uses the capacity of 1.
        long next = applicationId(vol, publishedActivity(otherOrg, "09:00", "10:00", 1, ""));
        decide(otherOrg, next, "accept").andExpect(status().isOk());
    }

    @Test
    void acceptAfterTheStartIs409() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1, "");
        long application = applicationId(vol, activity);
        moveToThePast(activity);

        decide(org, application, "accept").andExpect(status().isConflict());
    }

    @Test
    void spotsCannotDropBelowTheAcceptedCount() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        decide(org, applicationId(vol, activity), "accept").andExpect(status().isOk());
        decide(org, applicationId(otherVol, activity), "accept").andExpect(status().isOk());

        updateSpots(activity, 1).andExpect(status().isConflict());
        updateSpots(activity, 2).andExpect(status().isOk());
    }

    @Test
    void organizationCannotApply() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1, "");

        apply(otherOrg, activity).andExpect(status().isForbidden());
    }

    // --- helpers ---

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

    private String activityJson(String start, String end, int spots, String extra) {
        return """
                { "title": "Help", "lat": 50.08, "lon": 14.42, "startsAt": "%sT%s", "endsAt": "%sT%s", "spotsNeeded": %d %s }
                """.formatted(DAY, start, DAY, end, spots, extra.isBlank() ? "" : ", " + extra);
    }

    private long createActivity(Cookie session, String start, String end, int spots, String extra) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(activityJson(start, end, spots, extra)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long publishedActivity(Cookie session, String start, String end, int spots, String extra) throws Exception {
        long id = createActivity(session, start, end, spots, extra);
        action(session, id, "publish");
        return id;
    }

    private void action(Cookie session, long activityId, String action) throws Exception {
        mockMvc.perform(post("/api/v1/activities/{id}/{action}", activityId, action).with(csrf()).cookie(session))
                .andExpect(status().isOk());
    }

    private ResultActions updateSpots(long activityId, int spots) throws Exception {
        return mockMvc.perform(put("/api/v1/activities/{id}", activityId).with(csrf()).cookie(org)
                .contentType(MediaType.APPLICATION_JSON).content(activityJson("09:00", "12:00", spots, "")));
    }

    private ResultActions apply(Cookie session, long activityId) throws Exception {
        return mockMvc.perform(post("/api/v1/activities/{id}/applications", activityId).with(csrf()).cookie(session));
    }

    private long applicationId(Cookie session, long activityId) throws Exception {
        String body = apply(session, activityId).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions decide(Cookie session, long applicationId, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/{action}", applicationId, action).with(csrf()).cookie(session));
    }

    private void setCertification(Cookie session, LocalDate validUntil) throws Exception {
        // validUntil must not be in the past (Q3f); DAY is 30 days ahead, so both dates used here are allowed.
        mockMvc.perform(put("/api/v1/volunteers/me/certifications").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"certifications\": [ { \"certificationId\": %d, \"validUntil\": \"%s\" } ] }"
                                .formatted(certificationId, validUntil)))
                .andExpect(status().isOk());
    }

    private void setCapacity(Cookie session, int capacity) throws Exception {
        mockMvc.perform(put("/api/v1/volunteers/me").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"maxActiveAssignments\": %d }".formatted(capacity)))
                .andExpect(status().isOk());
    }

    private void moveToThePast(long activityId) {
        entityManager.flush();
        Activity activity = activityRepository.findById(activityId).orElseThrow();
        activity.setStartsAt(activity.getStartsAt().minusYears(10));
        activity.setEndsAt(activity.getEndsAt().minusYears(10));
        entityManager.flush();
    }
}
