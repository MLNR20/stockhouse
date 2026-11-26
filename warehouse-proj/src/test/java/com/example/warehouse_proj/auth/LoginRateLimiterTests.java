package com.example.warehouse_proj.auth;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTests {

    /** A clock the test moves by hand. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final LoginRateLimiter limiter = new LoginRateLimiter(clock);

    private void fail(String email, String address, int times) {
        for (int i = 0; i < times; i++) {
            limiter.signInFailed(email, address);
        }
    }

    @Test
    void blocksAnAccountAfterFiveFailuresThenLetsItBackInLater() {
        fail("a@example.com", "10.0.0.1", 4);
        assertDoesNotThrow(() -> limiter.checkSignIn("a@example.com", "10.0.0.1"));

        fail("A@Example.com ", "10.0.0.1", 1); // the same account, however it is typed
        ResponseStatusException e = assertThrows(
                ResponseStatusException.class, () -> limiter.checkSignIn("a@example.com", "10.0.0.1"));
        assertEquals(429, e.getStatusCode().value());

        clock.advance(LoginRateLimiter.WINDOW.plusSeconds(1));
        assertDoesNotThrow(() -> limiter.checkSignIn("a@example.com", "10.0.0.1"));
    }

    @Test
    void anotherAddressCannotLockTheOwnerOut() {
        fail("a@example.com", "10.0.0.66", LoginRateLimiter.MAX_PER_ACCOUNT);
        assertDoesNotThrow(() -> limiter.checkSignIn("a@example.com", "10.0.0.1"));
    }

    @Test
    void oneAddressTryingManyAccountsIsBlocked() {
        for (int i = 0; i < LoginRateLimiter.MAX_PER_ADDRESS; i++) {
            limiter.signInFailed("user" + i + "@example.com", "10.0.0.66");
        }
        assertThrows(ResponseStatusException.class, () -> limiter.checkSignIn("new@example.com", "10.0.0.66"));
    }

    @Test
    void aCorrectPasswordClearsTheAccountsCount() {
        fail("a@example.com", "10.0.0.1", LoginRateLimiter.MAX_PER_ACCOUNT - 1);
        limiter.signInSucceeded("a@example.com", "10.0.0.1");
        fail("a@example.com", "10.0.0.1", 1);
        assertDoesNotThrow(() -> limiter.checkSignIn("a@example.com", "10.0.0.1"));
    }

    @Test
    void resetRequestsAreLimitedToo() {
        for (int i = 0; i < LoginRateLimiter.MAX_PER_ACCOUNT; i++) {
            limiter.checkResetRequest("a@example.com", "10.0.0.1");
        }
        assertThrows(ResponseStatusException.class,
                () -> limiter.checkResetRequest("a@example.com", "10.0.0.1"));
    }
}
