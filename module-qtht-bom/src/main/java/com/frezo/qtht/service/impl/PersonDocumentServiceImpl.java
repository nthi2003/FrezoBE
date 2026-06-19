package com.frezo.qtht.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.service.MinioService;
import com.frezo.qtht.entity.PersonDocument;
import com.frezo.qtht.repository.PersonDocumentRepository;
import com.frezo.qtht.service.PersonDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonDocumentServiceImpl implements PersonDocumentService {

    private final PersonDocumentRepository personDocumentRepository;
    private final MinioService minioService;

    @Override
    @Transactional(readOnly = true)
    public List<PersonDocument> getDocuments(String personId, String type) {
        if (type != null && !type.isBlank()) {
            return personDocumentRepository.findByPersonIdAndTypeOrderByCreatedDateDesc(personId, type);
        }
        return personDocumentRepository.findByPersonIdOrderByCreatedDateDesc(personId);
    }

    @Override
    @Transactional
    public PersonDocument uploadDocument(String personId, String type, String title, String description, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String objectName;
        switch (type.toUpperCase()) {
            case "CV":
                objectName = "frezo-user/cv/" + personId + "/" + UUID.randomUUID() + extension;
                break;
            case "CERTIFICATE":
            case "ACHIEVEMENT":
                objectName = "frezo-user/crf/" + personId + "/" + UUID.randomUUID() + extension;
                break;
            default:
                objectName = "frezo-user/other/" + personId + "/" + UUID.randomUUID() + extension;
        }

        String fileUrl = minioService.uploadFile(objectName, file);

        PersonDocument doc = PersonDocument.builder()
                .personId(personId)
                .type(type.toUpperCase())
                .title(title)
                .description(description)
                .fileName(originalFilename)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();

        PersonDocument saved = personDocumentRepository.save(doc);
        log.info("Document uploaded: type={}, personId={}, objectName={}", type, personId, objectName);
        return saved;
    }

    @Override
    @Transactional
    public void deleteDocument(String personId, String documentId) {
        PersonDocument doc = personDocumentRepository.findById(documentId)
                .orElseThrow(() -> new QTHTException("Document not found"));
        if (!doc.getPersonId().equals(personId)) {
            throw new QTHTException("Document does not belong to this person");
        }

        if (doc.getFileUrl() != null) {
            try {
                String objectName = minioService.extractObjectName(doc.getFileUrl());
                minioService.deleteFile(objectName);
            } catch (Exception e) {
                log.error("Failed to delete file from MinIO for document {}: {}", documentId, e.getMessage());
            }
        }

        personDocumentRepository.delete(doc);
        log.info("Document deleted: id={}, personId={}", documentId, personId);
    }
}
