package com.example.warehouse_proj.notification;

import com.example.warehouse_proj.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates STOMP clients. Browsers cannot set headers on the WebSocket handshake, so the JWT is
 * sent in the STOMP CONNECT frame ("Authorization: Bearer ..."). Clients are receive-only: they may
 * subscribe to their own /user/queue/** destinations and nothing else.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";
    private static final String USER_QUEUE_PREFIX = "/user/queue/";

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith(BEARER)) {
                throw new MessagingException("Missing bearer token");
            }
            try {
                Claims claims = jwtService.parse(header.substring(BEARER.length()).trim(), JwtService.ACCESS);
                accessor.setUser(new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of()));
            } catch (JwtException | IllegalArgumentException e) {
                throw new MessagingException("Invalid or expired token");
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith(USER_QUEUE_PREFIX)) {
                throw new MessagingException("Subscription not allowed");
            }
        } else if (StompCommand.SEND.equals(command)) {
            throw new MessagingException("Clients cannot send messages");
        }
        return message;
    }
}
