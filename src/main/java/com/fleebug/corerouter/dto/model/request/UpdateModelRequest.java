package com.fleebug.corerouter.dto.model.request;

import com.fleebug.corerouter.enums.model.ModelStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateModelRequest {

    private String fullname;

    private String username;

    private String provider;

    private String parameterCount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePer1kTokens;

    private String endpointUrl;

    private ModelStatus status;
}
