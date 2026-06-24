package com.frezo.qlns.repository;

import com.frezo.qlns.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, String> {

    Optional<LeaveBalance> findByPersonIdAndYear(String personId, Integer year);
}
