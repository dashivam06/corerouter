package com.fleebug.corerouter.dto.apikey.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Admin API Key Insights Response", description = "High-level API key counts for admin dashboard")
public class AdminApiKeyInsightsResponse {

    @Schema(description = "Total API keys", example = "120")
    private Long totalKeys;

    @Schema(description = "Active API keys", example = "80")
    private Long active;

    @Schema(description = "Inactive API keys", example = "25")
    private Long inactive;

    @Schema(description = "Revoked API keys", example = "15")
    private Long revoked;
}