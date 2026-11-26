package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Forgot password: a random single-use token is sent to the account's email; only its hash is stored. Presenting
 * it within 30 minutes sets a new password and signs the account out everywhere.
 */
@Service
public class PasswordResetService {

    static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotifier notifier;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            AppUserRepository users, PasswordEncoder passwordEncoder, PasswordResetNotifier notifier) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.notifier = notifier;
    }

    /** Does nothing for unknown or deleted accounts; the caller answers the same either way. */
    @Transactional
    public void requestReset(String email) {
        users.findByEmailIgnoreCase(email.trim())
                .filter(AppUser::isActive)
                .ifPresent(user -> {
                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    user.setResetTokenHash(TokenHashes.sha256(token));
                    user.setResetTokenExpiresAt(Instant.now().plus(TOKEN_LIFETIME));
                    users.save(user);
                    notifier.send(user.getEmail(), token);
                });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        AppUser user = users.findByResetTokenHash(TokenHashes.sha256(token))
                .filter(AppUser::isActive)
                .filter(u -> u.getResetTokenExpiresAt() != null && u.getResetTokenExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "This reset link is invalid or has expired. Request a new one."));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetTokenHash(null);
        user.setResetTokenExpiresAt(null);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
        users.save(user);
    }
}
