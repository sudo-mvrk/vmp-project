package com.volunnear.volunteer;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class VolunteerProfileIntegrationTest extends AbstractIntegrationTest {
    private static final String VOLUNTEER_A_JSON = """
            {
                "username": "volA", "password": "volPassword1", "email": "volA@example.com",
                "firstName": "Vera", "lastName": "Novak", "dateOfBirth": "2000-01-01"
            }
            """;
    private static final String VOLUNTEER_B_JSON = """
            {
                "username": "volB", "password": "volPassword1", "email": "volB@example.com",
                "firstName": "Boris", "lastName": "Kral", "dateOfBirth": "1999-05-05"
            }
            """;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager entityManager;

    private Cookie volA;
    private Cookie volB;
    private long skillId;
    private long otherSkillId;
    private long certificationId;

    @BeforeEach
    void setUp() throws Exception {
        register("/api/v1/auth/register/volunteer", VOLUNTEER_A_JSON);
        register("/api/v1/auth/register/volunteer", VOLUNTEER_B_JSON);
        volA = login("volA", "volPassword1");
        volB = login("volB", "volPassword1");

        JsonNode skills = readJson(mockMvc.perform(get("/api/v1/skills")).andExpect(status().isOk()));
        skillId = skills.get(0).get("id").asLong();
        otherSkillId = skills.get(1).get("id").asLong();
        JsonNode certifications = readJson(mockMvc.perform(get("/api/v1/certifications")).andExpect(status().isOk()));
        certificationId = certifications.get(0).get("id").asLong();
    }

    @Test
    void dictionariesAreSeededAndPublic() throws Exception {
        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)))
                .andExpect(jsonPath("$[0].category").exists());
        mockMvc.perform(get("/api/v1/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void newVolunteerHasDefaultCapacityAndOwnProfileShowsUsername() throws Exception {
        mockMvc.perform(get("/api/v1/volunteers/me").cookie(volA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("volA"))
                .andExpect(jsonPath("$.firstName").value("Vera"))
                .andExpect(jsonPath("$.dateOfBirth").value("2000-01-01"))
                .andExpect(jsonPath("$.maxActiveAssignments").value(1))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void fullProfileRoundTrip() throws Exception {
        String validUntil = LocalDate.now().plusYears(1).toString();

        putA("", """
                { "bio": "Likes gardening", "lat": 0.0, "lon": 14.42, "city": "Prague", "countryCode": "CZ",
                  "radius": 25, "maxActiveAssignments": 3 }
                """).andExpect(status().isOk());
        putA("/skills", "{ \"skillIds\": [%d, %d] }".formatted(skillId, otherSkillId)).andExpect(status().isOk());
        putA("/certifications", """
                { "certifications": [ { "certificationId": %d, "validUntil": "%s" } ] }
                """.formatted(certificationId, validUntil)).andExpect(status().isOk());
        putA("/availability", """
                { "slots": [
                    { "dayOfWeek": "SATURDAY", "start": "09:00", "end": "12:00" },
                    { "dayOfWeek": "MONDAY", "start": "18:00", "end": "21:00" },
                    { "dayOfWeek": "SATURDAY", "start": "12:00", "end": "15:00" } ] }
                """).andExpect(status().isOk());
        flushAndClear();

        mockMvc.perform(get("/api/v1/volunteers/me").cookie(volA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Likes gardening"))
                .andExpect(jsonPath("$.lat").value(0.0))
                .andExpect(jsonPath("$.lon").value(14.42))
                .andExpect(jsonPath("$.radius").value(25))
                .andExpect(jsonPath("$.maxActiveAssignments").value(3))
                .andExpect(jsonPath("$.firstName").value("Vera"))
                .andExpect(jsonPath("$.skills", hasSize(2)))
                .andExpect(jsonPath("$.certifications[0].certificationId").value(certificationId))
                .andExpect(jsonPath("$.certifications[0].validUntil").value(validUntil))
                .andExpect(jsonPath("$.availability", hasSize(3)))
                .andExpect(jsonPath("$.availability[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.availability[1].start").value("09:00:00"));
    }

    @Test
    void emptyListsClearCollections() throws Exception {
        putA("/skills", "{ \"skillIds\": [%d] }".formatted(skillId)).andExpect(status().isOk());

        putA("/skills", "{ \"skillIds\": [] }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills", hasSize(0)));
    }

    @Test
    void replacingWithTheSameRowsTwiceWorks() throws Exception {
        // Collection rows have composite primary keys; the old rows must be deleted before the new ones are written.
        String slots = """
                { "slots": [ { "dayOfWeek": "MONDAY", "start": "09:00", "end": "12:00" } ] }
                """;
        String certifications = "{ \"certifications\": [ { \"certificationId\": %d } ] }".formatted(certificationId);
        for (int i = 0; i < 2; i++) {
            putA("/availability", slots).andExpect(status().isOk());
            putA("/certifications", certifications).andExpect(status().isOk());
            putA("/skills", "{ \"skillIds\": [%d] }".formatted(skillId)).andExpect(status().isOk());
            flushAndClear();
        }

        mockMvc.perform(get("/api/v1/volunteers/me").cookie(volA))
                .andExpect(jsonPath("$.availability", hasSize(1)))
                .andExpect(jsonPath("$.certifications", hasSize(1)))
                .andExpect(jsonPath("$.skills", hasSize(1)));
    }

    @Test
    void unknownDictionaryIdsAre400() throws Exception {
        putA("/skills", "{ \"skillIds\": [999999] }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown skill id(s): [999999]"));
        putA("/certifications", "{ \"certifications\": [ { \"certificationId\": 999999 } ] }")
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidInputIs400() throws Exception {
        putA("/certifications", """
                { "certifications": [ { "certificationId": %d, "validUntil": "2000-01-01" } ] }
                """.formatted(certificationId)).andExpect(status().isBadRequest());
        putA("/availability", """
                { "slots": [ { "dayOfWeek": "MONDAY", "start": "09:00", "end": "12:00" },
                             { "dayOfWeek": "MONDAY", "start": "11:00", "end": "13:00" } ] }
                """).andExpect(status().isBadRequest());
        putA("/availability", """
                { "slots": [ { "dayOfWeek": "MONDAY", "start": "12:00", "end": "09:00" } ] }
                """).andExpect(status().isBadRequest());
        putA("", "{ \"maxActiveAssignments\": -1 }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxActiveAssignments").exists());
        putA("", "{ \"lat\": 50.0 }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.location").exists());
    }

    @Test
    void volunteerChangesOnlyOwnProfile() throws Exception {
        putA("", "{ \"bio\": \"A's bio\" }").andExpect(status().isOk());
        putA("/skills", "{ \"skillIds\": [%d] }".formatted(skillId)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/volunteers/me").cookie(volB))
                .andExpect(jsonPath("$.username").value("volB"))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.skills", hasSize(0)));
    }

    @Test
    void organizationCannotUseVolunteerEndpoints() throws Exception {
        register("/api/v1/auth/register/organization", """
                { "username": "org", "password": "orgPassword1", "email": "org@example.com", "organizationName": "Org" }
                """);
        Cookie org = login("org", "orgPassword1");

        mockMvc.perform(get("/api/v1/volunteers/me").cookie(org)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/volunteers/me/skills").with(csrf()).cookie(org)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"skillIds\": [] }"))
                .andExpect(status().isForbidden());
    }

    /** The test transaction never commits; flush writes the SQL and clear makes the next read hit the database. */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private ResultActions putA(String path, String json) throws Exception {
        return mockMvc.perform(put("/api/v1/volunteers/me" + path).with(csrf()).cookie(volA)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode readJson(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
