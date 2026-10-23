package com.volunnear.distribution;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Map;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 6c on a small fixed dataset. Expected scores are computed by hand with the global weights
 * 0.4 / 0.4 / 0.2 and the half-distance 10 km, so u_geo = 2^(-d / 10 km).
 * Distances are along the meridian 14.42 E, where the haversine distance is 6371 km * (delta lat in radians):
 * 0.03 deg = 3.3358 km, 0.06 deg = 6.6717 km, 0.09 deg = 10.0075 km.
 */
@Transactional
class RecommendationIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate DAY = LocalDate.now().plusDays(30);
    private static final double LAT = 50.08;
    private static final double LON = 14.42;
    private static final double EPS = 1e-4;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ActivityRepository activityRepository;

    private Cookie org;
    private Cookie otherOrg;
    private long skill1;
    private long skill2;

    @BeforeEach
    void setUp() throws Exception {
        org = registerOrganization("org");
        otherOrg = registerOrganization("otherOrg");
        JsonNode skills = objectMapper.readTree(mockMvc.perform(get("/api/v1/skills"))
                .andReturn().getResponse().getContentAsString());
        skill1 = skills.get(0).get("id").asLong();
        skill2 = skills.get(1).get("id").asLong();
    }

    // --- activity -> volunteers ---

    @Test
    void volunteersAreRankedWithHandComputedScores() throws Exception {
        long activity = publishedActivity(org, LAT, "\"priority\": \"HIGH\", \"requiredSkillIds\": [%d, %d]".formatted(skill1, skill2));
        // A: at the activity, one of two skills.  geo 1, skill sqrt(1/2) = 0.70711, prio 2/3
        //    total = 0.4 + 0.4 * 0.70711 + 0.2 * 2/3 = 0.81618
        long a = volunteer("volA", LAT, LON, null, 1, skill1).id();
        // B: 0.09 deg north, both skills.  geo 2^(-1.00075) = 0.49974, skill 1
        //    total = 0.4 * 0.49974 + 0.4 + 0.13333 = 0.73323
        long b = volunteer("volB", LAT + 0.09, LON, null, 1, skill1, skill2).id();
        // C: no location (Q5), both skills.  total = 0 + 0.4 + 0.13333 = 0.53333
        long c = volunteer("volC", null, null, null, 1, skill1, skill2).id();
        // Absent: no availability (infeasible), capacity 0 (Q3e), already applied (Q15), banned.
        registerVolunteer("noSlots");
        volunteer("zeroCapacity", LAT, LON, null, 0, skill1, skill2);
        Cookie applied = volunteer("applied", LAT, LON, null, 1, skill1, skill2).session();
        mockMvc.perform(post("/api/v1/activities/{id}/applications", activity).with(csrf()).cookie(applied))
                .andExpect(status().isCreated());
        ban(volunteer("banned", LAT, LON, null, 1, skill1, skill2).id());

        recommendedVolunteers(org, activity, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].volunteer.id").value(contains((int) a, (int) b, (int) c)))
                .andExpect(jsonPath("$[0].score.geo").value(closeTo(1.0, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.skill").value(closeTo(0.70711, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.priority").value(closeTo(2.0 / 3, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.total").value(closeTo(0.81618, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.geo").value(closeTo(0.49974, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.skill").value(closeTo(1.0, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.total").value(closeTo(0.73323, EPS), Double.class))
                .andExpect(jsonPath("$[2].score.geo").value(closeTo(0.0, EPS), Double.class))
                .andExpect(jsonPath("$[2].score.total").value(closeTo(0.53333, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.alpha").value(closeTo(0.4, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.beta").value(closeTo(0.4, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.gamma").value(closeTo(0.2, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.halfDistanceKm").value(closeTo(10.0, EPS), Double.class))
                // Q3a: no account data in a view by an organization
                .andExpect(jsonPath("$[0].volunteer.username").doesNotExist())
                .andExpect(jsonPath("$[0].volunteer.email").doesNotExist());

        recommendedVolunteers(org, activity, "?limit=1")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].volunteer.id").value(a));
    }

    @Test
    void overrideChangesTheVolunteerRanking() throws Exception {
        // Only skills count: B and C both 1, tie broken by the larger u_geo (B), then A with 0.70711.
        long activity = publishedActivity(org, LAT, "\"priority\": \"HIGH\", \"requiredSkillIds\": [%d, %d], %s"
                .formatted(skill1, skill2, onlySkills()));
        long a = volunteer("volA", LAT, LON, null, 1, skill1).id();
        long b = volunteer("volB", LAT + 0.09, LON, null, 1, skill1, skill2).id();
        long c = volunteer("volC", null, null, null, 1, skill1, skill2).id();

        recommendedVolunteers(org, activity, "")
                .andExpect(jsonPath("$[*].volunteer.id").value(contains((int) b, (int) c, (int) a)))
                .andExpect(jsonPath("$[0].score.total").value(closeTo(1.0, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.total").value(closeTo(1.0, EPS), Double.class))
                .andExpect(jsonPath("$[2].score.total").value(closeTo(0.70711, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.alpha").value(closeTo(0.0, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.beta").value(closeTo(1.0, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.halfDistanceKm").value(closeTo(10.0, EPS), Double.class));
    }

    @Test
    void onlyTheOwningOrganizationGetsRecommendations() throws Exception {
        long activity = publishedActivity(org, LAT, "");
        Cookie vol = volunteer("vol", LAT, LON, null, 1).session();

        recommendedVolunteers(otherOrg, activity, "").andExpect(status().isForbidden());
        recommendedVolunteers(vol, activity, "").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/activities/{id}/recommended-volunteers", activity))
                .andExpect(status().isUnauthorized());
        recommendedVolunteers(org, 999_999L, "").andExpect(status().isNotFound());
        recommendedVolunteers(org, activity, "").andExpect(status().isOk());
    }

    @Test
    void activityThatIsNotOpenOrHasStartedIs409() throws Exception {
        long draft = createActivity(org, LAT, "");
        recommendedVolunteers(org, draft, "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("OPEN")));

        long cancelled = publishedActivity(org, LAT, "");
        mockMvc.perform(post("/api/v1/activities/{id}/cancel", cancelled).with(csrf()).cookie(org))
                .andExpect(status().isOk());
        recommendedVolunteers(org, cancelled, "").andExpect(status().isConflict());

        long started = publishedActivity(org, LAT, "");
        moveToThePast(started);
        recommendedVolunteers(org, started, "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("started")));
    }

    @Test
    void limitMustBeBetween1And50() throws Exception {
        long activity = publishedActivity(org, LAT, "");
        Cookie vol = volunteer("vol", LAT, LON, null, 1).session();

        recommendedVolunteers(org, activity, "?limit=0").andExpect(status().isBadRequest());
        recommendedVolunteers(org, activity, "?limit=51").andExpect(status().isBadRequest());
        recommendedVolunteers(org, activity, "?limit=50").andExpect(status().isOk());
        recommendedActivities(vol, "?limit=0").andExpect(status().isBadRequest());
        recommendedActivities(vol, "?limit=51").andExpect(status().isBadRequest());
    }

    // --- volunteer -> activities ---

    @Test
    void activitiesAreRankedWithGlobalWeightsEvenWithAnOverride() throws Exception {
        // X: at the volunteer, one of two skills, HIGH, override "only skills".
        //    Global: 0.4 + 0.4 * 0.70711 + 0.2 * 2/3 = 0.81618. With the override it would be 0.70711 and fall below Y.
        long x = publishedActivity(org, LAT, "\"priority\": \"HIGH\", \"requiredSkillIds\": [%d, %d], %s"
                .formatted(skill1, skill2, onlySkills()));
        // Y: 0.03 deg north, nothing required, LOW.  geo 2^(-0.33358) = 0.79356
        //    total = 0.4 * 0.79356 + 0.4 * 1 + 0 = 0.71742
        long y = publishedActivity(otherOrg, LAT + 0.03, "\"priority\": \"LOW\"");
        // Absent: a draft, an activity that has started, a full one.
        createActivity(org, LAT, "");
        moveToThePast(publishedActivity(org, LAT, ""));
        long full = publishedActivity(org, LAT, 1, "");
        Vol taken = volunteer("taken", LAT, LON, null, 1);
        accept(taken.session(), full);
        Cookie vol = volunteer("vol", LAT, LON, null, 1, skill1).session();

        recommendedActivities(vol, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].activity.id").value(contains((int) x, (int) y)))
                .andExpect(jsonPath("$[0].score.total").value(closeTo(0.81618, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.alpha").value(closeTo(0.4, EPS), Double.class))
                .andExpect(jsonPath("$[0].score.weights.beta").value(closeTo(0.4, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.geo").value(closeTo(0.79356, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.skill").value(closeTo(1.0, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.priority").value(closeTo(0.0, EPS), Double.class))
                .andExpect(jsonPath("$[1].score.total").value(closeTo(0.71742, EPS), Double.class));
    }

    @Test
    void radiusPrefilterDropsActivitiesOutsideTheRadius() throws Exception {
        long inside = publishedActivity(org, LAT + 0.03, "");   // 3.34 km
        long outside = publishedActivity(org, LAT + 0.06, "");  // 6.67 km
        Cookie withRadius = volunteer("near", LAT, LON, 5, 1).session();
        Cookie withoutRadius = volunteer("far", LAT, LON, null, 1).session();
        Cookie withoutLocation = volunteer("nowhere", null, null, 5, 1).session();

        recommendedActivities(withRadius, "")
                .andExpect(jsonPath("$[*].activity.id").value(contains((int) inside)));
        // Q3g: no radius, no prefilter; Q5: no location, no prefilter and u_geo = 0.
        recommendedActivities(withoutRadius, "")
                .andExpect(jsonPath("$[*].activity.id").value(contains((int) inside, (int) outside)));
        recommendedActivities(withoutLocation, "")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].score.geo").value(closeTo(0.0, EPS), Double.class));
    }

    @Test
    void organizationCannotAskForVolunteerRecommendations() throws Exception {
        recommendedActivities(org, "").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/volunteers/me/recommended-activities")).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private static String onlySkills() {
        return "\"weights\": { \"alpha\": 0, \"beta\": 1, \"gamma\": 0 }";
    }

    private ResultActions recommendedVolunteers(Cookie session, long activityId, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/activities/" + activityId + "/recommended-volunteers" + query).cookie(session));
    }

    private ResultActions recommendedActivities(Cookie session, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/volunteers/me/recommended-activities" + query).cookie(session));
    }

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

    private record Vol(long id, Cookie session) {
    }

    /** A volunteer available 08:00-18:00 on the activity day. */
    private Vol volunteer(String name, Double lat, Double lon, Integer radius, int capacity, long... skillIds) throws Exception {
        Cookie session = registerVolunteer(name);
        String location = lat == null ? "" : "\"lat\": %s, \"lon\": %s,".formatted(lat, lon);
        String radiusJson = radius == null ? "" : "\"radius\": %d,".formatted(radius);
        String body = mockMvc.perform(put("/api/v1/volunteers/me").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ %s %s \"maxActiveAssignments\": %d }".formatted(location, radiusJson, capacity)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(put("/api/v1/volunteers/me/skills").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("skillIds", skillIds))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/volunteers/me/availability").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"slots\": [ { \"dayOfWeek\": \"%s\", \"start\": \"08:00\", \"end\": \"18:00\" } ] }"
                                .formatted(DAY.getDayOfWeek())))
                .andExpect(status().isOk());
        return new Vol(objectMapper.readTree(body).get("id").asLong(), session);
    }

    private String activityJson(double lat, int spots, String extra) {
        return """
                { "title": "Help", "lat": %s, "lon": %s, "startsAt": "%sT09:00", "endsAt": "%sT12:00", "spotsNeeded": %d %s }
                """.formatted(lat, LON, DAY, DAY, spots, extra.isBlank() ? "" : ", " + extra);
    }

    private long createActivity(Cookie session, double lat, String extra) throws Exception {
        return createActivity(session, lat, 2, extra);
    }

    private long createActivity(Cookie session, double lat, int spots, String extra) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(activityJson(lat, spots, extra)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long publishedActivity(Cookie session, double lat, String extra) throws Exception {
        return publishedActivity(session, lat, 2, extra);
    }

    private long publishedActivity(Cookie session, double lat, int spots, String extra) throws Exception {
        long id = createActivity(session, lat, spots, extra);
        mockMvc.perform(post("/api/v1/activities/{id}/publish", id).with(csrf()).cookie(session))
                .andExpect(status().isOk());
        return id;
    }

    /** The volunteer applies and the activity's organization (always org here) accepts. */
    private void accept(Cookie volunteer, long activityId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/activities/{id}/applications", activityId).with(csrf()).cookie(volunteer))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/v1/applications/{id}/accept", objectMapper.readTree(body).get("id").asLong())
                        .with(csrf()).cookie(org))
                .andExpect(status().isOk());
    }

    private void moveToThePast(long activityId) {
        entityManager.flush();
        Activity activity = activityRepository.findById(activityId).orElseThrow();
        activity.setStartsAt(activity.getStartsAt().minusYears(10));
        activity.setEndsAt(activity.getEndsAt().minusYears(10));
        entityManager.flush();
    }

    private void ban(long userId) {
        entityManager.flush();
        entityManager.createQuery("update AppUser u set u.isBanned = true where u.id = :id")
                .setParameter("id", userId).executeUpdate();
        entityManager.clear();
    }
}
