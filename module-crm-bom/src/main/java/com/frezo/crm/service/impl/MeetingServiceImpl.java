package com.frezo.crm.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.crm.dto.MeetingRequest;
import com.frezo.crm.dto.MeetingResponse;
import com.frezo.crm.entity.Meeting;
import com.frezo.crm.repository.MeetingRepository;
import com.frezo.crm.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final MeetingRepository meetingRepository;

    @Override
    public List<MeetingResponse> list(String dealId) {
        List<Meeting> list = dealId != null
                ? meetingRepository.findByDealIdAndIsDeletedFalse(dealId)
                : meetingRepository.findByIsDeletedFalseOrderByStartAtDesc();
        return list.stream().map(this::toDto).toList();
    }

    @Override
    public MeetingResponse get(String id) {
        return toDto(find(id));
    }

    @Override
    @Transactional
    public MeetingResponse create(MeetingRequest req) {
        Meeting m = Meeting.builder()
                .title(req.getTitle())
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .dealId(req.getDealId())
                .customerId(req.getCustomerId())
                .location(req.getLocation())
                .meetingLink(req.getMeetingLink())
                .attendees(req.getAttendees())
                .status(req.getStatus() != null ? req.getStatus() : "SCHEDULED")
                .notes(req.getNotes())
                .build();
        m.setId(UUID.randomUUID().toString());
        return toDto(meetingRepository.save(m));
    }

    @Override
    @Transactional
    public MeetingResponse update(String id, MeetingRequest req) {
        Meeting m = find(id);
        if (req.getTitle() != null) m.setTitle(req.getTitle());
        if (req.getStartAt() != null) m.setStartAt(req.getStartAt());
        if (req.getEndAt() != null) m.setEndAt(req.getEndAt());
        if (req.getDealId() != null) m.setDealId(req.getDealId());
        if (req.getCustomerId() != null) m.setCustomerId(req.getCustomerId());
        if (req.getLocation() != null) m.setLocation(req.getLocation());
        if (req.getMeetingLink() != null) m.setMeetingLink(req.getMeetingLink());
        if (req.getAttendees() != null) m.setAttendees(req.getAttendees());
        if (req.getStatus() != null) m.setStatus(req.getStatus());
        if (req.getNotes() != null) m.setNotes(req.getNotes());
        return toDto(meetingRepository.save(m));
    }

    @Override
    @Transactional
    public void delete(String id) {
        Meeting m = find(id);
        m.softDelete(SystemUtils.getCurrentUsername());
        meetingRepository.save(m);
    }

    private Meeting find(String id) {
        return meetingRepository.findById(id)
                .filter(m -> Boolean.FALSE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Meeting không tồn tại"));
    }

    private MeetingResponse toDto(Meeting m) {
        return MeetingResponse.builder()
                .id(m.getId()).title(m.getTitle())
                .startAt(m.getStartAt() != null ? m.getStartAt().format(ISO) : null)
                .endAt(m.getEndAt() != null ? m.getEndAt().format(ISO) : null)
                .dealId(m.getDealId()).customerId(m.getCustomerId())
                .location(m.getLocation()).meetingLink(m.getMeetingLink())
                .attendees(m.getAttendees()).status(m.getStatus()).notes(m.getNotes())
                .build();
    }
}
