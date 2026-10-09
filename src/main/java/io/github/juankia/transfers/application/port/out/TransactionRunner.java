package io.github.juankia.transfers.application.port.out;

import java.util.function.Supplier;

/**
 * Keeps transaction demarcation explicit in the use cases without coupling them to Spring.
 */
public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);
}
