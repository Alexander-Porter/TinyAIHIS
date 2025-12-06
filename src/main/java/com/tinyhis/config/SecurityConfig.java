package com.tinyhis.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/schedule/departments", "/api/schedule/list").permitAll()
                .requestMatchers("/api/queue/**").permitAll()
                .requestMatchers("/api/triage/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Patient endpoints
                .requestMatchers("/api/registration/**").hasAnyRole("PATIENT", "ADMIN")
                .requestMatchers("/api/payment/**").hasAnyRole("PATIENT", "ADMIN")
                // Patients can view their own EMR records (for payment page)
                .requestMatchers(HttpMethod.GET, "/api/emr/registration/**", "/api/emr/prescriptions/**", "/api/emr/laborders/**").hasAnyRole("PATIENT", "DOCTOR", "CHIEF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/emr/patient/**").hasAnyRole("PATIENT", "DOCTOR", "CHIEF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/lab/patient/**").hasAnyRole("PATIENT", "LAB", "DOCTOR", "CHIEF", "ADMIN")
                
                // Doctor/Chief endpoints
                .requestMatchers("/api/doctor/**").hasAnyRole("DOCTOR", "CHIEF", "ADMIN")
                .requestMatchers("/api/emr/**").hasAnyRole("DOCTOR", "CHIEF", "ADMIN")
                
                // Lab endpoints
                .requestMatchers("/api/lab/**").hasAnyRole("LAB", "DOCTOR", "CHIEF", "ADMIN")
                
                // Pharmacy endpoints  
                .requestMatchers("/api/pharmacy/**").hasAnyRole("PHARMACY", "DOCTOR", "CHIEF", "ADMIN")
                
                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
