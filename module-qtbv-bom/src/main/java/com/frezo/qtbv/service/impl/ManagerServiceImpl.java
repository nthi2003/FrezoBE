package com.frezo.qtbv.service.impl;

import com.frezo.qtbv.dto.response.ManagerResponse;
import com.frezo.qtbv.service.ManagerService;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerServiceImpl implements ManagerService {

    private final PersonRepository personRepository;

    /** Lấy danh sách người quản lý đang kích hoạt từ danh sách nhân sự và chuyển thành dạng ManagerResponse. */
    @Override
    @Transactional(readOnly = true)
    public List<ManagerResponse> getManagersList() {
        return personRepository.findAll().stream()
                .filter(p -> p.getActivated() != null && p.getActivated())
                .map(this::toManagerResponse)
                .toList();
    }

    /** Chuyển đổi một Person thành ManagerResponse, trả về null nếu Person rỗng. */
    private ManagerResponse toManagerResponse(Person person) {
        if (person == null) {
            return null;
        }

        return ManagerResponse.builder()
                .id(person.getId())
                .code(person.getCode())
                .name(person.getName())
                .email(person.getEmail())
                .phone(person.getPhone())
                .jobTitle(person.getJobTitle())
                .build();
    }
}
