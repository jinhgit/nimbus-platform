package io.nimbus.platform.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.nimbus.platform.auth.domain.GlobalRole;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(NimbusPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getAccessTokenTtlSeconds());
        return Jwts.builder()
                .subject(principal.userId().toString())
                .claim("email", principal.email())
                .claim("name", principal.name())
                .claim("role", principal.role().name())
                .claim("workspaceId", principal.workspaceId() != null ? principal.workspaceId().toString() : null)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    public NimbusPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            String name = claims.get("name", String.class);
            GlobalRole role = GlobalRole.valueOf(claims.get("role", String.class));
            String workspaceRaw = claims.get("workspaceId", String.class);
            UUID workspaceId = workspaceRaw != null && !workspaceRaw.isBlank()
                    ? UUID.fromString(workspaceRaw)
                    : null;
            return new NimbusPrincipal(userId, email, name, role, workspaceId);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.AUTH_JWT_EXPIRED);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_SIGNATURE);
        }
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtlSeconds();
    }

    public long getRefreshTokenTtlSeconds() {
        return properties.getRefreshTokenTtlSeconds();
    }
}
