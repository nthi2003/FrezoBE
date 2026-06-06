package com.frezo.email.repository;

import com.frezo.email.entity.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailConfigRepository
        extends JpaRepository<EmailConfig, String>, JpaSpecificationExecutor<EmailConfig> {

    Boolean existsByCode(String code);

    List<EmailConfig> findByActivatedTrue();

    Optional<EmailConfig> findById(String id);

}
