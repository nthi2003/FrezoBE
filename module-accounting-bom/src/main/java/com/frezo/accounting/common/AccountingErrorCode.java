package com.frezo.accounting.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountingErrorCode implements ErrorCode {

    ACCOUNT_NOT_FOUND("accounting.account.not_found", HttpStatus.NOT_FOUND,
            "Tài khoản kế toán không tồn tại: {0}"),
    ACCOUNT_CODE_EXISTS("accounting.account.code_exists", HttpStatus.CONFLICT, "Số hiệu tài khoản đã tồn tại"),
    ACCOUNT_NOT_POSTABLE("accounting.account.not_postable", HttpStatus.BAD_REQUEST,
            "Tài khoản {0} không cho phép ghi trực tiếp (chỉ dùng TK lá / postable)"),
    ACCOUNT_REQUIRES_PARTNER("accounting.account.requires_partner", HttpStatus.BAD_REQUEST,
            "Tài khoản {0} bắt buộc phải khai đối tượng chi tiết"),

    PERIOD_NOT_FOUND("accounting.period.not_found", HttpStatus.NOT_FOUND, "Không tìm thấy kỳ kế toán"),
    PERIOD_CLOSED("accounting.period.closed", HttpStatus.CONFLICT, "Kỳ kế toán đã bị khóa, không thể ghi sổ"),
    PERIOD_ALREADY_CLOSED("accounting.period.already_closed", HttpStatus.CONFLICT, "Kỳ kế toán đã ở trạng thái đóng"),
    PERIOD_LOCKED("accounting.period.locked", HttpStatus.CONFLICT,
            "Kỳ kế toán đã khóa cứng (LOCKED) — không thể đóng/mở lại từ UI"),

    JOURNAL_NOT_FOUND("accounting.journal.not_found", HttpStatus.NOT_FOUND, "Chứng từ không tồn tại"),
    JOURNAL_UNBALANCED("accounting.journal.unbalanced", HttpStatus.BAD_REQUEST, "Tổng phát sinh Nợ khác Có"),
    JOURNAL_EMPTY_LINES("accounting.journal.empty_lines", HttpStatus.BAD_REQUEST, "Chứng từ phải có ít nhất 2 dòng bút toán"),
    JOURNAL_LINE_INVALID("accounting.journal.line_invalid", HttpStatus.BAD_REQUEST, "Dòng bút toán không hợp lệ: một dòng phải là Nợ hoặc Có (không được cả hai)"),
    JOURNAL_ALREADY_POSTED("accounting.journal.already_posted", HttpStatus.CONFLICT, "Chứng từ đã ghi sổ, không thể sửa"),
    JOURNAL_ALREADY_REVERSED("accounting.journal.already_reversed", HttpStatus.CONFLICT, "Chứng từ đã bị đảo trước đó"),

    FISCAL_YEAR_NOT_FOUND("accounting.fiscal_year.not_found", HttpStatus.NOT_FOUND, "Năm tài chính không tồn tại"),
    FISCAL_YEAR_EXISTS("accounting.fiscal_year.exists", HttpStatus.CONFLICT, "Năm tài chính đã tồn tại"),

    SETTING_NOT_INITIALIZED("accounting.setting.not_initialized", HttpStatus.PRECONDITION_FAILED,
            "Chưa khởi tạo cấu hình kế toán. Vui lòng vào Cài đặt kế toán để chọn chuẩn TT133/TT99 và seed COA."),

    PAYROLL_ALREADY_POSTED("accounting.payroll.already_posted", HttpStatus.CONFLICT,
            "Bảng lương kỳ này đã được hạch toán trước đó"),
    PAYROLL_NOT_APPROVED("accounting.payroll.not_approved", HttpStatus.BAD_REQUEST,
            "Bảng lương chưa được duyệt, không thể hạch toán");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
