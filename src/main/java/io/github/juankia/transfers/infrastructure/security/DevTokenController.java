package io.github.juankia.transfers.infrastructure.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Mints short-lived tokens for local testing. Only exists in the 'dev' profile; in real
 * deployments tokens come from the bank's identity provider.
 */
@RestController
@Profile("dev")
@Tag(name = "dev")
class DevTokenController {

    private static final List<String> DEFAULT_SCOPES = List.of("transfers:write", "transfers:read", "accounts:read");

    private final DevTokenIssuer issuer;

    DevTokenController(DevTokenIssuer issuer) {
        this.issuer = issuer;
    }

    @PostMapping("/dev/tokens")
    @Operation(summary = "Issue a development JWT (dev profile only)")
    Map<String, String> token(@RequestParam String subject,
                              @RequestParam(required = false) List<String> scope) {
        List<String> scopes = scope == null || scope.isEmpty() ? DEFAULT_SCOPES : scope;
        return Map.of("access_token", issuer.issue(subject, scopes, Duration.ofHours(1)), "token_type", "Bearer");
    }
}
