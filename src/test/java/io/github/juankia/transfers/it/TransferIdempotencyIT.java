package io.github.juankia.transfers.it;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.juankia.transfers.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferIdempotencyIT extends AbstractIntegrationTest {

    @Test
    void retryWithSameKeyAndPayloadReturnsSameTransferAndMovesMoneyOnce() throws Exception {
        UUID payer = openAccount("alice", "1000");
        UUID payee = openAccount("bob", "0");
        String key = newKey();

        JsonNode first = read(postTransfer("alice", key, body(payer, payee, "150"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(jsonPath("$.status").value("COMPLETED")));

        // Same payload, written differently (150.00 vs 150): still the same request.
        JsonNode replay = read(postTransfer("alice", key, body(payer, payee, "150.00"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true")));

        assertThat(replay.get("id").asText()).isEqualTo(first.get("id").asText());
        assertThat(replay.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(account(payer).balance()).isEqualTo(Money.of("850", "USD"));
        assertThat(account(payee).balance()).isEqualTo(Money.of("150", "USD"));
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejectedWith409() throws Exception {
        UUID payer = openAccount("alice", "1000");
        UUID payee = openAccount("bob", "0");
        String key = newKey();

        postTransfer("alice", key, body(payer, payee, "10")).andExpect(status().isCreated());

        postTransfer("alice", key, body(payer, payee, "11"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency-Key reused"));
        assertThat(account(payer).balance()).isEqualTo(Money.of("990", "USD"));
    }

    @Test
    void keysAreScopedPerCaller() throws Exception {
        UUID alicePayer = openAccount("alice", "100");
        UUID bobPayer = openAccount("bob", "100");
        UUID payee = openAccount("carol", "0");
        String sharedKey = newKey();

        JsonNode a = read(postTransfer("alice", sharedKey, body(alicePayer, payee, "1")).andExpect(status().isCreated()));
        JsonNode b = read(postTransfer("bob", sharedKey, body(bobPayer, payee, "1")).andExpect(status().isCreated()));

        assertThat(a.get("id").asText()).isNotEqualTo(b.get("id").asText());
    }

    @Test
    void missingOrMalformedKeyIsBadRequest() throws Exception {
        UUID payer = openAccount("alice", "100");
        UUID payee = openAccount("bob", "0");

        postTransfer("alice", null, body(payer, payee, "1")).andExpect(status().isBadRequest());
        postTransfer("alice", "short", body(payer, payee, "1")).andExpect(status().isBadRequest());
    }

    @Test
    void concurrentDuplicatesProduceExactlyOneTransfer() throws Exception {
        UUID payer = openAccount("alice", "1000");
        UUID payee = openAccount("bob", "0");
        String key = newKey();
        String body = body(payer, payee, "100");

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                calls.add(() -> read(postTransfer("alice", key, body)).path("id").asText(""));
            }
            Set<String> ids = new java.util.HashSet<>();
            for (Future<String> f : pool.invokeAll(calls)) {
                ids.add(f.get());
            }
            // Every response that succeeded points at the same transfer (losers may also see 409 on
            // a concurrent-modification race, which carries no id).
            assertThat(ids.stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet())).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(account(payer).balance()).isEqualTo(Money.of("900", "USD"));
        assertThat(account(payee).balance()).isEqualTo(Money.of("100", "USD"));
    }
}
