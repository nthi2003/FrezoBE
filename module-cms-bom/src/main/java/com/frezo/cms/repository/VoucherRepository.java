package com.frezo.cms.repository;

import com.frezo.cms.entity.Vouchers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Vouchers, String>, JpaSpecificationExecutor<Vouchers> {
    Optional<Vouchers> findByCode(String code);
}
