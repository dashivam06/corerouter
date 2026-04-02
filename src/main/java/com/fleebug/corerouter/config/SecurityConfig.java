package com.fleebug.corerouter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.constants.ApiPaths;
import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.security.filter.AuthRateLimitFilter;
import com.fleebug.corerouter.security.filter.ChatRateLimitFilter;
import com.fleebug.corerouter.security.filter.JwtAuthenticationFilter;
import com.fleebug.corerouter.security.filter.MdcLoggingFilter;
import com.fleebug.corerouter.security.filter.ServiceTokenAuthenticationFilter;
import com.fleebug.corerouter.security.filter.TaskRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String AUTH_ERROR_REASON_ATTR = "auth_error_reason";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ServiceTokenAuthenticationFilter serviceTokenAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final TaskRateLimitFilter taskRateLimitFilter;
    private final ChatRateLimitFilter chatRateLimitFilter;
    private final MdcLoggingFilter mdcLoggingFilter;
    private final ObjectMapper objectMapper;

        @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,https://corerouter.me,https://www.corerouter.me,https://api.corerouter.me}")
        private String allowedOrigins;


    @Bean
    @Order(1)
    public SecurityFilterChain chatFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(ApiPaths.CHAT_COMPLETIONS)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(mdcLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(chatRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                    .anyRequest().permitAll() 
                );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain taskFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(ApiPaths.TASKS_ALL)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(mdcLoggingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(serviceTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                        .addFilterAfter(taskRateLimitFilter, JwtAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.POST,  ApiPaths.TASKS).hasRole("USER")
                                .requestMatchers(HttpMethod.GET,   ApiPaths.TASKS_ALL).permitAll()
                .requestMatchers(HttpMethod.PATCH, ApiPaths.TASKS_STATUS).hasRole("WORKER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeError(req, res, HttpStatus.UNAUTHORIZED, resolveUnauthorizedMessage(req)))
                .accessDeniedHandler((req, res, e) ->
                        writeError(req, res, HttpStatus.FORBIDDEN, "Forbidden: Insufficient role for this endpoint"))
            );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // ── Public ─────────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,
                        ApiPaths.AUTH_REGISTER,
                        ApiPaths.AUTH_LOGIN,
                        ApiPaths.AUTH_REQUEST_OTP,
                        ApiPaths.AUTH_VERIFY_OTP).permitAll()
                .requestMatchers(HttpMethod.GET,
                        ApiPaths.MODELS,
                        "/test-payment.html",
                        ApiPaths.MODELS_ALL).permitAll()
                .requestMatchers(
                        ApiPaths.SCALAR_ALL,
                        ApiPaths.API_DOCS,
                        ApiPaths.API_DOCS_ALL).permitAll()
                // ── Wallet / Payment ──────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,
                        ApiPaths.WALLET_TOPUP_SUCCESS,
                        ApiPaths.WALLET_TOPUP_FAILURE).permitAll()
                // ── Admin + Worker ──────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,
                        ApiPaths.ADMIN_MODELS,
                        ApiPaths.ADMIN_BILLING_CONFIG,
                        ApiPaths.ADMIN_BILLING_CONFIG_MODEL).hasAnyRole("ADMIN", "WORKER")
                .requestMatchers(HttpMethod.POST,
                        ApiPaths.INTERNAL_WORKER_HEARTBEAT,
                        ApiPaths.ADMIN_BILLING_USAGE).hasAnyRole("ADMIN", "WORKER")
                // ── Admin only ──────────────────────────────────────────────────
                .requestMatchers(ApiPaths.ADMIN_SERVICE_TOKENS,
                        ApiPaths.ADMIN_TECHNICAL_ALL).hasRole("ADMIN")
                // ── Everything else ─────────────────────────────────────────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(mdcLoggingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(serviceTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeError(req, res, HttpStatus.UNAUTHORIZED, resolveUnauthorizedMessage(req)))
                .accessDeniedHandler((req, res, e) ->
                        writeError(req, res, HttpStatus.FORBIDDEN, "Forbidden: Admin role required"))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toList());
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Service-Token"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(HttpServletRequest request,
                            HttpServletResponse response,
                            HttpStatus status,
                            String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(status, message, request))
        );
    }

        private String resolveUnauthorizedMessage(HttpServletRequest request) {
                Object reason = request.getAttribute(AUTH_ERROR_REASON_ATTR);

                if ("expired".equals(reason)) {
                        return "Token expired. Please log in again";
                }

                if ("invalid".equals(reason)) {
                        return "Invalid authentication token";
                }

                return "Authentication token is missing";
        }
}