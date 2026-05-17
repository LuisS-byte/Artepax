package com.catolica.Artepax.authentication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import java.io.IOException;

/**
 * JWT authentication filter that intercepts every HTTP request.
 * <p>
 * Extracts the JWT from the {@code Authorization: Bearer <token>} header,
 * validates it via {@link JwtService}, and sets the Spring Security
 * {@link SecurityContextHolder} if the token is valid.
 * <p>
 * If no token is present the request continues unauthenticated; public
 * endpoints permitted by {@link SecurityConfig} will still be accessible.
 * Invalid or expired tokens result in a {@code 401 UNAUTHORIZED} response.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Filters each request to extract and validate a JWT token.
     * <p>
     * Processing steps:
     * <ol>
     *   <li>Reads the {@code Authorization} header</li>
     *   <li>If missing or not a Bearer token, continues the chain</li>
     *   <li>Extracts and validates the token via {@link JwtService}</li>
     *   <li>If valid, sets the {@link SecurityContextHolder} authentication</li>
     *   <li>If {@link io.jsonwebtoken.JwtException} is thrown, returns 401</li>
     *   <li>Always continues the filter chain</li>
     * </ol>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 🔒 Si no hay token, sigue normal
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 📦 Extraer token
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);

            // 🔍 Validar si no hay autenticación aún
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // ✅ Validar token
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 🔐 Guardar en contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 🔄 Continuar filtro
        filterChain.doFilter(request, response);
    }
}