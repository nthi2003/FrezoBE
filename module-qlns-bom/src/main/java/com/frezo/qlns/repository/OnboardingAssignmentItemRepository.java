package com.frezo.qlns.repository;

import com.frezo.qlns.entity.OnboardingAssignmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingAssignmentItemRepository extends JpaRepository<OnboardingAssignmentItem, String> {
    List<OnboardingAssignmentItem> findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(String assignmentId);
}
