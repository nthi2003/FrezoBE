package com.frezo.qlns.repository;

import com.frezo.qlns.entity.OnboardingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingAssignmentRepository extends JpaRepository<OnboardingAssignment, String> {
    List<OnboardingAssignment> findByIsDeletedFalseOrderByCreatedDateDesc();
    List<OnboardingAssignment> findByPersonIdAndIsDeletedFalse(String personId);
}
