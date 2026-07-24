package com.frezo.common.workflow;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes cho workflow engine (module-common).
 * Giữ nguyên i18n key đang dùng trong {@code messages*.properties}.
 */
@Getter
@RequiredArgsConstructor
public enum WorkflowErrorCode implements ErrorCode {

    DEFINITION_NOT_FOUND("error.workflow.definition.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy định nghĩa workflow"),
    DEFINITION_NAME_REQUIRED("error.workflow.definition.name.required", HttpStatus.BAD_REQUEST, "Tên định nghĩa bắt buộc"),
    DEFINITION_CODE_REQUIRED("error.workflow.definition.code.required", HttpStatus.BAD_REQUEST, "Mã định nghĩa bắt buộc"),
    DEFINITION_MODULE_REQUIRED("error.workflow.definition.module.required", HttpStatus.BAD_REQUEST, "Module bắt buộc"),
    DEFINITION_CODE_DUPLICATE("error.workflow.definition.code.duplicate", HttpStatus.CONFLICT, "Mã định nghĩa đã tồn tại"),
    DEFINITION_INACTIVE("error.workflow.definition.inactive", HttpStatus.BAD_REQUEST, "Định nghĩa workflow đang tắt"),
    DEFINITION_NO_STEPS("error.workflow.definition.no.steps", HttpStatus.BAD_REQUEST, "Định nghĩa chưa có bước duyệt"),

    INSTANCE_NOT_FOUND("error.workflow.instance.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy instance workflow"),
    INSTANCE_NOT_RUNNING("error.workflow.instance.not.running", HttpStatus.BAD_REQUEST, "Instance không đang chạy"),
    INSTANCE_CANCEL_FORBIDDEN("error.workflow.instance.cancel.forbidden", HttpStatus.FORBIDDEN, "Không được huỷ instance này"),

    TASK_NOT_FOUND("error.workflow.task.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy task workflow"),
    TASK_NOT_PENDING("error.workflow.task.not.pending", HttpStatus.BAD_REQUEST, "Task không ở trạng thái chờ duyệt"),
    TASK_REJECT_REASON_REQUIRED("error.workflow.task.reject.reason.required", HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do từ chối"),
    TASK_FORBIDDEN("error.workflow.task.forbidden", HttpStatus.FORBIDDEN, "Không có quyền xử lý task này"),

    STEP_APPROVER_TYPE_INVALID("error.workflow.step.approver.type.invalid", HttpStatus.BAD_REQUEST, "Loại người duyệt không hợp lệ"),

    TEMPLATE_NOT_FOUND("error.workflow.template.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy template workflow"),
    TEMPLATE_READ_ONLY("error.workflow.template.read.only", HttpStatus.FORBIDDEN, "Template chỉ đọc, không được sửa"),
    GRAPH_REQUIRED("error.workflow.graph.required", HttpStatus.BAD_REQUEST, "Graph workflow bắt buộc"),
    GRAPH_INVALID("error.workflow.graph.invalid", HttpStatus.BAD_REQUEST, "Graph workflow không hợp lệ");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
