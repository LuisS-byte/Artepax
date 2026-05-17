package com.catolica.Artepax.authentication.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token generation, validation, and claim extraction.
 * <p>
 * Reads {@code app.jwt.secret} (Base64-encoded HMAC key) and
 * {@code app.jwt.exp-hours} from application properties.
 * Tokens are signed with HMAC-SHA256.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.exp-hours}")
    private int expirationHours;

    /**
     * Extracts the username (subject / email) from a JWT.
     *
     * @param token the JWT string
     * @return the subject claim (user email)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts an arbitrary claim from a JWT using a resolver function.
     *
     * @param <T>            the type of the claim value
     * @param token          the JWT string
     * @param claimsResolver a function that extracts the desired claim from {@link Claims}
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT for the given user with no extra claims.
     *
     * @param userDetails the user details (subject will be the username/email)
     * @return a signed JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT with extra claims for the given user.
     *
     * @param extraClaims a map of additional claims to include in the token
     * @param userDetails the user details (subject will be the username/email)
     * @return a signed JWT string
     */
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // email
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationHours * 60L * 60L * 1000L))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a JWT against the given user details.
     * <p>
     * Checks that the token's subject matches the user's username and
     * that the token has not expired.
     *
     * @param token       the JWT string
     * @param userDetails the user details to validate against
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks whether a JWT has expired.
     *
     * @param token the JWT string
     * @return {@code true} if the token's expiration date is before now
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT.
     *
     * @param token the JWT string
     * @return the {@link Date} when the token expires
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parses a JWT and extracts all claims.
     *
     * @param token the JWT string
     * @return the {@link Claims} contained in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Decodes the Base64-encoded secret key and returns an HMAC-SHA key.
     *
     * @return the {@link Key} used for signing and verification
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}