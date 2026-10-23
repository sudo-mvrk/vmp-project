package com.volunnear.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(VolunnearProperties.class)
public class TimeConfig {
    /** "Now" in the configured zone; injected so tests can fix the time. */
    @Bean
    public Clock clock(VolunnearProperties properties) {
        return Clock.system(properties.timeZone());
    }
}
