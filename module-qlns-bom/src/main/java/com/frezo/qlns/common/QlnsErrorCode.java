package com.frezo.qlns.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error code tập trung cho module {@code qlns} (nhân sự / lương / chấm công / HĐ).
 * Giữ nguyên i18n key cũ khi đã có trong bundle; message tiếng Việt thuần dùng làm key + defaultMessage.
 */
@Getter
@RequiredArgsConstructor
public enum QlnsErrorCode implements ErrorCode {

    // -------------------- GENERIC --------------------
    CODE_EXISTS("valid.code.exists", HttpStatus.CONFLICT, "Mã đã tồn tại"),
    ENTITY_NOT_FOUND("valid.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    PERSON_NOT_FOUND("error.person.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy nhân sự"),

    // -------------------- CONTRACT --------------------
    CONTRACT_NOT_FOUND("contract.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng"),
    CANNOT_ROLE("can.not.role", HttpStatus.FORBIDDEN, "Không có quyền thực hiện"),
    CONTRACT_TEMPLATE_NOT_FOUND("qlns.contract.template.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy mẫu hợp đồng"),
    CANNOT_CREATE_VERSION("can.not.create.version", HttpStatus.BAD_REQUEST, "Không thể tạo phiên bản hợp đồng"),
    VERSION_NOT_FOUND("version.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy phiên bản"),
    CANNOT_COMPARE_VERSIONS("can.not.compare.versions", HttpStatus.BAD_REQUEST, "Không thể so sánh phiên bản"),
    CANNOT_BUILD_SNAPSHOT("can.not.build.snapshot", HttpStatus.BAD_REQUEST, "Không thể tạo snapshot"),

    // -------------------- LEAVE --------------------
    LEAVE_REQUEST_NOT_FOUND("error.leave.request.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy đơn nghỉ phép"),
    LEAVE_REQUEST_INVALID_STATUS("error.leave.request.invalid.status", HttpStatus.BAD_REQUEST, "Trạng thái đơn nghỉ không hợp lệ"),
    LEAVE_REQUEST_PERMISSION_DENIED("error.leave.request.permission.denied", HttpStatus.FORBIDDEN, "Không có quyền hủy đơn nghỉ"),
    LEAVE_NOT_FOUND("exception.leave.not_found", HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi nghỉ phép"),

    // -------------------- ATTENDANCE --------------------
    ATTENDANCE_NOT_CHECKED_IN("error.attendance.not.checked.in", HttpStatus.BAD_REQUEST, "Chưa chấm công vào"),
    ATTENDANCE_NOT_FOUND("error.attendance.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi chấm công"),
    TIMESHEET_EXPORT_FAILED("qlns.timesheet.export.failed", HttpStatus.INTERNAL_SERVER_ERROR, "Bảng chấm công xuất Excel thất bại"),

    // -------------------- PAYROLL PERIOD --------------------
    PAYROLL_PERIOD_EXISTS("qlns.payroll.period.exists", HttpStatus.CONFLICT,
            "Kỳ lương đã tồn tại cho tháng {0}/{1}"),
    PAYROLL_PERIOD_LOCKED_EDIT("qlns.payroll.period.locked.edit", HttpStatus.CONFLICT,
            "Không thể sửa kỳ lương đã khóa"),
    PAYROLL_PERIOD_ALREADY_LOCKED("qlns.payroll.period.already.locked", HttpStatus.CONFLICT,
            "Kỳ lương đã được khóa"),
    PAYROLL_PERIOD_CLOSED_UNLOCK("qlns.payroll.period.closed.unlock", HttpStatus.CONFLICT,
            "Không thể mở khóa kỳ lương đã đóng"),
    PAYROLL_PERIOD_NO_WORKFLOW_LOCK("qlns.payroll.period.no.workflow.lock", HttpStatus.BAD_REQUEST,
            "Kỳ lương chưa có workflow — bấm 'Khoá kỳ' trước để bắt đầu duyệt."),
    PAYROLL_PERIOD_REJECT_REASON_REQUIRED("qlns.payroll.period.reject.reason.required", HttpStatus.BAD_REQUEST,
            "Vui lòng nhập lý do từ chối"),
    PAYROLL_PERIOD_NO_WORKFLOW("qlns.payroll.period.no.workflow", HttpStatus.BAD_REQUEST,
            "Kỳ lương chưa có workflow"),
    PAYROLL_PERIOD_NO_PENDING_STEP("qlns.payroll.period.no.pending.step", HttpStatus.BAD_REQUEST,
            "Không có bước nào đang chờ duyệt"),
    PAYROLL_PERIOD_NOT_FOUND("qlns.payroll.period.not.found", HttpStatus.NOT_FOUND,
            "Không tìm thấy kỳ lương"),

    // -------------------- PAYROLL --------------------
    PAYROLL_NOT_FOUND("error.payroll.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy bảng lương"),
    PAYROLL_CANNOT_UPDATE("error.payroll.cannot.update", HttpStatus.CONFLICT, "Không thể cập nhật bảng lương"),
    PAYROLL_LOCKED_PERIOD("qlns.payroll.locked.period", HttpStatus.CONFLICT,
            "Không thể thay đổi bảng lương trong kỳ đã khóa"),
    PAYROLL_PAY_REQUIRES_CONFIRMED("qlns.payroll.pay.requires.confirmed", HttpStatus.BAD_REQUEST,
            "Chỉ có thể thanh toán bảng lương đã xác nhận"),
    NO_ACTIVE_CONTRACT("qlns.payroll.no.active.contract", HttpStatus.BAD_REQUEST,
            "Không thể tính lương: nhân viên chưa có hợp đồng đang hiệu lực (activated/ACTIVE). personId={0}"),
    PAYROLL_MONTH_INVALID("qlns.payroll.month.invalid", HttpStatus.BAD_REQUEST,
            "Tham số month không hợp lệ (1–12)"),
    PAYROLL_YEAR_INVALID("qlns.payroll.year.invalid", HttpStatus.BAD_REQUEST,
            "Tham số year không hợp lệ"),

    // -------------------- DEPENDENT / INSURANCE --------------------
    DEPENDENT_NOT_FOUND("qlns.dependent.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy người phụ thuộc"),
    INSURANCE_CONFIG_NOT_FOUND("qlns.insurance.config.not.found", HttpStatus.NOT_FOUND,
            "Không tìm thấy cấu hình bảo hiểm cho năm {0}"),

    // -------------------- OFFBOARDING --------------------
    RESIGNATION_NOT_FOUND("qlns.resignation.not.found", HttpStatus.NOT_FOUND,
            "Không tìm thấy đơn nghỉ việc"),
    RESIGNATION_INVALID_STATUS("qlns.resignation.invalid.status", HttpStatus.BAD_REQUEST,
            "Trạng thái đơn nghỉ việc không hợp lệ cho thao tác này"),
    RESIGNATION_PERSON_REQUIRED("qlns.resignation.person.required", HttpStatus.BAD_REQUEST,
            "Vui lòng chọn nhân viên"),
    RESIGNATION_LAST_DAY_REQUIRED("qlns.resignation.last.day.required", HttpStatus.BAD_REQUEST,
            "Vui lòng nhập ngày làm việc cuối dự kiến"),
    RESIGNATION_PERSON_INACTIVE("qlns.resignation.person.inactive", HttpStatus.BAD_REQUEST,
            "Nhân viên đã ngừng hoạt động — không thể tạo đơn nghỉ việc"),
    RESIGNATION_HANDOVER_INCOMPLETE("qlns.resignation.handover.incomplete", HttpStatus.BAD_REQUEST,
            "Vui lòng xác nhận đủ checklist bàn giao (laptop, thẻ, tài liệu)");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
