package com.frezo.customer.repository;

import com.frezo.customer.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String>,
        JpaSpecificationExecutor<Voucher> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);
}
