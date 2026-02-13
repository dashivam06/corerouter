package com.fleebug.corerouter.dto.model.request;

import com.fleebug.corerouter.enums.model.ModelType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateModelRequest {

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullname;

    @NotBlank(message = "Username cannot be blank")
    @Size(max = 255, message = "Username must not exceed 255 characters")
    private String username;

    @NotBlank(message = "Provider cannot be blank")
    @Size(max = 255, message = "Provider must not exceed 255 characters")
    private String provider;

    @NotBlank(message = "Parameter count cannot be blank")
    private String parameterCount;

    @NotNull(message = "Price per 1k tokens is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePer1kTokens;

    @NotBlank(message = "Endpoint URL cannot be blank")
    private String endpointUrl;

    @NotNull(message = "Type cannot be null")
    private ModelType type;
}
