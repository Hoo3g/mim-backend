package com.hus.mim_backend.infrastructure.config;

import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.infrastructure.adapter.security.JwtAuthenticationFilter;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration — Stateless JWT, CORS, role-based authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize on controllers
public class SecurityConfig {

    private static final ObjectMapper SECURITY_OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final TokenProvider tokenProvider;
    private final ManageRbacUseCase manageRbacUseCase;
    private final List<String> allowedOrigins;

    public SecurityConfig(TokenProvider tokenProvider,
            ManageRbacUseCase manageRbacUseCase,
            @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:4000}") String allowedOriginsCsv) {
        this.tokenProvider = tokenProvider;
        this.manageRbacUseCase = manageRbacUseCase;
        this.allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
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
                                    SECURITY_OBJECT_MAPPER.writeValueAsString(
                                            ApiResponse.error("Authentication required", "UNAUTHORIZED")));
                        })
                        // JSON 403 when authenticated but insufficient role
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    SECURITY_OBJECT_MAPPER.writeValueAsString(
                                            ApiResponse.error("Access denied: insufficient permissions", "FORBIDDEN")));
                        }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/storage/research-pdfs/**").authenticated()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**",
                                "/actuator/caches", "/actuator/caches/**",
                                "/actuator/prometheus")
                        .hasAuthority("PERM_" + RbacPermissions.ADMIN_DASHBOARD_VIEW)
                        .requestMatchers(HttpMethod.GET, "/api/v1/profile/me", "/api/v1/profile/me/dashboard").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/profile/me/student", "/api/v1/profile/me/company",
                                "/api/v1/profile/me/lecturer").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/profile/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/me").authenticated()
                        .requestMatchers("/api/v1/posts/applications/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/apply").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*/apply").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/research-papers/bookmarks/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/research-papers/*/bookmarks").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/research-papers/*/bookmarks").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/research-papers/*/views",
                                "/api/v1/research-papers/*/downloads")
                        .authenticated()
                        .requestMatchers("/api/v1/research-papers/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/storage/profile-cvs").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/storage/avatars").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/research-categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/specializations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recruitment-categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/research-papers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/content/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/**").permitAll()
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

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // preflight cache 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
