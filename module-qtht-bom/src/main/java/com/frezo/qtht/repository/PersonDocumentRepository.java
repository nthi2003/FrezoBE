package com.frezo.qtht.repository;

import com.frezo.qtht.entity.PersonDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonDocumentRepository extends JpaRepository<PersonDocument, String> {
    List<PersonDocument> findByPersonIdOrderByCreatedDateDesc(String personId);
    List<PersonDocument> findByPersonIdAndTypeOrderByCreatedDateDesc(String personId, String type);
    void deleteByPersonIdAndId(String personId, String id);
}
