package com.frezo.qlns.repository;

import com.frezo.qlns.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, String> {

    List<Interview> findByApplicationIdAndIsDeletedFalseOrderByScheduledAtAsc(String applicationId);
}
