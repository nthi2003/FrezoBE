package com.frezo.qlns.repository;

import com.frezo.qlns.entity.OnboardingTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTemplateItemRepository extends JpaRepository<OnboardingTemplateItem, String> {
    List<OnboardingTemplateItem> findByTemplateIdAndIsDeletedFalseOrderBySortOrderAsc(String templateId);
}
