package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.application.port.out.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
class ProcessedEventStoreAdapter implements ProcessedEventStore {

    private final ProcessedEventJpaRepository repository;
    private final Clock clock;

    ProcessedEventStoreAdapter(ProcessedEventJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public boolean markProcessed(UUID eventId, String consumer) {
        var key = new ProcessedEventEntity.Key(eventId, consumer);
        if (repository.existsById(key)) {
            return false;
        }
        // Two consumers racing on the same event: the loser gets a constraint violation, its
        // transaction rolls back and the redelivery hits the existsById branch above.
        repository.saveAndFlush(new ProcessedEventEntity(key, clock.instant()));
        return true;
    }
}
