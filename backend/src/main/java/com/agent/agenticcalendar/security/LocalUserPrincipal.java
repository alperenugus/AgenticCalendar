package com.agent.agenticcalendar.security;

import com.agent.agenticcalendar.model.LocalAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal for username/password accounts. Carries the account id
 * and display name so the auth endpoints can build the same user payload shape the
 * frontend already consumes for Google users.
 */
public class LocalUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String displayName;
    private final String passwordHash;

    public LocalUserPrincipal(LocalAccount account) {
        this.id = account.getId();
        this.email = account.getEmail();
        this.displayName = account.getDisplayName();
        this.passwordHash = account.getPasswordHash();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Stable, namespaced owner key used to scope calendar data (mirrors a Google sub). */
    public String getOwnerKey() {
        return "local-" + id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
