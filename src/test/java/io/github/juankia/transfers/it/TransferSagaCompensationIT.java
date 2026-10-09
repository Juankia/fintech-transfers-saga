package io.github.juankia.transfers.it;

import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.infrastructure.outbox.OutboxEventEntity;
import io.github.juankia.transfers.infrastructure.outbox.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferSagaCompensationIT extends AbstractIntegrationTest {

    @Autowired
    OutboxEventJpaRepository outbox;

    @Test
    void creditFailureRefundsThePayerAndRecordsCompensationInTheOutbox() throws Exception {
        UUID payer = openAccount("alice", "500");
        UUID frozenPayee = openFrozenAccount("carol");

        var json = read(postTransfer("alice", newKey(), body(payer, frozenPayee, "200"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("CREDIT_FAILED")));

        assertThat(account(payer).balance()).isEqualTo(Money.of("500", "USD"));
        assertThat(account(payer).reserved()).isEqualTo(Money.of("0", "USD"));
        assertThat(account(frozenPayee).balance()).isEqualTo(Money.of("0", "USD"));

        UUID transferId = UUID.fromString(json.get("id").asText());
        assertThat(outbox.findByAggregateIdOrderByIdAsc(transferId))
                .extracting(OutboxEventEntity::getEventType)
                .containsExactly("TransferInitiated", "FundsReserved", "RiskApproved", "FundsDebited",
                        "CompensationStarted", "DebitReversed", "TransferFailed");
    }

    @Test
    void riskRejectionReleasesTheHold() throws Exception {
        UUID payer = openAccount("alice", "50000");
        UUID payee = openAccount("bob", "0");

        postTransfer("alice", newKey(), body(payer, payee, "15000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.failureReason").value("RISK_REJECTED"))
                .andExpect(jsonPath("$.riskScore").value(75));

        assertThat(account(payer).balance()).isEqualTo(Money.of("50000", "USD"));
        assertThat(account(payer).availableBalance()).isEqualTo(Money.of("50000", "USD"));
    }

    @Test
    void insufficientFundsIsRejectedWithoutTouchingBalances() throws Exception {
        UUID payer = openAccount("alice", "20");
        UUID payee = openAccount("bob", "0");

        postTransfer("alice", newKey(), body(payer, payee, "20.01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.failureReason").value("INSUFFICIENT_FUNDS"));

        assertThat(account(payer).balance()).isEqualTo(Money.of("20", "USD"));
    }

    @Test
    void invalidPayloadIsRejectedBeforeTheSagaStarts() throws Exception {
        UUID payer = openAccount("alice", "20");
        UUID payee = openAccount("bob", "0");

        postTransfer("alice", newKey(), body(payer, payee, "-5")).andExpect(status().isBadRequest());
        // Well-formed JSON, but more precision than USD allows: rejected by the Money value object.
        postTransfer("alice", newKey(), body(payer, payee, "1.001")).andExpect(status().isUnprocessableEntity());
        postTransfer("alice", newKey(), body(payer, payer, "1")).andExpect(status().isUnprocessableEntity());
    }
}
