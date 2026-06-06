package com.frezo.qlns.repository;

import com.frezo.qlns.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String>, JpaSpecificationExecutor<LeaveRequest> {

    List<LeaveRequest> findByContractIdAndStatus(String contractId, String status);

    List<LeaveRequest> findByStatus(String status);

    @Query("SELECT COALESCE(SUM(lr.durationDays), 0) FROM LeaveRequest lr " +
           "WHERE lr.contractId = :contractId " +
           "AND lr.status = 'APPROVED' " +
           "AND lr.leaveType = :leaveType " +
           "AND lr.startDate >= :from AND lr.endDate <= :to")
    double sumApprovedLeavesByTypeAndPeriod(
            @Param("contractId") String contractId,
            @Param("leaveType") String leaveType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(lr.durationDays), 0) FROM LeaveRequest lr " +
           "WHERE lr.contractId = :contractId " +
           "AND lr.status = 'APPROVED' " +
           "AND FUNCTION('MONTH', lr.startDate) = :month " +
           "AND FUNCTION('YEAR', lr.startDate) = :year")
    double sumApprovedLeavesByContractAndMonth(
            @Param("contractId") String contractId,
            @Param("month") int month,
            @Param("year") int year);
}
