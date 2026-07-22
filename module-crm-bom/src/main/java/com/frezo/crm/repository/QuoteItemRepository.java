package com.frezo.crm.repository;

import com.frezo.crm.entity.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteItemRepository extends JpaRepository<QuoteItem, String> {
    List<QuoteItem> findByQuoteIdOrderByLineNoAsc(String quoteId);
    void deleteByQuoteId(String quoteId);
}
