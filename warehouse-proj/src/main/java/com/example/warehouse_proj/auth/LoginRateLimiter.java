package com.example.warehouse_proj.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing. Failed sign-ins are counted over a sliding window, both for one account from one
 * address and for one address overall; once either limit is reached further attempts get 429 until old failures
 * age out. Counting per account *and address* means a stranger cannot lock the real owner out.
 *
 * <p>The counts live in memory, which suits a single server; several instances would need a shared store.
 */
@Component
public class LoginRateLimiter {

    static final int MAX_PER_ACCOUNT = 5;
    static final int MAX_PER_ADDRESS = 20;
    static final Duration WINDOW = Duration.ofMinutes(15);
    /** Above this many tracked keys, expired ones are swept out. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public LoginRateLimiter() {
        this(Clock.systemUTC());
    }

    LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Refuses the attempt (429) if this account or address has failed too often lately. */
    public void checkSignIn(String email, String address) {
        check(accountKey(email, address), MAX_PER_ACCOUNT);
        check(addressKey(address), MAX_PER_ADDRESS);
    }

    public void signInFailed(String email, String address) {
        record(accountKey(email, address));
        record(addressKey(address));
    }

    /** A correct password clears this account's count (the address's overall count stays). */
    public void signInSucceeded(String email, String address) {
        attempts.remove(accountKey(email, address));
    }

    /** Counts one password-reset request and refuses it if there have been too many. */
    public void checkResetRequest(String email, String address) {
        String account = "reset|" + normalise(email) + "|" + address;
        String overall = "reset-address|" + address;
        check(account, MAX_PER_ACCOUNT);
        check(overall, MAX_PER_ADDRESS);
        record(account);
        record(overall);
    }

    private void check(String key, int max) {
        Deque<Instant> times = attempts.get(key);
        if (times == null) {
            return;
        }
        synchronized (times) {
            dropExpired(times);
            if (times.size() >= max) {
                Duration wait = Duration.between(clock.instant(), times.peekFirst().plus(WINDOW));
                long minutes = Math.max(1, (wait.toSeconds() + 59) / 60);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many attempts. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes."));
            }
        }
    }

    private void record(String key) {
        if (attempts.size() > SWEEP_THRESHOLD) {
            sweep();
        }
        Deque<Instant> times = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            dropExpired(times);
            times.addLast(clock.instant());
        }
    }

    private void sweep() {
        attempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                dropExpired(entry.getValue());
                return entry.getValue().isEmpty();
            }
        });
    }

    private void dropExpired(Deque<Instant> times) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) {
            times.pollFirst();
        }
    }

    private static String accountKey(String email, String address) {
        return "account|" + normalise(email) + "|" + address;
    }

    private static String addressKey(String address) {
        return "address|" + address;
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase();
    }
}
