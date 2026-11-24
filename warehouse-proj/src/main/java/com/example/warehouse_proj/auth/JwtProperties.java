package com.example.warehouse_proj.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, long accessTokenMinutes, long refreshTokenDays) {
}
