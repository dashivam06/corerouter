package com.fleebug.corerouter.dto.documentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDocumentationResponse {

    private Integer docId;
    private String title;
    private String content;
    private Integer modelId;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
