package com.agrosentinel.agrosentinel.config;

import java.util.Arrays;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.services.JwtService;
import com.agrosentinel.agrosentinel.services.UserRoleService;

@Configuration
public class SecurityConfig {

    @Value("${agrosentinel.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            UserRoleService userRoleService) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/inventory/items")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "ALMACENISTA", "TECNICO", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET, "/api/inventory/movements", "/api/inventory/tasks")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "ALMACENISTA", "SUPERVISOR")
                .requestMatchers("/api/inventory/tasks", "/api/inventory/tasks/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "ALMACENISTA")
                .requestMatchers("/api/inventory/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "ALMACENISTA")
                .requestMatchers(HttpMethod.POST, "/api/predictive/records", "/api/predictive/records/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/predictive/records/*/feedback")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.GET, "/api/assets/assigned", "/api/assets/assigned/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO", "SUPERVISOR", "ALMACENISTA",
                            "OPERATIVO_CAMPO", "OPERARIO_CAMPO")
                .requestMatchers(HttpMethod.GET, "/api/assets")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET, "/api/users")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.GET, "/api/assets/*/resume")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers("/api/work-groups/options")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO", "SUPERVISOR", "ALMACENISTA",
                            "OPERATIVO_CAMPO", "OPERARIO_CAMPO")
                .requestMatchers("/api/work-groups/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/categories")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.GET, "/api/preventive-maintenances", "/api/preventive-maintenances/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO", "SUPERVISOR")
                .requestMatchers("/api/preventive-maintenances/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers("/api/maintenance-history/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers("/uploads/maintenance-evidence/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers("/api/failure-reports", "/api/failure-reports/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO", "SUPERVISOR", "ALMACENISTA",
                            "OPERATIVO_CAMPO", "OPERARIO_CAMPO")
                .requestMatchers("/api/categories/**", "/api/users/**", "/api/assets/**", "/api/usuarios/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN")
                .anyRequest().authenticated()
            )
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"Se requiere un token Bearer válido para este endpoint.\"}");
                }))
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtService, usuarioRepository, userRoleService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
