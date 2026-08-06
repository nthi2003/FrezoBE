package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.PersonWorkHistoryRequest;
import com.frezo.qlns.dto.response.PersonWorkHistoryResponse;

import java.util.List;

public interface PersonWorkHistoryService {
    List<PersonWorkHistoryResponse> listByPerson(String personId);

    PersonWorkHistoryResponse create(PersonWorkHistoryRequest request);

    void delete(String id);
}
