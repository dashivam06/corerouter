package com.fleebug.corerouter.dto.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fleebug.corerouter.enums.model.ModelStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateModelStatusRequest {

    @NotNull(message = "Status cannot be null")
    private ModelStatus status;

    private String reason;
}
