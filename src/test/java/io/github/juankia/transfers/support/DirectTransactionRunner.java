package io.github.juankia.transfers.support;

import io.github.juankia.transfers.application.port.out.TransactionRunner;

import java.util.function.Supplier;

public class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }
}
