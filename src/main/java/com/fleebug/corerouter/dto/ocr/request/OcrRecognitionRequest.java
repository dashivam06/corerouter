package com.fleebug.corerouter.dto.ocr.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrRecognitionRequest {

    @NotBlank(message = "Image data is required (base64 or URL)")
    private String image;

    @NotBlank(message = "Model is required")
    private String model;

    private String language;

    private Boolean includeConfidence;

    private String imageType; // base64, url

    private Map<String, Object> additionalParameters;
}
