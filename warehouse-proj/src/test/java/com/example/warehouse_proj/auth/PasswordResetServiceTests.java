package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTests {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private AppUserRepository users;
    private PasswordResetNotifier notifier;
    private PasswordResetService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        notifier = mock(PasswordResetNotifier.class);
        service = new PasswordResetService(users, encoder, notifier);
        user = new AppUser("Test", "User", "a@example.com", encoder.encode("old-password"));
        user.setRefreshToken("session");
        when(users.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(user));
    }

    private String requestToken() {
        service.requestReset("a@example.com");
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(notifier).send(eq("a@example.com"), token.capture());
        when(users.findByResetTokenHash(TokenHashes.sha256(token.getValue()))).thenReturn(Optional.of(user));
        return token.getValue();
    }

    @Test
    void onlyTheHashOfTheTokenIsStored() {
        String token = requestToken();
        assertNotEquals(token, user.getResetTokenHash());
        assertEquals(TokenHashes.sha256(token), user.getResetTokenHash());
    }

    @Test
    void aValidTokenSetsTheNewPasswordAndSignsOutEverywhere() {
        String token = requestToken();

        service.resetPassword(token, "brand-new-password");

        assertTrue(encoder.matches("brand-new-password", user.getPasswordHash()));
        assertNull(user.getRefreshToken());
        assertNull(user.getResetTokenHash(), "a token works only once");
    }

    @Test
    void anExpiredTokenIsRefused() {
        String token = requestToken();
        user.setResetTokenExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(ResponseStatusException.class, () -> service.resetPassword(token, "brand-new-password"));
        assertTrue(encoder.matches("old-password", user.getPasswordHash()));
    }

    @Test
    void unknownAndDeletedAccountsGetNothing() {
        service.requestReset("nobody@example.com");
        user.setActive(false);
        service.requestReset("a@example.com");

        verify(notifier, never()).send(anyString(), anyString());
    }
}
