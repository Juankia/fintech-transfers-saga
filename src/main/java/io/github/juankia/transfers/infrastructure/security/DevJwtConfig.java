package io.github.juankia.transfers.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * <b>Local development and tests only.</b> Replaces the external identity provider with an
 * in-process HS256 key that is randomly generated at startup (nothing is committed, tokens die
 * with the process). In any other profile the app validates tokens from
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
 */
@Configuration
@Profile({"dev", "test"})
class DevJwtConfig {

    private final SecretKey key;

    DevJwtConfig() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        this.key = new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    DevTokenIssuer devTokenIssuer(JwtEncoder encoder) {
        return new DevTokenIssuer(encoder);
    }
}
