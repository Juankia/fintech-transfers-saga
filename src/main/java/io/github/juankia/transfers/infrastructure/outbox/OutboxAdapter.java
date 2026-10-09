package io.github.juankia.transfers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.juankia.transfers.application.port.out.OutboxPort;
import io.github.juankia.transfers.domain.event.TransferEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class OutboxAdapter implements OutboxPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    OutboxAdapter(OutboxEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(TransferEvent event) {
        try {
            repository.save(new OutboxEventEntity(UUID.randomUUID(), "Transfer", event.transferId(), event.type(),
                    objectMapper.writeValueAsString(event), event.occurredAt()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise " + event.type(), e);
        }
    }
}
