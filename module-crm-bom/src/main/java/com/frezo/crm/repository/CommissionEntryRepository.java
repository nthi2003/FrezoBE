package com.frezo.crm.repository;

import com.frezo.crm.entity.CommissionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionEntryRepository extends JpaRepository<CommissionEntry, String> {

    Optional<CommissionEntry> findByInvoiceIdAndIsDeletedFalse(String invoiceId);

    List<CommissionEntry> findByIsDeletedFalseOrderByAccruedAtDesc();

    List<CommissionEntry> findBySalespersonUsernameAndIsDeletedFalseOrderByAccruedAtDesc(String username);

    @Query(value = """
            SELECT salesperson_username AS uname,
                   COUNT(*) AS invoice_count,
                   COALESCE(SUM(commission_amount), 0) AS total_commission,
                   COALESCE(SUM(base_amount), 0) AS total_base,
                   COALESCE(SUM(item_quantity), 0) AS total_qty
            FROM crm_commission_entry
            WHERE is_deleted = false AND status <> 'VOID'
            GROUP BY salesperson_username
            ORDER BY total_commission DESC
            """, nativeQuery = true)
    List<Object[]> dashboardBySalesperson();
}
