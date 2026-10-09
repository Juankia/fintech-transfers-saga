package io.github.juankia.transfers.infrastructure.config;

import io.github.juankia.transfers.application.port.out.*;
import io.github.juankia.transfers.application.projection.StatementProjection;
import io.github.juankia.transfers.application.saga.SagaRecoveryService;
import io.github.juankia.transfers.application.saga.TransferSagaOrchestrator;
import io.github.juankia.transfers.application.service.InitiateTransferService;
import io.github.juankia.transfers.application.service.QueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Wires the framework-free application layer. Domain and application classes carry no Spring
 * annotations (enforced by ArchitectureTest).
 */
@Configuration
class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TransferSagaOrchestrator transferSagaOrchestrator(TransferRepository transfers, AccountRepository accounts,
                                                      RiskScoringPort risk, OutboxPort outbox,
                                                      TransactionRunner tx, Clock clock) {
        return new TransferSagaOrchestrator(transfers, accounts, risk, outbox, tx, clock);
    }

    @Bean
    InitiateTransferService initiateTransferService(AccountRepository accounts, TransferRepository transfers,
                                                    IdempotencyStore idempotencyStore, OutboxPort outbox,
                                                    TransferSagaOrchestrator orchestrator, TransactionRunner tx,
                                                    Clock clock) {
        return new InitiateTransferService(accounts, transfers, idempotencyStore, outbox, orchestrator, tx, clock,
                UUID::randomUUID);
    }

    @Bean
    QueryService queryService(TransferRepository transfers, AccountRepository accounts, StatementRepository statements) {
        return new QueryService(transfers, accounts, statements);
    }

    @Bean
    StatementProjection statementProjection(ProcessedEventStore processed, StatementRepository statements,
                                            TransactionRunner tx) {
        return new StatementProjection(processed, statements, tx);
    }

    @Bean
    SagaRecoveryService sagaRecoveryService(TransferRepository transfers, TransferSagaOrchestrator orchestrator,
                                            Clock clock,
                                            @Value("${transfers.saga.recovery.stuck-after:PT30S}") Duration stuckAfter) {
        return new SagaRecoveryService(transfers, orchestrator, clock, stuckAfter);
    }
}
