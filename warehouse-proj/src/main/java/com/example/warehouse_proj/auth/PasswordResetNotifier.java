package com.example.warehouse_proj.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Delivers password-reset links. There is no mail server configured, so for local development the link can be
 * written to the server log (app.password-reset.log-links=true). To send real email, replace the body of
 * {@link #send} with a mail client; nothing else needs to change.
 */
@Component
public class PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotifier.class);

    private final boolean logLinks;
    private final String clientUrl;

    public PasswordResetNotifier(
            @Value("${app.password-reset.log-links:false}") boolean logLinks,
            @Value("${app.client-url:http://localhost:5173}") String clientUrl) {
        this.logLinks = logLinks;
        this.clientUrl = clientUrl;
    }

    public void send(String email, String token) {
        if (!logLinks) {
            log.warn("A password reset was requested, but no delivery is configured, so nothing was sent. "
                    + "For local development set LOG_RESET_LINKS=true.");
            return;
        }
        // The token goes after '#', so it is never sent to a server or written to web server access logs.
        String link = UriComponentsBuilder.fromUriString(clientUrl).path("/reset-password").build() + "#" + token;
        log.info("Password reset link for {} (valid 30 minutes): {}", email, link);
    }
}
