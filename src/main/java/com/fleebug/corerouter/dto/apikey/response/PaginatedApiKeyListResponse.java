package com.fleebug.corerouter.dto.apikey.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Paginated API Key List Response",
    description = "Paginated list of API keys with optional status filtering"
)
public class PaginatedApiKeyListResponse {

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total number of API keys matching the filter", example = "150")
    private long totalElements;

    @Schema(description = "Total pages", example = "8")
    private int totalPages;

    @Schema(description = "API keys for this page")
    private List<ApiKeyResponse> apiKeys;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean lastPage;

    public static PaginatedApiKeyListResponse fromPage(Page<ApiKeyResponse> page) {
        return PaginatedApiKeyListResponse.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .apiKeys(page.getContent())
                .lastPage(page.isLast())
                .build();
    }
}
