package com.volunnear.admin;

import com.volunnear.auth.AppUserRepository;
import com.volunnear.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Q23 against a real Redis with the production session setup ({@code repository-type: indexed}), which the
 * in-memory sessions of the other tests only imitate. Its own context: no {@code TestSessionConfig}, Redis
 * auto-configuration on. Shares the PostgreSQL container; the data is removed after the test.
 */
@AutoConfigureMockMvc
@SpringBootTest
class BanEndsRedisSessionsIntegrationTest {
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String username = "r" + UUID.randomUUID().toString().substring(0, 8) + "_v";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AbstractIntegrationTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AbstractIntegrationTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AbstractIntegrationTest.POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM app_users WHERE username = ?", username);
    }

    @Test
    void banDeletesEveryRedisSessionOfTheUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/volunteer").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "%s", "password": "password1", "email": "%s@example.com",
                                  "firstName": "First", "lastName": "Last", "dateOfBirth": "2000-01-01" }
                                """.formatted(username, username)))
                .andExpect(status().isCreated());
        Cookie first = login(username, "password1");
        Cookie second = login(username, "password1");
        Cookie admin = login("admin", "adminpass1");
        assertThat(sessionRepository.findByPrincipalName(username)).hasSize(2);
        mockMvc.perform(get("/api/v1/volunteers/me").cookie(first)).andExpect(status().isOk());

        long userId = appUserRepository.findAppUserByUsername(username).orElseThrow().getId();
        mockMvc.perform(post("/api/v1/admin/users/{id}/ban", userId).with(csrf()).cookie(admin))
                .andExpect(status().isOk());

        assertThat(sessionRepository.findByPrincipalName(username)).isEmpty();
        mockMvc.perform(get("/api/v1/volunteers/me").cookie(first)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/volunteers/me").cookie(second)).andExpect(status().isUnauthorized());
        // Other users keep their sessions.
        mockMvc.perform(get("/api/v1/auth/me").cookie(admin)).andExpect(status().isOk());
    }

    private Cookie login(String user, String password) throws Exception {
        Cookie session = mockMvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"username\": \"%s\", \"password\": \"%s\" }".formatted(user, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();
        return session;
    }
}
