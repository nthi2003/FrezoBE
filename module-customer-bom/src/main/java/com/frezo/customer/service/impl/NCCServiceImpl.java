package com.frezo.customer.service.impl;

import com.frezo.customer.dto.NCCCertificateDTO;
import com.frezo.customer.dto.request.NCCFilterRequest;
import com.frezo.customer.dto.request.NCCRequest;
import com.frezo.customer.dto.response.NCCResponse;
import com.frezo.customer.entity.NCC;
import com.frezo.customer.entity.NCCCertificate;
import com.frezo.customer.mapper.NCCMapper;
import com.frezo.customer.repository.NCCCertificateRepository;
import com.frezo.customer.repository.NCCRepository;
import com.frezo.customer.service.NCCService;
import com.frezo.qtbv.entity.Category;
import com.frezo.qtbv.repository.CategoryRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.frezo.common.exception.QTHTException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NCCServiceImpl implements NCCService {

    private final NCCRepository nccRepository;
    private final NCCCertificateRepository certificateRepository;
    private final CategoryRepository categoryRepository;
    private final com.frezo.common.service.MinioService minioService;
    private final NCCMapper nccMapper;

    @Override
    public Map<String, Object> all(NCCFilterRequest filter) {
        Specification<NCC> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                String keyword = "%" + filter.getKeyword().toLowerCase() + "%";
                // Phone search uses SHA-256 hash for security
                String phoneHash = com.frezo.common.utils.CryptoUtils.hashSHA256(filter.getKeyword());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("code")), keyword),
                        cb.equal(root.get("phoneHash"), phoneHash),   // search by hash
                        cb.like(cb.lower(root.get("representative")), keyword)
                ));
            }
            if (filter.getClassificationCode() != null && !filter.getClassificationCode().isEmpty()) {
                predicates.add(cb.equal(root.get("classificationCode"), filter.getClassificationCode()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<NCC> nccs = nccRepository.findAll(spec);
        List<NCCResponse> responses = nccs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", responses);
        result.put("total", responses.size());
        return result;
    }

    @Override
    public NCCResponse getById(String id) {
        NCC ncc = nccRepository.findById(id).orElseThrow(() -> new QTHTException("exception.ncc.not_found"));
        return mapToResponse(ncc);
    }

    @Override
    @Transactional
    public NCCResponse createNCC(NCCRequest request) {
        String code = request.getCode();
        if (code == null || code.trim().isEmpty()) {
            boolean exists = true;
            while (exists) {
                code = com.frezo.common.utils.SecureCodeGenerator.generateCode("NCC");
                exists = nccRepository.findByCode(code).isPresent();
            }
            request.setCode(code);
        } else {
            if (nccRepository.findByCode(code).isPresent()) {
                throw new QTHTException("exception.ncc.code.exists", code);
            }
        }
        
        NCC ncc = nccMapper.toEntity(request);
        ncc = nccRepository.save(ncc);

        saveCertificates(ncc.getId(), request.getCertificates());

        return mapToResponse(ncc);
    }

    @Override
    @Transactional
    public NCCResponse updateNCC(String id, NCCRequest request) {
        NCC ncc = nccRepository.findById(id).orElseThrow(() -> new QTHTException("exception.ncc.not_found"));
        
        Optional<NCC> existingByCode = nccRepository.findByCode(request.getCode());
        if (existingByCode.isPresent() && !existingByCode.get().getId().equals(id)) {
            throw new QTHTException("exception.ncc.code.exists", request.getCode());
        }

        nccMapper.updateEntity(ncc, request);
        ncc = nccRepository.save(ncc);

        certificateRepository.deleteByNccId(id);
        saveCertificates(id, request.getCertificates());

        return mapToResponse(ncc);
    }

    @Override
    @Transactional
    public void delete(String id) {
        nccRepository.deleteById(id);
        certificateRepository.deleteByNccId(id);
    }

    private void saveCertificates(String nccId, List<NCCCertificateDTO> dtos) {
        if (dtos != null) {
            List<NCCCertificate> certificates = dtos.stream()
                    .map(dto -> {
                        NCCCertificate cert = nccMapper.toCertificateEntity(dto);
                        cert.setNccId(nccId);
                        return cert;
                    })
                    .collect(Collectors.toList());
            certificateRepository.saveAll(certificates);
        }
    }

    private NCCResponse mapToResponse(NCC ncc) {
        NCCResponse response = nccMapper.toResponse(ncc);
        
        // Map classification name from Category
        if (ncc.getClassificationCode() != null) {
            categoryRepository.findByCode(ncc.getClassificationCode()).ifPresent(cat -> {
                response.setClassificationName(cat.getName());
            });
        }

        // Map certificates
        List<NCCCertificate> certificates = certificateRepository.findByNccId(ncc.getId());
        response.setCertificates(certificates.stream()
                .map(nccMapper::toCertificateDTO)
                .collect(Collectors.toList()));

        return response;
    }

    @Override
    public String uploadCertificate(String nccCode, MultipartFile file) {
        String fileName = nccCode + "/cert_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        return minioService.uploadFile("ncc/" + fileName, file);
    }
}
