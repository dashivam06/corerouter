package com.fleebug.corerouter.dto.user.response;

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
    title = "User Analytics Response",
    description = "Analytics data for user creation, deletion, and revocation over a date range"
)
public class UserAnalyticsResponse {

    @Schema(
        description = "List of daily user analytics sorted by date ascending",
        example = "[{\"date\": \"2026-04-02\", \"created\": 5, \"deleted\": 2, \"revoked\": 1}]"
    )
    private List<DailyUserAnalyticsResponse> dailyAnalytics;

    @Schema(description = "Total users created in the date range", example = "150")
    private long totalCreated;

    @Schema(description = "Total users deleted in the date range", example = "30")
    private long totalDeleted;

    @Schema(description = "Total users revoked/suspended in the date range", example = "15")
    private long totalRevoked;
}
