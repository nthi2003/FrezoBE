package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.PersonAddRequest;
import com.frezo.qtht.dto.request.PersonFilterRequest;
import com.frezo.qtht.dto.request.PersonUpdateRequest;
import com.frezo.qtht.dto.response.PersonResponse;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.common.response.PageResponse;

import java.util.List;

public interface PersonService {
    PageResponse<PersonResponse> all(PersonFilterRequest filter);

    void delete(String id);

    ApiResponse<PersonResponse> createPerson (PersonAddRequest personAddRequest);

    ApiResponse<PersonResponse> updatePerson(String id, PersonUpdateRequest request);
    void activate (String id);

    void deactivate (String id);

    boolean isAdmin(String id);

    String uploadAvatarTemp(String userName, org.springframework.web.multipart.MultipartFile file);

    PersonResponse getById(String id);

    List<ComboboxResponse> getCombobox(PersonFilterRequest filter);

    /**
     * Combobox nhân sự.
     * @param valueField {@code "id"} (mặc định) = personId; {@code "username"} = user_name (bỏ person chưa có tài khoản)
     */
    List<ComboboxResponse> getCombobox(PersonFilterRequest filter, String valueField);
}
