package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.HireRequest;
import com.frezo.qlns.dto.request.OfferRequest;
import com.frezo.qlns.dto.response.OfferResponse;

public interface OfferService {

    OfferResponse create(OfferRequest req);

    OfferResponse send(String id);

    OfferResponse accept(String id);

    /** Accept + (policy A) truyền HireRequest để tạo User+Role khi auto-hire. */
    OfferResponse accept(String id, HireRequest hireRequest);

    OfferResponse reject(String id);
}
