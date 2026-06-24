package com.frezo.qtht.service;

import com.frezo.qtht.entity.PersonDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PersonDocumentService {
    List<PersonDocument> getDocuments(String personId, String type);
    PersonDocument uploadDocument(String personId, String type, String title, String description, MultipartFile file);
    void deleteDocument(String personId, String documentId);
}
