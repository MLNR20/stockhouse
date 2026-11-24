package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.access.AccessDtos.AccessSummary;
import com.example.warehouse_proj.access.AccessService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Authenticates requests carrying a valid "Authorization: Bearer &lt;access token&gt;" header.
 * The user's permissions are read from the database on each request (not from the token), so a role
 * change or a deactivated account takes effect immediately.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final AccessService accessService;

    public JwtAuthenticationFilter(JwtService jwtService, AccessService accessService) {
        this.jwtService = jwtService;
        this.accessService = accessService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(header.substring(BEARER.length()).trim(), JwtService.ACCESS);
                Optional<AccessSummary> access = accessService.summaryFor(UUID.fromString(claims.getSubject()));
                if (access.isPresent()) {
                    var authentication =
                            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities(access.get()));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid/expired token: leave the request unauthenticated; the entry point answers 401.
            }
        }
        chain.doFilter(request, response);
    }

    private static List<GrantedAuthority> authorities(AccessSummary access) {
        List<GrantedAuthority> out = new ArrayList<>();
        if (access.role() != null) {
            out.add(new SimpleGrantedAuthority("ROLE_" + access.role()));
        }
        access.permissions().forEach(p -> out.add(new SimpleGrantedAuthority(p)));
        return out;
    }
}
