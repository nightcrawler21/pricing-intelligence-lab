package com.example.pricinglab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Pricing Lab application.
 *
 * Implements basic role-based access control with two roles:
 * - ADMIN: Full access to all features including experiment approval
 * - ANALYST: Can create and view experiments, run simulations
 *
 * Uses in-memory authentication for prototype phase.
 * Production deployment should integrate with corporate SSO/LDAP.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disabled for API usage; enable for production with proper CSRF handling
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // Allow H2 console frames
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll() // H2 console for local development
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll() // OpenAPI docs
                .requestMatchers("/actuator/health", "/actuator/info").permitAll() // Health checks
                .requestMatchers("/api/**").authenticated() // All API endpoints require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // Basic auth for simplicity in prototype

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // In-memory users for prototype phase
        // TODO: Replace with LDAP/SSO integration for production
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN", "ANALYST")
            .build();

        UserDetails analyst = User.builder()
            .username("analyst")
            .password(passwordEncoder.encode("analyst123"))
            .roles("ANALYST")
            .build();

        return new InMemoryUserDetailsManager(admin, analyst);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
