package com.volunnear.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@TestConfiguration
@EnableSpringHttpSession
public class TestSessionConfig {
    // Indexed like production (Q23), so a ban can find and delete the user's sessions.
    @Bean
    public IndexedMapSessionRepository sessionRepository() {
        return new IndexedMapSessionRepository();
    }
}
