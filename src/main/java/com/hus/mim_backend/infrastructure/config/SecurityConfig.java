package com.hus.mim_backend.infrastructure.config;

import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.infrastructure.adapter.security.JwtAuthenticationFilter;
import com.hus.mim_backend.shared.api.ApiResponse;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Security configuration — Stateless JWT, CORS, role-based authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize on controllers
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final ManageRbacUseCase manageRbacUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(TokenProvider tokenProvider, ManageRbacUseCase manageRbacUseCase) {
        this.tokenProvider = tokenProvider;
        this.manageRbacUseCase = manageRbacUseCase;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Allow PDF endpoints to be embedded in iframe on frontend.
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JSON 401 when no/invalid token is provided
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    objectMapper.writeValueAsString(
                                            ApiResponse.error("Authentication required", "UNAUTHORIZED")));
                        })
                        // JSON 403 when authenticated but insufficient role
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    objectMapper.writeValueAsString(
                                            ApiResponse.error("Access denied: insufficient permissions", "FORBIDDEN")));
                        }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/v1/research-papers/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/research-papers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/content/**").permitAll()
                        // Fine-grained RBAC is enforced with @PreAuthorize per endpoint.
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        .anyRequest().authenticated())

                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, manageRbacUseCase),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins: dev (Angular) + production (update when deploying)
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:4000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // preflight cache 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
