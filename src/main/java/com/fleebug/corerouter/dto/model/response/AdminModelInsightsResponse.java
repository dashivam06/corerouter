package com.fleebug.corerouter.dto.model.response;

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
@Schema(title = "Admin Model Insights Response", description = "High-level model and provider counts for admin dashboard")
public class AdminModelInsightsResponse {

    @Schema(description = "Total number of models", example = "42")
    private Long totalModels;

    @Schema(description = "Number of active models", example = "30")
    private Long activeModels;

    @Schema(description = "Total number of providers", example = "8")
    private Long providers;
}