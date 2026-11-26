package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.access.AccessCatalog;
import com.example.warehouse_proj.access.RoleRepository;
import com.example.warehouse_proj.auth.AuthDtos.LoginRequest;
import com.example.warehouse_proj.auth.AuthDtos.RegisterRequest;
import com.example.warehouse_proj.auth.AuthDtos.TokenResponse;
import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roles;
    private final boolean registrationEnabled;

    public AuthService(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RoleRepository roles,
            @Value("${app.auth.registration-enabled:false}") boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Self-registration is off unless app.auth.registration-enabled is set; admins add users instead. */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (!registrationEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Registration is closed. Ask an administrator to create your account.");
        }
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        AppUser user = new AppUser(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()));
        // Self-registered people start with the least access; an admin can promote them.
        user.setRole(roles.findByNameIgnoreCase(AccessCatalog.DEFAULT_ROLE).orElse(null));
        return issueTokens(users.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(AppUser::isActive)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return issueTokens(user);
    }

    /** Rotates the refresh token: the presented one stops working once a new pair is issued. */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        AppUser user;
        try {
            Claims claims = jwtService.parse(refreshToken, JwtService.REFRESH);
            user = users.findByIdAndActiveTrue(UUID.fromString(claims.getSubject())).orElseThrow(this::invalidRefresh);
        } catch (JwtException | IllegalArgumentException e) {
            throw invalidRefresh();
        }

        String stored = user.getRefreshToken();
        Instant expiresAt = user.getRefreshTokenExpiresAt();
        boolean matches = stored != null
                && MessageDigest.isEqual(hash(refreshToken).getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
        if (!matches || expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw invalidRefresh();
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(UUID userId) {
        users.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiresAt(null);
        });
    }

    private TokenResponse issueTokens(AppUser user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        user.setRefreshToken(hash(refresh));
        user.setRefreshTokenExpiresAt(Instant.now().plus(jwtService.refreshTokenLifetime()));
        users.save(user);
        return new TokenResponse(access, refresh, "Bearer", jwtService.accessTokenLifetime().toSeconds());
    }

    private ResponseStatusException invalidRefresh() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    private static String hash(String token) {
        return TokenHashes.sha256(token);
    }
}
