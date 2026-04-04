package com.fleebug.corerouter.dto.apikey.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "API Key Analytics Response",
    description = "Analytics data for API key creation and revocation over a date range"
)
public class ApiKeyAnalyticsResponse {

    @Schema(description = "Daily analytics sorted by date ascending")
    private List<DailyApiKeyAnalyticsResponse> dailyAnalytics;

    @Schema(description = "Total API keys created in range", example = "180")
    private long totalCreated;

    @Schema(description = "Total API keys revoked in range", example = "24")
    private long totalRevoked;
}
