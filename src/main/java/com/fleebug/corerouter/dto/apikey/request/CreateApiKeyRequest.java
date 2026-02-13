package com.fleebug.corerouter.dto.apikey.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyRequest {

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @Min(value = 1, message = "Daily limit must be at least 1")
    private Integer dailyLimit;

    @Min(value = 1, message = "Monthly limit must be at least 1")
    private Integer monthlyLimit;
}
