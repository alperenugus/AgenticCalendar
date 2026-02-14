package com.agent.agenticcalendar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Configuration to ensure OAuth2 redirect URIs use HTTPS when behind a proxy (Railway).
 * This fixes the redirect_uri_mismatch error by ensuring Spring Boot trusts proxy headers.
 * 
 * The redirect URI is explicitly set in application.yml:
 * spring.security.oauth2.client.registration.google.redirect-uri
 */
@Configuration
public class OAuth2Config {

    /**
     * Filter to process X-Forwarded-* headers from Railway proxy.
     * This ensures redirect URIs use HTTPS instead of HTTP.
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}

