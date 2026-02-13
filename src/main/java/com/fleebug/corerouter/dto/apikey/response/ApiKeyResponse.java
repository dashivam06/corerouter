package com.fleebug.corerouter.dto.apikey.response;

import java.time.LocalDateTime;
import com.fleebug.corerouter.enums.apikey.ApiKeyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private Integer apiKeyId;
    private String key;
    private String description;
    private Integer dailyLimit;
    private Integer monthlyLimit;
    private ApiKeyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
