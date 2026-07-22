package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.OfferRequest;
import com.frezo.qlns.dto.response.OfferResponse;

public interface OfferService {

    OfferResponse create(OfferRequest req);

    OfferResponse send(String id);

    OfferResponse accept(String id);

    OfferResponse reject(String id);
}
