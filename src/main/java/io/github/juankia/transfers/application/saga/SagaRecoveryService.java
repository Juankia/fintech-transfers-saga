package io.github.juankia.transfers.application.saga;

import io.github.juankia.transfers.application.port.out.TransferRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resumes sagas left in a non-terminal status (e.g. the instance crashed mid-transfer).
 * Safe to run concurrently with live requests thanks to optimistic locking.
 */
public class SagaRecoveryService {

    private static final Logger log = Logger.getLogger(SagaRecoveryService.class.getName());

    private final TransferRepository transfers;
    private final TransferSagaOrchestrator orchestrator;
    private final Clock clock;
    private final Duration stuckThreshold;

    public SagaRecoveryService(TransferRepository transfers, TransferSagaOrchestrator orchestrator,
                               Clock clock, Duration stuckThreshold) {
        this.transfers = transfers;
        this.orchestrator = orchestrator;
        this.clock = clock;
        this.stuckThreshold = stuckThreshold;
    }

    public int resumeStuckTransfers(int batchSize) {
        List<UUID> stuck = transfers.findInFlightUpdatedBefore(clock.instant().minus(stuckThreshold), batchSize);
        int resumed = 0;
        for (UUID id : stuck) {
            try {
                orchestrator.run(id);
                resumed++;
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Could not resume transfer " + id + "; will retry on next run", e);
            }
        }
        return resumed;
    }
}
