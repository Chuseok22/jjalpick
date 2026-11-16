package com.chuseok22.jjalpick.auth.infrastructure.config;

import com.chuseok22.jjalpick.auth.core.port.IssuedToken;
import com.chuseok22.jjalpick.auth.core.port.TokenProvider;
import com.chuseok22.jjalpick.auth.infrastructure.properties.JwtProperties;
import com.chuseok22.jjalpick.common.core.exception.CustomException;
import com.chuseok22.jjalpick.common.core.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider implements TokenProvider {

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.secretKey()));
    }

    @Override
    public IssuedToken issueAccessToken(UUID memberId) {
        return issue(memberId, ACCESS_TYPE, jwtProperties.accessExpMillis());
    }

    @Override
    public IssuedToken issueRefreshToken(UUID memberId) {
        return issue(memberId, REFRESH_TYPE, jwtProperties.refreshExpMillis());
    }

    @Override
    public UUID parseAccessToken(String token) {
        return parse(token, ACCESS_TYPE, ErrorCode.ACCESS_TOKEN_EXPIRED, ErrorCode.UNAUTHENTICATED);
    }

    @Override
    public UUID parseRefreshToken(String token) {
        return parse(token, REFRESH_TYPE, ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // jti는 같은 시각에 발급한 토큰도 서로 다르게 만들어 갱신 때 이전 토큰과 값이 같아지는 것을 막는다
    private IssuedToken issue(UUID memberId, String type, long expMillis) {
        Instant issuedAt = clock.instant();
        // JWT의 exp는 초 단위이므로 응답의 만료 시각도 같은 값으로 맞춘다
        Instant expiresAt = issuedAt.plusMillis(expMillis).truncatedTo(ChronoUnit.SECONDS);
        String token = Jwts.builder()
            .subject(memberId.toString())
            .claim(TYPE_CLAIM, type)
            .id(UUID.randomUUID().toString())
            .issuer(jwtProperties.issuer())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey, Jwts.SIG.HS512)
            .compact();
        return new IssuedToken(token, expiresAt);
    }

    private UUID parse(String token, String expectedType, ErrorCode expiredCode, ErrorCode invalidCode) {
        try {
            Jws<Claims> signedClaims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token);
            if (!Jwts.SIG.HS512.getId().equals(signedClaims.getHeader().getAlgorithm())) {
                throw new CustomException(invalidCode);
            }
            Claims claims = signedClaims.getPayload();
            String subject = claims.getSubject();
            if (subject == null || !expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
                throw new CustomException(invalidCode);
            }
            return UUID.fromString(subject);
        } catch (ExpiredJwtException exception) {
            throw new CustomException(expiredCode, exception);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(invalidCode, exception);
        }
    }

}
