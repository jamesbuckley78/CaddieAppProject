package com.caddieapp.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.caddieapp.caddie.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs on every request: if there's a valid "Authorization: Bearer <token>" header, tells
 * Spring Security who the user is. Created in SecurityConfig (not a @Component, otherwise
 * Spring Boot would also register it a second time as a plain servlet filter).
 *
 * Also checks the token's version against the database (one quick lookup by id), so a password
 * change or an admin reset logs that person out everywhere straight away.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.substring(7))
                .filter(user -> users.findTokenVersion(user.id()).map(v -> v == user.tokenVersion()).orElse(false))
                .ifPresent(user -> {
                    // Someone still on their setup code gets no role, so the only endpoints they
                    // can reach are /api/auth/** (to set a password).
                    List<GrantedAuthority> authorities = user.mustChangePassword()
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        }
        chain.doFilter(request, response);
    }
}
