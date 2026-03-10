package com.fleebug.corerouter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.fleebug.corerouter.security.filter.JwtAuthenticationFilter;
import com.fleebug.corerouter.security.filter.ServiceTokenAuthenticationFilter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ServiceTokenAuthenticationFilter serviceTokenAuthenticationFilter;

    @Lazy
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ServiceTokenAuthenticationFilter serviceTokenAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.serviceTokenAuthenticationFilter = serviceTokenAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Filter chain for task endpoints — supports both JWT (users) and service-token (workers).
     * Ordered first so it matches before the main chain.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain taskFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/v1/tasks/**")
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(serviceTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.POST, "/v1/tasks").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/v1/tasks/**").hasRole("USER")
                .requestMatchers(HttpMethod.PATCH, "/v1/tasks/worker/status").hasRole("WORKER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(401);
                    response.getWriter().write(
                        "{\"timestamp\":\"" + LocalDateTime.now() +
                        "\",\"status\":401,\"success\":false,\"message\":\"Unauthorized: Missing or invalid authentication token\"," +
                        "\"path\":\"" + request.getRequestURI() +
                        "\",\"method\":\"" + request.getMethod() + "\",\"data\":null}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(403);
                    response.getWriter().write(
                        "{\"timestamp\":\"" + LocalDateTime.now() +
                        "\",\"status\":403,\"success\":false,\"message\":\"Forbidden: Insufficient role for this endpoint\"," +
                        "\"path\":\"" + request.getRequestURI() +
                        "\",\"method\":\"" + request.getMethod() + "\",\"data\":null}"
                    );
                })
            );

        return http.build();
    }

    /**
     * Main filter chain — everything except /v1/tasks/**.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login","/api/v1/auth/**").permitAll()
                // User models - public read access
                .requestMatchers(HttpMethod.GET, "/api/v1/models", "/api/v1/models/**","/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(401);
                    response.getWriter().write(
                        "{\"timestamp\":\"" + LocalDateTime.now() + 
                        "\",\"status\":401,\"success\":false,\"message\":\"Unauthorized: Missing or invalid authentication token\"," +
                        "\"path\":\"" + request.getRequestURI() + 
                        "\",\"method\":\"" + request.getMethod() + "\",\"data\":null}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(403);
                    response.getWriter().write(
                        "{\"timestamp\":\"" + LocalDateTime.now() + 
                        "\",\"status\":403,\"success\":false,\"message\":\"Forbidden: Admin role required\"," +
                        "\"path\":\"" + request.getRequestURI() + 
                        "\",\"method\":\"" + request.getMethod() + "\",\"data\":null}"
                    );
                })
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Service-Token"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
