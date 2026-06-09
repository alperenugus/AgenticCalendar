package com.agent.agenticcalendar.controller;

import com.agent.agenticcalendar.model.LocalAccount;
import com.agent.agenticcalendar.repository.LocalAccountRepository;
import com.agent.agenticcalendar.security.LocalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AuthenticationManager authenticationManager;
    private final LocalAccountRepository localAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager,
                          LocalAccountRepository localAccountRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.localAccountRepository = localAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns the currently authenticated user, whether they signed in via Google
     * OAuth (an {@link OAuth2User} principal) or username/password (a
     * {@link LocalUserPrincipal}). The {@code id} field is the opaque owner key the
     * frontend forwards to scope calendar data.
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUser(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();

        Object principal = authentication != null ? authentication.getPrincipal() : null;

        if (principal instanceof OAuth2User oauthUser) {
            userInfo.put("authenticated", true);
            userInfo.put("provider", "google");
            userInfo.put("id", oauthUser.getAttribute("sub"));
            userInfo.put("email", oauthUser.getAttribute("email"));
            userInfo.put("name", oauthUser.getAttribute("name"));
            userInfo.put("picture", oauthUser.getAttribute("picture"));
        } else if (principal instanceof LocalUserPrincipal localUser) {
            userInfo.put("authenticated", true);
            userInfo.put("provider", "local");
            userInfo.put("id", localUser.getOwnerKey());
            userInfo.put("email", localUser.getEmail());
            userInfo.put("name", localUser.getDisplayName());
            userInfo.put("picture", null);
        } else {
            userInfo.put("authenticated", false);
        }

        return ResponseEntity.ok(userInfo);
    }

    /** Registers a new username/password account and signs the user in. */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request,
                                                         HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String password = request.password() == null ? "" : request.password();
        String name = request.name() == null ? "" : request.name().trim();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return error(HttpStatus.BAD_REQUEST, "Please enter a valid email address.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return error(HttpStatus.BAD_REQUEST, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (name.isEmpty()) {
            name = email.substring(0, email.indexOf('@'));
        }
        if (localAccountRepository.existsByEmail(email)) {
            return error(HttpStatus.CONFLICT, "An account with this email already exists. Please sign in instead.");
        }

        LocalAccount account = new LocalAccount(email, passwordEncoder.encode(password), name);
        localAccountRepository.save(account);

        // Sign the new user in immediately so the session is established.
        try {
            Authentication authentication = establishSession(email, password, httpRequest, httpResponse);
            return ResponseEntity.ok(buildUserPayload(authentication));
        } catch (AuthenticationException e) {
            // Should not happen right after creating the account.
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Account created, but automatic sign-in failed. Please sign in.");
        }
    }

    /** Authenticates a username/password login and establishes a session. */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String password = request.password() == null ? "" : request.password();

        try {
            Authentication authentication = establishSession(email, password, httpRequest, httpResponse);
            return ResponseEntity.ok(buildUserPayload(authentication));
        } catch (AuthenticationException e) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> loginInfo() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "POST credentials to /api/auth/login, or use /oauth2/authorization/google");
        response.put("googleLoginUrl", "/oauth2/authorization/google");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    /** Authenticates the credentials and persists the SecurityContext to the HTTP session. */
    private Authentication establishSession(String email, String password,
                                            HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return authentication;
    }

    private Map<String, Object> buildUserPayload(Authentication authentication) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("authenticated", true);
        payload.put("provider", "local");
        if (authentication.getPrincipal() instanceof LocalUserPrincipal localUser) {
            payload.put("id", localUser.getOwnerKey());
            payload.put("email", localUser.getEmail());
            payload.put("name", localUser.getDisplayName());
        }
        payload.put("picture", null);
        return payload;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", false);
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }

    public record RegisterRequest(String email, String password, String name) {}

    public record LoginRequest(String email, String password) {}
}
