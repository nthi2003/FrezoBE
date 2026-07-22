package com.frezo.qlns.repository;

import com.frezo.qlns.entity.LeaveRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestHistoryRepository extends JpaRepository<LeaveRequestHistory, String> {

    /** Timeline theo thứ tự cũ → mới cho drawer chi tiết. */
    List<LeaveRequestHistory> findByRequestIdOrderByCreatedDateAsc(String requestId);
}
