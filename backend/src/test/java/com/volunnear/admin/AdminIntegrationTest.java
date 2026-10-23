package com.volunnear.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunnear.application.ApplicationRepository;
import com.volunnear.application.ApplicationStatus;
import com.volunnear.activity.ActivityRepository;
import com.volunnear.activity.ActivityStatus;
import com.volunnear.auth.AppUserRepository;
import com.volunnear.notification.Notification;
import com.volunnear.notification.NotificationSender;
import com.volunnear.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 8b (Q21 … Q32). Not @Transactional: a ban deletes sessions only after a real commit (Q23), so every request
 * commits and the data is removed after each test (names carry a random prefix). The first admin comes from V7
 * with the placeholders of the test application.yml. The sender mock records notifications (Q25, Q26, Q31).
 */
class AdminIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate DAY = LocalDate.now().plusDays(30);

    @MockBean
    private NotificationSender sender;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String prefix;
    private Cookie admin;
    private Cookie org;

    @BeforeEach
    void setUp() throws Exception {
        prefix = "a" + UUID.randomUUID().toString().substring(0, 8);
        admin = login("admin", "adminpass1");
        org = registerOrganization("org");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("""
                DELETE FROM organizations WHERE id IN (
                    SELECT m.organization_id FROM organization_member m JOIN app_users u ON u.id = m.user_id
                    WHERE u.username LIKE ?)
                """, prefix + "%");
        jdbcTemplate.update("DELETE FROM app_users WHERE username LIKE ?", prefix + "%");
    }

    @Test
    void freshDatabaseContainsTheConfiguredAdmin() throws Exception {
        // Q21, Q28: inserted by V7 from the placeholders; the login above already used its password.
        mockMvc.perform(get("/api/v1/auth/me").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.roles", contains("ROLE_ADMIN")));
    }

    @Test
    void nonAdminsGet403OnEveryAdminEndpointAndAnonymousCallers401() throws Exception {
        Cookie volunteer = registerVolunteer("v");
        long target = userId("v");
        long activity = publishedActivity();

        for (Cookie caller : new Cookie[]{volunteer, org}) {
            mockMvc.perform(get("/api/v1/admin/users").cookie(caller)).andExpect(status().isForbidden());
            adminPost(caller, "/api/v1/admin/users/{id}/ban", target).andExpect(status().isForbidden());
            adminPost(caller, "/api/v1/admin/users/{id}/unban", target).andExpect(status().isForbidden());
            adminPost(caller, "/api/v1/admin/activities/{id}/cancel", activity).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        assertThat(appUserRepository.findById(target).orElseThrow().isBanned()).isFalse();
    }

    @Test
    void adminHasNoAccessToVolunteerOrOrganizationEndpoints() throws Exception {
        // Q32
        mockMvc.perform(get("/api/v1/volunteers/me").cookie(admin)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/organizations/me").cookie(admin)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(activityJson("09:00", "12:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListsAndFiltersUsers() throws Exception {
        // Q29
        registerVolunteer("anna");
        registerVolunteer("ben");
        adminPost(admin, "/api/v1/admin/users/{id}/ban", userId("ben")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users").param("query", prefix.toUpperCase()).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].username",
                        containsInAnyOrder(prefix + "_org", prefix + "_anna", prefix + "_ben")));
        // The email matches too.
        mockMvc.perform(get("/api/v1/admin/users").param("query", prefix + "_anna@example").cookie(admin))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value(prefix + "_anna@example.com"))
                .andExpect(jsonPath("$.content[0].roles", contains("ROLE_VOLUNTEER")))
                .andExpect(jsonPath("$.content[0].banned").value(false))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/users").param("query", prefix).param("role", "ROLE_ORGANIZATION").cookie(admin))
                .andExpect(jsonPath("$.content[*].username", contains(prefix + "_org")));
        mockMvc.perform(get("/api/v1/admin/users").param("query", prefix).param("banned", "true").cookie(admin))
                .andExpect(jsonPath("$.content[*].username", contains(prefix + "_ben")));
        mockMvc.perform(get("/api/v1/admin/users").param("query", prefix).param("size", "2").param("page", "1").cookie(admin))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/v1/admin/users").param("size", "101").cookie(admin)).andExpect(status().isBadRequest());
    }

    @Test
    void banEndsTheOpenSessionAndRefusesLoginUntilUnban() throws Exception {
        // Q23, Q27
        Cookie volunteer = registerVolunteer("v");
        long activity = publishedActivity();
        mockMvc.perform(get("/api/v1/volunteers/me").cookie(volunteer)).andExpect(status().isOk());

        adminPost(admin, "/api/v1/admin/users/{id}/ban", userId("v"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(true));

        mockMvc.perform(get("/api/v1/volunteers/me").cookie(volunteer)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/activities/{id}/applications", activity).with(csrf()).cookie(volunteer))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(prefix + "_v", "password1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is banned"));

        adminPost(admin, "/api/v1/admin/users/{id}/unban", userId("v"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(false));
        login(prefix + "_v", "password1");
    }

    @Test
    void adminCannotBanThemselvesOrAnotherAdmin() throws Exception {
        // Q30
        long adminId = appUserRepository.findAppUserByUsername("admin").orElseThrow().getId();
        adminPost(admin, "/api/v1/admin/users/{id}/ban", adminId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot ban yourself"));

        jdbcTemplate.update("INSERT INTO app_users (username, email, password, is_banned) VALUES (?, ?, 'x', FALSE)",
                prefix + "_admin2", prefix + "_admin2@example.com");
        jdbcTemplate.update("INSERT INTO user_role (user_id, roles) SELECT id, 'ROLE_ADMIN' FROM app_users WHERE username = ?",
                prefix + "_admin2");
        adminPost(admin, "/api/v1/admin/users/{id}/ban", userId("admin2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An admin cannot be banned"));

        adminPost(admin, "/api/v1/admin/users/{id}/ban", 0L).andExpect(status().isNotFound());
    }

    @Test
    void repeatedBanOrUnbanChangesNothing() throws Exception {
        registerVolunteer("v");
        adminPost(admin, "/api/v1/admin/users/{id}/ban", userId("v")).andExpect(status().isOk());
        adminPost(admin, "/api/v1/admin/users/{id}/ban", userId("v"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.banned").value(true));
        adminPost(admin, "/api/v1/admin/users/{id}/unban", userId("v")).andExpect(status().isOk());
        adminPost(admin, "/api/v1/admin/users/{id}/unban", userId("v"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.banned").value(false));
    }

    @Test
    void banningAVolunteerReleasesWhatHasNotStartedAndKeepsTheRest() throws Exception {
        // Q25, Q27
        Cookie volunteer = registerVolunteer("v");
        setCapacity(volunteer, 3);
        makeAvailable(volunteer);
        long acceptedActivity = publishedActivity("09:00", "10:00", 2);
        long accepted = applicationId(volunteer, acceptedActivity);
        accept(accepted);
        long pending = applicationId(volunteer, publishedActivity("11:00", "12:00", 2));
        long invitation = invitationId(publishedActivity("13:00", "14:00", 2), userId("v"));
        long pastActivity = publishedActivity("15:00", "16:00", 2);
        long past = applicationId(volunteer, pastActivity);
        accept(past);
        moveToThePast(pastActivity);
        mockMvc.perform(post("/api/v1/activities/{id}/complete", pastActivity).with(csrf()).cookie(org))
                .andExpect(status().isOk());
        clearInvocations(sender);

        adminPost(admin, "/api/v1/admin/users/{id}/ban", userId("v")).andExpect(status().isOk());

        assertThat(statusOf(accepted)).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(statusOf(pending)).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(statusOf(invitation)).isEqualTo(ApplicationStatus.DECLINED);
        assertThat(statusOf(past)).isEqualTo(ApplicationStatus.ACCEPTED);
        // Only the accepted, upcoming assignment tells the organization.
        assertThat(onlySent().to()).isEqualTo(email("org"));
        assertThat(onlySent().subject()).isEqualTo("A volunteer withdrew");

        // Q27: unban restores nothing.
        adminPost(admin, "/api/v1/admin/users/{id}/unban", userId("v")).andExpect(status().isOk());
        assertThat(statusOf(accepted)).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void banningAnOrganizationCancelsItsActivitiesThatHaveNotStarted() throws Exception {
        // Q26
        long draft = createActivity("09:00", "10:00", 1);
        long open = publishedActivity("11:00", "12:00", 1);
        Cookie volunteer = registerVolunteer("v");
        applicationId(volunteer, open);
        long started = publishedActivity("13:00", "14:00", 1);
        moveToThePast(started);
        clearInvocations(sender);

        adminPost(admin, "/api/v1/admin/users/{id}/ban", userId("org")).andExpect(status().isOk());

        assertThat(activityStatus(draft)).isEqualTo(ActivityStatus.CANCELLED);
        assertThat(activityStatus(open)).isEqualTo(ActivityStatus.CANCELLED);
        assertThat(activityStatus(started)).isEqualTo(ActivityStatus.OPEN);
        assertThat(onlySent().to()).isEqualTo(email("v"));
        assertThat(onlySent().subject()).isEqualTo("Activity cancelled");
        mockMvc.perform(get("/api/v1/organizations/me").cookie(org)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCancelsAnyActivity() throws Exception {
        // Q31
        long activity = publishedActivity();
        applicationId(registerVolunteer("v"), activity);
        clearInvocations(sender);

        adminPost(admin, "/api/v1/admin/activities/{id}/cancel", activity)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(onlySent().to()).isEqualTo(email("v"));
        adminPost(admin, "/api/v1/admin/activities/{id}/cancel", activity).andExpect(status().isConflict());
        adminPost(admin, "/api/v1/admin/activities/{id}/cancel", 0L).andExpect(status().isNotFound());
    }

    // --- helpers ---

    private Notification onlySent() {
        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(sender, atLeastOnce()).send(sent.capture());
        assertThat(sent.getAllValues()).hasSize(1);
        return sent.getValue();
    }

    private ResultActions adminPost(Cookie session, String url, long id) throws Exception {
        return mockMvc.perform(post(url, id).with(csrf()).cookie(session));
    }

    private ApplicationStatus statusOf(long applicationId) {
        return applicationRepository.findById(applicationId).orElseThrow().getStatus();
    }

    private ActivityStatus activityStatus(long activityId) {
        return activityRepository.findById(activityId).orElseThrow().getStatus();
    }

    private void moveToThePast(long activityId) {
        jdbcTemplate.update("UPDATE activities SET starts_at = starts_at - interval '10 years',"
                + " ends_at = ends_at - interval '10 years' WHERE id = ?", activityId);
    }

    private String email(String name) {
        return prefix + "_" + name + "@example.com";
    }

    private long userId(String name) {
        return appUserRepository.findAppUserByUsername(prefix + "_" + name).orElseThrow().getId();
    }

    private Cookie registerOrganization(String name) throws Exception {
        String username = prefix + "_" + name;
        register("/api/v1/auth/register/organization", """
                { "username": "%s", "password": "password1", "email": "%s@example.com", "organizationName": "Org" }
                """.formatted(username, username));
        return login(username, "password1");
    }

    private Cookie registerVolunteer(String name) throws Exception {
        String username = prefix + "_" + name;
        register("/api/v1/auth/register/volunteer", """
                { "username": "%s", "password": "password1", "email": "%s@example.com",
                  "firstName": "First", "lastName": "Last", "dateOfBirth": "2000-01-01" }
                """.formatted(username, username));
        return login(username, "password1");
    }

    private static String activityJson(String start, String end) {
        return """
                { "title": "Help", "lat": 50.08, "lon": 14.42, "startsAt": "%sT%s", "endsAt": "%sT%s", "spotsNeeded": 2 }
                """.formatted(DAY, start, DAY, end);
    }

    private long createActivity(String start, String end, int spots) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(org)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "title": "Help", "lat": 50.08, "lon": 14.42, "startsAt": "%sT%s", "endsAt": "%sT%s", "spotsNeeded": %d }
                                """.formatted(DAY, start, DAY, end, spots)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long publishedActivity(String start, String end, int spots) throws Exception {
        long id = createActivity(start, end, spots);
        mockMvc.perform(post("/api/v1/activities/{id}/publish", id).with(csrf()).cookie(org)).andExpect(status().isOk());
        return id;
    }

    private long publishedActivity() throws Exception {
        return publishedActivity("09:00", "12:00", 2);
    }

    private long applicationId(Cookie volunteer, long activityId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities/{id}/applications", activityId).with(csrf()).cookie(volunteer))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void accept(long applicationId) throws Exception {
        mockMvc.perform(post("/api/v1/applications/{id}/accept", applicationId).with(csrf()).cookie(org))
                .andExpect(status().isOk());
    }

    private long invitationId(long activityId, long volunteerId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities/{id}/invitations", activityId).with(csrf()).cookie(org)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"volunteerId\": %d }".formatted(volunteerId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void setCapacity(Cookie volunteer, int capacity) throws Exception {
        mockMvc.perform(put("/api/v1/volunteers/me").with(csrf()).cookie(volunteer)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"maxActiveAssignments\": %d }".formatted(capacity)))
                .andExpect(status().isOk());
    }

    private void makeAvailable(Cookie volunteer) throws Exception {
        mockMvc.perform(put("/api/v1/volunteers/me/availability").with(csrf()).cookie(volunteer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"slots\": [ { \"dayOfWeek\": \"%s\", \"start\": \"08:00\", \"end\": \"18:00\" } ] }"
                                .formatted(DAY.getDayOfWeek())))
                .andExpect(status().isOk());
    }
}
