package com.vpoluboyarov.otp.auth;

import com.vpoluboyarov.otp.shared.AuthenticatedUser;
import com.vpoluboyarov.otp.shared.UnauthorizedException;
import com.vpoluboyarov.otp.user.Role;
import com.vpoluboyarov.otp.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${otp.jwt.secret}") String secret,
                      @Value("${otp.jwt.ttl-seconds}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public TokenIssue generate(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("login", user.getLogin())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new TokenIssue(token, expiresAt);
    }

    public AuthenticatedUser parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            return new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("login", String.class),
                    Role.valueOf(claims.get("role", String.class))
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("invalid or expired token");
        }
    }

    public record TokenIssue(String token, Instant expiresAt) {
    }
}
