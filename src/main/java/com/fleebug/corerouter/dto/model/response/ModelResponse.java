package com.fleebug.corerouter.dto.model.response;

import com.fleebug.corerouter.enums.model.ModelStatus;
import com.fleebug.corerouter.enums.model.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelResponse {

    private Integer modelId;

    private String fullname;

    private String username;

    private String provider;

    private String parameterCount;

    private BigDecimal pricePer1kTokens;

    private ModelStatus status;

    private String endpointUrl;

    private ModelType type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
