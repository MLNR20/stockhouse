package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.identity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TYPE_CLAIM = "type";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] bytes = Decoders.BASE64.decode(props.secret());
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be a base64 string of at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public Duration accessTokenLifetime() {
        return Duration.ofMinutes(props.accessTokenMinutes());
    }

    public Duration refreshTokenLifetime() {
        return Duration.ofDays(props.refreshTokenDays());
    }

    public String generateAccessToken(AppUser user) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .claim(TYPE_CLAIM, ACCESS)
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(accessTokenLifetime())))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(AppUser user) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // makes every refresh token unique
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .claim(TYPE_CLAIM, REFRESH)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(refreshTokenLifetime())))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies signature, issuer, expiry and token type.
     *
     * @throws JwtException             if the token is invalid, expired or of the wrong type
     * @throws IllegalArgumentException if the token is blank
     */
    public Claims parse(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Unexpected token type");
        }
        return claims;
    }
}
