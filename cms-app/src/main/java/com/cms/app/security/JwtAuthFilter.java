package com.cms.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AppJwtService jwtService;

    public JwtAuthFilter(AppJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        // Login needs API key only. Authorize is APS channel traffic (API key only).
        if (path.startsWith("/user")
                || path.startsWith("/actuator")
                || path.equals("/error")
                || path.startsWith("/card/transaction/authorize")) {
            return true;
        }
        // All other /card/* routes need a user JWT.
        return !path.startsWith("/card");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing or invalid Authorization Bearer token");
            return;
        }
        String token = authHeader.substring(7).trim();
        if (!jwtService.validateToken(token)) {
            writeUnauthorized(response, "Invalid or expired user token");
            return;
        }
        String loginId = jwtService.getLoginId(token);
        List<SimpleGrantedAuthority> authorities = jwtService.getRoles(token).stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginId, null, authorities);
        Map<String, String> details = new HashMap<>();
        String relationshipNum = jwtService.getRelationshipNum(token);
        if (relationshipNum != null && !relationshipNum.isBlank()) {
            details.put(AuthContext.DETAIL_RELATIONSHIP_NUM, relationshipNum.trim());
        }
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"responseCode\":401,\"responseMessage\":\"" + message + "\",\"responseBody\":null}");
    }
}
