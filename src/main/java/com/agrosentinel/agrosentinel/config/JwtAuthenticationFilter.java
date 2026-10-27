package com.agrosentinel.agrosentinel.config;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.services.JwtService;
import com.agrosentinel.agrosentinel.services.UserRoleService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final UserRoleService userRoleService;

    public JwtAuthenticationFilter(
            JwtService jwtService, UsuarioRepository usuarioRepository, UserRoleService userRoleService) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.userRoleService = userRoleService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return HttpMethod.POST.matches(request.getMethod())
                && ("/api/predictive/records".equals(path)
                        || path.startsWith("/api/predictive/records/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            Authentication authentication = jwtService.getAuthentication(authorization.substring(7));
            if (authentication == null) {
                logger.warn("Rejected invalid or expired bearer token for {} {}", request.getMethod(), request.getRequestURI());
                writeUnauthorized(response, "El token es inválido o venció. Inicia sesión nuevamente.");
                return;
            }
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
            if (usuario == null || Boolean.FALSE.equals(usuario.getActivo()) || usuario.getRol() == null) {
                logger.warn("Rejected token for an unavailable account on {} {}", request.getMethod(), request.getRequestURI());
                writeUnauthorized(response, "La cuenta asociada al token está inactiva o ya no existe.");
                return;
            }

            Authentication currentAuthentication = new UsernamePasswordAuthenticationToken(
                    usuario.getEmail(), null, userRoleService.getEffectiveRoles(usuario).stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList());
            SecurityContextHolder.getContext().setAuthentication(currentAuthentication);
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}