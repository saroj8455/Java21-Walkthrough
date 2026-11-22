package com.walkthrough.api.transaction;

import com.walkthrough.api.common.PageResponse;
import com.walkthrough.api.transaction.dto.BankTransactionResponse;
import com.walkthrough.api.transaction.dto.CounterpartyResponse;
import com.walkthrough.api.transaction.dto.FinancialYearResponse;
import com.walkthrough.api.transaction.dto.TransactionStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read API for imported transactions. The same filter parameters work on every endpoint:
 * <pre>
 *   ?type=DEBIT&q=udemy&from=2022-04-01&to=2023-03-31&minAmount=500&maxAmount=5000
 * </pre>
 * {@link TransactionFilter} is a record without annotations on the parameter: Spring treats it as a
 * "model attribute" and fills it from the query string by calling its constructor.
 * {@code @ParameterObject} only tells Swagger to document each field as a separate query parameter.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Browse and analyse imported bank transactions")
public class TransactionController {

    private final TransactionQueryService queryService;

    public TransactionController(TransactionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Search transactions (paged)")
    public PageResponse<BankTransactionResponse> search(
            @ParameterObject TransactionFilter filter,
            @ParameterObject
            @PageableDefault(size = 20, sort = "transactedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return queryService.search(filter, pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "KPIs (count, debit/credit totals, averages, largest) for the filtered transactions")
    public TransactionStatsResponse stats(@ParameterObject TransactionFilter filter) {
        return queryService.stats(filter);
    }

    @GetMapping("/counterparties")
    @Operation(summary = "Most frequent counterparties within the filter")
    public List<CounterpartyResponse> counterparties(@ParameterObject TransactionFilter filter,
                                                     @RequestParam(defaultValue = "8") int limit) {
        return queryService.topCounterparties(filter, limit);
    }

    @GetMapping("/periods")
    @Operation(summary = "Financial years (Apr-Mar) that contain transactions, for quick-filter chips")
    public List<FinancialYearResponse> periods() {
        return queryService.financialYears();
    }
}
