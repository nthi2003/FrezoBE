package com.frezo.email.repository;

import com.frezo.email.entity.EmailTemplate;
import com.frezo.util.web.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository
        extends JpaRepository<EmailTemplate, String>, JpaSpecificationExecutor<EmailTemplate> {

    Boolean existsByCode(String code);

    Boolean existsByName(String name);

    Optional<EmailTemplate> findByIdAndIsDeletedFalse(String id);

    Optional<EmailTemplate> findByCode(String code);

}
