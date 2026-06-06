package com.frezo.customer.repository;

import com.frezo.customer.entity.NCC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NCCRepository extends JpaRepository<NCC, String>, JpaSpecificationExecutor<NCC> {
    Optional<NCC> findByCode(String code);
}
