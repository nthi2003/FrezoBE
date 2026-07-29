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

    /** "Đơn của tôi" — mọi trạng thái, sort mới nhất trước. */
    List<LeaveRequest> findByContractIdOrderByCreatedDateDesc(String contractId);

    /** Alternate lookup theo personId (khi FE không có contractId trong tay). */
    List<LeaveRequest> findByPersonIdOrderByCreatedDateDesc(String personId);

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

    /** Tổng phép đã duyệt theo khoảng ngày (portable — không dùng MONTH/YEAR MySQL). */
    @Query("SELECT COALESCE(SUM(lr.durationDays), 0) FROM LeaveRequest lr " +
           "WHERE lr.contractId = :contractId " +
           "AND lr.status = 'APPROVED' " +
           "AND lr.startDate >= :from AND lr.startDate <= :to")
    double sumApprovedLeavesByContractAndPeriod(
            @Param("contractId") String contractId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
