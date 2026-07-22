package com.frezo.qtht.constant;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error code tập trung cho module {@code qtht} (Quản trị hệ thống).
 * Xem hướng dẫn ở {@code FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md §10}.
 * <p>
 * <b>Cách dùng:</b>
 * <pre>
 * // Cũ — magic string, rải rác literal
 * throw new QTHTException("invalid.department.entity.not.found", id);
 *
 * // Mới — enum tập trung
 * throw new AppException(QthtErrorCode.DEPARTMENT_NOT_FOUND, id);
 * </pre>
 * <p>
 * Mỗi entry giữ nguyên i18n key đang tồn tại trong {@code messages_vi.properties} để không cần đổi bundle.
 * Khi thêm error mới: thêm 1 dòng ở đây + 1 entry trong bundle.
 */
@Getter
@RequiredArgsConstructor
public enum QthtErrorCode implements ErrorCode {

    // -------------------- USER --------------------
    USER_NOT_FOUND("exception.user.not.found", HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"),
    USER_EXISTS("exception.user.exists", HttpStatus.CONFLICT, "Tài khoản đã tồn tại"),
    EMAIL_EXISTS("exception.email.exists", HttpStatus.CONFLICT, "Email đã được đăng ký"),
    AUTH_FAILED("exception.auth.failed", HttpStatus.UNAUTHORIZED, "Xác thực thất bại"),

    // -------------------- ROLE --------------------
    ROLE_NOT_FOUND("exception.role.not.found", HttpStatus.NOT_FOUND, "Vai trò không tồn tại"),
    ROLE_EXISTS("exception.role.exists", HttpStatus.CONFLICT, "Vai trò đã tồn tại"),
    ROLE_CONFLICT("exception.role.conflict", HttpStatus.CONFLICT, "Xung đột dữ liệu vai trò"),
    USER_ROLE_EXISTS("exception.userRole.exists", HttpStatus.CONFLICT, "User đã được gán vai trò này"),

    // -------------------- MENU / PERMISSION --------------------
    MENU_NOT_FOUND("exception.menu.not.found", HttpStatus.NOT_FOUND, "Menu không tồn tại"),
    MENU_CODE_EXISTS("exception.menu.code.exists", HttpStatus.CONFLICT, "Mã menu đã tồn tại"),
    DATA_ACTION_INVALID("exception.dataAction.invalid", HttpStatus.BAD_REQUEST, "Data action không hợp lệ"),

    // -------------------- PERSON --------------------
    PERSON_NOT_FOUND("invalid.person.entity.not.found", HttpStatus.NOT_FOUND, "Nhân sự không tồn tại"),
    PERSON_CODE_EXISTS("exception.person.code.exists", HttpStatus.CONFLICT, "Mã nhân sự đã tồn tại"),

    // -------------------- ORGANIZATION --------------------
    ORGANIZATION_NOT_FOUND("invalid.organization.entity.not.found", HttpStatus.NOT_FOUND, "Tổ chức không tồn tại"),
    ORGANIZATION_CODE_EXISTS("organization.code.already.exists", HttpStatus.CONFLICT, "Mã tổ chức đã tồn tại"),
    ORGANIZATION_TAX_EXISTS("organization.taxcode.already.exists", HttpStatus.CONFLICT, "Mã số thuế đã tồn tại"),
    ORGANIZATION_PARENT_NOT_FOUND("organization.parent.not.found", HttpStatus.NOT_FOUND, "Tổ chức cha không tồn tại"),
    ORGANIZATION_LEVEL_INVALID("organization.level.invalid", HttpStatus.BAD_REQUEST, "Cấp tổ chức không hợp lệ"),

    // -------------------- DEPARTMENT --------------------
    DEPARTMENT_NOT_FOUND("invalid.department.entity.not.found", HttpStatus.NOT_FOUND, "Phòng ban không tồn tại"),
    DEPARTMENT_CODE_EXISTS("department.code.already.exists", HttpStatus.CONFLICT, "Mã phòng ban đã tồn tại"),

    // -------------------- SETTING --------------------
    SETTING_NOT_FOUND("setting.not.found", HttpStatus.NOT_FOUND, "Cấu hình không tồn tại"),
    SETTING_ORG_ALREADY_EXISTS("setting.org.already.exists", HttpStatus.CONFLICT, "Cấu hình cho tổ chức đã tồn tại"),

    // -------------------- IP TRUST / WHITELIST --------------------
    IP_NAME_ALREADY_USED("ip.name.is.used", HttpStatus.CONFLICT, "Tên IP đã được sử dụng"),
    IP_NUMBER_ALREADY_USED("ip.number.is.used", HttpStatus.CONFLICT, "Số IP đã được sử dụng"),
    IP_WHITELIST_EXISTS("ip.whitelist.exists", HttpStatus.CONFLICT, "IP đã tồn tại trong danh sách whitelist"),

    // -------------------- GENERIC (module-scoped) --------------------
    ENTITY_NOT_FOUND("valid.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
