package io.github.juankia.transfers.application.projection;

import io.github.juankia.transfers.application.port.in.ProjectStatementEventUseCase;
import io.github.juankia.transfers.application.port.out.ProcessedEventStore;
import io.github.juankia.transfers.application.port.out.StatementRepository;
import io.github.juankia.transfers.application.port.out.TransactionRunner;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Builds the account-statement read model from money-movement events. Deduplication and the
 * projection write share one transaction, so a redelivered event is either fully applied or
 * skipped, never applied twice.
 */
public class StatementProjection implements ProjectStatementEventUseCase {

    static final String CONSUMER = "statement-projection";

    private static final Map<String, StatementEntry.EntryType> HANDLED = Map.of(
            "FundsDebited", StatementEntry.EntryType.DEBIT,
            "FundsCredited", StatementEntry.EntryType.CREDIT,
            "DebitReversed", StatementEntry.EntryType.REVERSAL);

    private final ProcessedEventStore processedEvents;
    private final StatementRepository statements;
    private final TransactionRunner tx;

    public StatementProjection(ProcessedEventStore processedEvents, StatementRepository statements, TransactionRunner tx) {
        this.processedEvents = processedEvents;
        this.statements = statements;
        this.tx = tx;
    }

    public static boolean handles(String eventType) {
        return HANDLED.containsKey(eventType);
    }

    @Override
    public boolean project(StatementEvent event) {
        StatementEntry.EntryType type = HANDLED.get(event.eventType());
        if (type == null) {
            return false;
        }
        return tx.inTransaction(() -> {
            if (!processedEvents.markProcessed(event.eventId(), CONSUMER)) {
                return false;
            }
            BigDecimal signed = type == StatementEntry.EntryType.DEBIT
                    ? event.amount().amount().negate()
                    : event.amount().amount();
            statements.append(new StatementEntry(event.eventId(), event.accountId(), event.transferId(), type,
                    signed, event.amount().currency().getCurrencyCode(), event.occurredAt()));
            return true;
        });
    }
}
