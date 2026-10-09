package io.github.juankia.transfers.application.port.out;

import io.github.juankia.transfers.application.projection.StatementEntry;

import java.util.List;
import java.util.UUID;

public interface StatementRepository {

    void append(StatementEntry entry);

    List<StatementEntry> findByAccount(UUID accountId);
}
