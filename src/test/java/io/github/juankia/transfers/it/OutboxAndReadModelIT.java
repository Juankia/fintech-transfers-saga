package io.github.juankia.transfers.it;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.juankia.transfers.application.port.in.ProjectStatementEventUseCase;
import io.github.juankia.transfers.application.projection.StatementEvent;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.infrastructure.outbox.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OutboxAndReadModelIT extends AbstractIntegrationTest {

    @Autowired
    OutboxEventJpaRepository outbox;
    @Autowired
    ProjectStatementEventUseCase projection;

    @Test
    void completedTransferFlowsThroughOutboxAndKafkaIntoTheStatementReadModel() throws Exception {
        UUID payer = openAccount("alice", "300");
        UUID payee = openAccount("bob", "0");

        postTransfer("alice", newKey(), body(payer, payee, "120.50")).andExpect(status().isCreated());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            JsonNode payerStatement = getJson("alice", "/api/v1/accounts/" + payer + "/statement");
            assertThat(payerStatement.get("entries")).hasSize(1);
            assertThat(payerStatement.get("entries").get(0).get("type").asText()).isEqualTo("DEBIT");
            assertThat(payerStatement.get("netMovement").decimalValue()).isEqualByComparingTo("-120.50");

            JsonNode payeeStatement = getJson("bob", "/api/v1/accounts/" + payee + "/statement");
            assertThat(payeeStatement.get("netMovement").decimalValue()).isEqualByComparingTo("120.50");
        });
        await().atMost(Duration.ofSeconds(10)).until(() -> outbox.countByPublishedAtIsNull() == 0);
    }

    @Test
    void compensatedTransferNetsToZeroInTheStatement() throws Exception {
        UUID payer = openAccount("alice", "300");
        UUID frozen = openFrozenAccount("carol");

        postTransfer("alice", newKey(), body(payer, frozen, "80")).andExpect(status().isCreated());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            JsonNode statement = getJson("alice", "/api/v1/accounts/" + payer + "/statement");
            assertThat(statement.get("entries")).hasSize(2);
            assertThat(statement.get("netMovement").decimalValue()).isEqualByComparingTo("0");
        });
    }

    @Test
    void redeliveredEventIsProjectedOnlyOnce() throws Exception {
        UUID account = openAccount("alice", "0");
        var event = new StatementEvent(UUID.randomUUID(), "FundsCredited", UUID.randomUUID(), account,
                Money.of("10", "USD"), Instant.now());

        assertThat(projection.project(event)).isTrue();
        assertThat(projection.project(event)).isFalse();

        JsonNode statement = getJson("alice", "/api/v1/accounts/" + account + "/statement");
        assertThat(statement.get("entries")).hasSize(1);
    }
}
