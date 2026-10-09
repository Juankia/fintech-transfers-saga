package io.github.juankia.transfers.application.port.in;

import io.github.juankia.transfers.application.projection.StatementEvent;

public interface ProjectStatementEventUseCase {

    /** @return {@code true} if the event changed the read model, {@code false} if ignored or duplicate. */
    boolean project(StatementEvent event);
}
