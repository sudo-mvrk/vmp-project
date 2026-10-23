package com.volunnear.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunnear.activity.ActivityRepository;
import com.volunnear.activity.ActivityService;
import com.volunnear.activity.ActivityStatus;
import com.volunnear.auth.AppUserRepository;
import com.volunnear.auth.CustomUserDetails;
import com.volunnear.common.exception.ConflictException;
import com.volunnear.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Q5d under real concurrency. Not @Transactional: every request commits, so the threads see each other's
 * work exactly as in production. The data is removed after each test; names carry a random prefix.
 */
class ApplicationConcurrencyIntegrationTest extends AbstractIntegrationTest {
    private static final int THREADS = 6;
    private static final LocalDate DAY = LocalDate.now().plusDays(30);

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String prefix;
    private Cookie org;
    private CustomUserDetails orgUser;

    @BeforeEach
    void setUp() throws Exception {
        prefix = "c" + UUID.randomUUID().toString().substring(0, 8);
        register("/api/v1/auth/register/organization", """
                { "username": "%s_org", "password": "password1", "email": "%s_org@example.com", "organizationName": "Org" }
                """.formatted(prefix, prefix));
        org = login(prefix + "_org", "password1");
        orgUser = new CustomUserDetails(appUserRepository.findAppUserByUsername(prefix + "_org").orElseThrow());
    }

    @AfterEach
    void cleanUp() {
        // Organizations first (cascades to activities and applications), then the accounts (volunteers, members).
        jdbcTemplate.update("""
                DELETE FROM organizations WHERE id IN (
                    SELECT m.organization_id FROM organization_member m JOIN app_users u ON u.id = m.user_id
                    WHERE u.username LIKE ?)
                """, prefix + "%");
        jdbcTemplate.update("DELETE FROM app_users WHERE username LIKE ?", prefix + "%");
    }

    @Test
    void manyAcceptsForTheLastSpotLetExactlyOneWin() throws Exception {
        long activity = publishedActivity("09:00", "12:00", 1);
        List<Long> applications = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Cookie volunteer = registerVolunteer("v" + i);
            applications.add(applicationId(volunteer, activity));
        }

        List<Throwable> outcomes = runAtOnce(applications.stream()
                .map(id -> (Callable<Object>) () -> applicationService.accept(id, orgUser))
                .toList());

        assertThat(outcomes).filteredOn(o -> o == null).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o != null).hasSize(THREADS - 1).allSatisfy(failure ->
                assertThat(failure).isInstanceOf(ConflictException.class).hasMessageContaining("no free spots"));
        assertThat(acceptedCount(activity)).isEqualTo(1);
    }

    @Test
    void acceptsOnDifferentActivitiesRespectCapacityOne() throws Exception {
        Cookie volunteer = registerVolunteer("v");
        List<Long> applications = new ArrayList<>();
        // Same day, separate hours: only capacity can stop the second accept.
        for (int i = 0; i < THREADS; i++) {
            long activity = publishedActivity("%02d:00".formatted(8 + i), "%02d:30".formatted(8 + i), 5);
            applications.add(applicationId(volunteer, activity));
        }

        List<Throwable> outcomes = runAtOnce(applications.stream()
                .map(id -> (Callable<Object>) () -> applicationService.accept(id, orgUser))
                .toList());

        assertThat(outcomes).filteredOn(o -> o == null).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o != null).allSatisfy(failure ->
                assertThat(failure).isInstanceOf(ConflictException.class).hasMessageContaining("capacity"));
        assertThat(applicationRepository.findAll()).filteredOn(a ->
                applications.contains(a.getId()) && a.getStatus() == ApplicationStatus.ACCEPTED).hasSize(1);
    }

    @Test
    void acceptAndWithdrawOfTheSameApplicationDoNotBothWin() throws Exception {
        long activity = publishedActivity("09:00", "12:00", 1);
        Cookie volunteer = registerVolunteer("v");
        long application = applicationId(volunteer, activity);
        CustomUserDetails volunteerUser = new CustomUserDetails(appUserRepository.findAppUserByUsername(prefix + "_v").orElseThrow());

        List<Throwable> outcomes = runAtOnce(List.of(
                () -> applicationService.accept(application, orgUser),
                () -> applicationService.withdraw(application, volunteerUser)));

        // Either order is valid (accept then withdraw, or withdraw then a refused accept); the row ends consistent.
        ApplicationStatus status = applicationRepository.findById(application).orElseThrow().getStatus();
        assertThat(status).isIn(ApplicationStatus.ACCEPTED, ApplicationStatus.WITHDRAWN);
        outcomes.stream().filter(o -> o != null).forEach(failure -> assertThat(failure)
                .isInstanceOfAny(ConflictException.class, ObjectOptimisticLockingFailureException.class));
    }

    @Test
    void manyInvitedVolunteersAcceptingTheLastSpotLetExactlyOneWin() throws Exception {
        // Q5k, Q5l: invitations go through the same locked accept as applications.
        long activity = publishedActivity("09:00", "12:00", 1);
        List<Callable<Object>> accepts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String name = "v" + i;
            Cookie volunteer = registerVolunteer(name);
            makeAvailable(volunteer);
            CustomUserDetails volunteerUser = new CustomUserDetails(
                    appUserRepository.findAppUserByUsername(prefix + "_" + name).orElseThrow());
            long invitation = invitationId(activity, volunteerUser.getUser().getId());
            accepts.add(() -> applicationService.acceptInvitation(invitation, volunteerUser));
        }

        List<Throwable> outcomes = runAtOnce(accepts);

        assertThat(outcomes).filteredOn(o -> o == null).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o != null).hasSize(THREADS - 1).allSatisfy(failure ->
                assertThat(failure).isInstanceOf(ConflictException.class).hasMessageContaining("no free spots"));
        assertThat(acceptedCount(activity)).isEqualTo(1);
    }

    @Test
    void cancelAndCompleteOfTheSameActivityDoNotBothWin() throws Exception {
        // Q8a: both end an open activity; without the row lock the later write silently overwrites the first.
        long activity = publishedActivity("09:00", "12:00", 1);
        jdbcTemplate.update("UPDATE activities SET starts_at = starts_at - interval '10 years',"
                + " ends_at = ends_at - interval '10 years' WHERE id = ?", activity);

        List<Throwable> outcomes = runAtOnce(List.of(
                () -> activityService.cancel(activity, orgUser),
                () -> activityService.complete(activity, orgUser)));

        assertThat(outcomes).filteredOn(o -> o == null).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o != null).singleElement()
                .isInstanceOf(ConflictException.class);
        assertThat(activityRepository.findById(activity).orElseThrow().getStatus())
                .isIn(ActivityStatus.CANCELLED, ActivityStatus.COMPLETED);
    }

    /** Starts all tasks together; returns null for a task that succeeded, otherwise its exception. */
    private static List<Throwable> runAtOnce(List<Callable<Object>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        start.await();
                        return task.call();
                    }))
                    .toList();
            start.countDown();
            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    outcomes.add(null);
                } catch (ExecutionException e) {
                    outcomes.add(e.getCause());
                }
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private long acceptedCount(long activityId) {
        return applicationRepository.findByActivity_IdOrderByAppliedAtAsc(activityId).stream()
                .filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
    }

    private Cookie registerVolunteer(String name) throws Exception {
        String username = prefix + "_" + name;
        register("/api/v1/auth/register/volunteer", """
                { "username": "%s", "password": "password1", "email": "%s@example.com",
                  "firstName": "First", "lastName": "Last", "dateOfBirth": "2000-01-01" }
                """.formatted(username, username));
        return login(username, "password1");
    }

    private long publishedActivity(String start, String end, int spots) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(org)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "title": "Help", "lat": 50.08, "lon": 14.42, "startsAt": "%sT%s", "endsAt": "%sT%s", "spotsNeeded": %d }
                                """.formatted(DAY, start, DAY, end, spots)))
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
