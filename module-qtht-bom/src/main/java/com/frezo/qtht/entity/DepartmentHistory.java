package com.frezo.qtht.entity;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DepartmentHistory {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", referencedColumnName = "id", nullable = false)
    private Department department;

    @Column(name = "department_id", insertable = false, updatable = false)
    private String departmentId;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType; // CREATE, UPDATE, DELETE, MERGE, etc.

    @Column(name = "field_name", length = 100)
    private String fieldName; // Tên trường thay đổi

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // Giá trị cũ

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // Giá trị mới

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason; // Lý do thay đổi

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom; // Có hiệu lực từ

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo; // Có hiệu lực đến

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

}
