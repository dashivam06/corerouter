package com.fleebug.corerouter.dto.user.response;

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
    title = "Daily User Analytics Response",
    description = "User creation, deletion, and revocation counts for a single day"
)
public class DailyUserAnalyticsResponse {

    @Schema(description = "Date of the analytics", example = "2026-04-02")
    private LocalDate date;

    @Schema(description = "Number of users created on this date", example = "5")
    private long created;

    @Schema(description = "Number of users deleted (status=DELETED) on this date", example = "2")
    private long deleted;

    @Schema(description = "Number of users revoked/suspended (status=SUSPENDED or BANNED) on this date", example = "1")
    private long revoked;
}
