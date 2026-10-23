package com.volunnear.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunnear.application.ApplicationRepository;
import com.volunnear.application.ApplicationStatus;
import com.volunnear.auth.AppUserRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Q7a … Q7f and Q8d end to end. Not @Transactional: notifications are sent only after a real commit, so every request
 * commits as in production; the data is removed after each test (names carry a random prefix, as in
 * ApplicationConcurrencyIntegrationTest). The sender is a mock that records what reached it.
 */
class NotificationIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate DAY = LocalDate.now().plusDays(30);

    @MockBean
    private NotificationSender sender;
    @Autowired
    private Notifier notifier;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String prefix;
    private Cookie org;

    @BeforeEach
    void setUp() throws Exception {
        prefix = "n" + UUID.randomUUID().toString().substring(0, 8);
        register("/api/v1/auth/register/organization", """
                { "username": "%s_org", "password": "password1", "email": "%s_org@example.com", "organizationName": "Org" }
                """.formatted(prefix, prefix));
        org = login(prefix + "_org", "password1");
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
    void acceptSendsOneNotificationWithTheOrganizationsContactAfterCommit() throws Exception {
        long activity = publishedActivity(1);
        long application = applicationId(registerVolunteer("v"), activity);

        accept(application).andExpect(status().isOk());

        Notification sent = onlySent();
        assertThat(sent.to()).isEqualTo(email("v"));
        assertThat(sent.subject()).isEqualTo("Your application was accepted");
        assertThat(sent.body()).contains(email("org"));
    }

    @Test
    void refusedAcceptSendsNothing() throws Exception {
        long activity = publishedActivity(1);
        long first = applicationId(registerVolunteer("v1"), activity);
        long second = applicationId(registerVolunteer("v2"), activity);
        accept(first).andExpect(status().isOk());
        clearInvocations(sender);

        accept(second).andExpect(status().isConflict());

        verifyNoInteractions(sender);
    }

    @Test
    void notificationPublishedInARolledBackTransactionIsNeverSent() throws Exception {
        // The case a 409 before the notifier call cannot show: the event is already published, then the
        // transaction rolls back (as after an optimistic-lock failure at commit).
        long activity = publishedActivity(1);
        long application = applicationId(registerVolunteer("v"), activity);

        transactionTemplate.executeWithoutResult(tx -> {
            notifier.applicationRejected(applicationRepository.findById(application).orElseThrow());
            tx.setRollbackOnly();
        });
        verifyNoInteractions(sender);

        transactionTemplate.executeWithoutResult(tx ->
                notifier.applicationRejected(applicationRepository.findById(application).orElseThrow()));
        verify(sender).send(any());
    }

    @Test
    void failingSenderLeavesTheAcceptCommitted() throws Exception {
        long activity = publishedActivity(1);
        long application = applicationId(registerVolunteer("v"), activity);
        doThrow(new IllegalStateException("SMTP down")).when(sender).send(any());

        accept(application).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(applicationRepository.findById(application).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(sender).send(any());
    }

    @Test
    void organizationSeesTheVolunteerEmailOnlyOnAcceptedRows() throws Exception {
        // Q7d
        long activity = publishedActivity(2);
        long accepted = applicationId(registerVolunteer("v1"), activity);
        applicationId(registerVolunteer("v2"), activity);
        accept(accepted).andExpect(status().isOk()).andExpect(jsonPath("$.volunteerEmail").value(email("v1")));

        mockMvc.perform(get("/api/v1/activities/{id}/applications", activity).cookie(org))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].volunteerEmail").value(email("v1")))
                .andExpect(jsonPath("$[1].status").value("PENDING"))
                .andExpect(jsonPath("$[1].volunteerEmail").value(nullValue()));
    }

    @Test
    void rejectNotifiesTheVolunteer() throws Exception {
        long activity = publishedActivity(1);
        long application = applicationId(registerVolunteer("v"), activity);

        mockMvc.perform(post("/api/v1/applications/{id}/reject", application).with(csrf()).cookie(org))
                .andExpect(status().isOk());

        assertThat(onlySent().to()).isEqualTo(email("v"));
        assertThat(onlySent().subject()).isEqualTo("Your application was not accepted");
    }

    @Test
    void withdrawNotifiesTheOrganizationOnlyForAnAcceptedApplication() throws Exception {
        long activity = publishedActivity(2);
        Cookie pendingVolunteer = registerVolunteer("v1");
        long pending = applicationId(pendingVolunteer, activity);
        Cookie acceptedVolunteer = registerVolunteer("v2");
        long accepted = applicationId(acceptedVolunteer, activity);
        accept(accepted).andExpect(status().isOk());
        clearInvocations(sender);

        withdraw(pending, pendingVolunteer);
        verifyNoInteractions(sender);

        withdraw(accepted, acceptedVolunteer);
        assertThat(onlySent().to()).isEqualTo(email("org"));
        assertThat(onlySent().subject()).isEqualTo("A volunteer withdrew");
    }

    @Test
    void invitationNotifiesTheVolunteerAndTheDeclineNotifiesTheOrganization() throws Exception {
        long activity = publishedActivity(1);
        Cookie volunteer = registerVolunteer("v");
        makeAvailable(volunteer);
        long invitation = invitationId(activity, userId("v"));
        assertThat(onlySent().to()).isEqualTo(email("v"));
        assertThat(onlySent().subject()).isEqualTo("You are invited to an activity");
        clearInvocations(sender);

        mockMvc.perform(post("/api/v1/invitations/{id}/decline", invitation).with(csrf()).cookie(volunteer))
                .andExpect(status().isOk());

        assertThat(onlySent().to()).isEqualTo(email("org"));
        assertThat(onlySent().subject()).isEqualTo("Invitation declined");
    }

    @Test
    void acceptedInvitationNotifiesTheOrganization() throws Exception {
        long activity = publishedActivity(1);
        Cookie volunteer = registerVolunteer("v");
        makeAvailable(volunteer);
        long invitation = invitationId(activity, userId("v"));
        clearInvocations(sender);

        mockMvc.perform(post("/api/v1/invitations/{id}/accept", invitation).with(csrf()).cookie(volunteer))
                .andExpect(status().isOk());

        assertThat(onlySent().to()).isEqualTo(email("org"));
        assertThat(onlySent().subject()).isEqualTo("Invitation accepted");
    }

    @Test
    void cancelNotifiesPendingAndAcceptedVolunteersButNotRejectedOnes() throws Exception {
        // Q7b
        long activity = publishedActivity(2);
        accept(applicationId(registerVolunteer("v1"), activity)).andExpect(status().isOk());
        long rejected = applicationId(registerVolunteer("v2"), activity);
        mockMvc.perform(post("/api/v1/applications/{id}/reject", rejected).with(csrf()).cookie(org))
                .andExpect(status().isOk());
        applicationId(registerVolunteer("v3"), activity);
        clearInvocations(sender);

        mockMvc.perform(post("/api/v1/activities/{id}/cancel", activity).with(csrf()).cookie(org))
                .andExpect(status().isOk());

        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(sender, times(2)).send(sent.capture());
        assertThat(sent.getAllValues()).map(Notification::to).containsExactlyInAnyOrder(email("v1"), email("v3"));
        assertThat(sent.getAllValues()).map(Notification::subject).containsOnly("Activity cancelled");
    }

    @Test
    void completionNotifiesAcceptedVolunteersOnly() throws Exception {
        // Q8d
        long activity = publishedActivity(2);
        accept(applicationId(registerVolunteer("v1"), activity)).andExpect(status().isOk());
        applicationId(registerVolunteer("v2"), activity);
        jdbcTemplate.update("UPDATE activities SET starts_at = starts_at - interval '10 years',"
                + " ends_at = ends_at - interval '10 years' WHERE id = ?", activity);
        clearInvocations(sender);

        mockMvc.perform(post("/api/v1/activities/{id}/complete", activity).with(csrf()).cookie(org))
                .andExpect(status().isOk());

        assertThat(onlySent().to()).isEqualTo(email("v1"));
        assertThat(onlySent().subject()).isEqualTo("Activity completed");
    }

    private Notification onlySent() {
        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(sender).send(sent.capture());
        return sent.getValue();
    }

    private ResultActions accept(long applicationId) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/accept", applicationId).with(csrf()).cookie(org));
    }

    private void withdraw(long applicationId, Cookie volunteer) throws Exception {
        mockMvc.perform(post("/api/v1/applications/{id}/withdraw", applicationId).with(csrf()).cookie(volunteer))
                .andExpect(status().isOk());
    }

    private String email(String name) {
        return prefix + "_" + name + "@example.com";
    }

    private long userId(String name) {
        return appUserRepository.findAppUserByUsername(prefix + "_" + name).orElseThrow().getId();
    }

    private Cookie registerVolunteer(String name) throws Exception {
        String username = prefix + "_" + name;
        register("/api/v1/auth/register/volunteer", """
                { "username": "%s", "password": "password1", "email": "%s@example.com",
                  "firstName": "First", "lastName": "Last", "dateOfBirth": "2000-01-01" }
                """.formatted(username, username));
        return login(username, "password1");
    }

    private long publishedActivity(int spots) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(org)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "title": "Help", "lat": 50.08, "lon": 14.42, "startsAt": "%sT09:00", "endsAt": "%sT12:00", "spotsNeeded": %d }
                                """.formatted(DAY, DAY, spots)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        mockMvc.perform(post("/api/v1/activities/{id}/publish", id).with(csrf()).cookie(org)).andExpect(status().isOk());
        return id;
    }

    private void makeAvailable(Cookie volunteer) throws Exception {
        mockMvc.perform(put("/api/v1/volunteers/me/availability").with(csrf()).cookie(volunteer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"slots\": [ { \"dayOfWeek\": \"%s\", \"start\": \"08:00\", \"end\": \"18:00\" } ] }"
                                .formatted(DAY.getDayOfWeek())))
                .andExpect(status().isOk());
    }

    private long invitationId(long activityId, long volunteerId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities/{id}/invitations", activityId).with(csrf()).cookie(org)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"volunteerId\": %d }".formatted(volunteerId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long applicationId(Cookie volunteer, long activityId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities/{id}/applications", activityId).with(csrf()).cookie(volunteer))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
