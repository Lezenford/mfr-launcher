package ru.fullrest.mfr.server.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.log4j.Log4j2;

import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

@Log4j2
public class JwtTokenService {

    private final String issuer;
    private final SecretKeySpec secretKey;
    private final JwtParser jwtParser;
    private final SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;

    public JwtTokenService(String secretKey, String issuer) {
        this.issuer = issuer;
        this.secretKey = new SecretKeySpec(secretKey.getBytes(), algorithm.getJcaName());
        this.jwtParser = Jwts.parserBuilder().setSigningKey(this.secretKey).build();
    }

    public String generateJwtToken(String subject) {
        return Jwts.builder()
                .setNotBefore(new Date())
                .setIssuedAt(new Date())
                .setSubject(subject)
                .setIssuer(issuer)
                .setExpiration(new Date(1609448400000L))
                .signWith(secretKey, algorithm)
                .compact();
    }

    public String getSubjectFromJwt(String jwt) {
        try {
            return jwtParser.parseClaimsJws(jwt).getBody().getSubject();
        } catch (JwtException e) {
            log.error("Token validation exception\n" + e.getMessage());
            return null;
        }
    }
}
