package com.agent.agenticcalendar.security;

import com.agent.agenticcalendar.repository.LocalAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads {@link LocalUserPrincipal}s by email for the username/password login flow.
 * Registering this bean (plus a PasswordEncoder) wires Spring Security's
 * DaoAuthenticationProvider into the global AuthenticationManager.
 */
@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final LocalAccountRepository localAccountRepository;

    public LocalUserDetailsService(LocalAccountRepository localAccountRepository) {
        this.localAccountRepository = localAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase();
        return localAccountRepository.findByEmail(email)
                .map(LocalUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for email: " + email));
    }
}
