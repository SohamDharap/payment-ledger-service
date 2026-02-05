package com.bank.DistributedPaymentLedger.WalletService.dto;

import lombok.*;

import java.util.List;

/**
 * Paginated response for ledger entries.
 * Supports efficient querying of transaction history.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerPageResponse {
    
    /**
     * List of ledger entries for this page
     */
    private List<LedgerEntryResponse> entries;

    /**
     * Current page number (zero-indexed)
     */
    private Integer pageNumber;

    /**
     * Page size (number of entries per page)
     */
    private Integer pageSize;

    /**
     * Total number of entries across all pages
     */
    private Long totalEntries;

    /**
     * Total number of pages
     */
    private Integer totalPages;

    /**
     * Whether there are more pages after this one
     */
    private Boolean hasMore;

    /**
     * Status message
     */
    private String message;
}
