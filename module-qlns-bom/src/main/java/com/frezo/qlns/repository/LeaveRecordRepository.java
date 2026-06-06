package com.frezo.qlns.repository;

import com.frezo.qlns.entity.LeaveRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRecordRepository extends JpaRepository<LeaveRecord, String> {
    
    @Query("SELECT l FROM LeaveRecord l WHERE l.personId = :personId " +
           "AND l.status = 'APPROVED' " +
           "AND ((l.startDate BETWEEN :start AND :end) OR (l.endDate BETWEEN :start AND :end))")
    List<LeaveRecord> findApprovedByPersonAndMonth(String personId, LocalDate start, LocalDate end);
}
