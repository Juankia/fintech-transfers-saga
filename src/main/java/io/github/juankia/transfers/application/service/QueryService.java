package io.github.juankia.transfers.application.service;

import io.github.juankia.transfers.application.exception.AccountNotFoundException;
import io.github.juankia.transfers.application.exception.TransferNotFoundException;
import io.github.juankia.transfers.application.port.in.AccountQueries;
import io.github.juankia.transfers.application.port.in.TransferQueries;
import io.github.juankia.transfers.application.port.out.AccountRepository;
import io.github.juankia.transfers.application.port.out.StatementRepository;
import io.github.juankia.transfers.application.port.out.TransferRepository;
import io.github.juankia.transfers.application.projection.AccountStatement;
import io.github.juankia.transfers.application.projection.StatementEntry;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.Transfer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class QueryService implements TransferQueries, AccountQueries {

    private final TransferRepository transfers;
    private final AccountRepository accounts;
    private final StatementRepository statements;

    public QueryService(TransferRepository transfers, AccountRepository accounts, StatementRepository statements) {
        this.transfers = transfers;
        this.accounts = accounts;
        this.statements = statements;
    }

    @Override
    public Transfer getTransfer(UUID transferId, String requesterId) {
        // Someone else's transfer is reported as "not found" so ids cannot be probed.
        return transfers.findById(transferId)
                .filter(t -> t.ownerId().equals(requesterId))
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    @Override
    public Account getAccount(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Override
    public AccountStatement getStatement(UUID accountId) {
        List<StatementEntry> entries = statements.findByAccount(accountId);
        BigDecimal net = entries.stream().map(StatementEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = entries.isEmpty() ? null : entries.get(0).currency();
        return new AccountStatement(accountId, entries, net, currency);
    }

    @Override
    public boolean isOwner(UUID accountId, String principal) {
        return accounts.findById(accountId).map(a -> a.isOwnedBy(principal)).orElse(false);
    }
}
