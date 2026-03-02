package com.fleebug.corerouter.dto.llm.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatCompletionRequest {

    @NotEmpty(message = "Messages cannot be empty")
    private List<Map<String, String>> messages;

    @NotBlank(message = "Model is required")
    private String model;

    private String systemPrompt;

    private Float temperature;

    private Integer maxTokens;

    private Float topP;

    private Integer topK;

    private Float frequencyPenalty;

    private Float presencePenalty;

    private List<String> stop;

    private Map<String, Object> additionalParameters;
}
