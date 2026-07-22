package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.dto.request.InterviewCompleteRequest;
import com.frezo.qlns.dto.request.InterviewRequest;
import com.frezo.qlns.dto.response.InterviewResponse;
import com.frezo.qlns.entity.Interview;
import com.frezo.qlns.entity.JobApplication;
import com.frezo.qlns.recruitment.RecruitmentConstants;
import com.frezo.qlns.recruitment.RecruitmentErrorCode;
import com.frezo.qlns.repository.InterviewRepository;
import com.frezo.qlns.repository.JobApplicationRepository;
import com.frezo.qlns.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;

    @Override
    @Transactional
    public InterviewResponse create(InterviewRequest req) {
        if (req.getType() == null || !RecruitmentConstants.INTERVIEW_TYPES.contains(req.getType())) {
            throw new AppException(RecruitmentErrorCode.INTERVIEW_TYPE_INVALID, req.getType());
        }
        JobApplication app = applicationRepository.findById(req.getApplicationId())
                .filter(a -> Boolean.FALSE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.APPLICATION_NOT_FOUND, req.getApplicationId()));

        Interview e = Interview.builder()
                .applicationId(app.getId())
                .type(req.getType())
                .scheduledAt(req.getScheduledAt())
                .interviewerUsername(req.getInterviewerUsername())
                .location(req.getLocation())
                .meetingLink(req.getMeetingLink())
                .status(RecruitmentConstants.INTV_SCHEDULED)
                .build();
        return toResponse(interviewRepository.save(e));
    }

    @Override
    public List<InterviewResponse> list(String applicationId) {
        return interviewRepository
                .findByApplicationIdAndIsDeletedFalseOrderByScheduledAtAsc(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public InterviewResponse complete(String id, InterviewCompleteRequest req) {
        Interview e = interviewRepository.findById(id)
                .filter(i -> Boolean.FALSE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.INTERVIEW_NOT_FOUND, id));
        if (!RecruitmentConstants.INTV_SCHEDULED.equals(e.getStatus())) {
            throw new AppException(RecruitmentErrorCode.INTERVIEW_STATUS_INVALID, e.getStatus());
        }
        e.setStatus(RecruitmentConstants.INTV_DONE);
        if (req != null) {
            e.setScore(req.getScore());
            e.setFeedback(req.getFeedback());
        }
        return toResponse(interviewRepository.save(e));
    }

    private InterviewResponse toResponse(Interview e) {
        return InterviewResponse.builder()
                .id(e.getId())
                .applicationId(e.getApplicationId())
                .type(e.getType())
                .scheduledAt(e.getScheduledAt())
                .interviewerUsername(e.getInterviewerUsername())
                .location(e.getLocation())
                .meetingLink(e.getMeetingLink())
                .status(e.getStatus())
                .score(e.getScore())
                .feedback(e.getFeedback())
                .build();
    }
}
