package com.frezo.qlns.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.service.MinioService;
import com.frezo.qlns.dto.response.ContractTemplateResponse;
import com.frezo.qlns.entity.ContractTemplate;
import com.frezo.qlns.repository.ContractTemplateRepository;
import com.frezo.qlns.service.ContractTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractTemplateServiceImpl implements ContractTemplateService {

    private final ContractTemplateRepository contractTemplateRepository;
    private final MinioService minioService;

    @Override
    public List<ContractTemplateResponse> getAll() {
        return contractTemplateRepository.findByIsDeletedFalseOrderByCreatedDateDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ContractTemplateResponse create(String name, String type, String fileUrl, String fileObjectName) {
        ContractTemplate entity = new ContractTemplate();
        entity.setName(name);
        entity.setType(type);
        entity.setFileUrl(fileUrl);
        entity.setFileObjectName(fileObjectName);
        entity.setIsDeleted(false);
        ContractTemplate saved = contractTemplateRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        ContractTemplate entity = contractTemplateRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy mẫu hợp đồng"));
        try {
            if (entity.getFileObjectName() != null) {
                minioService.deleteFile(entity.getFileObjectName());
            }
        } catch (Exception e) {
            log.warn("Không thể xóa file trên MinIO: {}", e.getMessage());
        }
        entity.setIsDeleted(true);
        contractTemplateRepository.save(entity);
    }

    private ContractTemplateResponse toResponse(ContractTemplate entity) {
        return ContractTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .fileUrl(entity.getFileUrl())
                .createdDate(entity.getCreatedDate())
                .build();
    }
}
