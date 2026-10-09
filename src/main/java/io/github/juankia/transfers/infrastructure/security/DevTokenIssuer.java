package io.github.juankia.transfers.infrastructure.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

public class DevTokenIssuer {

    public static final String ISSUER = "fintech-transfers-saga-dev";

    private final JwtEncoder encoder;

    DevTokenIssuer(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String issue(String subject, Collection<String> scopes, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("scope", String.join(" ", scopes))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
