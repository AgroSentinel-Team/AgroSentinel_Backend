package com.agrosentinel.agrosentinel.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
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
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired bearer token");
                return;
            }
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName());
            if (usuario == null || Boolean.FALSE.equals(usuario.getActivo()) || usuario.getRol() == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account is unavailable");
                return;
            }

            String authority = usuario.getRol().startsWith("ROLE_")
                    ? usuario.getRol()
                    : "ROLE_" + usuario.getRol();
            Authentication currentAuthentication = new UsernamePasswordAuthenticationToken(
                    usuario.getEmail(), null, List.of(new SimpleGrantedAuthority(authority)));
            SecurityContextHolder.getContext().setAuthentication(currentAuthentication);
        }
        filterChain.doFilter(request, response);
    }
}