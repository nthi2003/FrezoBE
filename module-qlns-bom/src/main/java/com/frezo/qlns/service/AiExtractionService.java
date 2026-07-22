package com.frezo.qlns.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiExtractionService {

    @Value("${app.ai.service-url:http://localhost:8001}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> extract(MultipartFile file) {
        try {
            var builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            var requestEntity = new HttpEntity<>(builder.build(), headers);

            String url = aiServiceUrl + "/ai/ocr/extract?use_llm=true";
            String response = restTemplate.postForObject(url, requestEntity, String.class);

            var root = objectMapper.readTree(response);
            Map<String, Object> result = new HashMap<>();

            if (root.has("text")) {
                result.put("content", root.get("text").asText());
            }
            if (root.has("html")) {
                result.put("html", root.get("html").asText());
            }
            if (root.has("fields") && root.get("fields").isObject()) {
                var fields = root.get("fields");
                fields.fieldNames().forEachRemaining(field -> {
                    var value = fields.get(field);
                    if (value != null && !value.isNull()) {
                        result.put(field, value.asText());
                    }
                });
            }

            return result;
        } catch (Exception e) {
            log.error("Lỗi khi gọi AI service: ", e);
            return Map.of("error", "AI service unavailable: " + e.getMessage());
        }
    }

    public String editContent(String text, String instruction) {
        try {
            var requestBody = Map.of(
                    "text", text,
                    "instruction", instruction,
                    "tone", "trang trọng, chuyên nghiệp"
            );
            String url = aiServiceUrl + "/ai/edit-content";
            String response = restTemplate.postForObject(url, requestBody, String.class);

            var root = objectMapper.readTree(response);
            if (root.has("edited")) {
                return root.get("edited").asText();
            }
            return text;
        } catch (Exception e) {
            log.error("Lỗi khi gọi AI edit content: ", e);
            return text;
        }
    }
}
