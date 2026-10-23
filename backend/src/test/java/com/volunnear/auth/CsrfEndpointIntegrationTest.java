package com.volunnear.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunnear.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The CSRF flow of a real client against the configured cookie repository.
 * The {@code csrf()} test post-processor permanently replaces the repository of the shared CsrfFilter
 * with a session-based one, so this class starts a fresh context and must never use {@code csrf()}.
 */
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfEndpointIntegrationTest extends AbstractIntegrationTest {
    private static final String VOLUNTEER_JSON = """
            {
                "username": "csrfVol",
                "password": "volPassword1",
                "email": "csrfVol@example.com",
                "firstName": "Vol",
                "lastName": "Unteer",
                "dateOfBirth": "2000-01-01"
            }
            """;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void csrfEndpointIssuesTokenThatARealClientCanUse() throws Exception {
        MockHttpServletResponse csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn().getResponse();
        JsonNode body = objectMapper.readTree(csrfResponse.getContentAsString());
        String headerName = body.get("headerName").asText();
        String token = body.get("token").asText();
        assertThat(headerName).isEqualTo("X-XSRF-TOKEN");
        assertThat(csrfResponse.getCookie("XSRF-TOKEN").getValue()).isEqualTo(token);

        // The request carries the issued cookie and header, as a browser client would.
        mockMvc.perform(post("/api/v1/auth/register/volunteer")
                        .cookie(csrfResponse.getCookie("XSRF-TOKEN"))
                        .header(headerName, token)
                        .contentType(MediaType.APPLICATION_JSON).content(VOLUNTEER_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void modifyingRequestWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/volunteer")
                        .contentType(MediaType.APPLICATION_JSON).content(VOLUNTEER_JSON))
                .andExpect(status().isForbidden());
    }
}
