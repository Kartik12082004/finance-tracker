package com.kartik.finance_tracker.security;

import com.kartik.finance_tracker.auth.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    public SecurityConfig(
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler
    ) {
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // This API uses JWT bearer tokens rather than browser sessions.
                // CSRF protection is therefore not needed for our REST API.
                .csrf(csrf -> csrf.disable())
                
                // Authentication endpoints and OAuth2 login flow must remain public.
                // All other application endpoints require authentication.
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Enable OAuth2 Login and send successful logins through
                // our own user-mapping handler.
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2LoginSuccessHandler)
                )

                // Enable JWT bearer-token authentication for REST requests.
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> {})
                );

        return http.build();
    }
}
