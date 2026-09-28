package com.civic.waterwatch.security.config;

import com.civic.waterwatch.security.jwt.CustomAccessDeniedHandler;
import com.civic.waterwatch.security.jwt.JwtAuthenticationEntryPoint;
import com.civic.waterwatch.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // Allows H2 console
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Static Dashboard & Assets
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        // Public Auth Endpoints (OTP)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Crowdsourced Report Intake & GeoJSON / Heatmap Streaming
                        .requestMatchers("/api/reports", "/api/reports/**").permitAll()
                        // Redis Cache Management, Geo Proximity & OTP Endpoints
                        .requestMatchers("/api/cache", "/api/cache/**").permitAll()
                        // Geospatial Clusters, PDF Dossiers & Ward Polygons
                        .requestMatchers("/api/clusters", "/api/clusters/**").permitAll()
                        .requestMatchers("/api/wards", "/api/wards/**").permitAll()
                        .requestMatchers("/api/geo", "/api/geo/**").permitAll()
                        // Media Serving & Municipal Webhooks
                        .requestMatchers("/api/media", "/api/media/**").permitAll()
                        .requestMatchers("/api/municipal/mock-webhook/**").permitAll()
                        .requestMatchers("/api/municipal/dispatch-logs").permitAll()
                        // Actuator Health, Info & Prometheus Metrics
                        .requestMatchers("/actuator/**").permitAll()
                        // OpenAPI / Swagger Documentation
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Embedded H2 Database Web Console
                        .requestMatchers("/h2-console/**").permitAll()
                        // Any other administrative route requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
