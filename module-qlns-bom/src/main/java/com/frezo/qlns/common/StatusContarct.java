package com.frezo.qlns.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
    public enum StatusContarct {

    DRAFT("DRAFT", "Lưu nháp"),
    PENDING_APPROVAL("PENDING_APPROVAL", "Chờ phê duyệt"),
    NEGOTIATING("NEGOTIATING", "Đang đàm phán"),
    NO_YEP_EFFECTIVE("NO_YEP_EFFECTIVE", "Chưa có hiệu lực"),
    ACTIVE("ACTIVE", "Đang có hiệu lực"),
    SUSPENDED("SUSPENDED", "Tạm ngưng"),

    // Trạng thái liên quan đến luồng giao việc

    WAITING_FOR_OP("WAITING_FOR_OP", "Chờ xử lý"),           // Chờ OP xử lý
    OP_PROCESSING("OP_PROCESSING", "Đang xử lý"),            // OP đang xử lý
    WAITING_FOR_RV("WAITING_FOR_RV", "Chờ xét duyệt"),       // OP xong, chờ RV xét duyệt
    RV_REVIEWING("RV_REVIEWING", "Đang xét duyệt"),          // RV đang xét duyệt
    OP_DONE("OP_DONE", "OP đã hoàn thành"),
    RV_DONE("RV_DONE", "RV đã hoàn thành"),
    RV_REJECTED("RV_REJECTED", "Từ chối duyệt"),             // RV từ chối
    OP_REWORK("OP_REWORK", "Xử lý lại"),                     // OP làm lại theo yêu cầu
    COMPLETED("COMPLETED", "Hoàn thành"),                    // Hoàn thành cả quy trình
    CANCELLED("CANCELLED", "Đã hủy");                        // Hủy bỏ


        private final String code;
        private final String description;


}
