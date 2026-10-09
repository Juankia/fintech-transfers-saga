package io.github.juankia.transfers.infrastructure.config;

import io.github.juankia.transfers.application.saga.SagaRecoveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "transfers.saga.recovery.enabled", havingValue = "true", matchIfMissing = true)
class SagaRecoveryScheduler {

    private final SagaRecoveryService recovery;

    SagaRecoveryScheduler(SagaRecoveryService recovery) {
        this.recovery = recovery;
    }

    @Scheduled(fixedDelayString = "${transfers.saga.recovery.interval-ms:15000}")
    void resume() {
        recovery.resumeStuckTransfers(50);
    }
}
