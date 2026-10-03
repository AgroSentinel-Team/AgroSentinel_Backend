package com.agrosentinel.agrosentinel.config;

import java.util.Arrays;

import jakarta.servlet.DispatcherType;
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

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UsuarioRepository usuarioRepository) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/inventory/items")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "ALMACENISTA", "TECNICO")
                .requestMatchers("/api/inventory/tasks", "/api/inventory/tasks/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "ALMACENISTA")
                .requestMatchers("/api/inventory/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "ALMACENISTA")
                .requestMatchers(HttpMethod.POST, "/api/predictive/records").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/predictive/records/*/feedback")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.GET, "/api/assets", "/api/users")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers(HttpMethod.GET, "/api/categories")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers("/api/preventive-maintenances/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers("/uploads/maintenance-evidence/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "TECNICO")
                .requestMatchers("/api/failure-reports", "/api/failure-reports/**")
                    .hasAnyRole("ADMINISTRADOR", "ADMIN", "OPERATIVO_CAMPO", "OPERARIO_CAMPO")
                .requestMatchers("/api/categories/**", "/api/users/**", "/api/assets/**", "/api/usuarios/**")
                    .hasRole("ADMINISTRADOR")
                .anyRequest().authenticated()
            )
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"Se requiere un token Bearer válido para este endpoint.\"}");
                }))
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtService, usuarioRepository),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
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
