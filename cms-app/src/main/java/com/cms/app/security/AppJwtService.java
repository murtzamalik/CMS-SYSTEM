package com.cms.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class AppJwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public AppJwtService(
            @Value("${cms.app.jwt.secret:cms-app-mobile-jwt-secret-change-me-32chars-min}") String secret,
            @Value("${cms.app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String loginId, List<String> roles, String relationshipNum) {
        var builder = Jwts.builder()
                .setSubject(loginId)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey);
        if (relationshipNum != null && !relationshipNum.isBlank()) {
            builder.claim(AuthContext.DETAIL_RELATIONSHIP_NUM, relationshipNum.trim());
        }
        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRelationshipNum(String token) {
        Object value = parseClaims(token).get(AuthContext.DETAIL_RELATIONSHIP_NUM);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return Collections.emptyList();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
