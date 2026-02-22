package com.fleebug.corerouter.dto.model.request;

import com.fleebug.corerouter.enums.model.ModelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    title = "Update Model Status Request",
    description = "Request to change the status of a model (activate, deactivate, deprecate). Optionally includes reason for the status change.",
    example = "{\"status\": \"DEPRECATED\", \"reason\": \"Replaced by GPT-4 Turbo with better performance\"}"
)
public class UpdateModelStatusRequest {

    @NotNull(message = "Status cannot be null")
    @Schema(
        description = "New status for the model. ACTIVE - model is available, INACTIVE - model is hidden, DEPRECATED - being phased out.",
        requiredMode = RequiredMode.REQUIRED,
        example = "ACTIVE",
        enumAsRef = true
    )
    private ModelStatus status;

    @Schema(
        description = "Optional reason for the status change (e.g., 'Maintenance window', 'Deprecated in favor of V2', 'Cost optimization')",
        example = "Replaced by GPT-4 Turbo with better performance",
        maxLength = 500
    )
    private String reason;
}
