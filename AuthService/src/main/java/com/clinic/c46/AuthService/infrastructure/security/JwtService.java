package com.clinic.c46.AuthService.infrastructure.security;

import com.clinic.c46.AuthService.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtService {
    
    private final JwtProperties jwtProperties;
    
    public String generateAccessToken(String accountName, Integer accountId, String staffId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", accountId);
        if (staffId != null) {
            claims.put("staffId", staffId);
        }
        return createToken(claims, accountName, jwtProperties.getAccessTokenExpiration());
    }
    
    public String generateAccessToken(String accountName, Integer accountId, String staffId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", accountId);
        claims.put("role", role);
        if (staffId != null) {
            claims.put("staffId", staffId);
        }
        return createToken(claims, accountName, jwtProperties.getAccessTokenExpiration());
    }
    
    public String generateRefreshToken(String accountName) {
        return createToken(new HashMap<>(), accountName, jwtProperties.getRefreshTokenExpiration());
    }
    
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Integer extractAccountId(String token) {
        return extractClaim(token, claims -> claims.get("accountId", Integer.class));
    }
    
    public String extractStaffId(String token) {
        return extractClaim(token, claims -> claims.get("staffId", String.class));
    }
    
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
