package com.paystream.ledger.web;

import com.paystream.ledger.service.LedgerQueryService;
import com.paystream.ledger.web.dto.LedgerAccountResponse;
import com.paystream.ledger.web.dto.LedgerEntryResponse;
import com.paystream.ledger.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Read-only HTTP API over the ledger. Balances change only by consuming payment events, never via HTTP.
 */
@Tag(name = "Ledger", description = "Read balances and journal entries")
@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;

    @GetMapping("/accounts/{accountNumber}")
    @Operation(summary = "Get an account's current balance")
    public LedgerAccountResponse getAccount(@PathVariable String accountNumber) {
        return ledgerQueryService.getAccount(accountNumber);
    }

    @GetMapping("/accounts/{accountNumber}/entries")
    @Operation(summary = "Account statement: the account's entries, newest first")
    public PageResponse<LedgerEntryResponse> getAccountEntries(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size) {
        return ledgerQueryService.getAccountEntries(accountNumber, page, size);
    }

    @GetMapping("/entries")
    @Operation(summary = "Debit and credit entries recorded for a payment")
    public List<LedgerEntryResponse> getEntriesForPayment(@RequestParam UUID paymentId) {
        return ledgerQueryService.getEntriesForPayment(paymentId);
    }
}
