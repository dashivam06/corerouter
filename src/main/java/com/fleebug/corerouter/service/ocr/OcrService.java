package com.fleebug.corerouter.service.ocr;

import com.fleebug.corerouter.util.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final HttpClientUtil httpClientUtil;

    @Value("${ocr.space.api-key:K89598388888957}")
    private String ocrSpaceApiKey;

    public Map<String, Object> parseImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'url' is required");
        }

        String requestUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api.ocr.space")
                .addPathSegment("parse")
                .addPathSegment("imageurl")
                .addQueryParameter("apikey", ocrSpaceApiKey)
                .addQueryParameter("url", imageUrl)
                .build()
                .toString();

        try {
            return httpClientUtil.getJsonMap(requestUrl, Map.of(), 5000, 15000);
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException("OCR provider request failed: " + ex.getMessage(), ex);
        }
    }
}