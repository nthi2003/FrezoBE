package com.frezo.qlns.repository;

import com.frezo.qlns.entity.OnboardingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTemplateRepository extends JpaRepository<OnboardingTemplate, String> {
    List<OnboardingTemplate> findByIsDeletedFalseOrderByCreatedDateDesc();
}
