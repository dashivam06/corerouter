package com.fleebug.corerouter.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Admin User Insights Response", description = "High-level user insights for admin dashboard")
public class AdminUserInsightsResponse {

    @Schema(description = "Total number of users", example = "3")
    private Long totalUsers;

    @Schema(description = "Percent change in total users from the start of the past month", example = "0.0")
    private BigDecimal usersChangeFromPastMonthPercent;

    @Schema(description = "Active users count", example = "2")
    private Long activeUsers;

    @Schema(description = "Inactive users count", example = "0")
    private Long inactiveUsers;

    @Schema(description = "Active users share in percent", example = "100.0")
    private BigDecimal activeSharePercent;

    @Schema(description = "Suspended users count", example = "1")
    private Long suspendedUsers;

    @Schema(description = "Admin users count", example = "1")
    private Long adminUsers;
}