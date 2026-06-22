package com.frezo.email.repository;

import com.frezo.email.entity.SendEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SendEmailRepository extends JpaRepository<SendEmail, String> {
}
