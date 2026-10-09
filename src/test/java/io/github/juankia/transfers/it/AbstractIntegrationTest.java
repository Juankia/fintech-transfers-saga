package io.github.juankia.transfers.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.juankia.transfers.application.port.out.AccountRepository;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.infrastructure.security.DevTokenIssuer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Full application context on H2 (PostgreSQL mode) + embedded Kafka; no Docker required.
 * Tests use real signed JWTs from the in-process dev issuer, so the security chain is exercised end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "transfers.events")
abstract class AbstractIntegrationTest {

    static final List<String> ALL_SCOPES = List.of("transfers:write", "transfers:read", "accounts:read");

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    AccountRepository accounts;
    @Autowired
    DevTokenIssuer tokens;

    UUID openAccount(String owner, String balance) {
        return accounts.save(Account.open(UUID.randomUUID(), owner, Money.of(balance, "USD"))).id();
    }

    UUID openFrozenAccount(String owner) {
        Account account = Account.open(UUID.randomUUID(), owner, Money.of("0", "USD"));
        account.freeze();
        return accounts.save(account).id();
    }

    Account account(UUID id) {
        return accounts.findById(id).orElseThrow();
    }

    String bearer(String subject, List<String> scopes) {
        return "Bearer " + tokens.issue(subject, scopes, Duration.ofMinutes(5));
    }

    String bearer(String subject) {
        return bearer(subject, ALL_SCOPES);
    }

    String body(UUID source, UUID target, String amount) {
        return """
                {"sourceAccountId":"%s","targetAccountId":"%s","amount":%s,"currency":"USD","reference":"it"}"""
                .formatted(source, target, amount);
    }

    ResultActions postTransfer(String subject, String idempotencyKey, String body) throws Exception {
        var request = post("/api/v1/transfers")
                .header("Authorization", bearer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mvc.perform(request);
    }

    JsonNode getJson(String subject, String path) throws Exception {
        String content = mvc.perform(get(path).header("Authorization", bearer(subject)))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(content);
    }

    JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    static String newKey() {
        return "it-" + UUID.randomUUID();
    }
}
