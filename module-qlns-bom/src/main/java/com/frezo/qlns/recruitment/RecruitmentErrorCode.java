package com.frezo.qlns.recruitment;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error code enum cho Recruitment ATS. Xem convention: {@code AI_BACKEND_ENGINEERING_GUIDE §4}.
 */
@Getter
@RequiredArgsConstructor
public enum RecruitmentErrorCode implements ErrorCode {

    REQUISITION_NOT_FOUND("recruitment.requisition.not.found", HttpStatus.NOT_FOUND,
            "Yêu cầu tuyển dụng không tồn tại"),
    REQUISITION_CLOSED("recruitment.requisition.closed", HttpStatus.CONFLICT,
            "Yêu cầu tuyển dụng đã đóng, không thể ứng tuyển"),

    CANDIDATE_NOT_FOUND("recruitment.candidate.not.found", HttpStatus.NOT_FOUND,
            "Ứng viên không tồn tại"),

    APPLICATION_NOT_FOUND("recruitment.application.not.found", HttpStatus.NOT_FOUND,
            "Đơn ứng tuyển không tồn tại"),
    APPLICATION_STAGE_INVALID("recruitment.application.stage.invalid", HttpStatus.BAD_REQUEST,
            "Trạng thái chuyển tiếp không hợp lệ"),
    APPLICATION_ALREADY_EXISTS("recruitment.application.exists", HttpStatus.CONFLICT,
            "Ứng viên đã ứng tuyển vị trí này"),
    APPLICATION_FINAL_STAGE("recruitment.application.final", HttpStatus.CONFLICT,
            "Đơn ứng tuyển đã ở trạng thái kết thúc"),

    INTERVIEW_NOT_FOUND("recruitment.interview.not.found", HttpStatus.NOT_FOUND,
            "Buổi phỏng vấn không tồn tại"),
    INTERVIEW_TYPE_INVALID("recruitment.interview.type.invalid", HttpStatus.BAD_REQUEST,
            "Loại phỏng vấn không hợp lệ"),
    INTERVIEW_STATUS_INVALID("recruitment.interview.status.invalid", HttpStatus.CONFLICT,
            "Buổi phỏng vấn không ở trạng thái có thể hoàn tất"),

    OFFER_NOT_FOUND("recruitment.offer.not.found", HttpStatus.NOT_FOUND,
            "Offer không tồn tại"),
    OFFER_STATUS_INVALID("recruitment.offer.status.invalid", HttpStatus.CONFLICT,
            "Offer không ở trạng thái cho phép thao tác này"),

    /** LNK-06 policy A: thiếu username/password khi hire. */
    HIRE_USER_REQUIRED("recruitment.hire.user.required", HttpStatus.BAD_REQUEST,
            "Hire bắt buộc tạo User — gửi username + password (+ roleCode)"),
    /** LNK-06 policy A: thiếu role. */
    HIRE_ROLE_REQUIRED("recruitment.hire.role.required", HttpStatus.BAD_REQUEST,
            "Hire bắt buộc gán Role — gửi roleCode (vd. STAFF)");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
