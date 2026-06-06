package com.frezo.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.entity.AiOcrDocumentRecord;
import com.frezo.common.model.ai.AiOcrAnalyzeResponse;
import com.frezo.common.model.ai.AiOcrDocumentDataDto;
import com.frezo.common.model.ai.AiOcrReviewSaveRequest;
import com.frezo.common.repository.AiOcrDocumentRecordRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AiDocumentService {

    private static final String REVIEW_STATUS_DRAFT = "DRAFT";
    private static final String REVIEW_STATUS_REVIEWED = "REVIEWED";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiOcrDocumentRecordRepository aiOcrDocumentRecordRepository;

    @Value("${frezo.ai.base-url:http://localhost:8001/api/v1/ai}")
    private String aiBaseUrl;

    public AiDocumentService(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        AiOcrDocumentRecordRepository aiOcrDocumentRecordRepository
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.aiOcrDocumentRecordRepository = aiOcrDocumentRecordRepository;
    }

    public AiOcrAnalyzeResponse analyzeDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
            aiBaseUrl + "/analyze-document",
            buildAnalyzeRequest(file),
            String.class
        );

        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("Empty response from AI OCR service");
        }

        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode dataNode = rootNode.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            throw new IOException("Invalid AI OCR response: missing data");
        }

        AiOcrDocumentDataDto analysis = normalizeAnalysis(
            objectMapper.treeToValue(dataNode, AiOcrDocumentDataDto.class)
        );

        String sourceFileName = extractSourceFileName(rootNode, file.getOriginalFilename());

        AiOcrDocumentRecord record = AiOcrDocumentRecord.builder()
            .sourceFileName(sourceFileName)
            .reviewStatus(REVIEW_STATUS_DRAFT)
            .build();

        updateSummaryFields(record, analysis);
        record.setWarningsJson(writeJson(analysis.getWarnings()));
        record.setExtractedDataJson(writeJson(analysis));

        AiOcrDocumentRecord savedRecord = aiOcrDocumentRecordRepository.save(record);
        return toAnalyzeResponse(savedRecord, analysis);
    }

    public AiOcrAnalyzeResponse saveReviewedDocument(AiOcrReviewSaveRequest request) {
        if (request == null || request.getRecordId() == null || request.getRecordId().isBlank()) {
            throw new IllegalArgumentException("recordId is required");
        }
        if (request.getAnalysis() == null) {
            throw new IllegalArgumentException("analysis is required");
        }

        AiOcrDocumentRecord record = aiOcrDocumentRecordRepository.findById(request.getRecordId())
            .orElseThrow(() -> new IllegalArgumentException("OCR record not found: " + request.getRecordId()));

        AiOcrDocumentDataDto reviewedAnalysis = normalizeAnalysis(request.getAnalysis());

        record.setSourceFileName(resolveSourceFileName(request.getSourceFileName(), record.getSourceFileName()));
        record.setReviewStatus(REVIEW_STATUS_REVIEWED);
        record.setOperatorNote(request.getOperatorNote());
        record.setReviewedDataJson(writeJson(reviewedAnalysis));
        updateSummaryFields(record, reviewedAnalysis);
        record.setWarningsJson(writeJson(reviewedAnalysis.getWarnings()));

        AiOcrDocumentRecord savedRecord = aiOcrDocumentRecordRepository.save(record);
        return toAnalyzeResponse(savedRecord, reviewedAnalysis);
    }

    private HttpEntity<MultiValueMap<String, Object>> buildAnalyzeRequest(MultipartFile file) throws IOException {
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(resolveContentType(file));
        fileHeaders.setContentDisposition(
            ContentDisposition.formData()
                .name("file")
                .filename(file.getOriginalFilename())
                .build()
        );

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, fileHeaders));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        return new HttpEntity<>(body, headers);
    }

    private MediaType resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    private String extractSourceFileName(JsonNode rootNode, String fallbackName) {
        String responseFileName = rootNode.path("filename").asText(null);
        if (responseFileName != null && !responseFileName.isBlank()) {
            return responseFileName;
        }
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }
        return "uploaded-document";
    }

    private AiOcrAnalyzeResponse toAnalyzeResponse(AiOcrDocumentRecord record, AiOcrDocumentDataDto analysis) {
        return AiOcrAnalyzeResponse.builder()
            .recordId(record.getId())
            .sourceFileName(record.getSourceFileName())
            .reviewStatus(record.getReviewStatus())
            .analysis(analysis)
            .build();
    }

    private void updateSummaryFields(AiOcrDocumentRecord record, AiOcrDocumentDataDto analysis) {
        record.setDocumentType(analysis.getDocumentType());
        record.setVendor(analysis.getVendor());
        record.setDocumentDate(analysis.getDocumentDate());
        record.setTotalAmount(analysis.getTotalAmount());
        record.setRawText(analysis.getRawText());
    }

    private AiOcrDocumentDataDto normalizeAnalysis(AiOcrDocumentDataDto analysis) {
        if (analysis == null) {
            return AiOcrDocumentDataDto.builder()
                .items(Collections.emptyList())
                .warnings(Collections.emptyList())
                .build();
        }

        if (analysis.getItems() == null) {
            analysis.setItems(Collections.emptyList());
        }
        if (analysis.getWarnings() == null) {
            analysis.setWarnings(Collections.emptyList());
        }

        return analysis;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize OCR data", exception);
        }
    }

    private String resolveSourceFileName(String requestedSourceFileName, String currentSourceFileName) {
        if (requestedSourceFileName != null && !requestedSourceFileName.isBlank()) {
            return requestedSourceFileName;
        }
        return currentSourceFileName;
    }
}