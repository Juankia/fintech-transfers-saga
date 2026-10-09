package io.github.juankia.transfers.infrastructure.web;

import io.github.juankia.transfers.application.port.in.InitiateTransferCommand;
import io.github.juankia.transfers.application.port.in.InitiateTransferResult;
import io.github.juankia.transfers.application.port.in.InitiateTransferUseCase;
import io.github.juankia.transfers.application.port.in.TransferQueries;
import io.github.juankia.transfers.domain.model.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "transfers")
class TransferController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_\\-:.]{8,64}");

    private final InitiateTransferUseCase initiateTransfer;
    private final TransferQueries queries;

    TransferController(InitiateTransferUseCase initiateTransfer, TransferQueries queries) {
        this.initiateTransfer = initiateTransfer;
        this.queries = queries;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_transfers:write')")
    @Operation(summary = "Initiate a transfer",
            description = "Runs the transfer saga synchronously and returns its final state. Retries with the "
                    + "same Idempotency-Key and payload replay the original result; a different payload yields 409.")
    ResponseEntity<TransferResponse> create(
            @Parameter(description = "Client-generated unique key (8-64 chars), e.g. a UUID")
            @RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (!VALID_KEY.matcher(idempotencyKey).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
        var command = new InitiateTransferCommand(jwt.getSubject(), idempotencyKey, request.sourceAccountId(),
                request.targetAccountId(), Money.of(request.amount(), request.currency()), request.reference());
        InitiateTransferResult result = initiateTransfer.initiate(command);
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + result.transfer().id()))
                .header(IDEMPOTENT_REPLAYED, String.valueOf(result.replayed()))
                .body(TransferResponse.from(result.transfer()));
    }

    @GetMapping("/{transferId}")
    @PreAuthorize("hasAuthority('SCOPE_transfers:read')")
    @Operation(summary = "Get a transfer owned by the caller")
    TransferResponse get(@PathVariable UUID transferId, @AuthenticationPrincipal Jwt jwt) {
        return TransferResponse.from(queries.getTransfer(transferId, jwt.getSubject()));
    }

    static class InvalidIdempotencyKeyException extends RuntimeException {
        InvalidIdempotencyKeyException() {
            super("Idempotency-Key must be 8-64 characters of [A-Za-z0-9_-:.]");
        }
    }
}
