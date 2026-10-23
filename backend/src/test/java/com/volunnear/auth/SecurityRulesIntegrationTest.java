package com.volunnear.auth;

import com.volunnear.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 1 acceptance: register and log in for both roles, then 401 without a session,
 * 403 for the wrong role, rejected login for a banned user, 404 for a missing profile.
 * Sessions come from a real login, not from a mocked principal.
 * The CSRF endpoint is tested in {@link CsrfEndpointIntegrationTest}.
 */
@Transactional
class SecurityRulesIntegrationTest extends AbstractIntegrationTest {
    private static final String VOLUNTEER_JSON = """
            {
                "username": "vol",
                "password": "volPassword1",
                "email": "vol@example.com",
                "firstName": "Vol",
                "lastName": "Unteer",
                "dateOfBirth": "2000-01-01"
            }
            """;
    private static final String ORGANIZATION_JSON = """
            {
                "username": "org",
                "password": "orgPassword1",
                "email": "org@example.com",
                "organizationName": "Org"
            }
            """;
    private static final String VOLUNTEER_UPDATE_JSON = """
            { "firstName": "Valentina" }
            """;
    private static final String ORGANIZATION_UPDATE_JSON = """
            { "description": "Helps people" }
            """;

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void volunteerRegistersLogsInAndUpdatesOwnProfile() throws Exception {
        register("/api/v1/auth/register/volunteer", VOLUNTEER_JSON);
        Cookie session = login("vol", "volPassword1");

        mockMvc.perform(put("/api/v1/volunteers/me").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(VOLUNTEER_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Valentina"));
    }

    @Test
    void organizationRegistersLogsInAndUpdatesOwnProfile() throws Exception {
        register("/api/v1/auth/register/organization", ORGANIZATION_JSON);
        Cookie session = login("org", "orgPassword1");

        mockMvc.perform(put("/api/v1/organizations/me").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Helps people"));
    }

    @Test
    void requestWithoutSessionIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/volunteers/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VOLUNTEER_UPDATE_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/organizations/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_UPDATE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void volunteerCannotUpdateOrganizationProfile() throws Exception {
        register("/api/v1/auth/register/volunteer", VOLUNTEER_JSON);
        Cookie session = login("vol", "volPassword1");

        mockMvc.perform(put("/api/v1/organizations/me").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_UPDATE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationCannotUpdateVolunteerProfile() throws Exception {
        register("/api/v1/auth/register/organization", ORGANIZATION_JSON);
        Cookie session = login("org", "orgPassword1");

        mockMvc.perform(put("/api/v1/volunteers/me").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(VOLUNTEER_UPDATE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void bannedUserCannotLogIn() throws Exception {
        register("/api/v1/auth/register/volunteer", VOLUNTEER_JSON);
        AppUser user = appUserRepository.findAppUserByUsername("vol").orElseThrow();
        user.setBanned(true);
        appUserRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("vol", "volPassword1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is banned"))
                .andExpect(unauthenticated());
    }

    @Test
    void loggedInUserWithoutProfileGets404() throws Exception {
        appUserRepository.save(AppUser.builder()
                .username("orphan")
                .email("orphan@example.com")
                .password(passwordEncoder.encode("orphanPassword1"))
                .roles(Set.of(Role.ROLE_ORGANIZATION))
                .build());
        Cookie session = login("orphan", "orphanPassword1");

        mockMvc.perform(put("/api/v1/organizations/me").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_UPDATE_JSON))
                .andExpect(status().isNotFound());
    }
}
