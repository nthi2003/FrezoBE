package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, String> {

    List<PurchaseRequest> findByIsDeletedFalseOrderByCreatedDateDesc();

    Optional<PurchaseRequest> findByCodeAndIsDeletedFalse(String code);
}
