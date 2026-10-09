package io.github.juankia.transfers.application.projection;

import io.github.juankia.transfers.application.port.out.ProcessedEventStore;
import io.github.juankia.transfers.application.port.out.StatementRepository;
import io.github.juankia.transfers.domain.model.Money;
import io.github.juankia.transfers.support.DirectTransactionRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementProjectionTest {

    @Mock
    ProcessedEventStore processed;
    @Mock
    StatementRepository statements;

    @Test
    void debitIsProjectedAsNegativeEntry() {
        var projection = new StatementProjection(processed, statements, new DirectTransactionRunner());
        var event = event("FundsDebited");
        when(processed.markProcessed(event.eventId(), StatementProjection.CONSUMER)).thenReturn(true);

        assertThat(projection.project(event)).isTrue();

        ArgumentCaptor<StatementEntry> entry = ArgumentCaptor.forClass(StatementEntry.class);
        verify(statements).append(entry.capture());
        assertThat(entry.getValue().amount()).isEqualByComparingTo(new BigDecimal("-42.50"));
        assertThat(entry.getValue().type()).isEqualTo(StatementEntry.EntryType.DEBIT);
    }

    @Test
    void duplicateDeliveryIsSkipped() {
        var projection = new StatementProjection(processed, statements, new DirectTransactionRunner());
        var event = event("FundsCredited");
        when(processed.markProcessed(event.eventId(), StatementProjection.CONSUMER)).thenReturn(false);

        assertThat(projection.project(event)).isFalse();
        verify(statements, never()).append(any());
    }

    @Test
    void irrelevantEventsAreIgnored() {
        var projection = new StatementProjection(processed, statements, new DirectTransactionRunner());

        assertThat(projection.project(event("FundsReserved"))).isFalse();
        verifyNoInteractions(processed, statements);
    }

    private static StatementEvent event(String type) {
        return new StatementEvent(UUID.randomUUID(), type, UUID.randomUUID(), UUID.randomUUID(),
                Money.of("42.50", "USD"), Instant.now());
    }
}
