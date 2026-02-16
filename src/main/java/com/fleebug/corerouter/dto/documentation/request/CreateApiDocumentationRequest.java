package com.fleebug.corerouter.dto.documentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiDocumentationRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;

    @NotBlank(message = "Content cannot be blank")
    @Size(min = 10, message = "Content must be at least 10 characters")
    private String content;
}
