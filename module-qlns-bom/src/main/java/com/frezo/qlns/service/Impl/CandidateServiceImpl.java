package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.dto.request.CandidateRequest;
import com.frezo.qlns.dto.response.CandidateResponse;
import com.frezo.qlns.entity.Candidate;
import com.frezo.qlns.recruitment.RecruitmentErrorCode;
import com.frezo.qlns.repository.CandidateRepository;
import com.frezo.qlns.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;

    /**
     * Tạo mới một ứng viên tuyển dụng từ dữ liệu yêu cầu và lưu vào cơ sở dữ liệu.
     */
    @Override
    @Transactional
    public CandidateResponse create(CandidateRequest req) {
        Candidate e = Candidate.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .source(req.getSource())
                .currentPosition(req.getCurrentPosition())
                .expectedSalary(req.getExpectedSalary())
                .cvUrl(req.getCvUrl())
                .linkedInUrl(req.getLinkedInUrl())
                .notes(req.getNotes())
                .build();
        return toResponse(candidateRepository.save(e));
    }

    /**
     * Lấy chi tiết một ứng viên theo id, không tồn tại hoặc đã xoá thì báo lỗi CANDIDATE_NOT_FOUND.
     */
    @Override
    public CandidateResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    /**
     * Tìm kiếm ứng viên chưa xoá theo từ khoá (tên/email/số điện thoại), sắp xếp theo ngày tạo mới nhất.
     */
    @Override
    public List<CandidateResponse> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return candidateRepository.findAll().stream()
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .filter(c -> kw.isEmpty()
                        || (c.getFullName() != null && c.getFullName().toLowerCase().contains(kw))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(kw))
                        || (c.getPhone() != null && c.getPhone().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(Candidate::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tra cứu ứng viên chưa xoá theo id, không tìm thấy thì ném AppException CANDIDATE_NOT_FOUND.
     */
    private Candidate findOrThrow(String id) {
        return candidateRepository.findById(id)
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.CANDIDATE_NOT_FOUND, id));
    }

    /**
     * Chuyển entity Candidate sang DTO CandidateResponse để trả về cho client.
     */
    private CandidateResponse toResponse(Candidate e) {
        return CandidateResponse.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .source(e.getSource())
                .currentPosition(e.getCurrentPosition())
                .expectedSalary(e.getExpectedSalary())
                .cvUrl(e.getCvUrl())
                .linkedInUrl(e.getLinkedInUrl())
                .notes(e.getNotes())
                .build();
    }
}
