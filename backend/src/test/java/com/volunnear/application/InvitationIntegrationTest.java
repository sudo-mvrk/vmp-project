package com.volunnear.application;

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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Slice 5b: invitations (Q5g … Q5m). */
@Transactional
class InvitationIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate DAY = LocalDate.now().plusDays(30);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager entityManager;

    private Cookie org;
    private Cookie otherOrg;
    private Vol vol;
    private Vol otherVol;

    private record Vol(long id, Cookie session) {
    }

    @BeforeEach
    void setUp() throws Exception {
        org = registerOrganization("org");
        otherOrg = registerOrganization("otherOrg");
        vol = availableVolunteer("vol");
        otherVol = availableVolunteer("otherVol");
    }

    @Test
    void invitedVolunteerAcceptsAndTakesTheSpot() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1, "");

        invite(org, activity, vol.id())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.origin").value("INVITATION"))
                .andExpect(jsonPath("$.volunteer.id").value(vol.id()))
                .andExpect(jsonPath("$.volunteer.username").doesNotExist());
        long invitation = lastInvitationOf(vol);

        // Q5m: visible in both existing lists, marked by its origin.
        mockMvc.perform(get("/api/v1/volunteers/me/applications").cookie(vol.session()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].origin").value("INVITATION"));
        mockMvc.perform(get("/api/v1/activities/{id}/applications", activity).cookie(org))
                .andExpect(jsonPath("$[0].origin").value("INVITATION"));
        // The invited volunteer leaves the recommendation list (Q15).
        mockMvc.perform(get("/api/v1/activities/{id}/recommended-volunteers", activity).cookie(org))
                .andExpect(jsonPath("$[*].volunteer.id").value(not(hasItem((int) vol.id()))));

        answer(vol, invitation, "accept")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedAt").exists());
        answer(vol, invitation, "accept").andExpect(status().isConflict());

        // The spot is used: a further invitation is refused.
        invite(org, activity, otherVol.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("no free spots")));
    }

    @Test
    void declinedInvitationIsFinal() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        long invitation = invitationId(org, activity, vol.id());

        answer(vol, invitation, "decline")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
        answer(vol, invitation, "accept").andExpect(status().isConflict());
        answer(vol, invitation, "decline").andExpect(status().isConflict());
        // Q5b: one row per volunteer and activity, ever.
        invite(org, activity, vol.id()).andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/activities/{id}/applications", activity).with(csrf()).cookie(vol.session()))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyTheOwningOrganizationInvites() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");

        invite(otherOrg, activity, vol.id()).andExpect(status().isForbidden());
        invite(vol.session(), activity, otherVol.id()).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/activities/{id}/invitations", activity).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"volunteerId\": %d }".formatted(vol.id())))
                .andExpect(status().isUnauthorized());
        invite(org, 999_999L, vol.id()).andExpect(status().isNotFound());
        invite(org, activity, 999_999L).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/activities/{id}/invitations", activity).with(csrf()).cookie(org)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheInvitedVolunteerAnswers() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        long invitation = invitationId(org, activity, vol.id());

        answer(otherVol, invitation, "accept").andExpect(status().isForbidden());
        answer(otherVol, invitation, "decline").andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/invitations/{id}/accept", invitation).with(csrf()).cookie(org))
                .andExpect(status().isForbidden());
    }

    @Test
    void eachSideDecidesOnlyWhatTheOtherStarted() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        long invitation = invitationId(org, activity, vol.id());
        long application = applicationId(otherVol, activity);

        // Q5i: the organization cannot accept or reject its own invitation ...
        decide(org, invitation, "accept").andExpect(status().isConflict());
        decide(org, invitation, "reject").andExpect(status().isConflict());
        // ... and the volunteer cannot answer an application as if it were an invitation.
        answer(otherVol, application, "accept").andExpect(status().isConflict());
        answer(otherVol, application, "decline").andExpect(status().isConflict());
        // A pending invitation is declined, not withdrawn.
        decide(vol.session(), invitation, "withdraw")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("decline")));
    }

    @Test
    void acceptedInvitationCanBeWithdrawnLikeAnApplication() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 1, "");
        long invitation = invitationId(org, activity, vol.id());
        answer(vol, invitation, "accept").andExpect(status().isOk());

        decide(vol.session(), invitation, "withdraw")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
        // The spot is free again.
        invite(org, activity, otherVol.id()).andExpect(status().isCreated());
    }

    @Test
    void infeasibleVolunteerCannotBeInvited() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");

        Vol noSlots = new Vol(volunteerId("noSlots"), null);
        invite(org, activity, noSlots.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("not available")));

        Vol resting = availableVolunteer("resting");
        setCapacity(resting, 0);
        invite(org, activity, resting.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("capacity")));

        long certificationId = objectMapper.readTree(mockMvc.perform(get("/api/v1/certifications"))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        long certified = publishedActivity(org, "09:00", "12:00", 2,
                "\"requiredCertificationIds\": [%d]".formatted(certificationId));
        invite(org, certified, vol.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("certification")));
    }

    @Test
    void volunteerWhoAppliedOrIsBannedCannotBeInvited() throws Exception {
        long activity = publishedActivity(org, "09:00", "12:00", 2, "");
        applicationId(vol, activity);

        invite(org, activity, vol.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already has an application")));

        entityManager.flush();
        entityManager.createQuery("update AppUser u set u.isBanned = true where u.id = :id")
                .setParameter("id", otherVol.id()).executeUpdate();
        entityManager.clear();
        invite(org, activity, otherVol.id()).andExpect(status().isConflict());
    }

    @Test
    void activityMustBeOpenAndNotStarted() throws Exception {
        long draft = createActivity(org, "09:00", "12:00", 2, "");
        invite(org, draft, vol.id()).andExpect(status().isConflict());

        long cancelled = publishedActivity(org, "09:00", "12:00", 2, "");
        mockMvc.perform(post("/api/v1/activities/{id}/cancel", cancelled).with(csrf()).cookie(org))
                .andExpect(status().isOk());
        invite(org, cancelled, vol.id()).andExpect(status().isConflict());
    }

    @Test
    void acceptingRespectsSpotsAndCapacity() throws Exception {
        // Q5l: more pending invitations than spots; whoever accepts first takes the spot.
        long oneSpot = publishedActivity(org, "09:00", "10:00", 1, "");
        long first = invitationId(org, oneSpot, vol.id());
        long second = invitationId(org, oneSpot, otherVol.id());
        answer(otherVol, second, "accept").andExpect(status().isOk());
        answer(vol, first, "accept")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("no free spots")));

        // Capacity 1: two invitations on separate hours, the second accept is refused.
        long morning = publishedActivity(otherOrg, "11:00", "12:00", 2, "");
        long evening = publishedActivity(otherOrg, "16:00", "17:00", 2, "");
        long a = invitationId(otherOrg, morning, vol.id());
        long b = invitationId(otherOrg, evening, vol.id());
        answer(vol, a, "accept").andExpect(status().isOk());
        answer(vol, b, "accept")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("capacity")));
    }

    // --- helpers ---

    private ResultActions invite(Cookie session, long activityId, long volunteerId) throws Exception {
        return mockMvc.perform(post("/api/v1/activities/{id}/invitations", activityId).with(csrf()).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"volunteerId\": %d }".formatted(volunteerId)));
    }

    private long invitationId(Cookie session, long activityId, long volunteerId) throws Exception {
        String body = invite(session, activityId, volunteerId).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long lastInvitationOf(Vol volunteer) throws Exception {
        String body = mockMvc.perform(get("/api/v1/volunteers/me/applications").cookie(volunteer.session()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(0).get("id").asLong();
    }

    private ResultActions answer(Vol volunteer, long invitationId, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/invitations/{id}/{action}", invitationId, action).with(csrf())
                .cookie(volunteer.session()));
    }

    private ResultActions decide(Cookie session, long applicationId, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/{action}", applicationId, action).with(csrf()).cookie(session));
    }

    private long applicationId(Vol volunteer, long activityId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities/{id}/applications", activityId).with(csrf())
                        .cookie(volunteer.session()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Cookie registerOrganization(String name) throws Exception {
        register("/api/v1/auth/register/organization", """
                { "username": "%s", "password": "password1", "email": "%s@example.com", "organizationName": "%s" }
                """.formatted(name, name, name));
        return login(name, "password1");
    }

    /** A registered volunteer without availability, so never feasible. */
    private long volunteerId(String name) throws Exception {
        register("/api/v1/auth/register/volunteer", """
                { "username": "%s", "password": "password1", "email": "%s@example.com",
                  "firstName": "%s first", "lastName": "Last", "dateOfBirth": "2000-01-01" }
                """.formatted(name, name, name));
        Cookie session = login(name, "password1");
        String body = mockMvc.perform(get("/api/v1/volunteers/me").cookie(session))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /** A volunteer available 08:00-18:00 on the activity day, capacity 1. */
    private Vol availableVolunteer(String name) throws Exception {
        long id = volunteerId(name);
        Cookie session = login(name, "password1");
        mockMvc.perform(put("/api/v1/volunteers/me/availability").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"slots\": [ { \"dayOfWeek\": \"%s\", \"start\": \"08:00\", \"end\": \"18:00\" } ] }"
                                .formatted(DAY.getDayOfWeek())))
                .andExpect(status().isOk());
        return new Vol(id, session);
    }

    private void setCapacity(Vol volunteer, int capacity) throws Exception {
        mockMvc.perform(put("/api/v1/volunteers/me").with(csrf()).cookie(volunteer.session())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"maxActiveAssignments\": %d }".formatted(capacity)))
                .andExpect(status().isOk());
    }

    private long createActivity(Cookie session, String start, String end, int spots, String extra) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "title": "Help", "lat": 50.08, "lon": 14.42, "startsAt": "%sT%s", "endsAt": "%sT%s", "spotsNeeded": %d %s }
                                """.formatted(DAY, start, DAY, end, spots, extra.isBlank() ? "" : ", " + extra)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long publishedActivity(Cookie session, String start, String end, int spots, String extra) throws Exception {
        long id = createActivity(session, start, end, spots, extra);
        mockMvc.perform(post("/api/v1/activities/{id}/publish", id).with(csrf()).cookie(session))
                .andExpect(status().isOk());
        return id;
    }
}
