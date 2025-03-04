package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Disable CSRF for APIs
        http.csrf().disable();

        // Configure authorization
        http.authorizeHttpRequests()
                // Allow OPTIONS requests for CORS
                .requestMatchers(request -> "OPTIONS".equals(request.getMethod())).permitAll()
                // Other authorization rules can go here
                .anyRequest().permitAll(); // For testing, allow all requests

        return http.build();
    }
}