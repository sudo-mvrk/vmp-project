package com.volunnear.organization;

import com.volunnear.auth.AppUserRepository;
import com.volunnear.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class OrganizationControllerIntegrationTest extends AbstractIntegrationTest {
    private static final String ORG_A_JSON = """
            { "username": "orgA", "password": "orgPassword1", "email": "orgA@example.com", "organizationName": "Org A" }
            """;
    private static final String ORG_B_JSON = """
            { "username": "orgB", "password": "orgPassword1", "email": "orgB@example.com", "organizationName": "Org B" }
            """;
    private static final String VOLUNTEER_JSON = """
            {
                "username": "vol", "password": "volPassword1", "email": "vol@example.com",
                "firstName": "Vol", "lastName": "Unteer", "dateOfBirth": "2000-01-01"
            }
            """;

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private OrganizationMemberRepository memberRepository;

    private Cookie orgA;
    private Long orgAId;
    private Long orgBId;

    @BeforeEach
    void setUp() throws Exception {
        register("/api/v1/auth/register/organization", ORG_A_JSON);
        register("/api/v1/auth/register/organization", ORG_B_JSON);
        orgA = login("orgA", "orgPassword1");
        orgAId = organizationIdOf("orgA");
        orgBId = organizationIdOf("orgB");
    }

    @Test
    void registrationCreatesExactlyOneOwnerMember() {
        Long userId = appUserRepository.findAppUserByUsername("orgA").orElseThrow().getId();
        OrganizationMember member = memberRepository.findById_UserId(userId).orElseThrow();

        assertThat(member.getRole()).isEqualTo(OrganizationMemberRole.OWNER);
        assertThat(memberRepository.findAll())
                .filteredOn(m -> m.getId().organizationId().equals(orgAId))
                .hasSize(1);
        // The organization has its own id now; it is not taken from the user (Q-E).
        assertThat(orgBId).isNotEqualTo(orgAId);
    }

    @Test
    void ownerReadsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/me").cookie(orgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orgAId))
                .andExpect(jsonPath("$.organizationName").value("Org A"))
                // Own profile carries the username (Q3b); the public read below does not.
                .andExpect(jsonPath("$.username").value("orgA"));
    }

    @Test
    void ownerUpdatesOwnProfileOnly() throws Exception {
        updateOrgA("""
                { "organizationName": "Org A renamed", "lat": 0.0, "lon": 14.42, "countryCode": "CZ" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Org A renamed"))
                .andExpect(jsonPath("$.lat").value(0.0))
                .andExpect(jsonPath("$.lon").value(14.42));

        mockMvc.perform(get("/api/v1/organizations/{id}", orgBId))
                .andExpect(jsonPath("$.organizationName").value("Org B"));
    }

    @Test
    void updateWithoutCoordinatesKeepsLocation() throws Exception {
        updateOrgA("""
                { "lat": 50.08, "lon": 14.42 }
                """).andExpect(status().isOk());

        updateOrgA("""
                { "description": "Helps people" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Helps people"))
                .andExpect(jsonPath("$.lat").value(50.08))
                .andExpect(jsonPath("$.lon").value(14.42));
    }

    @Test
    void invalidUpdateIsRejected() throws Exception {
        updateOrgA("""
                { "lat": 50.08 }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.location").exists());
        updateOrgA("""
                { "lat": 91.0, "lon": 14.42 }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").exists());
        updateOrgA("""
                { "organizationName": "   " }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.organizationName").exists());
        updateOrgA("""
                { "websiteUrl": "not a url", "countryCode": "cze" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.websiteUrl").exists())
                .andExpect(jsonPath("$.errors.countryCode").exists());
    }

    @Test
    void volunteerCannotReadOrUpdateOwnOrganization() throws Exception {
        register("/api/v1/auth/register/volunteer", VOLUNTEER_JSON);
        Cookie volunteer = login("vol", "volPassword1");

        mockMvc.perform(get("/api/v1/organizations/me").cookie(volunteer))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/organizations/me").with(csrf()).cookie(volunteer)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"description\": \"x\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadOwnProfileEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicReadShowsProfileWithoutAccountData() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/{id}", orgAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orgAId))
                .andExpect(jsonPath("$.organizationName").value("Org A"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void publicReadOfUnknownOrganizationIs404() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions updateOrgA(String json) throws Exception {
        return mockMvc.perform(put("/api/v1/organizations/me").with(csrf()).cookie(orgA)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Long organizationIdOf(String username) {
        Long userId = appUserRepository.findAppUserByUsername(username).orElseThrow().getId();
        return memberRepository.findById_UserId(userId).orElseThrow().getId().organizationId();
    }
}
