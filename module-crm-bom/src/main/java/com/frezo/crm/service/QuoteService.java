package com.frezo.crm.service;

import com.frezo.crm.dto.QuoteRequest;
import com.frezo.crm.entity.Quote;
import com.frezo.crm.entity.QuoteItem;

import java.util.List;

public interface QuoteService {
    Quote create(QuoteRequest req);
    Quote update(String id, QuoteRequest req);
    void delete(String id);
    Quote get(String id);
    List<Quote> list();
    List<QuoteItem> items(String quoteId);
    Quote send(String id);
    Quote accept(String id);
    Quote reject(String id);
}
