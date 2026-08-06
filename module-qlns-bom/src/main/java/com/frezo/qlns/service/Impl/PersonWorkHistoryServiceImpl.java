package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.qlns.dto.request.PersonWorkHistoryRequest;
import com.frezo.qlns.dto.response.PersonWorkHistoryResponse;
import com.frezo.qlns.entity.PersonWorkHistory;
import com.frezo.qlns.repository.PersonWorkHistoryRepository;
import com.frezo.qlns.service.PersonWorkHistoryService;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonWorkHistoryServiceImpl implements PersonWorkHistoryService {

    private final PersonWorkHistoryRepository personWorkHistoryRepository;
    private final PersonRepository personRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PersonWorkHistoryResponse> listByPerson(String personId) {
        assertPersonExists(personId);
        return personWorkHistoryRepository.findByPersonIdAndIsDeletedFalseOrderByFromDateDesc(personId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PersonWorkHistoryResponse create(PersonWorkHistoryRequest request) {
        assertPersonExists(request.getPersonId());
        PersonWorkHistory entity = PersonWorkHistory.builder()
                .personId(request.getPersonId())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .departmentName(request.getDepartmentName())
                .positionName(request.getPositionName())
                .jobPositionId(request.getJobPositionId())
                .note(request.getNote())
                .build();
        entity.setIsDeleted(false);
        return toResponse(personWorkHistoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        PersonWorkHistory entity = personWorkHistoryRepository.findById(id)
                .filter(h -> !Boolean.TRUE.equals(h.getIsDeleted()))
                .orElseThrow(() -> new AppException(QlnsErrorCode.ENTITY_NOT_FOUND));
        entity.setIsDeleted(true);
        personWorkHistoryRepository.save(entity);
    }

    private void assertPersonExists(String personId) {
        personRepository.findByIdAndIsDeletedFalse(personId)
                .orElseThrow(() -> new AppException(QlnsErrorCode.PERSON_NOT_FOUND));
    }

    private PersonWorkHistoryResponse toResponse(PersonWorkHistory e) {
        return PersonWorkHistoryResponse.builder()
                .id(e.getId())
                .personId(e.getPersonId())
                .fromDate(e.getFromDate())
                .toDate(e.getToDate())
                .departmentName(e.getDepartmentName())
                .positionName(e.getPositionName())
                .jobPositionId(e.getJobPositionId())
                .note(e.getNote())
                .build();
    }
}
