package com.frezo.email.repository;

import com.frezo.email.entity.EmailGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailGroupRepository extends JpaRepository<EmailGroup, String> {
}
