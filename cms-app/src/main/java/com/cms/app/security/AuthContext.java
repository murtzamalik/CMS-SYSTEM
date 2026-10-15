package com.cms.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the authenticated user from the JWT filter.
 */
public final class AuthContext {

    public static final String DETAIL_RELATIONSHIP_NUM = "relationshipNum";

    private AuthContext() {
    }

    public static String loginId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        return String.valueOf(auth.getPrincipal());
    }

    public static String relationshipNum() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof java.util.Map<?, ?> details)) {
            return null;
        }
        Object value = details.get(DETAIL_RELATIONSHIP_NUM);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }
}
