package com.agent.agenticcalendar.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Resolves the canonical, server-trusted owner key and email for the
 * authenticated principal, regardless of how they signed in.
 *
 * <p>The owner key is the value used to scope all calendar data:
 * <ul>
 *   <li>Google OAuth users: the OIDC {@code sub} claim.</li>
 *   <li>Username/password users: {@code "local-" + accountId}.</li>
 * </ul>
 *
 * Identity is ALWAYS derived from the session principal here — never from
 * client-supplied request parameters or headers — which is what closes the
 * cross-tenant IDOR where any caller could read another user's data by
 * supplying their id.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** Returns the owner key for the authenticated user, or {@code null} if unauthenticated. */
    public static String ownerKey(Authentication authentication) {
        Object principal = principal(authentication);
        if (principal instanceof OAuth2User oauthUser) {
            return oauthUser.getAttribute("sub");
        }
        if (principal instanceof LocalUserPrincipal localUser) {
            return localUser.getOwnerKey();
        }
        return null;
    }

    /** Returns the email for the authenticated user, or {@code null} if unavailable. */
    public static String email(Authentication authentication) {
        Object principal = principal(authentication);
        if (principal instanceof OAuth2User oauthUser) {
            return oauthUser.getAttribute("email");
        }
        if (principal instanceof LocalUserPrincipal localUser) {
            return localUser.getEmail();
        }
        return null;
    }

    private static Object principal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal();
    }
}
