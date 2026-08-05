package com.frezo.dmdc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.dmdc.dto.response.UxPopupResponse;
import com.frezo.dmdc.service.UxPopupService;
import com.frezo.qtbv.entity.Category;
import com.frezo.qtbv.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UxPopupServiceImpl implements UxPopupService {

    public static final String GROUP_UX_POPUP = "UX_POPUP";

    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<UxPopupResponse> resolve(String eventCode) {
        if (eventCode == null || eventCode.isBlank()) {
            return Optional.empty();
        }
        String code = eventCode.trim();
        return categoryRepository
                .findByGroupCodeAndCodeAndIsDeletedFalse(GROUP_UX_POPUP, code)
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .map(this::toResponse);
    }

    private UxPopupResponse toResponse(Category category) {
        String body = category.getDescription();
        String imageUrl = null;

        if (body != null && body.trim().startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(body);
                if (node.hasNonNull("body")) {
                    body = node.get("body").asText();
                }
                if (node.hasNonNull("imageUrl")) {
                    imageUrl = node.get("imageUrl").asText();
                }
            } catch (Exception e) {
                log.debug("UX popup description is not JSON for {}: {}", category.getCode(), e.getMessage());
            }
        }

        return UxPopupResponse.builder()
                .eventCode(category.getCode())
                .title(category.getName())
                .body(body)
                .imageUrl(imageUrl)
                .enabled(true)
                .build();
    }
}
