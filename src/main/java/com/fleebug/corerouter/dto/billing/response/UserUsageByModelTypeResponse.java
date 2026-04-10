package com.fleebug.corerouter.dto.billing.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "User Usage By Model Type Response", description = "Lifetime usage request counts by model type using active API keys")
public class UserUsageByModelTypeResponse {

    @Schema(description = "Request counts grouped by model type")
    private Map<String, Long> usageByModelTypeCounts;
}