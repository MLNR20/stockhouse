package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.access.AccessService;
import com.example.warehouse_proj.auth.AuthDtos.ForgotPasswordRequest;
import com.example.warehouse_proj.auth.AuthDtos.LoginRequest;
import com.example.warehouse_proj.auth.AuthDtos.ResetPasswordRequest;
import com.example.warehouse_proj.auth.AuthDtos.RefreshRequest;
import com.example.warehouse_proj.auth.AuthDtos.RegisterRequest;
import com.example.warehouse_proj.auth.AuthDtos.TokenResponse;
import com.example.warehouse_proj.auth.AuthDtos.UserResponse;
import com.example.warehouse_proj.identity.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppUserRepository users;
    private final PasswordResetService passwordReset;
    private final LoginRateLimiter rateLimiter;

    public AuthController(
            AuthService authService,
            AppUserRepository users,
            PasswordResetService passwordReset,
            LoginRateLimiter rateLimiter) {
        this.authService = authService;
        this.users = users;
        this.passwordReset = passwordReset;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** Repeated wrong passwords are answered with 429 for a while (see LoginRateLimiter). */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String address = http.getRemoteAddr();
        rateLimiter.checkSignIn(request.email(), address);
        try {
            TokenResponse tokens = authService.login(request);
            rateLimiter.signInSucceeded(request.email(), address);
            return tokens;
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                rateLimiter.signInFailed(request.email(), address);
            }
            throw e;
        }
    }

    /** Always 202, whether or not the account exists, so this cannot be used to discover accounts. */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimiter.checkResetRequest(request.email(), http.getRemoteAddr());
        passwordReset.requestReset(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.resetPassword(request.token(), request.password());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        authService.logout(UUID.fromString(authentication.getName()));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return users.findByIdAndActiveTrue(UUID.fromString(authentication.getName()))
                .map(u -> {
                    var access = AccessService.summaryOf(u);
                    return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
                            access.role(), access.permissions(), access.pages());
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
