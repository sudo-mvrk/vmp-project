package com.volunnear.support;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests: full application context against PostgreSQL with PostGIS.
 * The container is started once and shared by every test class of the run; Flyway builds the schema.
 * Sessions are kept in memory, so no Redis is needed.
 * {@link #register} and {@link #login} go through the real endpoints; reuse the returned SESSION cookie.
 */
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "management.health.redis.enabled=false"
})
@Import(TestSessionConfig.class)
public abstract class AbstractIntegrationTest {
    // Same image as docker-compose.yml. Public so a test with its own context (BanEndsRedisSessionsIntegrationTest) can share it.
    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected void register(String url, String json) throws Exception {
        mockMvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    protected Cookie login(String username, String password) throws Exception {
        Cookie session = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();
        return session;
    }

    protected static String loginJson(String username, String password) {
        return """
                { "username": "%s", "password": "%s" }
                """.formatted(username, password);
    }
}
