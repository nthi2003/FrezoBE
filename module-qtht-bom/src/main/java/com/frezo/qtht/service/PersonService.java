package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.PersonAddRequest;
import com.frezo.qtht.dto.request.PersonFilterRequest;
import com.frezo.qtht.dto.request.PersonUpdateRequest;
import com.frezo.qtht.dto.response.PersonResponse;
import com.frezo.qtht.entity.Person;
import com.frezo.util.web.Response;

import java.util.Map;
import java.util.Optional;

public interface PersonService {
    Map<String, Object> all(PersonFilterRequest filter) ;

    void delete(String id);

    Response<PersonResponse> createPerson (PersonAddRequest personAddRequest);

    Response<PersonResponse> updatePerson(String id, PersonUpdateRequest request);
    void activate (String id);

    void deactivate (String id);

    boolean isAdmin(String id);

    String uploadAvatarTemp(String userName, org.springframework.web.multipart.MultipartFile file);

    PersonResponse getById(String id);
}
