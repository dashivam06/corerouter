package com.fleebug.corerouter.dto.apikey.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Daily API Key Analytics Response",
    description = "API key creation and revocation counts for a single day"
)
public class DailyApiKeyAnalyticsResponse {

    @Schema(description = "Date of the analytics", example = "2026-04-02")
    private LocalDate date;

    @Schema(description = "Number of API keys created on this date", example = "12")
    private long created;

    @Schema(description = "Number of API keys revoked on this date", example = "3")
    private long revoked;
}
