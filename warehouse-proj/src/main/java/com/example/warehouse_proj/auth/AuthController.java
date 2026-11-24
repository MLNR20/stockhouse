package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.access.AccessService;
import com.example.warehouse_proj.auth.AuthDtos.LoginRequest;
import com.example.warehouse_proj.auth.AuthDtos.RefreshRequest;
import com.example.warehouse_proj.auth.AuthDtos.RegisterRequest;
import com.example.warehouse_proj.auth.AuthDtos.TokenResponse;
import com.example.warehouse_proj.auth.AuthDtos.UserResponse;
import com.example.warehouse_proj.identity.AppUserRepository;
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

    public AuthController(AuthService authService, AppUserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
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
