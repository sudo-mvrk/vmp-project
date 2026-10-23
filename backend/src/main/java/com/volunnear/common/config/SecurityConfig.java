package com.volunnear.common.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String roleVolunteer = "VOLUNTEER";
    private static final String roleOrganization = "ORGANIZATION";
    private static final String roleAdmin = "ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf ->
                        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger.json"
                        ).permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Platform administration (Slice 8b, Q32); no other rule gives an admin access.
                        .requestMatchers("/api/v1/admin/**").hasRole(roleAdmin)
                        .requestMatchers("/api/v1/volunteers/**").hasRole(roleVolunteer)
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register/**").permitAll()
                        // Applications (Slice 5), before the general activity rules they would otherwise match.
                        .requestMatchers(HttpMethod.POST, "/api/v1/activities/*/applications").hasRole(roleVolunteer)
                        .requestMatchers(HttpMethod.GET, "/api/v1/activities/*/applications").hasRole(roleOrganization)
                        // Recommendations (Slice 6c), likewise before the public GET rule for activities.
                        .requestMatchers(HttpMethod.GET, "/api/v1/activities/*/recommended-volunteers").hasRole(roleOrganization)
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/withdraw").hasRole(roleVolunteer)
                        // Invitations (Slice 5b): the organization invites, the volunteer answers.
                        .requestMatchers(HttpMethod.POST, "/api/v1/activities/*/invitations").hasRole(roleOrganization)
                        .requestMatchers("/api/v1/invitations/**").hasRole(roleVolunteer)
                        .requestMatchers("/api/v1/applications/**").hasRole(roleOrganization)
                        .requestMatchers(HttpMethod.GET, "/api/v1/activities/**").permitAll()
                        .requestMatchers("/api/v1/activities/**").hasRole(roleOrganization)
                        .requestMatchers(HttpMethod.GET, "/api/v1/skills", "/api/v1/certifications").permitAll()
                        // Own profile before the public read rule, which would otherwise let anonymous callers in.
                        .requestMatchers("/api/v1/organizations/me", "/api/v1/organizations/me/**").hasRole(roleOrganization)
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizations/**").permitAll()
                        .requestMatchers("/api/v1/organizations/**").hasRole(roleOrganization)

                        .requestMatchers("/api/v1/auth/csrf").permitAll()

                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Without a login form or HTTP Basic, Spring answers 403 to a missing session; an API client expects 401.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SESSION")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SneakyThrows
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}