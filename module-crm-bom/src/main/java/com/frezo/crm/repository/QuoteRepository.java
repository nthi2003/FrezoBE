package com.frezo.crm.repository;

import com.frezo.crm.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, String> {
    Optional<Quote> findByCode(String code);
    List<Quote> findByCustomerIdAndIsDeletedFalseOrderByIssuedDateDesc(String customerId);
    List<Quote> findByDealIdAndIsDeletedFalseOrderByIssuedDateDesc(String dealId);
    List<Quote> findByIsDeletedFalseOrderByIssuedDateDesc();
    long countByIsDeletedFalse();
}
