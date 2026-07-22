package com.frezo.auth.security;

import com.frezo.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * JWT sign / parse / validate.
 * <p>
 * <b>v1.1 fixes:</b>
 * <ul>
 *   <li>Secret & TTL đọc từ {@link JwtProperties} (validated) — KHÔNG hardcode default trong {@code @Value}.</li>
 *   <li>Issuer + audience thêm vào token.</li>
 *   <li>Log lỗi qua {@code log.warn} thay {@code log.error} cho các trường hợp client (invalid signature, expired).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + props.getExpiration());

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(props.getIssuer())
                .setAudience(props.getAudience())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + props.getExpiration());

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(props.getIssuer())
                .setAudience(props.getAudience())
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(String username, List<String> roles, Short dataAction, String orgId,
                                 String appCode, Boolean isAdmin) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + props.getExpiration());

        Claims claims = Jwts.claims().setSubject(username);
        claims.setIssuer(props.getIssuer());
        claims.setAudience(props.getAudience());
        claims.put("roles", roles);
        claims.put("dataAction", dataAction);
        claims.put("orgid", orgId);
        claims.put("appCode", appCode);
        claims.put("isAdmin", isAdmin != null ? isAdmin : false);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + props.getRefreshExpiration());

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(props.getIssuer())
                .claim("isRefresh", true)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        return getClaimsFromJWT(token).getSubject();
    }

    public Boolean getIsAdminFromJWT(String token) {
        Claims claims = getClaimsFromJWT(token);
        return claims.get("isAdmin", Boolean.class);
    }

    private Claims getClaimsFromJWT(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate JWT — log warn cho client error, chỉ log error cho system error.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
